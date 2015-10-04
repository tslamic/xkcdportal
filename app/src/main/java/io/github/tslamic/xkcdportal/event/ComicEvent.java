package io.github.tslamic.xkcdportal.event;

public final class ComicEvent {

    private final boolean isSuccessful;

    public final int comicNumber;
    public final boolean isLatestComic;

    private ComicEvent(boolean isSuccessful, int latestComicCount, boolean isLatestComic) {
        this.isSuccessful = isSuccessful;
        this.comicNumber = latestComicCount;
        this.isLatestComic = isLatestComic;
    }

    public static ComicEvent asSuccess(int comicNumber, boolean isLatestComic) {
        return new ComicEvent(true, comicNumber, isLatestComic);
    }

    public static ComicEvent asFailure(int comicNumber, boolean isLatestComic) {
        return new ComicEvent(false, comicNumber, isLatestComic);
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public boolean isFailed() {
        return !isSuccessful;
    }

}
