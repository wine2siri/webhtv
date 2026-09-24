package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fongmi.android.tv.drive.DriveCookieSync;
import com.fongmi.android.tv.utils.Notify;

public class DriveCookieLoginActivity extends AppCompatActivity {

    private Spinner provider;
    private WebView webView;

    public static void start(Context context) {
        context.startActivity(new Intent(context, DriveCookieLoginActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("网盘登录与同步");
        setContentView(createContent());
        configureWebView();
        provider.setSelection(0);
        loadProvider(0);
    }

    private LinearLayout createContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        int padding = Math.round(12 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);

        provider = new Spinner(this);
        provider.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, DriveCookieSync.LABELS));
        provider.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                loadProvider(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        root.addView(provider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        webView = new WebView(this);
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        Button sync = new Button(this);
        sync.setText("登录完成并同步");
        sync.setOnClickListener(view -> upload());
        root.addView(sync, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    private void configureWebView() {
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String type = DriveCookieSync.TYPES[provider.getSelectedItemPosition()];
                if (DriveCookieSync.isAllowedLoginUrl(type, request.getUrl().toString())) return false;
                Notify.show("已阻止离开网盘官方域名");
                return true;
            }
        });
    }

    private void loadProvider(int position) {
        if (webView == null || position < 0 || position >= DriveCookieSync.TYPES.length) return;
        webView.loadUrl(DriveCookieSync.loginUrl(DriveCookieSync.TYPES[position]));
    }

    private void upload() {
        int position = provider.getSelectedItemPosition();
        String type = DriveCookieSync.TYPES[position];
        CookieManager manager = CookieManager.getInstance();
        String currentUrl = webView.getUrl();
        String cookie = DriveCookieSync.mergeCookies(
                manager.getCookie(DriveCookieSync.loginUrl(type)),
                currentUrl == null ? "" : manager.getCookie(currentUrl));
        DriveCookieSync.upload(type, cookie, (success, message) -> {
            if (isFinishing() || isDestroyed()) return;
            Notify.show(message);
            if (success) {
                DriveCookieSync.syncNow();
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
        }
        super.onDestroy();
    }
}
