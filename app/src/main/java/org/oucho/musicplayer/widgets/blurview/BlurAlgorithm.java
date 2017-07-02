package org.oucho.musicplayer.widgets.blurview;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

interface BlurAlgorithm {

    Bitmap blur(Bitmap bitmap, float blurRadius);

    void destroy();

    @SuppressWarnings("SameReturnValue")
    @NonNull
    Bitmap.Config getSupportedBitmapConfig();
}
