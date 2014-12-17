package com.tbocek.android.combatmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.dungeonsketch.BuildConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Manages a set of token images.  Anything needing a token image can request a bitmap.
 * If the token image is already loaded, the image will be provided right away.  Otherwise
 * it will be loaded off the UI thread.
 *
 * Token images are returned wrapped in a reference-counted bitmap.  Disposing a token image
 * will reduce the reference counts.  Memory for loaded token images will be recycled.
 * Created by Tim on 4/17/14.
 */
public class TokenImageManager {
    private static final String TAG = "TokenImageManager";

    private static final float POOL_EXPANSION_RATIO = 0.2f;
    private static final int INITIAL_POOL_SIZE = 50;
    private static TokenImageManager mInstance;

    public static TokenImageManager getInstance() {
        if (mInstance == null) {
            mInstance = new TokenImageManager();
            mInstance.initializePool(INITIAL_POOL_SIZE);
        }
        return mInstance;
    }

    public static TokenImageManager getInstanceOrNull() {
        return mInstance;
    }

    public class Loader extends HandlerThread {
        private static final int MESSAGE_LOAD = 0;
        Handler mHandler;
        final Handler mResponseHandler;
        final Context mContext;
        final Map<String, Callback> mCallbacks = new HashMap<String, Callback>();

        private Loader(Context context, Handler responseHandler) {
            super("TokenImageManager.Loader");
            mResponseHandler = responseHandler;
            mContext = context.getApplicationContext();
        }

