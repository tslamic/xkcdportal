package io.github.tslamic.xkcdportal.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.squareup.otto.Subscribe;

import io.github.tslamic.xkcdportal.Analytics;
import io.github.tslamic.xkcdportal.BusProvider;
import io.github.tslamic.xkcdportal.R;
import io.github.tslamic.xkcdportal.Util;
import io.github.tslamic.xkcdportal.event.ComicEvent;
import io.github.tslamic.xkcdportal.event.ConnectionEvent;
import io.github.tslamic.xkcdportal.fragment.RateDialogFragment;
import io.github.tslamic.xkcdportal.fragment.XkcdFragment;
import io.github.tslamic.xkcdportal.xkcd.XkcdEngine;
import io.github.tslamic.xkcdportal.xkcd.XkcdPreferences;
import io.github.tslamic.xkcdportal.xkcd.XkcdTitleHandler;

public class MainActivity extends BaseActivity implements XkcdTitleHandler {

    private static final String KEY_IGNORE_LAST_POSITION = "XkcdActivity.KEY_IGNORE_LAST_POSITION";
    private static final String KEY_LAST_POSITION = "XkcdActivity.KEY_LOCAL_LAST_POSITION";

    private MenuItem mSearchMenuItem;
    private XkcdAdapter mAdapter;
    private int mCurrentComic = -1;

    /**
     * Returns an Intent ignoring the last comic user was viewing. Instead, show the latest comic.
     */
    public static Intent getShowLatestComicIntent(@NonNull Context context) {
        final Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(KEY_IGNORE_LAST_POSITION, true);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!ignoreLastPosition()) {
            if (null == savedInstanceState) {
                mCurrentComic = XkcdPreferences.INSTANCE.getLastSeenComic();
            } else {
                mCurrentComic = savedInstanceState.getInt(KEY_LAST_POSITION, -1);
            }
        }

        // These two dialogs are never shown at the same time.
        showTutorialOnFirstLaunch();
        showRateAppDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BusProvider.INSTANCE.register(this);

