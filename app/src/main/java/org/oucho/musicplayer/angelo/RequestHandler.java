package org.oucho.musicplayer.angelo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import okio.Source;

import static org.oucho.musicplayer.angelo.Utils.checkNotNull;


public abstract class RequestHandler {

    public static final class Result {
        private final Angelo.LoadedFrom loadedFrom;
        private final Bitmap bitmap;
        private final Source source;
        private final int exifOrientation;

        Result(@NonNull Bitmap bitmap, @NonNull Angelo.LoadedFrom loadedFrom) {
            this(checkNotNull(bitmap, "bitmap == null"), null, loadedFrom, 0);
        }

        Result(@NonNull Source source, @NonNull Angelo.LoadedFrom loadedFrom) {
            this(null, checkNotNull(source, "source == null"), loadedFrom, 0);
        }

        Result(@Nullable Bitmap bitmap, @Nullable Source source, @NonNull Angelo.LoadedFrom loadedFrom, int exifOrientation) {
            if ((bitmap != null) == (source != null)) {
                throw new AssertionError();
            }
            this.bitmap = bitmap;
            this.source = source;
            this.loadedFrom = checkNotNull(loadedFrom, "loadedFrom == null");
            this.exifOrientation = exifOrientation;
        }


        @Nullable
        public Bitmap getBitmap() {
            return bitmap;
        }


        @Nullable
        public Source getSource() {
            return source;
        }

        @NonNull
        Angelo.LoadedFrom getLoadedFrom() {
            return loadedFrom;
        }

        int getExifOrientation() {
            return exifOrientation;
        }
    }


    public abstract boolean canHandleRequest(Request data);


    @Nullable
    public abstract Result load(Request request) throws IOException;

    int getRetryCount() {
        return 0;
    }

    boolean shouldRetry() {
        return false;
    }

    static BitmapFactory.Options createBitmapOptions(Request data) {
        final boolean justBounds = data.hasSize();
        final boolean hasConfig = data.config != null;
        BitmapFactory.Options options = null;

        if (justBounds || hasConfig || data.purgeable) {
            options = new BitmapFactory.Options();
            options.inJustDecodeBounds = justBounds;

            if (hasConfig)
                options.inPreferredConfig = data.config;

        }
        return options;
    }

    static boolean requiresInSampleSize(BitmapFactory.Options options) {
        return options != null && options.inJustDecodeBounds;
    }

    static void calculateInSampleSize(int reqWidth, int reqHeight, BitmapFactory.Options options, Request request) {
        calculateInSampleSize(reqWidth, reqHeight, options.outWidth, options.outHeight, options, request);
    }

    static void calculateInSampleSize(int reqWidth, int reqHeight, int width, int height, BitmapFactory.Options options, Request request) {
        int sampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio;
            final int widthRatio;
            if (reqHeight == 0) {
                sampleSize = (int) Math.floor((float) width / (float) reqWidth);
            } else if (reqWidth == 0) {
                sampleSize = (int) Math.floor((float) height / (float) reqHeight);
            } else {
                heightRatio = (int) Math.floor((float) height / (float) reqHeight);
                widthRatio = (int) Math.floor((float) width / (float) reqWidth);
                sampleSize = request.centerInside ? Math.max(heightRatio, widthRatio) : Math.min(heightRatio, widthRatio);
            }
        }
        options.inSampleSize = sampleSize;
        options.inJustDecodeBounds = false;
    }
}
