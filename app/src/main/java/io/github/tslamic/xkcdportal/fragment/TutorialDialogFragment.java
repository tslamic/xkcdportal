package io.github.tslamic.xkcdportal.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.viewpagerindicator.CirclePageIndicator;

import io.github.tslamic.xkcdportal.Analytics;
import io.github.tslamic.xkcdportal.R;
import io.github.tslamic.xkcdportal.xkcd.XkcdPreferences;

public class TutorialDialogFragment extends DialogFragment {

    public static final String TAG = "TutorialDialogFragment.TAG";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();

        final View view = LayoutInflater.from(context).inflate(R.layout.help_content, null);
        final ViewPager pager = (ViewPager) view.findViewById(R.id.help_pager);
        pager.setAdapter(new HelpAdapter(context));

        final CirclePageIndicator indicator = (CirclePageIndicator)
                view.findViewById(R.id.help_pager_indicator);
        indicator.setViewPager(pager);
        indicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Analytics.trackEvent(Analytics.Category.TUTORIAL,
                        Analytics.Action.TUTORIAL_PAGE, String.valueOf(position));
            }
        });

        return new AlertDialog.Builder(context)
                .setView(view)
                .create();
    }

    @Override
    public void onResume() {
        super.onResume();
        XkcdPreferences.INSTANCE.setShowTutorialOnFirstLaunch(false);
    }

    private static class HelpAdapter extends PagerAdapter {

        private static final int[] HELP_TXT = {
                R.string.help_swipe,
                R.string.help_content,
                R.string.help_zoom_double_tap,
                R.string.help_zoom,
        };

        private static final int[] HELP_IMG = {
                R.drawable.one_finger_drag_gestureworks,
                R.drawable.one_finger_hold_gestureworks,
                R.drawable.one_finger_double_tap_gestureworks,
                R.drawable.two_finger_scale_gestureworks,
        };

        private final Context mContext;
        private final Picasso mPicasso;

        public HelpAdapter(Context context) {
            mContext = context;
            mPicasso = Picasso.with(mContext);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final View view = LayoutInflater.from(mContext).inflate(R.layout.help, container, false);

            final int txt = HELP_TXT[position];
            final int img = HELP_IMG[position];

            final TextView content = (TextView) view.findViewById(R.id.help_content);
            content.setText(txt);

            final ImageView image = (ImageView) view.findViewById(R.id.help_image);
            mPicasso.load(img).into(image);

            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return HELP_TXT.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

    }

}
