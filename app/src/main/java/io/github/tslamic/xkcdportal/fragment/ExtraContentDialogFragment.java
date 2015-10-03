package io.github.tslamic.xkcdportal.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import io.github.tslamic.xkcdportal.Analytics;
import io.github.tslamic.xkcdportal.R;
import io.github.tslamic.xkcdportal.Util;
import io.github.tslamic.xkcdportal.activity.ExplainComicActivity;
import io.github.tslamic.xkcdportal.xkcd.XkcdComic;

public class ExtraContentDialogFragment extends DialogFragment {

    private static final String KEY_ALT_TEXT = "ExtraContentDialogFragment.KEY_ALT_TEXT";
    private static final String KEY_TITLE = "ExtraContentDialogFragment.KEY_TITLE";
    private static final String KEY_NUM = "ExtraContentDialogFragment.KEY_NUM";

    public static final String TAG = "ExtraContentDialogFragment.TAG";

    public static ExtraContentDialogFragment newInstance(XkcdComic comic) {
        final Bundle args = new Bundle(3);
        args.putString(KEY_ALT_TEXT, comic.getAlt());
        args.putString(KEY_TITLE, comic.getSafe_title());
        args.putInt(KEY_NUM, comic.getNum());

        final ExtraContentDialogFragment fragment = new ExtraContentDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.ensureFragmentArgsAreValid(getArguments(), 3);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();

        final View view = LayoutInflater.from(context).inflate(R.layout.extra_content, null);
        final TextView textView = (TextView) view.findViewById(R.id.extra_content_text);
        textView.setText(getExtraText());

        return new AlertDialog.Builder(context)
                .setPositiveButton(R.string.explain_comic_short,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onExplain(context);
                            }
                        })
                .setView(view)
                .create();
    }

    private String getExtraText() {
        String text = getArguments().getString(KEY_ALT_TEXT);
        if (TextUtils.isEmpty(text)) {
            text = getString(R.string.no_alt);
        }
        return text;
    }

    private void onExplain(@NonNull Context context) {
        if (Util.isNetworkAvailable()) {
            final String title = getArguments().getString(KEY_TITLE);
            final int comicNumber = getArguments().getInt(KEY_NUM);
            ExplainComicActivity.start(context, title, comicNumber);
        } else {
            Util.showToast(context, R.string.explain_network_error);
        }
        Analytics.trackEvent(Analytics.Category.EXPLAIN,
                Analytics.Action.EXPLAIN_FROM_EXTRA_CONTENT);
    }

}
