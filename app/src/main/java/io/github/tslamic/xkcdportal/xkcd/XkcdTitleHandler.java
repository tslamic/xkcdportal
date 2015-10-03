package io.github.tslamic.xkcdportal.xkcd;

/**
 * Handles the title changes between comics.
 */
public interface XkcdTitleHandler {

    void onNewTitle(int comicNumber, String title);

}
