package com.fongmi.android.tv.drive;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.utils.Notify;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class DriveCookieSync {

    public interface Callback {
        void onResult(boolean success, String message);
    }

    public static final String[] TYPES = {"quark", "uc", "baidu"};
    public static final String[] LABELS = {"夸克网盘", "UC 网盘", "百度网盘"};

    private static final String PREFS = "drive_cookie_sync";
    private static final String KEY_LAST_SYNC = "last_sync";
    private static final String KEY_COUNT = "count";
    private static final String KEY_EXPIRED = "expired";
    private static final String KEY_EXPIRED_NOTICE = "expired_notice";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final AtomicBoolean STARTED = new AtomicBoolean();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "drive-cookie-sync");
        thread.setDaemon(true);
        return thread;
    });
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();
    private static final Map<String, String> LOGIN_URLS = new LinkedHashMap<>();

    static {
        LOGIN_URLS.put("quark", "https://pan.quark.cn/");
        LOGIN_URLS.put("uc", "https://drive.uc.cn/");
        LOGIN_URLS.put("baidu", "https://pan.baidu.com/");
    }

    private DriveCookieSync() {
    }

    public static void start() {
        if (!STARTED.compareAndSet(false, true)) return;
        EXECUTOR.scheduleWithFixedDelay(DriveCookieSync::pullSafely, 0, 6, TimeUnit.HOURS);
    }

    public static void syncNow() {
        EXECUTOR.execute(DriveCookieSync::pullSafely);
    }

    public static String loginUrl(String type) {
        return LOGIN_URLS.getOrDefault(type, LOGIN_URLS.get("quark"));
    }

    public static boolean isAllowedLoginUrl(String type, String url) {
        if (empty(url)) return false;
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) return false;
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            String suffix = switch (type) {
                case "uc" -> "uc.cn";
                case "baidu" -> "baidu.com";
                default -> "quark.cn";
            };
            return host.equals(suffix) || host.endsWith("." + suffix);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String hubBase(String configUrl) {
        if (empty(configUrl)) return "";
        try {
            URI source = URI.create(configUrl.trim());
            String scheme = source.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || source.getHost() == null) return "";
            return new URI(scheme.toLowerCase(Locale.ROOT), null, source.getHost(), source.getPort(), null, null, null).toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    public static String mergeCookies(String... values) {
        Map<String, String> pairs = new LinkedHashMap<>();
        for (String value : values) {
            if (empty(value)) continue;
            for (String token : value.split(";\\s*")) {
                int index = token.indexOf('=');
                if (index <= 0) continue;
                String name = token.substring(0, index).trim();
                String content = token.substring(index + 1).trim();
                if (!name.isEmpty() && !content.isEmpty()) pairs.put(name, content);
            }
        }
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : pairs.entrySet()) {
            if (result.length() > 0) result.append("; ");
            result.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return result.toString();
    }

    public static void upload(String type, String cookie, Callback callback) {
        String base = hubBase(VodConfig.getUrl());
        if (empty(base)) {
            post(callback, false, "当前点播配置不是有效的 HTTP 地址");
            return;
        }
        if (!LOGIN_URLS.containsKey(type) || empty(cookie)) {
            post(callback, false, "尚未检测到登录 Cookie");
            return;
        }
        EXECUTOR.execute(() -> {
            JsonObject json = new JsonObject();
            json.addProperty("cookie", cookie);
            json.addProperty("sync", false);
            Request request = new Request.Builder()
                    .url(base + "/api/cookie_hub/drives/" + type)
                    .post(RequestBody.create(json.toString(), JSON))
                    .header("Cache-Control", "no-store")
                    .build();
            try (Response response = CLIENT.newCall(request).execute()) {
                post(callback, response.isSuccessful(), response.isSuccessful() ? "登录凭据已安全同步" : "同步失败（HTTP " + response.code() + "）");
            } catch (Exception e) {
                post(callback, false, "同步失败，请检查 Moonbox 连接");
            }
        });
    }

    public static String summary(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int count = prefs.getInt(KEY_COUNT, 0);
        long timestamp = prefs.getLong(KEY_LAST_SYNC, 0);
        if (timestamp == 0) return "尚未同步";
        if (prefs.getInt(KEY_EXPIRED, 0) > 0) return "凭据已过期，请重新登录";
        return "已同步 " + count + " 个 · " + new java.text.SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(timestamp);
    }

    private static void pullSafely() {
        String base = hubBase(VodConfig.getUrl());
        if (empty(base)) return;
        Request request = new Request.Builder()
                .url(base + "/api/cookie_hub/tv-sync")
                .header("Cache-Control", "no-store")
                .build();
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return;
            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonObject drives = root.has("drives") ? root.getAsJsonObject("drives") : new JsonObject();
            int count = 0;
            int expired = 0;
            for (String type : TYPES) {
                JsonElement element = drives.get(type);
                if (element == null || !element.isJsonObject()) continue;
                String cookie = string(element.getAsJsonObject(), "credential");
                if (empty(cookie)) continue;
                if ("expired".equals(string(element.getAsJsonObject(), "status"))) expired++;
                applyCookie(type, cookie);
                count++;
            }
            SharedPreferences prefs = App.get().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            prefs.edit()
                    .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                    .putInt(KEY_COUNT, count)
                    .putInt(KEY_EXPIRED, expired)
                    .apply();
            long lastNotice = prefs.getLong(KEY_EXPIRED_NOTICE, 0);
            if (expired > 0 && System.currentTimeMillis() - lastNotice > TimeUnit.HOURS.toMillis(24)) {
                prefs.edit().putLong(KEY_EXPIRED_NOTICE, System.currentTimeMillis()).apply();
                App.post(() -> Notify.show("网盘登录凭据已过期，请在增强设置中重新登录"));
            }
        } catch (Exception ignored) {
            // 凭据绝不进入日志；网络失败等待下一次周期同步。
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void applyCookie(String type, String cookie) {
        CookieManager manager = CookieManager.getInstance();
        manager.setAcceptCookie(true);
        Set<String> pairs = new LinkedHashSet<>();
        for (String token : cookie.split(";\\s*")) {
            int index = token.indexOf('=');
            if (index > 0) pairs.add(token.substring(0, index).trim() + "=" + token.substring(index + 1).trim());
        }
        String url = loginUrl(type);
        for (String pair : pairs) manager.setCookie(url, pair + "; Path=/; Secure");
        manager.flush();
    }

    private static void post(Callback callback, boolean success, String message) {
        if (callback != null) App.post(() -> callback.onResult(success, message));
    }
}