        protected void onLooperPrepared() {
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    if (msg.what == MESSAGE_LOAD) {
                        String tokenId = (String)msg.obj;
                        Log.v(TAG, "Handling load request for " + tokenId);
                        handleRequest(tokenId);
                    }
                }
            };
        }

        private void handleRequest(final String tokenId) {
            // If this request no longer has a callback associated with it, assume that the load
            // has been cancelled.
            TokenImageWrapper newImage = null;
            final TokenImageManager mgr = TokenImageManager.getInstance();
            TokenDatabase db = TokenDatabase.getInstanceOrNull();

            synchronized(TokenImageManager.this) {
                if (!mCallbacks.containsKey(tokenId)) {
                    Log.v(TAG, "Load request for " + tokenId + " cancelled before loading.");
                    return;
                }
                // If this token has been loaded since the request was created, just increase
                // the ref count.
                if (mgr.mCurrentImages.containsKey(tokenId)) {
                    mgr.mCurrentImages.get(tokenId).mReferenceCount++;
                    Log.v(TAG, "Image for " + tokenId + " already found.  Now refcnt=" +
                            mgr.mCurrentImages.get(tokenId).mReferenceCount );
                } else {
                    newImage = mgr.getUnusedImage(mContext, mResponseHandler);

                }
            }

            // Check if a new bitmap needs to be loaded.  This should be done outside of a
            // synchronized block because it is an expensive process and is the reason we are doing
            // this off the main thread.
            if (newImage != null) {
                Log.v(TAG, "Image allocated.  Now loading token image for " + tokenId);
                BaseToken token = db.createToken(tokenId);
                Bitmap b = token.loadBitmap(newImage.mImage);
                newImage.mImage = b;
                newImage.mDrawable = new BitmapDrawable(mContext.getResources(), b);
                newImage.mToken = token;
                mgr.mCurrentImages.put(tokenId, newImage);
            }

            if (TokenImageManager.this.getTokenDrawable(tokenId) == null) {
                Log.e(TAG, "Failed to allocate an unused image for " + tokenId);
                synchronized(TokenImageManager.this) {
                    mCallbacks.remove(tokenId);
                }
                return;
            }

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    Callback cb = null;
                    synchronized(TokenImageManager.this) {
                        if (mCallbacks.containsKey(tokenId)) {
                            cb = mCallbacks.get(tokenId);
                            mCallbacks.remove(tokenId);
                        } else {
                            Log.v(TAG, "Load request for " + tokenId + " cancelled during loading.");
                            mgr.releaseTokenImage(tokenId);
                        }
                    }
                    if (cb != null) {
                        if (TokenImageManager.this.getTokenDrawable(tokenId) == null) {
                            Log.e(TAG, "Lost token image for " + tokenId + " before callback called.");
                            return;
                        }
                        Log.v(TAG, "Posting token load callback for " + tokenId);
                        cb.imageLoaded(tokenId);
                    }
                }
            });
        }

        public void queueTokenLoad(String tokenId, Callback callback) {
            synchronized(TokenImageManager.this) {
                mCallbacks.put(tokenId, callback);
                if (mHandler != null) {
                    Message m = mHandler.obtainMessage(MESSAGE_LOAD, tokenId);
                    m.sendToTarget();
                    Log.v(TAG, "Token load queued: " + tokenId);
                }
            }
        }

        public void clearQueue() {
            mHandler.removeMessages(MESSAGE_LOAD);
            mCallbacks.clear();
        }

        public void cancelTokenLoad(String tokenId) {
            // TODO: Do something sane in the case of a multi-token load callback.
            Callback cb = null;
            synchronized(TokenImageManager.this) {
                if (mCallbacks.containsKey(tokenId)) {
                    cb = mCallbacks.get(tokenId);
                    mCallbacks.remove(tokenId);
                }
            }
            if (cb != null) {
                cb.loadCancelled(tokenId);
            }
        }

        /**
         * If the given token is queued for load, cancels the load.  Otherwise, assumes that
         * the token is already loaded and discards it.
         * @param tokenId ID of the token to discard or dequeue.
         */
        public void discardOrCancelTokenLoad(String tokenId) {
            synchronized (TokenImageManager.this) {
                TokenImageManager mgr = TokenImageManager.getInstance();
                if (this.mCallbacks.containsKey(tokenId)) {
                    Log.v(TAG, "Cancelling token load: " + tokenId);
                    cancelTokenLoad(tokenId);
                } else {
                    mgr.releaseTokenImage(tokenId);
                    Log.v(TAG, "Releasing token image: " + tokenId);
                }
            }
        }
    }

    public class TokenImageWrapper {
        private Bitmap mImage;
        private Drawable mDrawable;
        private BaseToken mToken;
        private int mReferenceCount;

        public Drawable getDrawable() { return mDrawable; }

        public void release() {
            mReferenceCount--;
            if (mReferenceCount <= 0) {
                mRecycledImages.addLast(this);
                Log.v(TAG, "Image for " + mToken.getTokenId() + " added to garbage heap.  Size=" +
                        mRecycledImages.size());
            } else {
                Log.v(TAG, "Image for " + mToken.getTokenId() + " still has " + mReferenceCount +
                        " users.");
            }
        }
    }

    public static abstract class Callback {
        public abstract void imageLoaded(String tokenId);

        public void loadCancelled(String tokenId) { }
    }

    public static abstract class MultiLoadCallback extends Callback {
        Collection<String> mTokens;
        Set<String> mStillNeedToLoad;

        protected abstract void imagesLoaded(Collection<String> tokenIds);

        public void imageLoaded(String tokenId) {
            mStillNeedToLoad.remove(tokenId);
            if (mStillNeedToLoad.isEmpty()) {
                imagesLoaded(mTokens);
            }
        }

        void setTokens(Collection<String> tokens) {
            mTokens = tokens;
            mStillNeedToLoad = new HashSet<String>(mTokens);
        }

        @Override
        public void loadCancelled(String tokenId) {
            mStillNeedToLoad.remove(tokenId);
        }
    }

    private final LinkedList<TokenImageWrapper> mRecycledImages = Lists.newLinkedList();
    private Map<String, TokenImageWrapper> mCurrentImages =  Maps.newHashMap();

    private TokenImageManager() { }

    public synchronized void requireTokenImages(
            Collection<String> tokens, Loader loader, MultiLoadCallback callback) {
        callback.setTokens(tokens);
        for (String t: tokens) {
            requireTokenImage(t, loader, callback);
        }
    }

    public synchronized void requireTokenImage(String tokenId, Loader loader, Callback callback) {
        TokenDatabase db = TokenDatabase.getInstanceOrNull();
        // If the token does not require an image load, the token is ready to draw.  This could
        // be the case in e.g. color or letter tokens.  In this case, run the callback and return
        // immediately.
        if (!db.createToken(tokenId).needsLoad()) {
            Log.v(TAG, "Token " + tokenId +  " does not need a load.");
            callback.imageLoaded(tokenId);
            return;
        }

        if (mCurrentImages.containsKey(tokenId)) {
            // If the token image is already loaded, we don't need to wait on anything.  Just
            // increase the refcount and return immediately.
            Log.v(TAG, "Token " + tokenId + " already loaded.");
            TokenImageWrapper image = mCurrentImages.get(tokenId);
            image.mReferenceCount++;
            callback.imageLoaded(tokenId);
        } else {
            loader.queueTokenLoad(tokenId, callback);
        }
    }

    public synchronized void releaseTokenImage(String tokenId) {
        if (mCurrentImages.containsKey(tokenId)) {
            mCurrentImages.get(tokenId).release();
        }
    }

    public synchronized Drawable getTokenDrawable(String tokenId) {
        if (mCurrentImages.containsKey(tokenId)) {
            return mCurrentImages.get(tokenId).getDrawable();
        } else {
            Log.d(TAG, "Token image requested for " + tokenId + " before image was ready");
        }
        return null;
    }

    private synchronized void initializePool(int poolSize) {
        for (int i = 0; i < poolSize; ++i) {
            TokenImageWrapper image = new TokenImageWrapper();
            // TODO: Create memory for standard token size.
            this.mRecycledImages.add(image);
        }
    }

    private synchronized TokenImageWrapper getUnusedImage(final Context context,
                                                          Handler mResponseHandler) {
        TokenImageWrapper newImageWrapper = null;

        // Remove images from the pool until we find one that is *actually* unused
        // or the pool is empty.
        //noinspection StatementWithEmptyBody
        while (!mRecycledImages.isEmpty() &&
                (newImageWrapper = mRecycledImages.remove()).mReferenceCount != 0 ) {}

        if (mRecycledImages.isEmpty()) {
            // Pool empty, allocate some more.
            int poolIncrease = (int)(mCurrentImages.size() * POOL_EXPANSION_RATIO) + 1;
            final String warning = "Increasing image pool size by " + poolIncrease;
            Log.w(TAG, warning);

            initializePool(poolIncrease);
            return mRecycledImages.removeFirst();
        } else {
            //noinspection PointlessBooleanExpression
            if (BuildConfig.DEBUG && newImageWrapper.mReferenceCount != 0) {
                throw new RuntimeException("Selected image has nonzero ref count");
            }
            
            if (newImageWrapper.mToken != null && mCurrentImages.containsKey(newImageWrapper.mToken.getTokenId())) {
                mCurrentImages.remove(newImageWrapper.mToken.getTokenId());
            }
            newImageWrapper.mToken = null;
            newImageWrapper.mReferenceCount++;

            Log.v(TAG, "Token garbage heap size=" + mRecycledImages.size());
            return newImageWrapper;
        }
    }

    public synchronized void recycleAll() {
        for (TokenImageWrapper image: mCurrentImages.values()) {
            image.mReferenceCount = 0;
            mRecycledImages.addLast(image);
        }
        mCurrentImages = Maps.newHashMap();
    }

    public Loader createLoader(Context context, Handler responseHandler) {
        return new Loader(context, responseHandler);
    }
}
