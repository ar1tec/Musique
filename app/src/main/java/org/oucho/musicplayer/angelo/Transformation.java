package org.oucho.musicplayer.angelo;

import android.graphics.Bitmap;

interface Transformation {

    Bitmap transform(Bitmap source);

    String key();
}
