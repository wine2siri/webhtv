package com.fongmi.android.tv.player.autoskip;

import androidx.media3.common.MediaEdition;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class AutoSkipChapterPolicy {

    private static final long MIN_MEDIA_MS = 5 * 60_000L;
    private static final long MIN_BOUNDARY_MS = 15_000L;
    private static final long MAX_OPENING_MS = 10 * 60_000L;
    private static final long MAX_ENDING_MS = 20 * 60_000L;
    private static final Pattern OPENING = Pattern.compile("(?iu)(^|[^\\p{L}\\p{N}])(op\\d*|opening|intro|opening credits|片头|片頭|片头曲|片頭曲|开场|開場|主题曲|主題曲)($|[^\\p{L}\\p{N}])");
    private static final Pattern ENDING = Pattern.compile("(?iu)(^|[^\\p{L}\\p{N}])(ed\\d*|ending|outro|credits|end credits|片尾|片尾曲|片尾曲|职员表|職員表)($|[^\\p{L}\\p{N}])");

    private AutoSkipChapterPolicy() {
    }

    public static Proposal detect(List<MediaEdition> source, long durationMs, boolean openingAllowed, boolean endingAllowed) {
        if (source == null || source.isEmpty() || durationMs < MIN_MEDIA_MS) return Proposal.EMPTY;
        List<Chapter> chapters = new ArrayList<>();
        for (MediaEdition edition : source) {
            if (edition == null) continue;
            long positionMs = Math.max(0, edition.durationUs / 1000L);
            if (positionMs >= durationMs) continue;
            chapters.add(new Chapter(positionMs, safe(edition.label)));
        }
        chapters.sort(Comparator.comparingLong(item -> item.positionMs));
        if (chapters.isEmpty()) return Proposal.EMPTY;

        long openingMs = 0;
        long endingMs = 0;
        String openingLabel = "";
        String endingLabel = "";
        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            if (openingAllowed && openingMs == 0 && isOpening(chapter.label) && i + 1 < chapters.size()) {
                long boundary = chapters.get(i + 1).positionMs;
                if (chapter.positionMs <= MAX_OPENING_MS && boundary >= MIN_BOUNDARY_MS
                        && boundary <= MAX_OPENING_MS && boundary <= durationMs * 35 / 100) {
                    openingMs = boundary;
                    openingLabel = chapter.label;
                }
            }
            if (endingAllowed && endingMs == 0 && isEnding(chapter.label)) {
                long remaining = durationMs - chapter.positionMs;
                if (chapter.positionMs >= durationMs * 55 / 100 && remaining >= MIN_BOUNDARY_MS && remaining <= MAX_ENDING_MS) {
                    endingMs = remaining;
                    endingLabel = chapter.label;
                }
            }
        }
        if (openingMs == 0 && endingMs == 0) return Proposal.EMPTY;
        return new Proposal(openingMs, endingMs, openingLabel, endingLabel, fingerprint(chapters, durationMs));
    }

    private static boolean isOpening(String label) {
        return OPENING.matcher(" " + safe(label).toLowerCase(Locale.ROOT) + " ").find();
    }

    private static boolean isEnding(String label) {
        return ENDING.matcher(" " + safe(label).toLowerCase(Locale.ROOT) + " ").find();
    }

    private static String fingerprint(List<Chapter> chapters, long durationMs) {
        StringBuilder value = new StringBuilder().append(durationMs / 1000L);
        for (Chapter chapter : chapters) value.append('|').append(chapter.positionMs / 1000L).append(':').append(chapter.label);
        return digest(value.toString());
    }

    static String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(safe(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : bytes) result.append(String.format(Locale.ROOT, "%02x", item));
            return result.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(safe(value).hashCode());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record Chapter(long positionMs, String label) {
    }

    public static final class Proposal extends AutoSkipRule {

        public static final Proposal EMPTY = new Proposal(0, 0, "", "", "");

        private final String openingLabel;
        private final String endingLabel;
        private final String fingerprint;

        private Proposal(long openingMs, long endingMs, String openingLabel, String endingLabel, String fingerprint) {
            super(openingMs, endingMs);
            this.openingLabel = openingLabel;
            this.endingLabel = endingLabel;
            this.fingerprint = fingerprint;
        }

        public String getOpeningLabel() {
            return openingLabel;
        }

        public String getEndingLabel() {
            return endingLabel;
        }

        public String getFingerprint() {
            return fingerprint;
        }
    }
}
