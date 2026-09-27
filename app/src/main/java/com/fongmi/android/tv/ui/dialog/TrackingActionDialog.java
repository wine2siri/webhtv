package com.fongmi.android.tv.ui.dialog;

import android.util.Base64;

import androidx.fragment.app.FragmentActivity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Native tracking controls. Resource subscription remains in WishlistActionDialog. */
public final class TrackingActionDialog {

    public static final String PREFIX = "injoy-follow:";

    public interface Listener {
        void onAction(String action);
    }

    private TrackingActionDialog() {
    }

    public static boolean show(FragmentActivity activity, String action, Listener listener) {
        if (action == null || !action.startsWith(PREFIX + "menu:")) return false;
        String token = action.substring((PREFIX + "menu:").length());
        Payload payload = Payload.parse(token);
        if (payload == null || payload.name.isEmpty()) return false;

        List<String> labels = new ArrayList<>();
        List<String> operations = new ArrayList<>();
        labels.add(payload.followed ? "取消追更（不影响资源订阅）" : "加入追更");
        operations.add(payload.followed ? "unfollow" : "follow");
        if (payload.followed) {
            labels.add(payload.pinned ? "取消置顶" : "置顶到追更中心");
            operations.add(payload.pinned ? "unpin" : "pin");
        }
        if ("tv".equals(payload.mediaType) || "anime".equals(payload.mediaType)) {
            boolean desc = "desc".equals(payload.displayOrder);
            labels.add(desc ? "恢复顺序展示" : "锁定倒序展示");
            operations.add(desc ? "order-normal" : "order-desc");
        }

        String state = payload.followed ? "正在追更" : "尚未追更";
        ChoiceDialog.showSingle(
                activity.getSupportFragmentManager(),
                payload.name + "\n" + state + "；连播始终按集数递增",
                labels.toArray(new String[0]),
                -1,
                which -> {
                    String operation = operations.get(which);
                    if ("unfollow".equals(operation)) {
                        ChoiceDialog.showConfirm(
                                activity.getSupportFragmentManager(),
                                "确认取消追更",
                                "从追更中心移除《" + payload.name + "》？\n\n不会取消订阅，也不会删除网盘文件。",
                                "确认移除",
                                () -> listener.onAction(PREFIX + operation + ":" + token));
                    } else {
                        listener.onAction(PREFIX + operation + ":" + token);
                    }
                });
        return true;
    }

    private static final class Payload {
        String name = "";
        String mediaType = "";
        String displayOrder = "asc";
        boolean followed;
        boolean pinned;

        static Payload parse(String token) {
            try {
                byte[] bytes = Base64.decode(token, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
                JsonObject object = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                Payload payload = new Payload();
                payload.name = text(object, "name");
                payload.mediaType = text(object, "media_type");
                payload.displayOrder = text(object, "episode_display_order");
                payload.followed = object.has("followed") && object.get("followed").getAsBoolean();
                payload.pinned = object.has("pinned") && object.get("pinned").getAsBoolean();
                return payload;
            } catch (Exception ignored) {
                return null;
            }
        }

        private static String text(JsonObject object, String key) {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
        }
    }
}
