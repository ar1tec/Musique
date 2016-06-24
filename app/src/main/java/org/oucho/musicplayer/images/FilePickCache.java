package org.oucho.musicplayer.images;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class FilePickCache {

    private static FilePickCache instance;

    private LruCache<String, Bitmap> mMemoryCache;


    private FilePickCache() {

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.

                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static FilePickCache getInstance() {

        if (instance == null) {

            instance = new FilePickCache();
        }

        return instance;
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
            Log.d("add", key);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        Log.d("get", key);

        return mMemoryCache.get(key);
    }

    public void removeFromCache(String key) {
        Log.d("remove", key);
        mMemoryCache.remove(key);
    }

    public synchronized void clear() {
        mMemoryCache.evictAll();
    }

}
