package com.videodownloader.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 10, 29);
    private static final int PANEL = Color.rgb(17, 23, 51);
    private static final int CYAN = Color.rgb(0, 220, 255);
    private static final int MAGENTA = Color.rgb(225, 40, 255);
    private EditText urlInput;
    private WebView browser;
    private TextView status;
    private ProgressBar progress;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(12), dp(16), 0);

        TextView logo = new TextView(this);
        logo.setText("▶ ↓  Video Downloader");
        logo.setTextColor(CYAN);
        logo.setTextSize(25);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logo.setGravity(android.view.Gravity.CENTER);
        root.addView(logo, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView subtitle = new TextView(this);
        subtitle.setText("Paste a direct media link or browse the web");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(13);
        subtitle.setGravity(android.view.Gravity.CENTER);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(28)));

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setTextColor(Color.WHITE);
        urlInput.setHintTextColor(Color.GRAY);
        urlInput.setHint("https://example.com/video.mp4");
        urlInput.setTextSize(14);
        urlInput.setPadding(dp(12), 0, dp(12), 0);
        urlInput.setBackgroundColor(PANEL);
        root.addView(urlInput, new LinearLayout.LayoutParams(-1, dp(50)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button open = makeButton("Open link", CYAN);
        Button download = makeButton("Download", MAGENTA);
        actions.addView(open, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0, dp(48), 1);
        dlp.leftMargin = dp(8);
        actions.addView(download, dlp);
        root.addView(actions);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        status = new TextView(this);
        status.setText("Ready. Only download content you have permission to save.");
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(12);
        status.setPadding(0, dp(8), 0, dp(8));
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(38)));

        browser = new WebView(this);
        browser.setBackgroundColor(BG);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setMediaPlaybackRequiresUserGesture(true);
        browser.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        browser.setWebViewClient(new WebViewClient());
        browser.setDownloadListener(new DownloadListener() {
            @Override public void onDownloadStart(String url, String userAgent, String contentDisposition,
                    String mimeType, long contentLength) {
                enqueueDownload(url, contentDisposition, mimeType);
            }
        });
        LinearLayout.LayoutParams webLp = new LinearLayout.LayoutParams(-1, 0, 1);
        webLp.topMargin = dp(4);
        root.addView(browser, webLp);

        // Reserved, non-intrusive banner area for a future ad SDK integration.
        TextView ad = new TextView(this);
        ad.setText("Advertisement");
        ad.setTextColor(Color.GRAY);
        ad.setTextSize(10);
        ad.setGravity(android.view.Gravity.CENTER);
        ad.setBackgroundColor(Color.rgb(12, 15, 34));
        root.addView(ad, new LinearLayout.LayoutParams(-1, dp(32)));

        setContentView(root);
        open.setOnClickListener(v -> openAddress());
        download.setOnClickListener(v -> downloadAddress());
        browser.loadUrl("https://www.google.com");
    }

    private Button makeButton(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color == CYAN ? Color.rgb(0, 105, 150) : Color.rgb(115, 28, 160)));
        return b;
    }

    private void openAddress() {
        String raw = urlInput.getText().toString().trim();
        if (raw.isEmpty()) { toast("Enter a URL first"); return; }
        if (!raw.matches("(?i)^https?://.*")) raw = "https://" + raw;
        try {
            Uri uri = Uri.parse(raw);
            if (uri.getHost() == null) throw new IllegalArgumentException();
            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(urlInput.getWindowToken(), 0);
            browser.loadUrl(uri.toString());
            status.setText("Opened: " + uri.getHost());
        } catch (Exception e) { toast("Please enter a valid web address"); }
    }

    private void downloadAddress() {
        String raw = urlInput.getText().toString().trim();
        if (raw.isEmpty()) { toast("Paste a direct downloadable file URL"); return; }
        if (!raw.matches("(?i)^https?://.*")) raw = "https://" + raw;
        enqueueDownload(raw, null, null);
    }

    private void enqueueDownload(String rawUrl, String disposition, String mimeType) {
        try {
            Uri uri = Uri.parse(rawUrl);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
                toast("Unsupported URL"); return;
            }
            String filename = android.webkit.URLUtil.guessFileName(rawUrl, disposition, mimeType);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(filename);
            request.setDescription("Downloading with Video Downloader");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            DownloadManager manager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            manager.enqueue(request);
            status.setText("Download started: " + filename);
            toast("Download started");
        } catch (Exception e) {
            status.setText("Could not start download. The site may require sign-in or block direct downloads.");
            toast("Download could not start");
        }
    }

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    @Override public void onBackPressed() {
        if (browser != null && browser.canGoBack()) browser.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (browser != null) browser.destroy();
        super.onDestroy();
    }
}
