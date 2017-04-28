package org.oucho.musicplayer.images;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import org.oucho.musicplayer.R;


public class ArtistImageHelper {

    private static Drawable sDefaultArtistImage;
    private static Drawable sDefaultArtistThumb;

    static Drawable getDefaultArtistImage(Context c) {
        if (sDefaultArtistImage == null) {
            sDefaultArtistImage = ContextCompat.getDrawable(c, R.drawable.ic_person_grey_600_48dp);

        }
        //noinspection ConstantConditions
        return sDefaultArtistImage.getConstantState().newDrawable(c.getResources()).mutate();
    }

    public static Drawable getDefaultArtistThumb(Context c) {
        if (sDefaultArtistThumb == null) {
            sDefaultArtistThumb = ContextCompat.getDrawable(c, R.drawable.ic_person_grey_600_48dp);

        }
        //noinspection ConstantConditions
        return sDefaultArtistThumb.getConstantState().newDrawable(c.getResources()).mutate();
    }
}
