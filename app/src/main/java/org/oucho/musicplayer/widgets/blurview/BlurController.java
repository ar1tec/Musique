package org.oucho.musicplayer.widgets.blurview;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

public interface BlurController {
    float DEFAULT_SCALE_FACTOR = 8f;
    float DEFAULT_BLUR_RADIUS = 16f;

    void destroy();

    void updateBlurViewSize();
    void onDrawEnd(Canvas canvas);
    void setBlurRadius(float radius);
    void setBlurEnabled(boolean enabled);
    void drawBlurredContent(Canvas canvas);
    void setBlurAutoUpdate(boolean enabled);
    void setBlurAlgorithm(BlurAlgorithm algorithm);
    void setWindowBackground(@Nullable Drawable windowBackground);
}
