package io.github.tslamic.xkcdportal.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.File;
import java.lang.ref.WeakReference;

import io.github.tslamic.xkcdportal.Analytics;
import io.github.tslamic.xkcdportal.BusProvider;
import io.github.tslamic.xkcdportal.R;
import io.github.tslamic.xkcdportal.Util;
import io.github.tslamic.xkcdportal.activity.ExplainComicActivity;
import io.github.tslamic.xkcdportal.event.ComicEvent;
import io.github.tslamic.xkcdportal.event.ConnectionEvent;
import io.github.tslamic.xkcdportal.ui.ZoomImageView;
import io.github.tslamic.xkcdportal.xkcd.XkcdComic;
import io.github.tslamic.xkcdportal.xkcd.XkcdEngine;
import io.github.tslamic.xkcdportal.xkcd.XkcdTitleHandler;
import io.realm.Realm;

public class XkcdFragment extends Fragment {

    // Must be in sync with AndroidManifest.xml
    private static final String AUTHORITY = "io.github.tslamic.xkcdportal.fileprovider";

    private static final String KEY_SHOW_FAVORITE_ICON = "XkcdFragment.KEY_SHOW_FAVORITE_ICON";
    private static final String KEY_COMIC_NUM = "XkcdFragment.KEY_COMIC_NUM";

    private ProgressCallback mProgressCallback;
    private XkcdTitleHandler mTitleHandler;
    private ZoomImageView mXkcdContent;
    private MenuItem mFavoriteMenuItem;
    private Picasso mPicasso;

    private XkcdComic mComic; // Any object assigned to this must come from Realm.
    private Realm mRealm;

    public static XkcdFragment newInstance(int comicNum) {
        return newInstance(comicNum, true);
    }

