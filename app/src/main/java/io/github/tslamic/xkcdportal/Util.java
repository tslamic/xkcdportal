package io.github.tslamic.xkcdportal;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.github.tslamic.xkcdportal.xkcd.XkcdApplication;
import io.github.tslamic.xkcdportal.xkcd.XkcdComic;

public class Util {

    // Must be in sync with xml/paths.xml
    private static final String COMICS_FOLDER = "comics";

    // Extension based on the bitmap compress format.
    private static final String COMIC_EXTENSION = ".jpeg";

    public static boolean isNetworkAvailable() {
        final Context context = XkcdApplication.getContext();
        final ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Saves a comic bitmap.
     * <p/>
     * A comic is saved on two occasions:
     * 1. when marked as favorite
     * 2. when shared
     * <p/>
     * For 1., the bitmap is saved in the getFilesDir() folder and deleted as soon as the user
     * un-favorites it.
     * For 2., the bitmap is saved in the getCacheDir() and deleted when the cache is cleared.
     */
    public static File saveComic(@NonNull XkcdComic comic, @NonNull Bitmap bitmap) {
        final Context context = XkcdApplication.getContext();
        final File dir = comic.isFavorite() ? context.getFilesDir() : context.getCacheDir();
        final File cache = new File(dir, COMICS_FOLDER);
        if (cache.exists() || cache.mkdirs()) {
            final String filename = String.valueOf(comic.getNum()) + COMIC_EXTENSION;
            return saveComic(cache, filename, bitmap);
        }
        return null;
    }

    private static File saveComic(File cache, String filename, Bitmap bitmap) {
        FileOutputStream outStream = null;
        File source = null;
        try {
            final File target = new File(cache, filename);
            outStream = new FileOutputStream(target);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)) {
                source = target;
            }
        } catch (FileNotFoundException ignore) {
        } finally {
            closeQuietly(outStream);
        }
        return source;
    }

    public static void closeQuietly(@Nullable Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static boolean deleteComic(@NonNull XkcdComic comic) {
        final String path = comic.getPath();
        return !TextUtils.isEmpty(path) && new File(path).delete();
    }

    public static File getSavedComicBitmap(@NonNull XkcdComic comic) {
        final String path = comic.getPath();
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        File file = new File(path);
        return (file.exists() && file.length() > 0) ? file : null;
    }

    public static boolean isHoneycombOrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    public static String convertStreamToStringAndClose(@NonNull InputStream in, int length)
            throws IOException {
        BufferedReader reader = null;
        try {
            if (length <= 0) {
                length = DEFAULT_BUFFER_SIZE;
            }
            reader = new BufferedReader(new InputStreamReader(in, "UTF-8"), length);
            final StringBuilder builder = new StringBuilder(length);
            String line;
            while (null != (line = reader.readLine())) {
                builder.append(line);
            }
            return builder.toString();
        } finally {
            closeQuietly(reader);
        }
    }

    public static void showToast(@Nullable Context context, @StringRes int stringResource) {
        if (null != context && stringResource > 0) {
            showToast(context, context.getString(stringResource));
        }
    }

    public static void showToast(@Nullable Context context, @Nullable String message) {
        if (null != context && !TextUtils.isEmpty(message)) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static void ensureFragmentArgsAreValid(@Nullable Bundle args, int expectedArgs) {
        if (null == args || args.size() != expectedArgs) {
            throw new IllegalStateException("invalid arguments");
        }
    }

    public static String createTitle(int comicNumber, @NonNull String safeTitle) {
        return comicNumber + ": " + safeTitle;
    }

    private Util() {
        throw new UnsupportedOperationException();
    }

}
