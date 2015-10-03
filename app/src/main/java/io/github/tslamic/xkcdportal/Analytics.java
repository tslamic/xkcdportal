package io.github.tslamic.xkcdportal;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import io.github.tslamic.xkcdportal.xkcd.XkcdApplication;

public class Analytics {

    public static final class Category {

        public static final String COMIC = "comic";
        public static final String MENU = "menu";
        public static final String RATE = "rate";
        public static final String SEARCH = "search";
        public static final String EXPLAIN = "explain";
        public static final String TUTORIAL = "tutorial";
        public static final String FAVORITES = "favorites";
        public static final String NAV_STRIP = "nav_strip";
        public static final String EXTERNALS = "external_links";
        public static final String NOTIFICATIONS = "notifications";

    }

    public static final class Action {

        public static final String SEARCH_QUERY = "search_query";
        public static final String TUTORIAL_SHOW = "tutorial_show_first_time";
        public static final String TUTORIAL_PAGE = "tutorial_page";
        public static final String FAVORITES_EMPTY = "empty_favs";
        public static final String MENU_SELECTED = "menu_selected";
        public static final String COMIC_SHOWN = "shown_comic";
        public static final String COMIC_SHARED = "comic_shared";
        public static final String COMIC_BITMAP_LOAD_DURATION = "bitmap_load_duration";
        public static final String COMIC_DOUBLE_TAP = "comic_double_tap";
        public static final String COMIC_LONG_PRESS = "comic_long_press";
        public static final String EXPLAIN_SCRAPE_DURATION = "explain_scrape_duration";
        public static final String EXPLAIN_SCRAPE_FAILED = "explain_scrape_failed";
        public static final String EXPLAIN_FROM_EXTRA_CONTENT = "explain_from_extra_content";
        public static final String EXPLAIN_COMIC = "explain_comic";
        public static final String EXTERNAL_LINK_CLICKED = "external_link_clicked";
        public static final String EXTERNAL_LINK_FAILED = "external_link_failed";
        public static final String NAV_STRIP_TAP = "nav_strip_clicked";
        public static final String RATE_OPEN_GOOGLE_PLAY = "rate_open_google_play";
        public static final String RATE_NOT_NOW = "rate_not_now";
        public static final String NOTIFICATIONS_SHOW = "show_new_comic_notification";
        public static final String NOTIFICATIONS_OPENED = "new_comic_notification_opened";

    }

    public static void trackTiming(String category, long value, String name, String label) {
        final Tracker tracker = XkcdApplication.getTracker();
        tracker.send(new HitBuilders.TimingBuilder()
                .setCategory(category)
                .setValue(value)
                .setVariable(name)
                .setLabel(label)
                .build());
    }

    public static void trackEvent(String category, String action) {
        final Tracker tracker = XkcdApplication.getTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .build());
    }

    public static void trackEvent(String category, String action, String label) {
        final Tracker tracker = XkcdApplication.getTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());
    }

    public static void trackEvent(String category, String action, String label, long value) {
        final Tracker tracker = XkcdApplication.getTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .setValue(value)
                .build());
    }

    private Analytics() {
        throw new UnsupportedOperationException();
    }

}
