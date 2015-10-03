package io.github.tslamic.xkcdportal.activity;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.Random;

import io.github.tslamic.xkcdportal.Analytics;
import io.github.tslamic.xkcdportal.R;
import io.github.tslamic.xkcdportal.Util;
import io.github.tslamic.xkcdportal.fragment.TutorialDialogFragment;
import io.github.tslamic.xkcdportal.ui.ZoomOutPageTransformer;
import io.github.tslamic.xkcdportal.xkcd.XkcdComicCheckService;

// No need to register, it only serves as a base class.
@SuppressLint("Registered")
class BaseActivity extends AppCompatActivity {

    private static final long INEXACT_COMIC_CHECK_INTERVAL = AlarmManager.INTERVAL_HALF_DAY;
    private static final Random RANDOM = new Random();

    protected ViewPager mPager;
    private TextView mInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager_content);

        mInfo = (TextView) findViewById(R.id.info);
        mPager = (ViewPager) findViewById(R.id.pager);
        if (Util.isHoneycombOrAbove()) {
            mPager.setPageTransformer(true, new ZoomOutPageTransformer());
        }

        final NavigationStripListener listener = new NavigationStripListener();
        final int[] navigationButtons = {
                R.id.nav_first,
                R.id.nav_prev,
                R.id.nav_random,
                R.id.nav_next,
                R.id.nav_last,
        };
        for (int id : navigationButtons) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setInexactBackgroundComicChecks(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        setInexactBackgroundComicChecks(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_base, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.menu_tutorial:
                showTutorial();
                Analytics.trackEvent(Analytics.Category.MENU,
                        Analytics.Action.MENU_SELECTED, "show_tutorial");
                return true;
            case R.id.menu_about:
                showAbout();
                Analytics.trackEvent(Analytics.Category.MENU,
                        Analytics.Action.MENU_SELECTED, "about");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void showInfo(int stringRes) {
        showInfo(stringRes, null);
    }

    protected void showInfo(int stringRes, View.OnClickListener listener) {
        mInfo.setText(stringRes);
        mInfo.setOnClickListener(listener);
        mInfo.setVisibility(View.VISIBLE);
    }

    protected void hideInfo() {
        mInfo.setVisibility(View.GONE);
    }

    protected void showTutorial() {
        final FragmentManager manager = getSupportFragmentManager();
        if (null == manager.findFragmentByTag(TutorialDialogFragment.TAG)) {
            final TutorialDialogFragment dialog = new TutorialDialogFragment();
            dialog.show(manager, TutorialDialogFragment.TAG);
        }
    }

    private void showAbout() {
        final Intent intent = new Intent(this, AboutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void setInexactBackgroundComicChecks(boolean enable) {
        final AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        final Intent intent = new Intent(this, XkcdComicCheckService.class);
        final int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        final PendingIntent pending = PendingIntent.getService(this, 0, intent, flags);
        if (enable) {
            final long interval = INEXACT_COMIC_CHECK_INTERVAL;
            alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, interval, interval, pending);
        } else {
            alarm.cancel(pending);
        }
    }

    private class NavigationStripListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (null == mPager) {
                return;
            }
            final PagerAdapter adapter = mPager.getAdapter();
            if (null == adapter) {
                return;
            }
            final int position;
            final String key;
            switch (v.getId()) {
                case R.id.nav_first:
                    position = 0;
                    key = "first";
                    break;
                case R.id.nav_prev:
                    position = mPager.getCurrentItem() - 1;
                    key = "prev";
                    break;
                case R.id.nav_random:
                    position = RANDOM.nextInt(adapter.getCount());
                    key = "random";
                    break;
                case R.id.nav_next:
                    position = mPager.getCurrentItem() + 1;
                    key = "next";
                    break;
                case R.id.nav_last:
                    position = adapter.getCount() - 1;
                    key = "last";
                    break;
                default:
                    position = -1;
                    key = "invalid";
                    break;
            }
            mPager.setCurrentItem(position, true);
            Analytics.trackEvent(Analytics.Category.NAV_STRIP,
                    Analytics.Action.NAV_STRIP_TAP, key);
        }

    }

}
