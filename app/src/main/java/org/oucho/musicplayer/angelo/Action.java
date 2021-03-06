package org.oucho.musicplayer.angelo;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import static org.oucho.musicplayer.angelo.Angelo.Priority;

abstract class Action<T> {
    static class RequestWeakReference<M> extends WeakReference<M> {
        final Action action;

        RequestWeakReference(Action action, M referent, ReferenceQueue<? super M> q) {
            super(referent, q);
            this.action = action;
        }
    }

    final Angelo angelo;
    private final Request request;
    final WeakReference<T> target;
    final boolean noFade;
    final int memoryPolicy;
    final int errorResId;
    final Drawable errorDrawable;
    private final String key;
    private final Object tag;

    private boolean cancelled;

    Action(Angelo angelo, T target, Request request, int memoryPolicy, int errorResId, Drawable errorDrawable, String key, Object tag, boolean noFade) {
        this.angelo = angelo;
        this.request = request;
        this.target =
                target == null ? null : new RequestWeakReference<>(this, target, angelo.referenceQueue);
        this.memoryPolicy = memoryPolicy;
        this.noFade = noFade;
        this.errorResId = errorResId;
        this.errorDrawable = errorDrawable;
        this.key = key;
        this.tag = (tag != null ? tag : this);
    }

    abstract void complete(Bitmap result, Angelo.LoadedFrom from);

    abstract void error(Exception e);

    void cancel() {
        cancelled = true;
    }

    Request getRequest() {
        return request;
    }

    T getTarget() {
        return target == null ? null : target.get();
    }

    String getKey() {
        return key;
    }

    boolean isCancelled() {
        return cancelled;
    }

    int getMemoryPolicy() {
        return memoryPolicy;
    }

    Angelo getAngelo() {
        return angelo;
    }

    Priority getPriority() {
        return request.priority;
    }

    Object getTag() {
        return tag;
    }
}
