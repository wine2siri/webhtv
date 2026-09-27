package com.fongmi.android.tv.ui.dialog;

import android.util.Base64;

import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class WishlistActionDialog {

    public static final String PREFIX = "injoy-wish:";

    public interface Listener {
        void onAction(String action);
    }

    private WishlistActionDialog() {
    }

    public static boolean isMenu(String action) {
        return action != null && action.startsWith(PREFIX + "menu:");
    }

    public static boolean show(FragmentActivity activity, String action, Listener listener) {
        if (!isMenu(action)) return false;
        String token = action.substring((PREFIX + "menu:").length());
        Payload payload = Payload.parse(token);
        if (payload == null || payload.name.isEmpty()) return false;

        List<String> labels = new ArrayList<>();
        List<String> operations = new ArrayList<>();
        if (!payload.exists) {
            labels.add("加入心愿并自动搜索");
            operations.add("add");
        } else {
            labels.add("刷新订阅状态");
            operations.add("status");
            labels.add("立即订阅 / 重新搜索");
            operations.add("subscribe");
            if ("tv".equals(payload.mediaType)) {
                boolean desc = "desc".equals(payload.episodeOrder);
                labels.add(desc ? "恢复顺序显示与播放" : "锁定倒序追更");
                operations.add(desc ? "order-normal" : "order-desc");
            }
            labels.add("取消订阅（保留网盘文件）");
            operations.add("cancel");
            labels.add("取关并删除网盘文件");
            operations.add("delete");
        }

        String message = payload.exists
                ? "当前状态：" + statusText(payload) + "\n请选择对当前心愿执行的操作。"
                : "当前尚未订阅。加入后会自动搜索并持续追更。";
        new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_WebHTV_LightDialog)
                .setTitle(payload.name)
                .setMessage(message)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    String operation = operations.get(which);
                    if ("cancel".equals(operation)) {
                        confirmCancel(activity, payload, token, listener);
                    } else if ("delete".equals(operation)) {
                        confirmDelete(activity, payload, token, listener);
                    } else {
                        listener.onAction(PREFIX + operation + ":" + token);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        return true;
    }

    private static void confirmCancel(FragmentActivity activity, Payload payload, String token, Listener listener) {
        new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_WebHTV_LightDialog)
                .setTitle("确认取消订阅")
                .setMessage("取消《" + payload.name + "》的订阅？\n\n已转存的网盘文件会保留。")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("确认取消", (dialog, which) -> listener.onAction(PREFIX + "cancel:" + token))
                .show();
    }

    private static void confirmDelete(FragmentActivity activity, Payload payload, String token, Listener listener) {
        new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_WebHTV_LightDialog)
                .setTitle("确认取关并删除")
                .setMessage("将取消《" + payload.name + "》的订阅，并把对应网盘目录移入回收站。\n\n此操作只允许删除与片名完全匹配的独立目录。")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("确认删除", (dialog, which) -> listener.onAction(PREFIX + "delete:" + token))
                .show();
    }

    private static String statusText(Payload payload) {
        String value = payload.subStatus.isEmpty() ? payload.status : payload.subStatus;
        return switch (value) {
            case "pending" -> "待搜索";
            case "searching" -> "搜索中";
            case "subscribed" -> "已订阅";
            case "failed", "no_resource" -> "暂未找到资源";
            case "lit" -> "已转存";
            case "not_transferred" -> "待转存";
            case "transferred_pending" -> "已转存待入库";
            case "in_library_ongoing" -> "已入库追更中";
            case "completed" -> "已完结";
            default -> value.isEmpty() ? "待处理" : value;
        };
    }

    private static final class Payload {
        String name = "";
        String mediaType = "";
        String status = "";
        String subStatus = "";
        String episodeOrder = "normal";
        boolean exists;

        static Payload parse(String token) {
            try {
                byte[] bytes = Base64.decode(token, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
                JsonObject object = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                Payload payload = new Payload();
                payload.name = text(object, "name");
                payload.mediaType = text(object, "media_type");
                payload.status = text(object, "status");
                payload.subStatus = text(object, "sub_status");
                payload.episodeOrder = text(object, "episode_order");
                payload.exists = object.has("exists") && object.get("exists").getAsBoolean();
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
