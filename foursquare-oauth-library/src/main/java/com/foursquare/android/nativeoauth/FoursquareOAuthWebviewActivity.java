package com.foursquare.android.nativeoauth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SuppressLint("SetJavaScriptEnabled")
public class FoursquareOAuthWebviewActivity extends Activity {

    private static final String TAG = FoursquareOAuthWebviewActivity.class.getSimpleName();

    private static final String OAUTH_URL = "https://foursquare.com/oauth2/authenticate?client_id=%s&response_type=code&container=android&androidKeyHash=%s";

    private static final String URI_MARKET_PAGE = "market://details?id=com.joelapenna.foursquared";
    private static final String MARKET_REFERRER = "utm_source=foursquare-android-oauth&utm_term=%s";

    private static final String HTTP_FOURSQUARE = "http://foursquare.com";

    private static final String URI_SCHEME = "foursquareauth";
    private static final String URI_AUTHORITY = "callback";

    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_SIGNATURE = "androidKeyHash";

    private static final int SUPPORTED_SDK_VERSION = 20130509;

    private static final String ERROR_CODE_UNSUPPORTED_VERSION = "unsupported_version";
    private static final String ERROR_CODE_INVALID_REQUEST = "invalid_request";
    private static final String ERROR_CODE_INTERNAL_ERROR = "internal_error";
    private static final String ERROR_CODE_ACCESS_DENIED = "access_denied";

    private String clientId;
    private String appSignature;
    private WebView webView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        if (uri != null) {
            // Finish if no client id was supplied.
            clientId = uri.getQueryParameter(PARAM_CLIENT_ID);
            if (TextUtils.isEmpty(clientId)) {
                onInvalidConnectRequest("Client id is missing.");
            }

            // Finish if no app signature was supplied.
            appSignature = uri.getQueryParameter(PARAM_SIGNATURE);
            if (TextUtils.isEmpty(appSignature)) {
                onInvalidConnectRequest("Android key hash is missing.");
            }

            // Finish if version code is missing or invalid.
            String versionString = uri.getQueryParameter("v");
            if (versionString == null) {
                onInvalidConnectRequest("Version code is missing.");
            } else if (Integer.parseInt(versionString) > SUPPORTED_SDK_VERSION) {
                onUnsupportedVersion(clientId);
            }
        }

