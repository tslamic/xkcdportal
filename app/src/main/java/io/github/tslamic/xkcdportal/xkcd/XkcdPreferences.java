package io.github.tslamic.xkcdportal.xkcd;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public enum XkcdPreferences {

    INSTANCE;

    // NOTE:
    // most keys have inappropriate namings. Alas, to support versions below 2.0,
    // they should remain intact.

    private static final String KEY_LATEST_COMIC_COUNT = "XkcdActivity.KEY_LATEST_COMIC_COUNT";
    private static final String KEY_SHOW_TUTORIAL = "XkcdActivity.KEY_SHOW_TUTORIAL";
    private static final String KEY_LAST_POSITION = "XkcdActivity.KEY_LAST_POSITION";
    private static final String KEY_RATE_APP = "XkcdActivity.KEY_RATE_APP";

    private final SharedPreferences mPrefs;

    XkcdPreferences() {
        final Context context = XkcdApplication.getContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean showTutorialOnFirstLaunch() {
        return mPrefs.getBoolean(KEY_SHOW_TUTORIAL, true);
    }

    public void setShowTutorialOnFirstLaunch(boolean show) {
        if (show != showTutorialOnFirstLaunch()) {
            mPrefs.edit().putBoolean(KEY_SHOW_TUTORIAL, show).apply();
        }
    }

    public int getLastSeenComic() {
        return mPrefs.getInt(KEY_LAST_POSITION, -1);
    }

    public void setLastSeenComic(int position) {
        mPrefs.edit().putInt(KEY_LAST_POSITION, position).apply();
    }

    public int getComicCount() {
        synchronized (XkcdPreferences.class) {
            return mPrefs.getInt(KEY_LATEST_COMIC_COUNT, -1);
        }
    }

    public void setComicCount(int comicCount) {
        synchronized (XkcdPreferences.class) {
            mPrefs.edit().putInt(KEY_LATEST_COMIC_COUNT, comicCount).apply();
        }
    }

    private static final int SHOW_RATE_APP_DIALOG_INTERVAL = 5;
    private static final int DISABLE_RATE_APP_DIALOG = -1;

    public boolean showRateAppDialog() {
        int count = mPrefs.getInt(KEY_RATE_APP, 0);

        final boolean show;
        if (count == DISABLE_RATE_APP_DIALOG) {
            show = false;
        } else {
            show = count++ >= SHOW_RATE_APP_DIALOG_INTERVAL;
            mPrefs.edit().putInt(KEY_RATE_APP, count % SHOW_RATE_APP_DIALOG_INTERVAL).apply();
        }

        return show;
    }

    public void disableRateAppDialog() {
        mPrefs.edit().putInt(KEY_RATE_APP, DISABLE_RATE_APP_DIALOG).apply();
    }

}
