package org.oucho.musicplayer.images;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;

import org.oucho.musicplayer.R;

public class ArtworkHelper {

    private static final Uri ARTWORK_URI = Uri.parse("content://media/external/audio/albumart");

    private static Drawable sDefaultArtworkDrawable;
    private static Drawable sDefaultThumbDrawable;

    static Uri getArtworkUri() {
        return ARTWORK_URI;
    }

    public static Drawable getDefaultArtworkDrawable(Context c) {
        if (sDefaultArtworkDrawable == null) {
            sDefaultArtworkDrawable = ContextCompat.getDrawable(c, R.drawable.default_artwork);
        }
        //noinspection ConstantConditions
        return sDefaultArtworkDrawable.getConstantState().newDrawable(c.getResources()).mutate();
    }

    public static Drawable getDefaultThumbDrawable(Context c) {
        if (sDefaultThumbDrawable == null) {
            sDefaultThumbDrawable = ContextCompat.getDrawable(c, R.drawable.default_artwork);
        }
        //noinspection ConstantConditions
        return sDefaultThumbDrawable.getConstantState().newDrawable(c.getResources()).mutate();
    }

}
