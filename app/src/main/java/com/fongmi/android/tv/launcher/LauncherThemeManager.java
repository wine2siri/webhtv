package com.fongmi.android.tv.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import com.fongmi.android.tv.R;
import com.github.catvod.utils.Prefers;

public final class LauncherThemeManager {

    private static final String PREF_KEY = "launcher_theme";

    public enum Theme {
        H3("h3", "H3", R.string.launcher_theme_h3, R.drawable.launcher_theme_h3_background, R.mipmap.ic_launcher_theme_h3),
        WOOD("wood", "Wood", R.string.launcher_theme_wood, R.drawable.launcher_theme_wood_background, R.mipmap.ic_launcher_theme_wood),
        GLASS("glass", "Glass", R.string.launcher_theme_glass, R.drawable.launcher_theme_glass_background, R.mipmap.ic_launcher_theme_glass),
        LOTUS("lotus", "Lotus", R.string.launcher_theme_lotus, R.drawable.launcher_theme_lotus_background, R.mipmap.ic_launcher_theme_lotus),
        MARBLE("marble", "Marble", R.string.launcher_theme_marble, R.drawable.launcher_theme_marble_background, R.mipmap.ic_launcher_theme_marble),
        WATER("water", "Water", R.string.launcher_theme_water, R.drawable.launcher_theme_water_background, R.mipmap.ic_launcher_theme_water);

        private final String id;
        private final String alias;
        private final int labelRes;
        private final int backgroundRes;
        private final int iconRes;

        Theme(String id, String alias, int labelRes, int backgroundRes, int iconRes) {
            this.id = id;
            this.alias = alias;
            this.labelRes = labelRes;
            this.backgroundRes = backgroundRes;
            this.iconRes = iconRes;
        }

        public int getBackgroundRes() {
            return backgroundRes;
        }

        public int getIconRes() {
            return iconRes;
        }
    }

    private LauncherThemeManager() {
    }

    public static Theme current() {
        String selected = Prefers.getString(PREF_KEY, Theme.H3.id);
        for (Theme theme : Theme.values()) if (theme.id.equals(selected)) return theme;
        return Theme.H3;
    }

    public static int currentIndex() {
        Theme current = current();
        Theme[] themes = Theme.values();
        for (int i = 0; i < themes.length; i++) if (themes[i] == current) return i;
        return 0;
    }

    public static CharSequence[] labels(Context context) {
        Theme[] themes = Theme.values();
        CharSequence[] labels = new CharSequence[themes.length];
        for (int i = 0; i < themes.length; i++) labels[i] = context.getString(themes[i].labelRes);
        return labels;
    }

    public static String currentLabel(Context context) {
        return context.getString(current().labelRes);
    }

    public static void apply(Context context, int index) {
        Theme[] themes = Theme.values();
        Theme selected = themes[Math.max(0, Math.min(index, themes.length - 1))];
        Prefers.put(PREF_KEY, selected.id);
        if (!context.getResources().getBoolean(R.bool.launcher_theme_changes_component)) return;
        PackageManager manager = context.getPackageManager();

        // Enable the destination first so an interrupted switch never leaves the app without a launcher entry.
        manager.setComponentEnabledSetting(component(context, selected), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        for (Theme theme : themes) {
            if (theme == selected) continue;
            manager.setComponentEnabledSetting(component(context, theme), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    private static ComponentName component(Context context, Theme theme) {
        return new ComponentName(context, context.getPackageName() + ".launcher." + theme.alias);
    }
}
