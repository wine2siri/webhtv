package com.fongmi.android.tv.api;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Final client-side safety net for cards returned by every configured site.
 *
 * <p>Source metadata is inconsistent, so this intentionally examines every useful text field.
 * The server-side navigation filter remains richer because it can also use TMDb genre IDs.</p>
 */
public final class ContentSafetyFilter {

    private static final String[] BLOCKED_TERMS = {
            "色情", "成人", "情色", "伦理片", "成人电影", "18禁", "18+", "r18", "nsfw",
            "erotic", "porn", "hentai", "uncensored", "无码", "有码", "换妻", "乱伦",
            "群交", "口交", "调教", "凌辱", "痴汉", "肉欲",
            "宝宝", "儿歌", "幼儿", "早教", "启蒙", "绘本", "托班", "幼儿园", "儿童",
            "亲子", "少儿", "小猪佩奇", "熊出没", "喜羊羊", "灰太狼", "汪汪队",
            "超级飞侠", "天线宝宝", "花园宝宝", "米奇妙妙", "宝宝巴士", "kids",
            "preschool", "nursery", "toddler",
            "lgbt", "lgbtq", "同性恋", "同志电影", "同志剧", "男同", "女同", "酷儿",
            "queer", "gay movie", "gay series", "lesbian", "bl剧", "gl剧"
    };

    private ContentSafetyFilter() {
    }

    @NonNull
    public static Result apply(@NonNull Result result) {
        List<Vod> source = result.getList();
        if (source.isEmpty()) return result;
        List<Vod> safe = new ArrayList<>(source.size());
        for (Vod vod : source) {
            if (!isBlocked(vod.getName(), vod.getTypeName(), vod.getRemarks(), vod.getContent())) {
                safe.add(vod);
            }
        }
        if (safe.size() != source.size()) result.setList(safe);
        return result;
    }

    static boolean isBlocked(String... values) {
        StringBuilder text = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isEmpty()) text.append(' ').append(value);
        }
        String normalized = text.toString().toLowerCase(Locale.ROOT);
        for (String term : BLOCKED_TERMS) {
            if (normalized.contains(term)) return true;
        }
        return false;
    }
}
