package io.github.tslamic.xkcdportal.xkcd;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import io.github.tslamic.xkcdportal.Util;
import io.realm.Realm;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * Saves the server response to Realm, returning the comic number associated with the request.
 */
public class XkcdConverter implements Converter {

    @Override
    public Object fromBody(TypedInput body, Type type) throws ConversionException {
        int comicNumber = -1;

        final Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        InputStream in = null;
        try {
            in = body.in();
            final XkcdComic comic = realm.createOrUpdateObjectFromJson(XkcdComic.class, in);
            comicNumber = comic.getNum();
            realm.commitTransaction();
        } catch (IOException e) {
            realm.cancelTransaction();
            throw new ConversionException(e);
        } finally {
            Util.closeQuietly(in);
            realm.close();
        }

        return comicNumber;
    }

    @Override
    public TypedOutput toBody(Object object) {
        throw new UnsupportedOperationException();
    }

}
