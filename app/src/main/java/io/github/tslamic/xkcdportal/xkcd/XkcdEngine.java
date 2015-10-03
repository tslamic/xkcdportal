package io.github.tslamic.xkcdportal.xkcd;

import android.content.Context;
import android.os.StatFs;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;

import java.io.File;

import io.github.tslamic.xkcdportal.BusProvider;
import io.github.tslamic.xkcdportal.event.ComicEvent;
import io.realm.Realm;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;

/**
 * Interacts with the xkcd API.
 */
public enum XkcdEngine {

    INSTANCE;

    private final XkcdApi mApi;

    XkcdEngine() {
        mApi = new RestAdapter.Builder()
                .setEndpoint(XkcdApi.ENDPOINT)
                .setClient(newOkClient())
                .setConverter(new XkcdConverter())
                .build()
                .create(XkcdApi.class);
    }

    /**
     * Tries to retrieve the latest xkcd comic without querying the cache.
     */
    public void getCurrentComic() {
        mApi.getCurrentComic(new XkcdEngineCallback(true));
    }

    /**
     * Tries to retrieve a specific xkcd comic.
     * If the comic is in the cache, no web request is made.
     *
     * @param comicNumber the comic number we wish to retrieve.
     */
    public void getComic(int comicNumber) {
        final boolean cached = isComicCached(comicNumber);
        if (cached) {
            BusProvider.INSTANCE.post(ComicEvent.asSuccess(comicNumber, false));
        } else {
            mApi.getComic(comicNumber, new XkcdEngineCallback(false));
        }
    }

    /**
     * Checks if a comic is in cache.
     *
     * @param comicNumber comic number.
     * @return {@code true} if in cache, {@code false} otherwise.
     */
    private boolean isComicCached(int comicNumber) {
        final Realm realm = Realm.getDefaultInstance();
        final XkcdComic comic = realm.where(XkcdComic.class)
                .equalTo("num", comicNumber)
                .findFirst();
        final boolean cached = null != comic;
        realm.close();
        return cached;
    }

    private static class XkcdEngineCallback implements Callback<Integer> {

        private final boolean mIsLatestComic;

        public XkcdEngineCallback(boolean isLatestComic) {
            mIsLatestComic = isLatestComic;
        }

        @Override
        public void success(Integer comicNumber, Response response) {
            if (mIsLatestComic) {
                XkcdPreferences.INSTANCE.setComicCount(comicNumber);
            }
            BusProvider.INSTANCE.post(ComicEvent.asSuccess(comicNumber, mIsLatestComic));
        }

        @Override
        public void failure(RetrofitError error) {
            final int comicNumber = mIsLatestComic ? XkcdPreferences.INSTANCE.getComicCount() : -1;
            BusProvider.INSTANCE.post(ComicEvent.asFailure(comicNumber, mIsLatestComic));
        }

    }

    // The following are helper methods for creating a new OkHttp client and its cache.
    // Code stolen from Picasso library: https://github.com/square/picasso.

    private static final int MIN_CACHE_SIZE = 1024 * 1024 * 5;
    private static final int MAX_CACHE_SIZE = 1024 * 1024 * 50;
    private static final String CACHE_DIR = "cache";

    private static OkClient newOkClient() {
        final OkHttpClient client = new OkHttpClient();

        final Context context = XkcdApplication.getContext();
        final File cache = context.getCacheDir();
        final File cacheDir = new File(cache, CACHE_DIR);
        final long cacheSize = calculateDiskCacheSize(cacheDir);
        client.setCache(new Cache(cacheDir, cacheSize));

        return new OkClient(client);
    }

    private static long calculateDiskCacheSize(File dir) {
        long size = MIN_CACHE_SIZE;
        try {
            final StatFs statFs = new StatFs(dir.getAbsolutePath());
            @SuppressWarnings("deprecation")
            long available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
            size = available / 50; // Target 2% of the total space.
        } catch (IllegalArgumentException ignored) {
        }
        return Math.max(Math.min(size, MAX_CACHE_SIZE), MIN_CACHE_SIZE);
    }

}
