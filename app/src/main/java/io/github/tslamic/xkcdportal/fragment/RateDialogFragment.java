package io.github.tslamic.xkcdportal.fragment;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.TextView;

import io.github.tslamic.xkcdportal.Analytics;
import io.github.tslamic.xkcdportal.R;
import io.github.tslamic.xkcdportal.xkcd.XkcdPreferences;

public class RateDialogFragment extends DialogFragment {

    public static final String TAG = "RateDialogFragment.TAG";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();

        final TextView view = (TextView) LayoutInflater.from(context)
                .inflate(R.layout.content_text, null);

        final String appName = getString(R.string.app_name);
        final String title = getString(R.string.rate_title, appName);
        final String content = getString(R.string.rate_desc, appName);
        view.setText(content);

        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_NEGATIVE:
                        Analytics.trackEvent(Analytics.Category.RATE,
                                Analytics.Action.RATE_NOT_NOW);
                        break;
                    case DialogInterface.BUTTON_POSITIVE:
                        openGooglePlay();
                        break;
                    default:
                        break;
                }
            }
        };

        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(view)
                .setNegativeButton(R.string.rate_not_now, listener)
                .setPositiveButton(R.string.rate_rate, listener)
                .create();
    }

    private void openGooglePlay() {
        final Context context = getActivity();
        if (null != context) {
            final String packageName = context.getPackageName();
            final Uri uri = Uri.parse("market://details?id=" + packageName);
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException ignore) {
                return; // Assume the app hasn't been rated.
            }
            XkcdPreferences.INSTANCE.disableRateAppDialog();
            Analytics.trackEvent(Analytics.Category.RATE,
                    Analytics.Action.RATE_OPEN_GOOGLE_PLAY);
        }
    }

}