        setupWebview();
    }

    private void setupWebview() {
        setContentView(R.layout.fragment_webview_oauth);

        webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "onPageStarted: " + url);
                setProgressBarIndeterminateVisibility(true);

                if (url.startsWith("https://foursquare.com/oauth2/authenticate?")) {
                    Uri uri = Uri.parse(url);
                    Intent result = new Intent();
                    String denied = uri.getQueryParameter("denied");
                    if ("1".equals(denied)) {
                        result.putExtra(FoursquareOAuth.INTENT_RESULT_DENIED, true);
                        setResult(Activity.RESULT_OK, result);
                    }
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                setProgressBarIndeterminateVisibility(false);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Intent data = new Intent();
                data.putExtra(FoursquareOAuth.INTENT_RESULT_ERROR, ERROR_CODE_INTERNAL_ERROR);
                data.putExtra(FoursquareOAuth.INTENT_RESULT_ERROR_MESSAGE, description);
                setResult(Activity.RESULT_OK, data);
                finish();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "shouldOverrideUrlLoading: " + url);
                Uri uri = Uri.parse(url);

                /*
                 * Check if redirect url is of the form
                 * foursquareauth://callback?code=CODE&error=ERROR, then extract
                 * the code or error code from it.
                 */
                if (URI_SCHEME.equals(uri.getScheme())
                        && URI_AUTHORITY.equals(uri.getAuthority())) {
                    String code = uri.getQueryParameter("code");
                    String error = uri.getQueryParameter("error");
                    Intent result = new Intent();

                    if (TextUtils.isEmpty(error)) {
                        result.putExtra(FoursquareOAuth.INTENT_RESULT_CODE, code);

                    } else {
                        if (ERROR_CODE_ACCESS_DENIED.equals(error)) {
                            result.putExtra(FoursquareOAuth.INTENT_RESULT_DENIED, true);
                        } else {
                            result.putExtra(FoursquareOAuth.INTENT_RESULT_ERROR, error);
                        }
                    }

                    setResult(Activity.RESULT_OK, result);
                    finish();
                    return true;
                }
                return false;
            }
        });

        CookieSyncManager.createInstance(this);
        setCookies(createCookiesArray());
        ensureCookieSyncManagerAvailable();
        CookieSyncManager.getInstance().sync();

        webView.loadUrl(String.format(OAUTH_URL, Uri.encode(clientId), Uri.encode(appSignature)));
    }

    private void onInvalidConnectRequest(String message) {
        Log.e(TAG, message);

        Intent data = new Intent();
        data.putExtra(FoursquareOAuth.INTENT_RESULT_ERROR, ERROR_CODE_INVALID_REQUEST);
        data.putExtra(FoursquareOAuth.INTENT_RESULT_ERROR_MESSAGE, getString(R.string.invalid_connect_request));
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    private void onUnsupportedVersion(String clientId) {
        Log.e(TAG, "Library version is not supported.");
        final String referrer = String.format(MARKET_REFERRER, clientId);
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse(URI_MARKET_PAGE)
                        .buildUpon()
                        .appendQueryParameter("referrer", referrer)
                        .build()
        ));

        Intent data = new Intent();
        data.putExtra(FoursquareOAuth.INTENT_RESULT_ERROR, ERROR_CODE_UNSUPPORTED_VERSION);
        data.putExtra(FoursquareOAuth.INTENT_RESULT_ERROR_MESSAGE, getString(R.string.unsupported_version));
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    /**
     * Called when the Activity is visible to the user and actively running.
     * Resumes the WebView.
     */
    @Override
    public void onPause() {
        super.onPause();
        webView.onPause();

        // Wipe any cookies we set when exiting.
        if (isFinishing()) {
            // We're going to cheat here and remove only the 'oauth_token'
            // cookie, it's the only one we want to protect.
            CookieManager cookieManager = CookieManager.getInstance();
            expireCookie(cookieManager);

            Log.v(TAG, "Cookie test: " + cookieManager.getCookie(HTTP_FOURSQUARE));

            webView.loadData("<html></html>", "text/html", "utf-8");
        }

        ensureCookieSyncManagerAvailable();
        CookieSyncManager.getInstance().stopSync();
    }

    /**
     * Called when the fragment is no longer resumed. Pauses the WebView.
     */
    @Override
    public void onResume() {
        ensureCookieSyncManagerAvailable();
        CookieSyncManager.getInstance().startSync();
        webView.onResume();
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected List<String> createCookiesArray() {
        List<String> cookies = new ArrayList<>();
        cookies.add("lang-pref=" + getLocaleString());
        cookies.add("v=20200317");

        return cookies;
    }

    protected void setCookies(List<String> cookies) {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        for (String cookie : cookies) {
            Log.e(TAG, "  " + cookie);
            cookieManager.setCookie(HTTP_FOURSQUARE, cookie);
        }

        Log.e(TAG, "cookie for http://foursquare.com : "
                + cookieManager.getCookie(HTTP_FOURSQUARE));
        Log.e(TAG, "cookie for url: " + cookieManager.getCookie(HTTP_FOURSQUARE));
    }

    private void ensureCookieSyncManagerAvailable() {
        try {
            CookieSyncManager.getInstance();
        } catch (Exception e) {
            CookieSyncManager.createInstance(this);
        }
    }

    private static void expireCookie(CookieManager cm) {
        @SuppressWarnings("deprecation") String gmtExpired = (new Date(System.currentTimeMillis() - 1000L)).toGMTString();
        cm.setCookie(HTTP_FOURSQUARE, "oauth_token" + "=deleted;expires=" + gmtExpired + ";secure");
    }

    private static String getLocaleString() {
        Locale locale = Locale.getDefault();
        String localeValue = locale.getLanguage() + "-" + locale.getCountry();

        // Special case Catalan and replace with Spanish
        if ("ca-ES".equals(localeValue)) {
            localeValue = "es-ES";
        }

        return localeValue;
    }
}