    public static XkcdFragment newInstance(int comicNum, boolean showFavoriteIcon) {
        final Bundle args = new Bundle(2);
        args.putInt(KEY_COMIC_NUM, comicNum);
        args.putBoolean(KEY_SHOW_FAVORITE_ICON, showFavoriteIcon);

        final XkcdFragment fragment = new XkcdFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof XkcdTitleHandler) {
            mTitleHandler = (XkcdTitleHandler) activity;
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (null != mTitleHandler && null != mComic && isVisibleToUser) {
            mTitleHandler.onNewTitle(mComic.getNum(), mComic.getSafe_title());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.ensureFragmentArgsAreValid(getArguments(), 2);

        setRetainInstance(true);

        final Context context = getActivity();
        mPicasso = Picasso.with(context);
        mRealm = Realm.getDefaultInstance();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.xkcd, container, false);

        final ProgressBar bar = (ProgressBar) view.findViewById(R.id.xkcd_progress);
        mProgressCallback = new ProgressCallback(bar);
        mXkcdContent = (ZoomImageView) view.findViewById(R.id.xkcd_content);
        mXkcdContent.setInteractionListener(new ZoomImageView.InteractionListener() {
            @Override
            public void onLongPress() {
                if (hasContent()) {
                    showExtraContent();
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_comic, menu);
        mFavoriteMenuItem = menu.findItem(R.id.menu_favorite_item);
        updateFavoriteMenuItem(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.menu_share:
                onMenuShare();
                return true;
            case R.id.menu_explain:
                onMenuExplain();
                return true;
            case R.id.menu_favorite_item:
                onMenuToggleFavorite();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onMenuShare() {
        if (hasContent() && shareComic()) {
            Analytics.trackEvent(Analytics.Category.MENU,
                    Analytics.Action.MENU_SELECTED, "share");
        } else {
            Util.showToast(getActivity(), R.string.share_error);
        }
    }

    private void onMenuExplain() {
        if (!hasContent()) {
            Util.showToast(getActivity(), R.string.explain_error);
        } else if (!Util.isNetworkAvailable()) {
            Util.showToast(getActivity(), R.string.explain_network_error);
        } else {
            ExplainComicActivity.start(getActivity(), mComic);
            Analytics.trackEvent(Analytics.Category.MENU,
                    Analytics.Action.MENU_SELECTED, "explain");
        }
    }

    private void onMenuToggleFavorite() {
        if (hasContent()) {
            toggleFavorite();
            Analytics.trackEvent(Analytics.Category.MENU,
                    Analytics.Action.MENU_SELECTED, "favorite");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.INSTANCE.register(this);
        requestComic();
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.INSTANCE.unregister(this);
    }

    @Override
    public void onDestroyView() {
        mPicasso.cancelRequest(mXkcdContent);
        mFavoriteMenuItem = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    // Triggered from XkcdEngine.
    @SuppressWarnings("unused")
    @Subscribe
    public void onComicEvent(ComicEvent event) {
        // XkcdFragment is shown inside of a ViewPager and three different instances
        // might receive this event. Ensure the comic is loaded in the right instance.
        final int comicNumber = getArguments().getInt(KEY_COMIC_NUM);
        if (comicNumber == event.comicNumber) {
            mComic = mRealm.where(XkcdComic.class)
                    .equalTo("num", comicNumber)
                    .findFirst();
            requestComicBitmap();
            updateFavoriteMenuItem(false);
            if (null != mTitleHandler) {
                mTitleHandler.onNewTitle(comicNumber, mComic.getSafe_title());
            }
        }
    }

    // Triggered from ConnectionReceiver.
    @SuppressWarnings("unused")
    @Subscribe
    public void onConnectionEvent(ConnectionEvent event) {
        switch (event) {
            case OFFLINE:
                mPicasso.cancelRequest(mXkcdContent);
                break;
            case ONLINE:
                requestComic();
                break;
        }
    }

    private boolean shareComic() {
        boolean shared = false;
        File saved = Util.getSavedComicBitmap(mComic);
        if (null == saved) {
            saved = saveComic();
        }
        if (null != saved) {
            final Context context = getActivity();
            if (null != context) {
                final Uri uri = FileProvider.getUriForFile(context, AUTHORITY, saved);
                final Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                try {
                    startActivity(intent);
                    shared = true;
                    Analytics.trackEvent(Analytics.Category.COMIC,
                            Analytics.Action.COMIC_SHARED, String.valueOf(mComic.getNum()));
                } catch (ActivityNotFoundException ignore) {
                }
            }
        }
        return shared;
    }

    private void requestComic() {
        final int comicNumber = getArguments().getInt(KEY_COMIC_NUM);
        XkcdEngine.INSTANCE.getComic(comicNumber);
    }

    private void requestComicBitmap() {
        Bitmap bitmap = null;
        if (hasContent()) {
            bitmap = getComicBitmap();
        }
        if (null == bitmap) {
            loadComicBitmap();
        }
    }

    private void loadComicBitmap() {
        final RequestCreator creator;
        final File cached = Util.getSavedComicBitmap(mComic);
        if (null == cached) {
            creator = mPicasso.load(mComic.getImg());
        } else {
            creator = mPicasso.load(cached);
        }
        mProgressCallback.onStart();
        creator.into(mXkcdContent, mProgressCallback);
    }

    private File saveComic() {
        if (hasContent()) {
            File file = Util.saveComic(mComic, getComicBitmap());
            if (null != file) {
                mRealm.beginTransaction();
                mComic.setPath(file.getPath());
                mRealm.commitTransaction();
            }
            return file;
        }
        return null;
    }

    // Assumes hasContent() yields true.
    private void toggleFavorite() {
        final boolean isFavorite = !mComic.isFavorite(); // Toggle.

        mRealm.beginTransaction();
        mComic.setFavorite(isFavorite);
        mRealm.commitTransaction();

        final int messageRes;
        if (isFavorite) {
            saveComic();
            messageRes = R.string.favorites_added;
        } else {
            Util.deleteComic(mComic);
            mRealm.beginTransaction();
            mComic.setPath("");
            mRealm.commitTransaction();
            messageRes = R.string.favorites_removed;
        }

        // NOTE:
        // if this XkcdFragment is viewed through FavoritesActivity and was just removed
        // from favorites, chances are it is already detached. Check.
        final Context context = getActivity();
        if (null != context) {
            updateFavoriteMenuItem(true);
            final String message = context.getString(messageRes, mComic.getTitle());
            Util.showToast(context, message);
        }
    }

    // Assumes hasContent() yields true.
    private Bitmap getComicBitmap() {
        final BitmapDrawable drawable = (BitmapDrawable) mXkcdContent.getDrawable();
        return drawable.getBitmap();
    }

    private boolean hasContent() {
        return null != mComic && null != mXkcdContent && null != mXkcdContent.getDrawable();
    }

    private void updateFavoriteMenuItem(boolean animate) {
        final Context context = getActivity();
        if (null != context && null != mFavoriteMenuItem && null != mComic) {
            final int icon;
            final int text;
            if (mComic.isFavorite()) {
                icon = R.drawable.ic_favorite;
                text = R.string.menu_rem_favorite;
            } else {
                icon = R.drawable.ic_favorite_outline;
                text = R.string.menu_add_favorite;
            }
            if (animate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                animateFavoriteMenuIcon(context, icon, text);
            } else {
                mFavoriteMenuItem.setIcon(icon);
                mFavoriteMenuItem.setTitle(text);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private void animateFavoriteMenuIcon(Context context, final int iconRes, final int textRes) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final ImageView actionView = (ImageView)
                inflater.inflate(R.layout.favorite_menu_item, null);
        actionView.setImageDrawable(mFavoriteMenuItem.getIcon());
        mFavoriteMenuItem.setActionView(actionView);

        final int duration = 100;
        actionView.animate()
                .scaleX(0.75f)
                .scaleY(0.75f)
                .setDuration(duration)
                .setListener(new AnimatorListenerAdapter() {
                    @SuppressWarnings("NewApi")
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        actionView.setImageResource(iconRes);
                        actionView.animate()
                                .scaleX(1)
                                .scaleY(1)
                                .setDuration(duration)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        // In very rare cases (monkey testing), this might be null.
                                        if (null != mFavoriteMenuItem) {
                                            mFavoriteMenuItem.setIcon(iconRes);
                                            mFavoriteMenuItem.setTitle(textRes);
                                            mFavoriteMenuItem.setActionView(null);
                                        }
                                    }
                                });
                    }
                });
    }

    private void showExtraContent() {
        final FragmentManager manager = getFragmentManager();
        if (null == manager.findFragmentByTag(ExtraContentDialogFragment.TAG)) {
            final ExtraContentDialogFragment f = ExtraContentDialogFragment.newInstance(mComic);
            f.show(manager, ExtraContentDialogFragment.TAG);
            Analytics.trackEvent(Analytics.Category.COMIC,
                    Analytics.Action.COMIC_LONG_PRESS);
        }
    }

    private static class ProgressCallback implements com.squareup.picasso.Callback {

        private final WeakReference<ProgressBar> mProgressRef;
        private long mRequestStart;

        ProgressCallback(ProgressBar progress) {
            mProgressRef = new WeakReference<>(progress);
        }

        @Override
        public void onSuccess() {
            onStop();
        }

        @Override
        public void onError() {
            onStop();
        }

        public void onStart() {
            setVisibility(View.VISIBLE);
            mRequestStart = SystemClock.elapsedRealtime();
        }

        private void onStop() {
            setVisibility(View.GONE);
            measureTime();
        }

        private void setVisibility(int visibility) {
            final ProgressBar bar = mProgressRef.get();
            if (null != bar) {
                bar.setVisibility(visibility);
            }
        }

        private void measureTime() {
            final long duration = SystemClock.elapsedRealtime() - mRequestStart;
            Analytics.trackTiming(Analytics.Category.COMIC, duration,
                    Analytics.Action.COMIC_BITMAP_LOAD_DURATION, "ProgressCallback");
        }

    }

}
