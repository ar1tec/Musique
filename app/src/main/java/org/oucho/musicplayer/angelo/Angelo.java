package org.oucho.musicplayer.angelo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static org.oucho.musicplayer.angelo.Action.RequestWeakReference;
import static org.oucho.musicplayer.angelo.Dispatcher.HUNTER_BATCH_COMPLETE;
import static org.oucho.musicplayer.angelo.Dispatcher.REQUEST_BATCH_RESUME;
import static org.oucho.musicplayer.angelo.Dispatcher.REQUEST_GCED;
import static org.oucho.musicplayer.angelo.MemoryPolicy.shouldReadFromMemoryCache;
import static org.oucho.musicplayer.angelo.Utils.THREAD_LEAK_CLEANING_MS;
import static org.oucho.musicplayer.angelo.Utils.THREAD_PREFIX;
import static org.oucho.musicplayer.angelo.Utils.checkMain;

/**
 * Image downloading, transformation, and caching manager.
 * <p>
 * Use  for the global singleton instance
 * or construct your own instance with {@link Builder}.
 */
public class Angelo {

    /** Callbacks for Angelo events. */
    public interface Listener {
        /**
         * Invoked when an image has failed to load. This is useful for reporting image failures to a
         * remote analytics service, for example.
         */
        void onImageLoadFailed(Angelo angelo, Uri uri, Exception exception);
    }

    /**
     * A transformer that is called immediately before every request is submitted. This can be used to
     * modify any information about a request.
     * <p>
     * For example, if you use a CDN you can change the hostname for the image based on the current
     * location of the user in order to get faster download speeds.
     * <p>
     * <b>NOTE:</b> This is a beta feature. The API is subject to change in a backwards incompatible
     * way at any time.
     */
    public interface RequestTransformer {
        /**
         * Transform a request before it is submitted to be processed.
         *
         * @return The original request or a new request to replace it. Must not be null.
         */
        Request transformRequest(Request request);

        /** A {@link RequestTransformer} which returns the original request. */
        RequestTransformer IDENTITY = request -> request;
    }

    /**
     * The priority of a request.
     *
     * @see RequestCreator#priority(Priority)
     */
    public enum Priority {
        LOW,
        NORMAL
    }

