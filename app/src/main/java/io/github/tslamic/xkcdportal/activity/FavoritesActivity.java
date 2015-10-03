package io.github.tslamic.xkcdportal.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.github.tslamic.xkcdportal.Analytics;
import io.github.tslamic.xkcdportal.R;
import io.github.tslamic.xkcdportal.Util;
import io.github.tslamic.xkcdportal.fragment.XkcdFragment;
import io.github.tslamic.xkcdportal.xkcd.XkcdComic;
import io.realm.Realm;
import io.realm.RealmBaseAdapter;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

// Ignoring the deprecation warnings on some ActionBar components.
@SuppressWarnings("deprecation")
public class FavoritesActivity extends BaseActivity {

    private final RealmChangeListener mRealmChangeListener = new RealmChangeListener() {
        @Override
        public void onChange() {
            if (mFavorites.isEmpty()) {
                onEmptyFavorites();
            }
        }
    };

    private RealmResults<XkcdComic> mFavorites;
    private FavoritesAdapter mAdapter;
    private Realm mRealm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRealm = Realm.getDefaultInstance();
        mFavorites = mRealm.where(XkcdComic.class)
                .equalTo("favorite", true)
                .findAll();

        if (mFavorites.isEmpty()) {
            onEmptyFavorites();
            return;
        }

        final ActionBar bar = getSupportActionBar();
        bar.setTitle("");

        mAdapter = new FavoritesAdapter(getSupportFragmentManager(), mFavorites);
        final DropdownAdapter dropdown = new DropdownAdapter(this, mFavorites);

        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                bar.setSelectedNavigationItem(position);
            }
        });

        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setListNavigationCallbacks(dropdown, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                mPager.setCurrentItem(itemPosition);
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRealm.addChangeListener(mRealmChangeListener);
        mRealm.addChangeListener(mAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRealm.removeChangeListener(mRealmChangeListener);
        mRealm.removeChangeListener(mAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    private void onEmptyFavorites() {
        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitle(R.string.favorites);
        showNavigationStrip(false);
        showInfo(R.string.no_favorites);
        Analytics.trackEvent(Analytics.Category.FAVORITES,
                Analytics.Action.FAVORITES_EMPTY);
    }

    private void showNavigationStrip(boolean show) {
        final int visibility = show ? View.VISIBLE : View.GONE;
        final View view = findViewById(R.id.navigation_strip);
        if (null != view) {
            view.setVisibility(visibility);
        }
    }

    private static class FavoritesAdapter extends FragmentStatePagerAdapter
            implements RealmChangeListener {

        private final RealmResults<XkcdComic> mFavorites;

        FavoritesAdapter(FragmentManager fm, RealmResults<XkcdComic> realmResults) {
            super(fm);
            mFavorites = realmResults;
        }

        @Override
        public Fragment getItem(int position) {
            final int comicNumber = mFavorites.get(position).getNum();
            return XkcdFragment.newInstance(comicNumber, false);
        }

        @Override
        public int getCount() {
            return mFavorites.size();
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public void onChange() {
            notifyDataSetChanged();
        }

    }

    private static class DropdownAdapter extends RealmBaseAdapter<XkcdComic> {

        DropdownAdapter(Context context, RealmResults<XkcdComic> realmResults) {
            super(context, realmResults, true);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                convertView = LayoutInflater
                        .from(context)
                        .inflate(R.layout.simple_list_item, parent, false);
            }
            final TextView view = (TextView) convertView;
            final XkcdComic comic = getItem(position);
            final String title = Util.createTitle(comic.getNum(), comic.getSafe_title());
            view.setText(title);
            return convertView;
        }

    }

}
