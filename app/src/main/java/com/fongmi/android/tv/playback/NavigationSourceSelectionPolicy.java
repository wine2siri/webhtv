package com.fongmi.android.tv.playback;

import java.util.Locale;

/** Source-selection policy for content-aggregating navigation sites. */
public final class NavigationSourceSelectionPolicy {

    private NavigationSourceSelectionPolicy() {
    }

    public static boolean preferRankedFirst(String siteKey, String siteApi) {
        String key = safe(siteKey).toLowerCase(Locale.ROOT);
        String api = safe(siteApi).toLowerCase(Locale.ROOT);
        return key.startsWith("影卓追剧-nav") || api.contains("/douban_nav.py");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
