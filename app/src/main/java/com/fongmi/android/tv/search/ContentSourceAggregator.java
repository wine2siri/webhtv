package com.fongmi.android.tv.search;

import com.fongmi.android.tv.bean.Vod;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ContentSourceAggregator {

    private static final Pattern TITLE_NOISE = Pattern.compile("[\\p{P}\\p{S}\\s]+");

    private ContentSourceAggregator() {
    }

    public static List<Vod> aggregate(List<Vod> input) {
        List<Vod> groups = new ArrayList<>();
        if (input == null) return groups;
        for (Vod item : input) add(groups, item);
        return groups;
    }

    private static void add(List<Vod> groups, Vod item) {
        if (item == null || normalizeTitle(item.getName()).isEmpty()) return;
        for (Vod group : groups) {
            if (!sameContent(group.getName(), group.getYear(), group.getTypeName(), item.getName(), item.getYear(), item.getTypeName())) continue;
            if (!containsSource(group, item)) group.addSourceItem(item);
            return;
        }
        groups.add(item.copyForSourceGroup());
    }

    private static boolean containsSource(Vod group, Vod candidate) {
        if (candidate.getId().isEmpty()) return false;
        for (Vod source : group.getSourceItems()) {
            if (source.getSiteKey().equals(candidate.getSiteKey()) && source.getId().equals(candidate.getId())) return true;
        }
        return false;
    }

    static boolean sameContent(String leftTitle, String leftYear, String leftType, String rightTitle, String rightYear, String rightType) {
        if (!normalizeTitle(leftTitle).equals(normalizeTitle(rightTitle))) return false;
        if (!compatible(leftYear, rightYear)) return false;
        String leftKind = contentKind(leftType);
        String rightKind = contentKind(rightType);
        return leftKind.isEmpty() || rightKind.isEmpty() || leftKind.equals(rightKind);
    }

    static String normalizeTitle(String value) {
        String normalized = Normalizer.normalize(safe(value), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        return TITLE_NOISE.matcher(normalized).replaceAll("");
    }

    private static boolean compatible(String left, String right) {
        String a = safe(left).trim();
        String b = safe(right).trim();
        return a.isEmpty() || b.isEmpty() || a.equals(b);
    }

    private static String contentKind(String value) {
        String type = safe(value).toLowerCase(Locale.ROOT);
        if (type.contains("电影") || type.contains("片") || type.contains("movie") || type.contains("film")) return "movie";
        if (type.contains("剧") || type.contains("series") || type.contains("tv") || type.contains("动漫") || type.contains("动画") || type.contains("综艺")) return "series";
        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
