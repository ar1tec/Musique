package org.oucho.musicplayer.angelo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;

import static android.content.ContentResolver.SCHEME_ANDROID_RESOURCE;
import static org.oucho.musicplayer.angelo.Angelo.LoadedFrom.DISK;

class ResourceRequestHandler extends org.oucho.musicplayer.angelo.RequestHandler {
    private final Context context;


    ResourceRequestHandler(Context context) {
        this.context = context;
    }

    @Override public boolean canHandleRequest(org.oucho.musicplayer.angelo.Request data) {
        return data.resourceId != 0 || SCHEME_ANDROID_RESOURCE.equals(data.uri.getScheme());

    }

    @Override public Result load(Request request) throws IOException {
        Resources res = org.oucho.musicplayer.angelo.Utils.getResources(context, request);
        int id = org.oucho.musicplayer.angelo.Utils.getResourceId(res, request);
        return new Result(decodeResource(res, id, request), DISK);
    }

    private static Bitmap decodeResource(Resources resources, int id, Request data) {
        final BitmapFactory.Options options = createBitmapOptions(data);
        if (requiresInSampleSize(options)) {
            BitmapFactory.decodeResource(resources, id, options);
            calculateInSampleSize(data.targetWidth, data.targetHeight, options, data);
        }
        return BitmapFactory.decodeResource(resources, id, options);
    }
}
