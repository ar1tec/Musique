package org.oucho.musicplayer.angelo;

import android.content.ContentResolver;
import android.content.Context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import okio.Okio;
import okio.Source;

import static android.content.ContentResolver.SCHEME_CONTENT;
import static org.oucho.musicplayer.angelo.Angelo.LoadedFrom.DISK;

class ContentStreamRequestHandler extends RequestHandler {

    final Context context;

    ContentStreamRequestHandler(Context context) {
        this.context = context;
    }

    @Override
    public boolean canHandleRequest(org.oucho.musicplayer.angelo.Request data) {
        return SCHEME_CONTENT.equals(data.uri.getScheme());
    }

    @Override
    public Result load(Request request) throws IOException {
        Source source = Okio.source(getInputStream(request));
        return new Result(source, DISK);
    }

    InputStream getInputStream(Request request) throws FileNotFoundException {
        ContentResolver contentResolver = context.getContentResolver();
        return contentResolver.openInputStream(request.uri);
    }
}
