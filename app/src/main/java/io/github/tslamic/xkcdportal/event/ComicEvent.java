package io.github.tslamic.xkcdportal.event;

public final class ComicEvent {

    private static final int XKCD_EVENT_FAILURE = 0;
    private static final int XKCD_EVENT_SUCCESS = 1;

    private final int type;
    public final int comicNumber;
    public final boolean isLatestComic;

    private ComicEvent(int type, int latestComicCount, boolean isLatestComic) {
        this.type = type;
        this.comicNumber = latestComicCount;
        this.isLatestComic = isLatestComic;
    }

    public static ComicEvent asSuccess(int comicNumber, boolean isLatestComic) {
        return new ComicEvent(XKCD_EVENT_SUCCESS, comicNumber, isLatestComic);
    }

    public static ComicEvent asFailure(int comicNumber, boolean isLatestComic) {
        return new ComicEvent(XKCD_EVENT_FAILURE, comicNumber, isLatestComic);
    }

    public boolean isSuccessful() {
        return XKCD_EVENT_SUCCESS == type;
    }

    public boolean isFailed() {
        return XKCD_EVENT_SUCCESS != type;
    }

}
