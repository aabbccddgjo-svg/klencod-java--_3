package com.mot998;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;

public class MainActivity extends Activity {

    private WebView webView;
    private EditText urlInput;
    private ProgressBar progressBar;
    private FrameLayout container;
    private LinearLayout homeView;
    private TextView blockedCounterTv;

    private int adsBlockedCount = 0;
    private boolean isDesktopMode = false;
    private boolean isIncognito = false;

    private static final String[] AD_FILTERS = {
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "adnxs.com", "facebook.com/tr/", "analytics.google.com", "adservice.google.com"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#121212"));

        // 1. شريط البحث العلوي (Brave Top Bar)
        LinearLayout topBar = createTopBar();
        rootLayout.addView(topBar);

        // شريط التقدم
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setVisibility(View.GONE);
        rootLayout.addView(progressBar);

        // 2. حاوي المحتوى الرئيسي (الصفحة الرئيسية + الـ WebView)
        container = new FrameLayout(this);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        container.setLayoutParams(containerParams);

        // إنشاء الـ WebView
        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        webView.setVisibility(View.GONE);
        setupBraveSettings();

        // إنشاء واجهة الصفحة الرئيسية (Brave Dashboard)
        homeView = createBraveHomeDashboard();

        container.addView(webView);
        container.addView(homeView);
        rootLayout.addView(container);

        // 3. شريط التحكم السفلي (Brave Bottom Bar)
        LinearLayout bottomBar = createBottomBar();
        rootLayout.addView(bottomBar);

        setContentView(rootLayout);
    }

