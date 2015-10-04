package io.github.tslamic.xkcdportal.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.tslamic.xkcdportal.Analytics;
import io.github.tslamic.xkcdportal.R;
import io.github.tslamic.xkcdportal.Util;
import io.github.tslamic.xkcdportal.xkcd.XkcdComic;

public class ExplainComicActivity extends AbstractWebViewActivity {

    private static final String KEY_COMIC_TITLE = "ExplainComicActivity.KEY_COMIC_TITLE";
    private static final String KEY_COMIC_NUM = "ExplainComicActivity.KEY_COMIC_NUM";

    private static final String EXPLAIN_XKCD_URL = "http://www.explainxkcd.com";
    private static final String EXPLAIN_COMIC_URL = EXPLAIN_XKCD_URL + "/wiki/index.php/%d";
    private static final String EXPLAIN_EDIT_HTML_REGEX = "(?i)\\[<a([^>]+)>edit</a>\\]";

    public static void start(@NonNull Context context, @NonNull XkcdComic comic) {
        start(context, comic.getSafe_title(), comic.getNum());
    }

    public static void start(@NonNull Context context,
                             @NonNull String comicTitle,
                             int comicNumber) {
        final Intent intent = new Intent(context, ExplainComicActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(KEY_COMIC_TITLE, comicTitle);
        intent.putExtra(KEY_COMIC_NUM, comicNumber);
        context.startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_explain, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.menu_explain_external == item.getItemId()) {
            final int comicNumber = getIntent().getIntExtra(KEY_COMIC_NUM, -1);
            if (comicNumber != -1) {
                openExternal(String.format(EXPLAIN_COMIC_URL, comicNumber));
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected String getActivityTitle() {
        String comicTitle = getIntent().getStringExtra(KEY_COMIC_TITLE);
        if (TextUtils.isEmpty(comicTitle)) {
            comicTitle = "";
        }
        return getString(R.string.explain_comic, comicTitle);
    }

    @Override
    protected void loadHtml(String html) {
        mHtmlContent = html;
        mWebView.loadDataWithBaseURL(EXPLAIN_XKCD_URL, html, "text/html", "UTF-8", EXPLAIN_XKCD_URL);
    }

    @Override
    protected void requestHtmlContent() {
        final Intent intent = getIntent();
        final int comicNumber = intent.getIntExtra(KEY_COMIC_NUM, -1);
        if (comicNumber == -1) {
            finish();
        } else {
            final String url = String.format(EXPLAIN_COMIC_URL, comicNumber);
            final String err = getString(R.string.explain_scrape_error);
            new ScrapingTask(this, url, err).execute();
            Analytics.trackEvent(Analytics.Category.EXPLAIN,
                    Analytics.Action.EXPLAIN_COMIC, String.valueOf(comicNumber));
        }
    }

    /**
     * AsyncTask for scraping the comic explanation from explainxkcd.com.
     */
    private static class ScrapingTask extends AsyncTask<Void, Void, String> {

        static final String HTML_TEMPLATE = "<!doctype html><body><h2>%s</h2></body></html>";
        static final String SCRAPE_MATCHER = "<span class=\"mw-headline\" id=\"Explanation\">Explanation</span></h2>(.*)</p><h2>";

        private final String mUrl;
        private final String mErrorMessage;
        private final WeakReference<ExplainComicActivity> mActivity;

        // Gather analytics on how well scraping actually performs.
        private long mElapsedTime;

        ScrapingTask(ExplainComicActivity activity, String url, String message) {
            mUrl = url;
            mErrorMessage = message;
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            mElapsedTime = SystemClock.elapsedRealtime();
        }

        @Override
        protected String doInBackground(Void... params) {
            String content = null;
            try {
                final String html = readHtmlContent(mUrl);
                final Pattern p = Pattern.compile(SCRAPE_MATCHER, Pattern.DOTALL);
                final Matcher m = p.matcher(html);
                if (m.find()) {
                    final String stripped = m.group().replaceAll(EXPLAIN_EDIT_HTML_REGEX, "");
                    content = String.format(HTML_TEMPLATE, stripped);
                }
            } catch (IOException ignore) {
            }
            return content;
        }

        @Override
        protected void onPostExecute(String html) {
            if (TextUtils.isEmpty(html)) {
                html = String.format(HTML_TEMPLATE, mErrorMessage);
                Analytics.trackEvent(Analytics.Category.EXPLAIN,
                        Analytics.Action.EXPLAIN_SCRAPE_FAILED, mUrl);
            } else {
                mElapsedTime = SystemClock.elapsedRealtime() - mElapsedTime;
                Analytics.trackTiming(Analytics.Category.EXPLAIN, mElapsedTime,
                        Analytics.Action.EXPLAIN_SCRAPE_DURATION, "ScrapingTask");
            }
            final ExplainComicActivity activity = mActivity.get();
            if (null != activity) {
                activity.loadHtml(html);
            }
        }

        private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 15 * 1000;
        private static final int DEFAULT_READ_TIMEOUT_MILLIS = 20 * 1000;

        private static String readHtmlContent(@NonNull String urlString) throws IOException {
            final URL url = new URL(urlString);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLIS);

            try {
                // Ignoring response code on purpose.
                final InputStream in = connection.getInputStream();
                return Util.convertStreamToStringAndClose(in, 0);
            } finally {
                connection.disconnect();
            }
        }

    }

}
