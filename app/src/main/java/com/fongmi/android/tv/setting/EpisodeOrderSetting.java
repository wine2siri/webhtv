package com.fongmi.android.tv.setting;

import com.github.catvod.utils.Prefers;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.Normalizer;
import java.util.Locale;

/** Per-title episode order shared by every source that exposes the same content. */
public final class EpisodeOrderSetting {

    private static final String KEY = "episode_order_by_title";

    private EpisodeOrderSetting() {
    }

    public static Boolean get(String title) {
        String identity = normalize(title);
        if (identity.isEmpty()) return null;
        JsonObject values = load();
        return values.has(identity) ? values.get(identity).getAsBoolean() : null;
    }

    public static void put(String title, boolean descending) {
        String identity = normalize(title);
        if (identity.isEmpty()) return;
        JsonObject values = load();
        values.addProperty(identity, descending);
        Prefers.put(KEY, values.toString());
    }

    static String normalize(String title) {
        String value = Normalizer.normalize(title == null ? "" : title, Normalizer.Form.NFKC);
        return value.toLowerCase(Locale.ROOT).replaceAll("[\\p{P}\\p{S}\\s]+", "");
    }

    private static JsonObject load() {
        try {
            String raw = Prefers.getString(KEY, "{}");
            return JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }
}
