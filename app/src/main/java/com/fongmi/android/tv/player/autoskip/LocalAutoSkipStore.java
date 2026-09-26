package com.fongmi.android.tv.player.autoskip;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.github.catvod.utils.Prefers;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class LocalAutoSkipStore {

    private static final String KEY = "local_auto_skip_rules_v1";
    private static final int MAX_ENTRIES = 240;
    private static final long MAX_AGE_MS = TimeUnit.DAYS.toMillis(120);
    private static final Type TYPE = new TypeToken<List<Entry>>() {}.getType();

    private LocalAutoSkipStore() {
    }

    static synchronized Entry get(String mediaId) {
        if (TextUtils.isEmpty(mediaId)) return new Entry();
        long now = System.currentTimeMillis();
        List<Entry> entries = read(now);
        for (Entry entry : entries) if (mediaId.equals(entry.mediaId)) return entry.copy();
        return new Entry();
    }

    static synchronized void accept(String mediaId, AutoSkipChapterPolicy.Proposal proposal) {
        if (TextUtils.isEmpty(mediaId) || proposal == null || proposal.isEmpty()) return;
        Entry entry = findOrCreate(read(System.currentTimeMillis()), mediaId);
        if (proposal.getOpeningMs() > 0 && !entry.manualOpening) entry.openingMs = proposal.getOpeningMs();
        if (proposal.getEndingMs() > 0 && !entry.manualEnding) entry.endingMs = proposal.getEndingMs();
        entry.rejectedFingerprint = "";
        entry.updatedAt = System.currentTimeMillis();
        saveWith(entry);
    }

    static synchronized void reject(String mediaId, String fingerprint) {
        if (TextUtils.isEmpty(mediaId) || TextUtils.isEmpty(fingerprint)) return;
        Entry entry = findOrCreate(read(System.currentTimeMillis()), mediaId);
        entry.rejectedFingerprint = fingerprint;
        entry.updatedAt = System.currentTimeMillis();
        saveWith(entry);
    }

    static synchronized void markManual(String mediaId, boolean opening) {
        if (TextUtils.isEmpty(mediaId)) return;
        Entry entry = findOrCreate(read(System.currentTimeMillis()), mediaId);
        if (opening) {
            entry.manualOpening = true;
            entry.openingMs = 0;
        } else {
            entry.manualEnding = true;
            entry.endingMs = 0;
        }
        entry.updatedAt = System.currentTimeMillis();
        saveWith(entry);
    }

    private static Entry findOrCreate(List<Entry> entries, String mediaId) {
        for (Entry entry : entries) if (mediaId.equals(entry.mediaId)) return entry;
        Entry entry = new Entry();
        entry.mediaId = mediaId;
        entries.add(entry);
        return entry;
    }

    private static void saveWith(Entry updated) {
        List<Entry> entries = read(System.currentTimeMillis());
        entries.removeIf(item -> TextUtils.equals(item.mediaId, updated.mediaId));
        entries.add(updated);
        entries.sort(Comparator.comparingLong((Entry item) -> item.updatedAt).reversed());
        if (entries.size() > MAX_ENTRIES) entries = new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        Prefers.put(KEY, App.gson().toJson(entries));
    }

    private static List<Entry> read(long now) {
        try {
            List<Entry> entries = App.gson().fromJson(Prefers.getString(KEY, "[]"), TYPE);
            if (entries == null) return new ArrayList<>();
            entries.removeIf(item -> item == null || TextUtils.isEmpty(item.mediaId)
                    || item.updatedAt <= 0 || now - item.updatedAt > MAX_AGE_MS);
            return entries;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    static final class Entry {
        String mediaId = "";
        long openingMs;
        long endingMs;
        long updatedAt;
        boolean manualOpening;
        boolean manualEnding;
        String rejectedFingerprint = "";

        Entry copy() {
            Entry copy = new Entry();
            copy.mediaId = mediaId;
            copy.openingMs = openingMs;
            copy.endingMs = endingMs;
            copy.updatedAt = updatedAt;
            copy.manualOpening = manualOpening;
            copy.manualEnding = manualEnding;
            copy.rejectedFingerprint = rejectedFingerprint;
            return copy;
        }
    }
}
