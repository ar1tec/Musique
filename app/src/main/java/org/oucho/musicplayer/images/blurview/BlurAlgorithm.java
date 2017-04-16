package org.oucho.musicplayer.images.blurview;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

interface BlurAlgorithm {

    Bitmap blur(Bitmap bitmap, float blurRadius);

    void destroy();

    boolean canModifyBitmap();

    @NonNull
    Bitmap.Config getSupportedBitmapConfig();
}
