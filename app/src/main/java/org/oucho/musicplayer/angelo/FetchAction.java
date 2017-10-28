package org.oucho.musicplayer.angelo;

import android.graphics.Bitmap;

class FetchAction extends org.oucho.musicplayer.angelo.Action<Object> {

    private final Object target;
    private org.oucho.musicplayer.angelo.Callback callback;

    FetchAction(Angelo angelo, Request data, int memoryPolicy, Object tag, String key, Callback callback) {
        super(angelo, null, data, memoryPolicy, 0, null, key, tag, false);
        this.target = new Object();
        this.callback = callback;
    }

    @Override void complete(Bitmap result, Angelo.LoadedFrom from) {
        if (callback != null) {
            callback.onSuccess();
        }
    }

    @Override void error(Exception e) {
        if (callback != null) {
            callback.onError();
        }
    }

    @Override void cancel() {
        super.cancel();
        callback = null;
    }

    @Override Object getTarget() {
        return target;
    }
}
