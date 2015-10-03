package io.github.tslamic.xkcdportal.xkcd;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.squareup.otto.Subscribe;

import java.util.Calendar;

import io.github.tslamic.xkcdportal.Analytics;
import io.github.tslamic.xkcdportal.BusProvider;
import io.github.tslamic.xkcdportal.R;
import io.github.tslamic.xkcdportal.activity.MainActivity;
import io.github.tslamic.xkcdportal.event.ComicEvent;

/**
 * Runs in background on Monday, Wednesday and Friday and checks if a new comic is
 * available. If it is, a notification is triggered.
 */
public class XkcdComicCheckService extends Service {

    public static final int NEW_COMIC_NOTIFICATION_ID = 0xBADA55;
    private int mLatestComic = -1;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BusProvider.INSTANCE.register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BusProvider.INSTANCE.unregister(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.MONDAY:
            case Calendar.WEDNESDAY:
            case Calendar.FRIDAY:
                mLatestComic = XkcdPreferences.INSTANCE.getComicCount();
                XkcdEngine.INSTANCE.getCurrentComic();
                break;
            default:
                stopSelf();
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    // Triggered from XkcdEngine.
    @SuppressWarnings("unused")
    @Subscribe
    public void onXkcdEvent(ComicEvent event) {
        if (mLatestComic > 0 && event.isSuccessful()) {
            final int latest = event.comicNumber;
            if (latest > mLatestComic) {
                final Context context = getApplicationContext();
                final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(getString(R.string.notification_new_comic_title))
                        .setContentText(getString(R.string.notification_new_comic_content))
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true);

                final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(MainActivity.getShowLatestComicIntent(context));

                final PendingIntent pendingIntent = stackBuilder
                        .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(pendingIntent);
                final NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                notificationManager.notify(NEW_COMIC_NOTIFICATION_ID, builder.build());
                Analytics.trackEvent(Analytics.Category.NOTIFICATIONS,
                        Analytics.Action.NOTIFICATIONS_SHOW);
            }
        }
        stopSelf();
    }

}
