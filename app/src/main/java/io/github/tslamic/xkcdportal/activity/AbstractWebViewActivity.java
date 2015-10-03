package io.github.tslamic.xkcdportal.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import io.github.tslamic.xkcdportal.Analytics;
import io.github.tslamic.xkcdportal.R;
import io.github.tslamic.xkcdportal.Util;

abstract class AbstractWebViewActivity extends AppCompatActivity {

    private static final String KEY_HTML_CONTENT = "AboutActivity.KEY_HTML_CONTENT";

    private ProgressBar mProgress;
    protected String mHtmlContent;
    protected WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.webview);
        setTitle(getActivityTitle());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProgress = (ProgressBar) findViewById(R.id.webview_progress);
        mWebView = (WebView) findViewById(R.id.webview_view);
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                openExternal(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mProgress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mProgress.setVisibility(View.GONE);
            }

        });

        String html = null;
        if (null != savedInstanceState) {
            html = savedInstanceState.getString(KEY_HTML_CONTENT);
        }
        if (TextUtils.isEmpty(html)) {
            requestHtmlContent();
        } else {
            loadHtml(html);
        }
    }

    @Override
    protected void onDestroy() {
        mWebView.stopLoading();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_HTML_CONTENT, mHtmlContent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void openExternal(String url) {
        final Uri uri = Uri.parse(url);
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(intent);
            Analytics.trackEvent(Analytics.Category.EXTERNALS,
                    Analytics.Action.EXTERNAL_LINK_CLICKED, url);
        } catch (ActivityNotFoundException e) {
            Util.showToast(this, R.string.external_link_error);
            Analytics.trackEvent(Analytics.Category.EXTERNALS,
                    Analytics.Action.EXTERNAL_LINK_FAILED, url);
        }
    }

    protected void loadHtml(String html) {
        mHtmlContent = html;
        mWebView.loadData(html, "text/html", "UTF-8");
    }

    protected abstract String getActivityTitle();

    // Must eventually invoke loadHtml(html).
    protected abstract void requestHtmlContent();

}
