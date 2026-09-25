package com.fongmi.android.tv.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.fongmi.android.tv.R;

/** A short theme-aware brand reveal layered over the home screen while it renders. */
public final class LauncherThemeTransition {

    private LauncherThemeTransition() {
    }

    public static void show(Activity activity, boolean freshLaunch) {
        if (!freshLaunch || activity.isFinishing()) return;
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        LauncherThemeManager.Theme theme = LauncherThemeManager.current();
        FrameLayout overlay = new FrameLayout(activity);
        Drawable background = AppCompatResources.getDrawable(activity, theme.getBackgroundRes());
        overlay.setBackground(background);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

        LinearLayout brand = new LinearLayout(activity);
        brand.setGravity(Gravity.CENTER);
        brand.setOrientation(LinearLayout.VERTICAL);

        ImageView icon = new ImageView(activity);
        icon.setImageResource(theme.getIconRes());
        boolean mobile = activity.getResources().getBoolean(R.bool.launcher_theme_mobile);
        int iconSize = dp(activity, mobile ? 132 : 190);
        brand.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        TextView name = new TextView(activity);
        name.setText(R.string.app_name);
        name.setTextColor(Color.WHITE);
        name.setTextSize(mobile ? 23 : 27);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        name.setGravity(Gravity.CENTER);
        name.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(activity, 14);
        brand.addView(name, nameParams);

        FrameLayout.LayoutParams brandParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        overlay.addView(brand, brandParams);
        decor.addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        brand.setAlpha(0f);
        brand.setScaleX(0.88f);
        brand.setScaleY(0.88f);
        brand.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(360).setInterpolator(new DecelerateInterpolator()).start();
        overlay.postDelayed(() -> overlay.animate().alpha(0f).setDuration(260).withEndAction(() -> decor.removeView(overlay)).start(), 650);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
