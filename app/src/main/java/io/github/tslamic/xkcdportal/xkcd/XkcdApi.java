package io.github.tslamic.xkcdportal.xkcd;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;

public interface XkcdApi {

    String ENDPOINT = "http://xkcd.com/";

    @GET("/info.0.json")
    void getCurrentComic(Callback<Integer> comic);

    @GET("/{num}/info.0.json")
    void getComic(@Path("num") int comicNumber, Callback<Integer> callback);

}
