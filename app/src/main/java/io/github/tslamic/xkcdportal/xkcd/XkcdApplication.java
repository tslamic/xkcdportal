package io.github.tslamic.xkcdportal.xkcd;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import io.github.tslamic.xkcdportal.BuildConfig;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class XkcdApplication extends Application {

    private static final String GOOGLE_ANALYTICS_TRACKER_ID = "UA-64236815-1";
    private static final int GOOGLE_ANALYTICS_DISPATCH_PERIOD = 1800;

    private static final String REALM_DATABASE = "xkcdportal.realm";
    private static final int REALM_SCHEMA_VERSION = 1;

    private static Tracker sTracker;
    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();

        // Contrary to a popular belief, this doesn't really cause memory leaks.
        // More info here: http://stackoverflow.com/a/987503
        sContext = getApplicationContext();

        final GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(GOOGLE_ANALYTICS_DISPATCH_PERIOD);
        analytics.setAppOptOut(BuildConfig.DEBUG_MODE);

        sTracker = analytics.newTracker(GOOGLE_ANALYTICS_TRACKER_ID);
        sTracker.enableExceptionReporting(true);
        sTracker.enableAdvertisingIdCollection(true);
        sTracker.enableAutoActivityTracking(true);

        // Deletes the old (pre 2.0) database, if present.
        //noinspection deprecation
        Realm.deleteRealmFile(this);

        final RealmConfiguration config = new RealmConfiguration.Builder(this)
                .name(REALM_DATABASE)
                .schemaVersion(REALM_SCHEMA_VERSION)
                .build();
        Realm.setDefaultConfiguration(config);

        if (BuildConfig.DEBUG_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
    }

    public static Tracker getTracker() {
        return sTracker;
    }

    public static Context getContext() {
        if (null == sContext) {
            // This should never happen, but if it does, chances are we can't recover.
            throw new IllegalStateException("application context is null");
        }
        return sContext;
    }

}
