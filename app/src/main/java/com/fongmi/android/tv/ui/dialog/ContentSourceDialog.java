package com.fongmi.android.tv.ui.dialog;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.sidesheet.SideSheetDialog;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public final class ContentSourceDialog extends DialogFragment {

    private final List<Vod> items = new ArrayList<>();
    private OnSourceSelected listener;
    private LinearLayout list;

    public interface OnSourceSelected {
        void onSourceSelected(Vod item);
    }

    public static void show(FragmentActivity activity, Vod item, OnSourceSelected listener) {
        show(activity.getSupportFragmentManager(), item, listener);
    }

    public static void show(Fragment fragment, Vod item, OnSourceSelected listener) {
        show(fragment.getChildFragmentManager(), item, listener);
    }

    private static void show(FragmentManager manager, Vod item, OnSourceSelected listener) {
        ContentSourceDialog dialog = new ContentSourceDialog();
        dialog.items.addAll(item.getSourceItems());
        dialog.listener = listener;
        dialog.show(manager, ContentSourceDialog.class.getSimpleName());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        SideSheetDialog dialog = new SideSheetDialog(requireContext());
        dialog.getBehavior().setDraggable(false);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(20));

        MaterialTextView title = new MaterialTextView(requireContext());
        title.setText(R.string.search_choose_source);
        title.setTextColor(Color.parseColor("#202124"));
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        MaterialTextView summary = new MaterialTextView(requireContext());
        summary.setText(getString(R.string.search_source_summary, items.size()));
        summary.setTextColor(Color.parseColor("#5F6368"));
        summary.setTextSize(14);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = dp(6);
        root.addView(summary, summaryParams);

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        list = new LinearLayout(requireContext());
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for (int i = 0; i < items.size(); i++) list.addView(createSource(items.get(i), i));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = dp(20);
        root.addView(scroll, scrollParams);
        return root;
    }

    private View createSource(Vod item, int position) {
        MaterialButton button = new MaterialButton(requireContext());
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setSingleLine(false);
        button.setMinHeight(dp(56));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setText(sourceLabel(item, position));
        button.setContentDescription(getString(R.string.search_source_accessibility, position + 1, items.size(), item.getSiteName()));
        button.setFocusable(true);
        button.setFocusableInTouchMode(Util.isLeanback());
        button.setOnClickListener(view -> select(item));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        button.setLayoutParams(params);
        return button;
    }

    private CharSequence sourceLabel(Vod item, int position) {
        String name = item.getSiteName().isEmpty() ? getString(R.string.search_source_fallback, position + 1) : item.getSiteName();
        if (position == 0) name = getString(R.string.search_source_recommended, name);
        String detail = item.getRemarks();
        return detail.isEmpty() ? name : name + "\n" + detail;
    }

    private void select(Vod item) {
        dismiss();
        if (listener != null) listener.onSourceSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) return;
        int width = (int) (ResUtil.getScreenWidth(requireContext()) * (ResUtil.isLand(requireContext()) ? 0.42f : 0.9f));
        FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.m3_side_sheet);
        if (sheet != null) {
            ViewGroup.LayoutParams params = sheet.getLayoutParams();
            params.width = width;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            sheet.setLayoutParams(params);
        }
        if (list != null && list.getChildCount() > 0) list.getChildAt(0).requestFocus();
    }

    private int dp(int value) {
        return ResUtil.dp2px(value);
    }
}
