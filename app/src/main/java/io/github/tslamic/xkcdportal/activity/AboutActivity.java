package io.github.tslamic.xkcdportal.activity;

import java.io.IOException;
import java.io.InputStream;

import io.github.tslamic.xkcdportal.R;
import io.github.tslamic.xkcdportal.Util;

public class AboutActivity extends AbstractWebViewActivity {

    @Override
    protected String getActivityTitle() {
        return getString(R.string.menu_about);
    }

    @Override
    protected void requestHtmlContent() {
        final InputStream in = getResources().openRawResource(R.raw.about);
        try {
            final String html = Util.convertStreamToStringAndClose(in, 0);
            loadHtml(html);
        } catch (IOException e) {
            finish();
        }
    }

}
