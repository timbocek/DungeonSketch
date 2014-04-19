package com.tbocek.android.combatmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.tbocek.android.combatmap.model.primitives.BaseToken;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.SynchronousQueue;

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
    private static final int INITIAL_POOL_SIZE = 100;
    private static TokenImageManager mInstance;

    public static TokenImageManager getInstance() {
        if (mInstance == null) throw new RuntimeException("No token image manager instance");
        return mInstance;
    }

    public static TokenImageManager getInstance(Context context) {
        context = context.getApplicationContext();
        if (mInstance == null) {
            mInstance = new TokenImageManager(context);
            mInstance.initializePool(INITIAL_POOL_SIZE);
        }
        return mInstance;
    }

    public static class Loader extends HandlerThread {
        private static final int MESSAGE_LOAD = 0;
        Handler mHandler;
        Handler mResponseHandler;
        Map<String, Callback> mCallbacks = new HashMap<String, Callback>();

        public Loader(Handler responseHandler) {
            super("TokenImageManager.Loader");
            mResponseHandler = responseHandler;
        }

        protected void onLooperPrepared() {
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    if (msg.what == MESSAGE_LOAD) {
                        String tokenId = (String)msg.obj;
                        Log.d(TAG, "Handling load request for " + tokenId);
                        handleRequest(tokenId);
                    }
                }
            };
        }

        private void handleRequest(final String tokenId) {
            //TODO: actually recycle bitmaps
            TokenImageManager mgr = TokenImageManager.getInstance();
            TokenDatabase db = TokenDatabase.getInstanceOrNull();
            // If this token has been loaded since the request was created, just increase
            // the ref count.
            if (mgr.mCurrentImages.containsKey(tokenId)) {
                mgr.mCurrentImages.get(tokenId).mReferenceCount++;
            } else {
                BaseToken token = db.createToken(tokenId);
                Bitmap b = token.loadBitmap();

                TokenImageWrapper w = mgr.getUnusedImage();
                if (w.mImage != null) {
                    Log.d(TAG, "RECYCLE");
                    w.mImage.recycle();
                }
                w.mImage = b;
                w.mDrawable = new BitmapDrawable(b);
                w.mToken = token;
                mgr.mCurrentImages.put(tokenId, w);
            }

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Posting token load callback for " + tokenId);
                    if (mCallbacks.containsKey(tokenId)) {
                        mCallbacks.get(tokenId).imageLoaded(tokenId);
                        mCallbacks.remove(tokenId);
                    }
                }
            });
        }

        public void queueTokenLoad(String tokenId, Callback callback) {
            mCallbacks.put(tokenId, callback);
            mHandler.obtainMessage(MESSAGE_LOAD, tokenId).sendToTarget();
            Log.d(TAG, "Token load queued: " + tokenId);
        }

        public void clearQueue() {
            mHandler.removeMessages(MESSAGE_LOAD);
            mCallbacks.clear();
        }

        public void cancelTokenLoad(String tokenId) {
            mCallbacks.remove(tokenId);
        }
    }

    public class TokenImageWrapper {
        private Bitmap mImage;
        private Drawable mDrawable;
        private BaseToken mToken;
        private int mReferenceCount;

        public Bitmap getImage() {
            return mImage;
        }
        public Drawable getDrawable() { return mDrawable; }

        public void release() {
            mReferenceCount--;
            if (mReferenceCount == 0) {
                mRecycledImages.addLast(this);
            }
        }
    }

    public interface Callback {
        void imageLoaded(String tokenId);
    };

    public abstract class MultiLoadCallback implements Callback {
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
    }

    LinkedList<TokenImageWrapper> mRecycledImages = new LinkedList<TokenImageWrapper>();
    Map<String, TokenImageWrapper> mCurrentImages = new HashMap<String, TokenImageWrapper>();
    private Context mContext;

    private TokenImageManager(Context context) {
        mContext = context;
    }

    public synchronized void requireTokenImages(Collection<String> tokens, Loader loader, MultiLoadCallback callback) {
        callback.setTokens(tokens);
        for (String t: tokens) {
            requireTokenImage(t, loader, callback);
        }
    }

    public synchronized void requireTokenImage(String tokenId, Loader loader, Callback callback) {
        TokenDatabase db = TokenDatabase.getInstanceOrNull();
        if (db.createToken(tokenId).needsLoad()) {
            callback.imageLoaded(tokenId);
        }
        if (mCurrentImages.containsKey(tokenId)) {
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

    public Bitmap getTokenImage(String tokenId) {
        if (mCurrentImages.containsKey(tokenId)) {
            return mCurrentImages.get(tokenId).getImage();
        }
        return null;
    }

    public Drawable getTokenDrawable(String tokenId) {
        if (mCurrentImages.containsKey(tokenId)) {
            return mCurrentImages.get(tokenId).getDrawable();
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

    private synchronized TokenImageWrapper getUnusedImage() {
        TokenImageWrapper newImageWrapper = null;

        // Remove images from the pool until we find one that is *actually* unsused
        // or the pool is empty.
        while (!mRecycledImages.isEmpty() &&
                (newImageWrapper = mRecycledImages.remove()).mReferenceCount != 0 ) {}

        if (mRecycledImages.isEmpty()) {
            // Pool empty, allocate some more.
            int poolIncrease = (int)(mCurrentImages.size() * POOL_EXPANSION_RATIO) + 1;
            Log.i(TAG, "Increasing image pool size by " + poolIncrease);
            initializePool(poolIncrease);
            return mRecycledImages.removeFirst();
        } else {
            assert newImageWrapper.mReferenceCount == 0;

            if (newImageWrapper.mToken != null && mCurrentImages.containsKey(newImageWrapper.mToken.getTokenId())) {
                mCurrentImages.remove(newImageWrapper.mToken.getTokenId());
            }
            newImageWrapper.mToken = null;
            newImageWrapper.mReferenceCount++;
            return newImageWrapper;
        }
    }
}
