package com.fongmi.android.tv.api.loader;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.chaquo.Loader;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PyLoader {

    private final ConcurrentHashMap<String, Spider> spiders;
    private final Loader loader;
    private volatile String recent;

    public PyLoader() {
        spiders = new ConcurrentHashMap<>();
        loader = new Loader();
    }

    public void clear() {
        spiders.values().forEach(Spider::destroy);
        spiders.clear();
        recent = null;
    }

    public void setRecent(String recent) {
        this.recent = recent;
    }

    public Spider getSpider(String key, String api, String ext) {
        return getOrCreate(spiders, key, () -> {
            Spider spider = loader.spider(api);
            spider.siteKey = key;
            spider.init(App.get(), normalizeExt(ext));
            return spider;
        });
    }

    static Spider getOrCreate(ConcurrentHashMap<String, Spider> spiders, String key, SpiderProvider provider) {
        Spider cached = spiders.get(key);
        if (cached != null) return cached;
        Spider created = null;
        try {
            created = provider.create();
            Spider winner = spiders.putIfAbsent(key, created);
            if (winner == null) return created;
            created.destroy();
            return winner;
        } catch (Throwable e) {
            e.printStackTrace();
            if (created != null) created.destroy();
            // A canceled or transient first download must not poison this site key
            // for the lifetime of the process. The next request should retry.
            return new SpiderNull();
        }
    }

    interface SpiderProvider {
        Spider create() throws Throwable;
    }

    private String normalizeExt(String ext) {
        String value = TextUtils.isEmpty(ext) ? "" : ext.trim();
        // Many live Python spiders treat ext as an option object and call .get().
        // TV-style configs commonly express an empty extension as [], which would
        // otherwise deserialize to a list and crash those spiders during init.
        return "[]".equals(value) ? "{}" : ext;
    }

    public Object[] proxy(Map<String, String> params) throws Exception {
        if (recent == null) return null;
        Spider spider = spiders.get(recent);
        return spider != null ? spider.proxy(params) : null;
    }
}
