
package com.google.android.youtube.pro;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.view.MotionEvent;
import android.os.SystemClock;

import com.google.android.youtube.pro.webview.YTProWebView;
import com.google.android.youtube.pro.webview.YTProWebViewClient;
import com.google.android.youtube.pro.webview.YTProWebChromeClient;
import com.google.android.youtube.pro.webview.WebAppInterface;
import com.google.android.youtube.pro.webview.BinaryStreamManager;
import com.google.android.youtube.pro.receivers.MediaCommandReceiver;

public class MainActivity extends Activity {

    public boolean portrait = false;
    public boolean isPlaying = false;
    public boolean mediaSession = false;
    public boolean isPip = false;
    public boolean dL = false;

    private YTProWebView web;
    private MediaCommandReceiver broadcastReceiver;
    private OnBackInvokedCallback backCallback;
    public BinaryStreamManager streamManager;

/**
     * Dialog sambutan CukYT KRIAN.
     * Tampilan sudah dipercantik dengan UI kustom (Rounded, Center, HTML Highlight).
     */
    private void showCukYTDialog() {
        // 1. Membuat Container Layout utama
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 70, 60, 30);
        layout.setGravity(android.view.Gravity.CENTER);

        // 2. Ikon Sambutan (Opsional, bikin makin cakep)
        android.widget.TextView iconView = new android.widget.TextView(this);
        iconView.setText("🚀");
        iconView.setTextSize(42);
        iconView.setGravity(android.view.Gravity.CENTER);
        iconView.setPadding(0, 0, 0, 15);

        // 3. Judul Aplikasi
        android.widget.TextView titleView = new android.widget.TextView(this);
        titleView.setText("CUKYT KRIAN");
        titleView.setTextSize(22);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(android.graphics.Color.parseColor("#FF0000")); // Merah YouTube
        titleView.setGravity(android.view.Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 25);