        // Each time MainActivity is resumed, check if a new comic is available.
        // Since this is the main interaction point, it shouldn't happen often.
        XkcdEngine.INSTANCE.getCurrentComic();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BusProvider.INSTANCE.unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_LAST_POSITION, mCurrentComic);
    }

    @Override
    protected void onDestroy() {
        XkcdPreferences.INSTANCE.setLastSeenComic(mCurrentComic);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        // Search is only visible if network is available.
        final SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem item = menu.findItem(R.id.search);
        item.setVisible(Util.isNetworkAvailable());

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setQueryHint(getString(R.string.menu_comic_number));
        searchView.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        mSearchMenuItem = item;

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
            return; // This method should only be invoked through search.
        }

        final String comicNumber = intent.getStringExtra(SearchManager.QUERY);
        if (TextUtils.isEmpty(comicNumber)) {
            return;
        }

        // Show a generic error message if the latest comic count is <= 0. This should only happen
        // if the user is trying the app for the first time and is without internet.
        final int latest = mAdapter.getCount();
        if (latest <= 0) {
            Util.showToast(this, R.string.comic_search_error);
            return;
        }

        int number = -1;
        try {
            number = Integer.parseInt(comicNumber);
        } catch (NumberFormatException ignore) {
        }
        if (number < 1 || number > latest) {
            Util.showToast(this, getString(R.string.comic_chooser_error, latest));
            return;
        }

        mPager.setCurrentItem(number - 1);
        collapseSearchMenuItem();
        Analytics.trackEvent(Analytics.Category.SEARCH,
                Analytics.Action.SEARCH_QUERY, comicNumber, number);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.menu_show_favorites == item.getItemId()) {
            showFavorites();
            Analytics.trackEvent(Analytics.Category.MENU,
                    Analytics.Action.MENU_SELECTED, "favorites");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Triggered from XkcdEngine.
    @SuppressWarnings("unused")
    @Subscribe
    public void onComicEvent(ComicEvent event) {
        if (!event.isLatestComic) {
            return;
        }
        final int latest = event.comicNumber;
        if (latest <= 0) {
            showInfo(R.string.connection_error); // There's nothing we can really show.
        } else {
            if (event.isFailed()) {
                showInfo(R.string.connection_error_limited_content, getHideInfoListener());
            }
            if (null == mAdapter) {
                createXkcdAdapter(latest);
            } else {
                mAdapter.update(latest);
            }
        }
    }

    // Triggered from ConnectionReceiver.
    @SuppressWarnings("unused")
    @Subscribe
    public void onConnectionEvent(ConnectionEvent event) {
        boolean showSearch = false;
        switch (event) {
            case OFFLINE:
                showInfo(R.string.connection_error_limited_content, getHideInfoListener());
                break;
            case ONLINE:
                XkcdEngine.INSTANCE.getCurrentComic(); // Refresh.
                hideInfo();
                showSearch = true;
                break;
        }
        if (null != mSearchMenuItem) {
            mSearchMenuItem.setVisible(showSearch);
        }
    }

    @Override
    public void onNewTitle(final int comicNumber, final String title) {
        if (null != mPager) {
            mPager.post(new Runnable() {
                @Override
                public void run() {
                    final int currentComicNumber = mCurrentComic + 1;
                    if (currentComicNumber == comicNumber) {
                        setTitle(Util.createTitle(comicNumber, title));
                    }
                }
            });
        }
    }

    private void createXkcdAdapter(final int latest) {
        mAdapter = new XkcdAdapter(getSupportFragmentManager(), latest);
        mPager.setAdapter(mAdapter);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mCurrentComic = position;
                collapseSearchMenuItem();
                Analytics.trackEvent(Analytics.Category.COMIC,
                        Analytics.Action.COMIC_SHOWN, String.valueOf(position + 1));
            }
        });

        final int position = mCurrentComic != -1 ? mCurrentComic : latest;
        mPager.setCurrentItem(position);
    }

    private void showFavorites() {
        final Intent intent = new Intent(this, FavoritesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private View.OnClickListener getHideInfoListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideInfo();
            }
        };
    }

    private void collapseSearchMenuItem() {
        if (null != mSearchMenuItem) {
            MenuItemCompat.collapseActionView(mSearchMenuItem);
        }
    }

    private boolean ignoreLastPosition() {
        final Intent intent = getIntent();
        if (null != intent && intent.getBooleanExtra(KEY_IGNORE_LAST_POSITION, false)) {
            intent.removeExtra(KEY_IGNORE_LAST_POSITION); // Ignore it one time only!
            Analytics.trackEvent(Analytics.Category.NOTIFICATIONS,
                    Analytics.Action.NOTIFICATIONS_OPENED);
            return true;
        }
        return false;
    }

    private void showTutorialOnFirstLaunch() {
        if (XkcdPreferences.INSTANCE.showTutorialOnFirstLaunch()) {
            showTutorial();
            Analytics.trackEvent(Analytics.Category.TUTORIAL,
                    Analytics.Action.TUTORIAL_SHOW);
        }
    }

    private void showRateAppDialog() {
        if (XkcdPreferences.INSTANCE.showRateAppDialog()) {
            final FragmentManager manager = getSupportFragmentManager();
            if (null == manager.findFragmentByTag(RateDialogFragment.TAG)) {
                final RateDialogFragment dialog = new RateDialogFragment();
                dialog.show(manager, RateDialogFragment.TAG);
            }
        }
    }

    private static class XkcdAdapter extends FragmentStatePagerAdapter {

        private int mComicCount;

        private XkcdAdapter(FragmentManager fm, int comicsCount) {
            super(fm);
            mComicCount = comicsCount;
        }

        @Override
        public Fragment getItem(int position) {
            // This adapter is 0-based, but xkcd comics are not.
            // Bump position by one to get the appropriate comic number.
            return XkcdFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return mComicCount;
        }

        void update(int count) {
            if (mComicCount != count) {
                mComicCount = count;
                notifyDataSetChanged();
            }
        }

    }

}
