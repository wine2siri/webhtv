package com.fongmi.android.tv.sync;

import android.provider.Settings;
import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.utils.Task;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Synchronizes the APP's built-in favorites with the InJoy follow center. */
public final class InjoyFavoriteSync {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build();
    private static volatile long lastSync;

    private InjoyFavoriteSync() {
    }

    public static void syncSoon() {
        syncSoon(false);
    }

    public static void syncSoon(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastSync < 60_000L) return;
        lastSync = now;
        Task.execute(InjoyFavoriteSync::sync);
    }

    private static void sync() {
        HttpUrl endpoint = endpoint();
        if (endpoint == null) return;
        List<Keep> keeps = Keep.getVod();
        JsonObject payload = new JsonObject();
        String deviceId = Settings.Secure.getString(App.get().getContentResolver(), Settings.Secure.ANDROID_ID);
        payload.addProperty("device_id", TextUtils.isEmpty(deviceId) ? "android" : deviceId);
        JsonArray items = new JsonArray();
        for (Keep keep : keeps) items.add(toJson(keep));
        payload.add("items", items);
        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder().url(endpoint).post(body).build();
        try (Response response = CLIENT.newCall(request).execute()) {
            // Best-effort sync. Local favorites remain authoritative when the hub is offline.
        } catch (Exception ignored) {
        }
    }

    private static JsonObject toJson(Keep keep) {
        JsonObject item = new JsonObject();
        item.addProperty("name", keep.getVodName());
        item.addProperty("pic", keep.getVodPic());
        item.addProperty("site_name", keep.getSiteName());
        item.addProperty("site_key", keep.getSiteKey());
        item.addProperty("vod_id", keep.getVodId());
        item.addProperty("created_at", keep.getCreateTime());
        String[] tmdb = parseTmdb(keep.getVodId());
        item.addProperty("media_type", tmdb[0]);
        item.addProperty("tmdb_id", tmdb[1]);
        return item;
    }

    private static String[] parseTmdb(String vodId) {
        String value = vodId == null ? "" : vodId;
        String[] parts = value.split("_", 3);
        if (parts.length == 3 && "tmdb".equals(parts[0]) && ("tv".equals(parts[1]) || "movie".equals(parts[1]))) {
            return new String[]{parts[1], parts[2]};
        }
        return new String[]{"", "0"};
    }

    private static HttpUrl endpoint() {
        HttpUrl config = HttpUrl.parse(VodConfig.getUrl());
        if (config == null || !("http".equals(config.scheme()) || "https".equals(config.scheme()))) return null;
        return config.newBuilder().encodedPath("/api/app-favorites/sync").query(null).fragment(null).build();
    }
}