        // 4. Pesan dengan format HTML (Biar bisa bedain warna teks)
        android.widget.TextView msgView = new android.widget.TextView(this);
        String htmlText = "Selamat datang di <b>CukYT!</b><br><br>" +
                          "Aplikasi ini merupakan versi modifikasi eksklusif dari proyek gabut oleh <br>" +
                          "<font color='#00E676'><b>Andika Bagus K GANTENG!</b></font><br><br>" +
                          "Terima kasih telah menggunakan aplikasi ini.";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            msgView.setText(android.text.Html.fromHtml(htmlText, android.text.Html.FROM_HTML_MODE_COMPACT));
        } else {
            msgView.setText(android.text.Html.fromHtml(htmlText));
        }
        msgView.setTextSize(15);
        msgView.setTextColor(android.graphics.Color.parseColor("#E0E0E0")); // Abu-abu terang biar enak dibaca di dark mode
        msgView.setGravity(android.view.Gravity.CENTER);
        msgView.setLineSpacing(0, 1.3f);

        // Masukkan semua elemen ke dalam Layout
        layout.addView(iconView);
        layout.addView(titleView);
        layout.addView(msgView);

        // 5. Membangun & Menampilkan Dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton("MENGERTI", (d, which) -> d.dismiss())
                .create();

        // 6. Styling Background & Tombol setelah dialog dibuat
        dialog.setOnShowListener(d -> {
            // Membuat background pop-up melengkung (Rounded)
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(50f); // Radius kelengkungan
            shape.setColor(android.graphics.Color.parseColor("#212121")); // Warna dark grey material

            // Terapkan background ke Window dialog
            dialog.getWindow().setBackgroundDrawable(shape);

            // Styling tombol MENGERTI
            android.widget.Button posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            posBtn.setTextColor(android.graphics.Color.parseColor("#FF0000")); // Teks tombol jadi merah
            posBtn.setTypeface(null, android.graphics.Typeface.BOLD);
            posBtn.setAllCaps(true);
        });

        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Tampilkan sambutan setiap kali Activity dibuat.
        showCukYTDialog();

        SharedPreferences prefs = getSharedPreferences("YTPRO", MODE_PRIVATE);
        if (!prefs.contains("bgplay")) {
            prefs.edit().putBoolean("bgplay", true).apply();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        load(false);
    }

    public void load(boolean dl) {

        this.dL = dl;
        web = findViewById(R.id.web);

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setSupportZoom(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(web, true);
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();
        String url = "https://m.youtube.com/";

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            url = data.toString();
        } else if (Intent.ACTION_SEND.equals(action)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null &&
                    (sharedText.contains("youtube.com") ||
                     sharedText.contains("youtu.be"))) {
                url = sharedText;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            web.getSettings().setMixedContentMode(
                    android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            );
        }

        web.addJavascriptInterface(new WebAppInterface(this, web), "Android");
        web.setWebChromeClient(new YTProWebChromeClient(this, web));
        web.setWebViewClient(new YTProWebViewClient(this, web));

        web.loadUrl(url);

        setupReceiver();
        setupBackNavigation();
        streamManager = new BinaryStreamManager(web, this);
    }

    private void setupReceiver() {
        broadcastReceiver = new MediaCommandReceiver(web);

        if (Build.VERSION.SDK_INT >= 34 &&
                getApplicationInfo().targetSdkVersion >= 34) {
            registerReceiver(
                    broadcastReceiver,
                    new IntentFilter("TRACKS_TRACKS"),
                    RECEIVER_EXPORTED
            );
        } else {
            registerReceiver(
                    broadcastReceiver,
                    new IntentFilter("TRACKS_TRACKS")
            );
        }
    }

    private void setupBackNavigation() {
        if (Build.VERSION.SDK_INT >= 33) {
            OnBackInvokedDispatcher dispatcher = getOnBackInvokedDispatcher();

            backCallback = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    handleBackPress();
                }
            };

            dispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    backCallback
            );
        }
    }

    private void handleBackPress() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults
        );

        if (requestCode == 101) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                web.loadUrl("https://m.youtube.com");
            } else {
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.grant_mic),
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else if (requestCode == 1) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.grant_storage),
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(
            boolean isInPictureInPictureMode,
            Configuration newConfig) {

        isPip = isInPictureInPictureMode;
        web.evaluateJavascript(
                isInPictureInPictureMode ? "PIPlayer();" : "removePIP();",
                null
        );
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();

        if (Build.VERSION.SDK_INT >= 26 &&
                web.getUrl() != null &&
                (web.getUrl().contains("watch") ||
                 web.getUrl().contains("shorts"))) {

            if (isPlaying) {
                try {
                    // Simulasikan gesture yang diperlukan untuk PiP.
                    web.loadUrl("javascript:showCurtain();");

                    long downTime = SystemClock.uptimeMillis();
                    long eventTime = downTime;

                    float x = web.getWidth() / 2f;
                    float y = 10f;

                    MotionEvent down = MotionEvent.obtain(
                            downTime, eventTime,
                            MotionEvent.ACTION_DOWN, x, y, 0
                    );
                    web.dispatchTouchEvent(down);

                    eventTime += 15;

                    MotionEvent up = MotionEvent.obtain(
                            downTime, eventTime,
                            MotionEvent.ACTION_UP, x, y, 0
                    );
                    web.dispatchTouchEvent(up);

                    down.recycle();
                    up.recycle();

                    web.loadUrl("javascript:hideCurtain();");

                    PictureInPictureParams params =
                            new PictureInPictureParams.Builder()
                                    .setAspectRatio(
                                            new Rational(
                                                    portrait ? 9 : 16,
                                                    portrait ? 16 : 9
                                            )
                                    )
                                    .build();

                    enterPictureInPictureMode(params);

                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopService(
                new Intent(getApplicationContext(), ForegroundService.class)
        );

        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }

        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher()
                    .unregisterOnBackInvokedCallback(backCallback);
        }

        if (streamManager != null) {
            streamManager.cleanup();
        }
    }
}
