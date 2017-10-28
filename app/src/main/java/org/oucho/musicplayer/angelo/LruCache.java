package org.oucho.musicplayer.angelo;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.oucho.musicplayer.angelo.Utils.KEY_SEPARATOR;


public class LruCache implements Cache {
    private final LinkedHashMap<String, Bitmap> map;
    private final int maxSize;

    private int size;


    LruCache(@NonNull Context context) {
        this(Utils.calculateMemoryCacheSize(context));
    }


    private LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be positive.");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<>(0, 0.75f, true);
    }

    @Override
    public Bitmap get(@NonNull String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        Bitmap mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                return mapValue;
            }
        }

        return null;
    }

    @Override
    public void set(@NonNull String key, @NonNull Bitmap bitmap) {
        if (key == null || bitmap == null) {
            throw new NullPointerException("key == null || bitmap == null");
        }

        int addedSize = Utils.getBitmapBytes(bitmap);
        if (addedSize > maxSize) {
            return;
        }

        synchronized (this) {
            size += addedSize;
            Bitmap previous = map.put(key, bitmap);
            if (previous != null) {
                size -= Utils.getBitmapBytes(previous);
            }
        }

        trimToSize(maxSize);
    }

    private void trimToSize(int maxSize) {
        while (true) {
            String key;
            Bitmap value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(
                            getClass().getName() + ".sizeOf() is reporting inconsistent results!");
                }

                if (size <= maxSize || map.isEmpty()) {
                    break;
                }

                Map.Entry<String, Bitmap> toEvict = map.entrySet().iterator().next();
                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= Utils.getBitmapBytes(value);
            }
        }
    }

    @Override
    public final synchronized void clearKeyUri(String uri) {
        int uriLength = uri.length();
        for (Iterator<Map.Entry<String, Bitmap>> i = map.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, Bitmap> entry = i.next();
            String key = entry.getKey();
            Bitmap value = entry.getValue();
            int newlineIndex = key.indexOf(KEY_SEPARATOR);
            if (newlineIndex == uriLength && key.substring(0, newlineIndex).equals(uri)) {
                i.remove();
                size -= Utils.getBitmapBytes(value);
            }
        }
    }

}
