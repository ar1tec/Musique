package org.oucho.musicplayer.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapHelper {

    private static final String TAG = "BitmapHelper";

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decode(InputStream in, int reqWidth, int reqHeight) throws IOException {
        BufferedInputStream inputStream = new BufferedInputStream(in);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        inputStream.mark(64 * 1024);
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.reset();
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeStream(inputStream, null, options);
    }


    // convert from byte array to bitmap
    public static Bitmap byteToBitmap(byte[] image) {

        try {
            return BitmapFactory.decodeByteArray(image, 0, image.length);
        } catch (NullPointerException e) {
            return null;
        }
    }


    // convert from bitmap to string
    public static String bitmapToString(Bitmap image) {

        byte[] two = getBytes(image);

        try {
            return Base64.encodeToString(two, Base64.DEFAULT);
        } catch (NullPointerException e) {
            return null;
        }
    }

    // convert from bitmap to byte array
    public static byte[] getBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
            return stream.toByteArray();
        } catch (NullPointerException e) {
            Log.w(TAG, "Bitmap to byte[] conversion: " + e);
            return null;
        }
    }

}