    static final String TAG = "Angelo";
    static final Handler HANDLER = new Handler(Looper.getMainLooper()) {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case HUNTER_BATCH_COMPLETE: {
                    @SuppressWarnings("unchecked") List<BitmapHunter> batch = (List<BitmapHunter>) msg.obj;
                    //noinspection ForLoopReplaceableByForEach
                    for (int i = 0, n = batch.size(); i < n; i++) {
                        BitmapHunter hunter = batch.get(i);
                        hunter.angelo.complete(hunter);
                    }
                    break;
                }
                case REQUEST_GCED: {
                    Action action = (Action) msg.obj;

                    action.angelo.cancelExistingRequest(action.getTarget());
                    break;
                }
                case REQUEST_BATCH_RESUME:
                    @SuppressWarnings("unchecked") List<Action> batch = (List<Action>) msg.obj;
                    //noinspection ForLoopReplaceableByForEach
                    for (int i = 0, n = batch.size(); i < n; i++) {
                        Action action = batch.get(i);
                        action.angelo.resumeAction(action);
                    }
                    break;
                default:
                    throw new AssertionError("Unknown handler message received: " + msg.what);
            }
        }
    };

    private static volatile Angelo singleton = null;

    private final Listener listener;
    private final RequestTransformer requestTransformer;
    private final List<RequestHandler> requestHandlers;

    final Context context;
    final Dispatcher dispatcher;
    final Cache cache;
    private final Map<Object, Action> targetToAction;
    private final Map<ImageView, DeferredRequestCreator> targetToDeferredRequestCreator;
    final ReferenceQueue<Object> referenceQueue;
    final Bitmap.Config defaultBitmapConfig;

    boolean indicatorsEnabled;

    private Angelo(Context context, Dispatcher dispatcher, Cache cache, Listener listener,
                   RequestTransformer requestTransformer, List<RequestHandler> extraRequestHandlers,
                   Bitmap.Config defaultBitmapConfig, boolean indicatorsEnabled) {
        this.context = context;
        this.dispatcher = dispatcher;
        this.cache = cache;
        this.listener = listener;
        this.requestTransformer = requestTransformer;
        this.defaultBitmapConfig = defaultBitmapConfig;

        int builtInHandlers = 7; // Adjust this as internal handlers are added or removed.
        int extraCount = (extraRequestHandlers != null ? extraRequestHandlers.size() : 0);
        List<RequestHandler> allRequestHandlers = new ArrayList<>(builtInHandlers + extraCount);

        // ResourceRequestHandler needs to be the first in the list to avoid
        // forcing other RequestHandlers to perform null checks on request.uri
        // to cover the (request.resourceId != 0) case.
        allRequestHandlers.add(new ResourceRequestHandler(context));
        if (extraRequestHandlers != null) {
            allRequestHandlers.addAll(extraRequestHandlers);
        }
        allRequestHandlers.add(new MediaStoreRequestHandler(context));
        allRequestHandlers.add(new ContentStreamRequestHandler(context));
        allRequestHandlers.add(new AssetRequestHandler(context));
        allRequestHandlers.add(new FileRequestHandler(context));
        requestHandlers = Collections.unmodifiableList(allRequestHandlers);

        this.targetToAction = new WeakHashMap<>();
        this.targetToDeferredRequestCreator = new WeakHashMap<>();
        this.indicatorsEnabled = indicatorsEnabled;
        this.referenceQueue = new ReferenceQueue<>();
        CleanupThread cleanupThread = new CleanupThread(referenceQueue, HANDLER);
        cleanupThread.start();
    }

    /** Cancel any existing requests for the specified target {@link ImageView}. */
    public void cancelRequest(@NonNull ImageView view) {
        // checkMain() is called from cancelExistingRequest()
        cancelExistingRequest(view);
    }

    /** Cancel any existing requests for the specified {@link Target} instance. */
    public void cancelRequest(@NonNull Target target) {
        // checkMain() is called from cancelExistingRequest()
        cancelExistingRequest(target);
    }

    /**
     * Start an image request using the specified URI.
     * <p>
     * Passing {@code null} as a {@code uri} will not trigger any request but will set a placeholder,
     * if one is specified.
     *
     * @see #load(File)
     * @see #load(String)
     * @see #load(int)
     */
    public RequestCreator load(@Nullable Uri uri) {
        return new RequestCreator(this, uri, 0);
    }

    /**
     * Start an image request using the specified path. This is a convenience method for calling
     * {@link #load(Uri)}.
     * <p>
     * This path may be a remote URL, file resource (prefixed with {@code file:}), content resource
     * (prefixed with {@code content:}), or android resource (prefixed with {@code
     * android.resource:}.
     * <p>
     * Passing {@code null} as a {@code path} will not trigger any request but will set a
     * placeholder, if one is specified.
     *
     * @see #load(Uri)
     * @see #load(File)
     * @see #load(int)
     * @throws IllegalArgumentException if {@code path} is empty or blank string.
     */
    public RequestCreator load(@Nullable String path) {
        if (path == null) {
            return new RequestCreator(this, null, 0);
        }
        if (path.trim().length() == 0) {
            throw new IllegalArgumentException("Path must not be empty.");
        }
        return load(Uri.parse(path));
    }

    /**
     * Start an image request using the specified image file. This is a convenience method for
     * calling {@link #load(Uri)}.
     * <p>
     * Passing {@code null} as a {@code file} will not trigger any request but will set a
     * placeholder, if one is specified.
     * <p>
     * Equivalent to calling {@link #load(Uri) load(Uri.fromFile(file))}.
     *
     * @see #load(Uri)
     * @see #load(String)
     * @see #load(int)
     */
    public RequestCreator load(@NonNull File file) {
        return load(Uri.fromFile(file));
    }

    /**
     * Start an image request using the specified drawable resource ID.
     *
     * @see #load(Uri)
     * @see #load(String)
     * @see #load(File)
     */
    public RequestCreator load(@DrawableRes int resourceId) {
        if (resourceId == 0) {
            throw new IllegalArgumentException("Resource ID must not be zero.");
        }
        return new RequestCreator(this, null, resourceId);
    }

    /**
     * Invalidate all memory cached images for the specified {@code uri}.
     *
     * @see #invalidate(String)
     * @see #invalidate(File)
     */
    public void invalidate(@Nullable Uri uri) {
        if (uri != null) {
            cache.clearKeyUri(uri.toString());
        }
    }

    /**
     * Invalidate all memory cached images for the specified {@code path}. You can also pass a
     * {@linkplain RequestCreator#stableKey stable key}.
     *
     * @see #invalidate(Uri)
     * @see #invalidate(File)
     */
    public void invalidate(@Nullable String path) {
        if (path != null) {
            invalidate(Uri.parse(path));
        }
    }

    /**
     * Invalidate all memory cached images for the specified {@code file}.
     *
     * @see #invalidate(Uri)
     * @see #invalidate(String)
     */
    public void invalidate(@NonNull File file) {
        invalidate(Uri.fromFile(file));
    }

    /** Toggle whether to display debug indicators on images. */
    @SuppressWarnings("UnusedDeclaration") public void setIndicatorsEnabled(boolean enabled) {
        indicatorsEnabled = enabled;
    }

    /** {@code true} if debug indicators should are displayed on images. */
    @SuppressWarnings("UnusedDeclaration") public boolean areIndicatorsEnabled() {
        return indicatorsEnabled;
    }

    List<RequestHandler> getRequestHandlers() {
        return requestHandlers;
    }

    Request transformRequest(Request request) {
        Request transformed = requestTransformer.transformRequest(request);
        if (transformed == null) {
            throw new IllegalStateException("Request transformer "
                    + requestTransformer.getClass().getCanonicalName()
                    + " returned null for "
                    + request);
        }
        return transformed;
    }

    void defer(ImageView view, DeferredRequestCreator request) {
        // If there is already a deferred request, cancel it.
        if (targetToDeferredRequestCreator.containsKey(view)) {
            cancelExistingRequest(view);
        }
        targetToDeferredRequestCreator.put(view, request);
    }

    void enqueueAndSubmit(Action action) {
        Object target = action.getTarget();
        if (target != null && targetToAction.get(target) != action) {
            // This will also check we are on the main thread.
            cancelExistingRequest(target);
            targetToAction.put(target, action);
        }
        submit(action);
    }

    void submit(Action action) {
        dispatcher.dispatchSubmit(action);
    }

    Bitmap quickMemoryCacheCheck(String key) {
        return cache.get(key);
    }

    private void complete(BitmapHunter hunter) {
        Action single = hunter.getAction();
        List<Action> joined = hunter.getActions();

        boolean hasMultiple = joined != null && !joined.isEmpty();
        boolean shouldDeliver = single != null || hasMultiple;

        if (!shouldDeliver) {
            return;
        }

        Uri uri = hunter.getData().uri;
        Exception exception = hunter.getException();
        Bitmap result = hunter.getResult();
        LoadedFrom from = hunter.getLoadedFrom();

        if (single != null) {
            deliverAction(result, from, single, exception);
        }

        if (hasMultiple) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, n = joined.size(); i < n; i++) {
                Action join = joined.get(i);
                deliverAction(result, from, join, exception);
            }
        }

        if (listener != null && exception != null) {
            listener.onImageLoadFailed(this, uri, exception);
        }
    }

    private void resumeAction(Action action) {
        Bitmap bitmap = null;
        if (shouldReadFromMemoryCache(action.memoryPolicy)) {
            bitmap = quickMemoryCacheCheck(action.getKey());
        }

        if (bitmap != null) {
            // Resumed action is cached, complete immediately.
            deliverAction(bitmap, LoadedFrom.MEMORY, action, null);
        } else {
            // Re-submit the action to the executor.
            enqueueAndSubmit(action);
        }
    }

    private void deliverAction(Bitmap result, LoadedFrom from, Action action, Exception e) {
        if (action.isCancelled()) {
            return;
        }
        if (result != null) {
            if (from == null) {
                throw new AssertionError("LoadedFrom cannot be null.");
            }
            action.complete(result, from);
        } else {
            action.error(e);
        }
    }

    private void cancelExistingRequest(Object target) {
        checkMain();
        Action action = targetToAction.remove(target);
        if (action != null) {
            action.cancel();
            dispatcher.dispatchCancel(action);
        }
        if (target instanceof ImageView) {
            ImageView targetImageView = (ImageView) target;
            DeferredRequestCreator deferredRequestCreator = targetToDeferredRequestCreator.remove(targetImageView);
            if (deferredRequestCreator != null) {
                deferredRequestCreator.cancel();
            }
        }
    }

    /**
     * When the target of an action is weakly reachable but the request hasn't been canceled, it
     * gets added to the reference queue. This thread empties the reference queue and cancels the
     * request.
     */
    private static class CleanupThread extends Thread {
        private final ReferenceQueue<Object> referenceQueue;
        private final Handler handler;

        CleanupThread(ReferenceQueue<Object> referenceQueue, Handler handler) {
            this.referenceQueue = referenceQueue;
            this.handler = handler;
            setDaemon(true);
            setName(THREAD_PREFIX + "refQueue");
        }

        @Override public void run() {
            Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
            while (true) {
                try {
                    // Prior to Android 5.0, even when there is no local variable, the result from
                    // remove() & obtainMessage() is kept as a stack local variable.
                    // We're forcing this reference to be cleared and replaced by looping every second
                    // when there is nothing to do.
                    // This behavior has been tested and reproduced with heap dumps.
                    RequestWeakReference<?> remove =
                            (RequestWeakReference<?>) referenceQueue.remove(THREAD_LEAK_CLEANING_MS);
                    Message message = handler.obtainMessage();
                    if (remove != null) {
                        message.what = REQUEST_GCED;
                        message.obj = remove.action;
                        handler.sendMessage(message);
                    } else {
                        message.recycle();
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (final Exception e) {
                    handler.post(() -> {
                        throw new RuntimeException(e);
                    });
                    break;
                }
            }
        }

    }


    public static Angelo with(Context context) {
        if (singleton == null) {
            synchronized (Angelo.class) {
                if (singleton == null) {
                    if (context == null) {
                        throw new IllegalStateException("context == null");
                    }
                    singleton = new Builder(context).build();
                }
            }
        }
        return singleton;
    }

    /** Fluent API for creating {@link Angelo} instances. */
    @SuppressWarnings("UnusedDeclaration") // Public API.
    public static class Builder {
        private final Context context;
        private ExecutorService service;
        private org.oucho.musicplayer.angelo.Cache cache;
        private Listener listener;
        private RequestTransformer transformer;
        private List<RequestHandler> requestHandlers;
        private Bitmap.Config defaultBitmapConfig;

        private boolean indicatorsEnabled;
        private boolean loggingEnabled;

        /** Start building a new {@link Angelo} instance. */
        public Builder(@NonNull Context context) {
            this.context = context.getApplicationContext();
        }

        /**
         * Specify the default {@link Bitmap.Config} used when decoding images. This can be overridden
         * on a per-request basis using {@link RequestCreator#config(Bitmap.Config) config(..)}.
         */
        public Builder defaultBitmapConfig(@NonNull Bitmap.Config bitmapConfig) {
            this.defaultBitmapConfig = bitmapConfig;
            return this;
        }


        /**
         * Specify the executor service for loading images in the background.
         * <p>
         * Note: Calling {@link Angelo#() shutdown()} will not shutdown supplied executors.
         */
        public Builder executor(@NonNull ExecutorService executorService) {
            if (this.service != null) {
                throw new IllegalStateException("Executor service already set.");
            }
            this.service = executorService;
            return this;
        }

        /** Specify the memory cache used for the most recent images. */
        public Builder memoryCache(@NonNull Cache memoryCache) {
            if (this.cache != null) {
                throw new IllegalStateException("Memory cache already set.");
            }
            this.cache = memoryCache;
            return this;
        }

        /** Specify a listener for interesting events. */
        public Builder listener(@NonNull Listener listener) {
            if (this.listener != null) {
                throw new IllegalStateException("Listener already set.");
            }
            this.listener = listener;
            return this;
        }

        /**
         * Specify a transformer for all incoming requests.
         * <p>
         * <b>NOTE:</b> This is a beta feature. The API is subject to change in a backwards incompatible
         * way at any time.
         */
        public Builder requestTransformer(@NonNull RequestTransformer transformer) {
            if (this.transformer != null) {
                throw new IllegalStateException("Transformer already set.");
            }
            this.transformer = transformer;
            return this;
        }

        /** Register a {@link RequestHandler}. */
        public Builder addRequestHandler(@NonNull RequestHandler requestHandler) {
            if (requestHandlers == null) {
                requestHandlers = new ArrayList<>();
            }
            if (requestHandlers.contains(requestHandler)) {
                throw new IllegalStateException("RequestHandler already registered.");
            }
            requestHandlers.add(requestHandler);
            return this;
        }

        /** Toggle whether to display debug indicators on images. */
        public Builder indicatorsEnabled(boolean enabled) {
            this.indicatorsEnabled = enabled;
            return this;
        }

        /**
         * Toggle whether debug logging is enabled.
         * <p>
         * <b>WARNING:</b> Enabling this will result in excessive object allocation. This should be only
         * be used for debugging purposes. Do NOT pass {@code BuildConfig.DEBUG}.
         */
        public Builder loggingEnabled(boolean enabled) {
            this.loggingEnabled = enabled;
            return this;
        }

        /** Create the {@link Angelo} instance. */
        public Angelo build() {
            Context context = this.context;

            if (cache == null) {
                cache = new LruCache(context);
            }
            if (service == null) {
                service = new AngeloExecutorService();
            }
            if (transformer == null) {
                transformer = RequestTransformer.IDENTITY;
            }

            Dispatcher dispatcher = new Dispatcher(context, service, HANDLER, cache);

            return new Angelo(context, dispatcher, cache, listener, transformer, requestHandlers,
                    defaultBitmapConfig, indicatorsEnabled);
        }
    }

    /** Describes where the image was loaded from. */
    public enum LoadedFrom {
        MEMORY(Color.GREEN),
        DISK(Color.BLUE);

        final int debugColor;

        LoadedFrom(int debugColor) {
            this.debugColor = debugColor;
        }
    }
}
