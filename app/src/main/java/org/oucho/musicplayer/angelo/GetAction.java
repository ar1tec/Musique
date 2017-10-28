package org.oucho.musicplayer.angelo;

import android.graphics.Bitmap;

class GetAction extends org.oucho.musicplayer.angelo.Action<Void> {
    GetAction(Angelo angelo, Request data, int memoryPolicy, Object tag, String key) {
        super(angelo, null, data, memoryPolicy, 0, null, key, tag, false);
    }

    @Override
    void complete(Bitmap result, Angelo.LoadedFrom from) {
    }

    @Override
    public void error(Exception e) {
    }
}
