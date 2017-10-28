package org.oucho.musicplayer.angelo;

import android.content.Context;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.IOException;
import okio.Okio;
import okio.Source;

import static android.content.ContentResolver.SCHEME_FILE;
import static android.media.ExifInterface.ORIENTATION_NORMAL;
import static android.media.ExifInterface.TAG_ORIENTATION;
import static org.oucho.musicplayer.angelo.Angelo.LoadedFrom.DISK;

class FileRequestHandler extends ContentStreamRequestHandler {

    FileRequestHandler(Context context) {
        super(context);
    }

    @Override public boolean canHandleRequest(org.oucho.musicplayer.angelo.Request data) {
        return SCHEME_FILE.equals(data.uri.getScheme());
    }

    @Override public Result load(Request request) throws IOException {
        Source source = Okio.source(getInputStream(request));
        return new Result(null, source, DISK, getFileExifRotation(request.uri));
    }

    private static int getFileExifRotation(Uri uri) throws IOException {
        ExifInterface exifInterface = new ExifInterface(uri.getPath());
        return exifInterface.getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
    }
}