    // شريط البحث العلوي
    private LinearLayout createTopBar() {
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(20, 15, 20, 15);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(Color.parseColor("#1E1E24"));

        TextView lionIcon = new TextView(this);
        lionIcon.setText("🦁 ");
        lionIcon.setTextSize(18);
        topBar.addView(lionIcon);

        urlInput = new EditText(this);
        urlInput.setHint("بحث في Google أو اكتب عنوانًا");
        urlInput.setHintTextColor(Color.GRAY);
        urlInput.setTextColor(Color.WHITE);
        urlInput.setSingleLine(true);
        urlInput.setTextSize(14);
        urlInput.setPadding(30, 20, 30, 20);

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#2B2B36"));
        inputBg.setCornerRadius(30);
        urlInput.setBackground(inputBg);

        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        urlParams.setMargins(10, 0, 10, 0);
        urlInput.setLayoutParams(urlParams);

        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            loadUrlFromInput();
            return true;
        });

        topBar.addView(urlInput);
        return topBar;
    }

    // واجهة Brave الرئيسية الحاوية لبطاقة الإحصائيات والاختصارات
    private LinearLayout createBraveHomeDashboard() {
        LinearLayout mainHome = new LinearLayout(this);
        mainHome.setOrientation(LinearLayout.VERTICAL);
        mainHome.setPadding(30, 40, 30, 30);
        mainHome.setBackgroundColor(Color.parseColor("#18181C"));

        // بطاقة إحصائيات الخصوصية (Privacy Stats)
        LinearLayout statsCard = new LinearLayout(this);
        statsCard.setOrientation(LinearLayout.VERTICAL);
        statsCard.setPadding(35, 35, 35, 35);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#25252E"));
        cardBg.setCornerRadius(30);
        statsCard.setBackground(cardBg);

        TextView statsTitle = new TextView(this);
        statsTitle.setText("🛡️ إحصائيات الخصوصية");
        statsTitle.setTextColor(Color.WHITE);
        statsTitle.setTextSize(16);
        statsCard.addView(statsTitle);

        LinearLayout numbersLayout = new LinearLayout(this);
        numbersLayout.setOrientation(LinearLayout.HORIZONTAL);
        numbersLayout.setPadding(0, 30, 0, 10);
        numbersLayout.setWeightSum(3);

        blockedCounterTv = createStatItem(numbersLayout, "3.6K", "تم حظر أدوات\nالتتبع والإعلانات");
        createStatItem(numbersLayout, "79 MB", "البيانات التقديرية\nالمحفوظة");
        createStatItem(numbersLayout, "3m", "الوقت المقدر\nالذي تم توفيره");

        statsCard.addView(numbersLayout);
        mainHome.addView(statsCard);

        // قائمة الاختصارات السريعة (Top Sites)
        LinearLayout shortcutsLayout = new LinearLayout(this);
        shortcutsLayout.setOrientation(LinearLayout.HORIZONTAL);
        shortcutsLayout.setPadding(0, 40, 0, 0);
        shortcutsLayout.setGravity(Gravity.CENTER);

        addShortcutItem(shortcutsLayout, "🌐", "Google", "https://google.com");
        addShortcutItem(shortcutsLayout, "🦆", "DuckDuckGo", "https://duckduckgo.com");
        addShortcutItem(shortcutsLayout, "🤖", "Arena AI", "https://chat.lmsys.org");
        addShortcutItem(shortcutsLayout, "✉️", "Temp Mail", "https://tempmail.com");

        mainHome.addView(shortcutsLayout);

        return mainHome;
    }

    private TextView createStatItem(LinearLayout parent, String number, String label) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        item.setLayoutParams(p);

        TextView numTv = new TextView(this);
        numTv.setText(number);
        numTv.setTextColor(Color.parseColor("#FF5722"));
        numTv.setTextSize(20);
        numTv.setGravity(Gravity.CENTER);

        TextView lblTv = new TextView(this);
        lblTv.setText(label);
        lblTv.setTextColor(Color.GRAY);
        lblTv.setTextSize(10);
        lblTv.setGravity(Gravity.CENTER);

        item.addView(numTv);
        item.addView(lblTv);
        parent.addView(item);
        return numTv;
    }

    private void addShortcutItem(LinearLayout parent, String icon, String name, final String url) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(20, 10, 20, 10);

        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(24);
        iconTv.setPadding(20, 20, 20, 20);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2B2B36"));
        bg.setCornerRadius(50);
        iconTv.setBackground(bg);

        TextView nameTv = new TextView(this);
        nameTv.setText(name);
        nameTv.setTextColor(Color.WHITE);
        nameTv.setTextSize(11);
        nameTv.setPadding(0, 10, 0, 0);

        item.addView(iconTv);
        item.addView(nameTv);

        item.setOnClickListener(v -> loadUrl(url));
        parent.addView(item);
    }

    // شريط التنقل السفلي
    private LinearLayout createBottomBar() {
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setPadding(10, 20, 10, 20);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setBackgroundColor(Color.parseColor("#18181C"));
        bottomBar.setWeightSum(5);

        addBottomNavItem(bottomBar, "⋮", v -> showBraveMenu());
        addBottomNavItem(bottomBar, "1", v -> Toast.makeText(this, "التبويبات المفتوحة: 1", Toast.LENGTH_SHORT).show());
        addBottomNavItem(bottomBar, "🔍", v -> urlInput.requestFocus());
        addBottomNavItem(bottomBar, "🔖", v -> Toast.makeText(this, "الإشارات المرجعية", Toast.LENGTH_SHORT).show());
        addBottomNavItem(bottomBar, "🏠", v -> showHomeScreen());

        return bottomBar;
    }

    private void addBottomNavItem(LinearLayout parent, String text, View.OnClickListener listener) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(18);
        btn.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btn.setLayoutParams(params);
        btn.setOnClickListener(listener);
        parent.addView(btn);
    }

    // قائمة الخيارات المطابقة لـ Brave Menu
    private void showBraveMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setPadding(40, 40, 40, 40);
        menuLayout.setBackgroundColor(Color.parseColor("#23232C"));

        addMenuItem(menuLayout, "➕  علامة تبويب جديدة", v -> { showHomeScreen(); });
        addMenuItem(menuLayout, "🕶️  علامة تبويب خاصة جديدة", v -> { toggleIncognito(); });
        addMenuItem(menuLayout, "🧹  حذف بيانات الموقع", v -> { clearCurrentSiteData(); });
        addMenuItem(menuLayout, "🖥️  موقع مصمَّم للكمبيوتر", v -> { toggleDesktopMode(); });
        addMenuItem(menuLayout, "🔍  البحث في الصفحة", v -> { });
        addMenuItem(menuLayout, "⚙️  الإعدادات", v -> { });

        scrollView.addView(menuLayout);
        builder.setView(scrollView);
        builder.show();
    }

    private void addMenuItem(LinearLayout parent, String title, View.OnClickListener listener) {
        TextView item = new TextView(this);
        item.setText(title);
        item.setTextColor(Color.WHITE);
        item.setTextSize(15);
        item.setPadding(20, 25, 20, 25);
        item.setOnClickListener(v -> {
            listener.onClick(v);
        });
        parent.addView(item);
    }

    private void setupBraveSettings() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSavePassword(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new SecureWebViewClient());
    }

    private void loadUrlFromInput() {
        String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) return;

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://" + url;
            } else {
                url = "https://duckduckgo.com/?q=" + url;
            }
        }
        loadUrl(url);
    }

    private void loadUrl(String url) {
        homeView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    private void showHomeScreen() {
        webView.setVisibility(View.GONE);
        homeView.setVisibility(View.VISIBLE);
        urlInput.setText("");
    }

    private void clearCurrentSiteData() {
        String currentUrl = webView.getUrl();
        if (currentUrl == null || homeView.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "قم بفتح موقع أولاً لمسح بياناته", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = Uri.parse(currentUrl);
            String host = uri.getHost();

            if (host != null) {
                WebStorage.getInstance().deleteOrigin(uri.getScheme() + "://" + host);
                webView.clearCache(true);
                webView.reload();
                Toast.makeText(this, "تم مسح بيانات: " + host, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "خطأ أثناء الحذف: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleDesktopMode() {
        isDesktopMode = !isDesktopMode;
        WebSettings settings = webView.getSettings();
        if (isDesktopMode) {
            settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
            Toast.makeText(this, "تم تفعيل عرض الكمبيوتر", Toast.LENGTH_SHORT).show();
        } else {
            settings.setUserAgentString(null);
            Toast.makeText(this, "تم تفعيل عرض الهاتف", Toast.LENGTH_SHORT).show();
        }
        webView.reload();
    }

    private void toggleIncognito() {
        isIncognito = !isIncognito;
        if (isIncognito) {
            CookieManager.getInstance().removeAllCookies(null);
            webView.clearCache(true);
            Toast.makeText(this, "تم التبديل للتصفح الخفي", Toast.LENGTH_SHORT).show();
        }
        showHomeScreen();
    }

    private class SecureWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            urlInput.setText(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString().toLowerCase();
            for (String adDomain : AD_FILTERS) {
                if (url.contains(adDomain)) {
                    runOnUiThread(() -> {
                        adsBlockedCount++;
                        blockedCounterTv.setText(String.valueOf(adsBlockedCount));
                    });
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream("".getBytes()));
                }
            }
            return super.shouldInterceptRequest(view, request);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
        } else if (webView.getVisibility() == View.VISIBLE) {
            showHomeScreen();
        } else {
            super.onBackPressed();
        }
    }
}
