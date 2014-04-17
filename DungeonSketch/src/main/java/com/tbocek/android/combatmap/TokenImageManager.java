package com.tbocek.android.combatmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.tbocek.android.combatmap.model.primitives.BaseToken;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
        Map<BaseToken, Callback> mCallbacks;

        public Loader(Handler responseHandler) {
            super("TokenImageManager.Loader");
            mResponseHandler = responseHandler;
        }

        protected void onLooperPrepared() {
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    if (msg.what == MESSAGE_LOAD) {
                        BaseToken token = (BaseToken)msg.obj;
                        handleRequest(token);
                    }
                }
            };
        }

        private void handleRequest(final BaseToken token) {
            // TODO: Load the damn token
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallbacks.get(token).imageLoaded(token);
                    mCallbacks.remove(token);
                }
            });
        }

        public void queueTokenLoad(BaseToken token, Callback callback) {
            mCallbacks.put(token, callback);
            mHandler.obtainMessage(MESSAGE_LOAD, token).sendToTarget();
        }

        public void clearQueue() {
            mHandler.removeMessages(MESSAGE_LOAD);
            mCallbacks.clear();
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
                mRecycledImages.add(this);
            }
        }
    }

    public interface Callback {
        void imageLoaded(BaseToken token);
    };

    public abstract class MultiLoadCallback implements Callback {
        Collection<BaseToken> mTokens;
        Set<BaseToken> mStillNeedToLoad;

        protected abstract void imagesLoaded(Collection<BaseToken> tokens);

        public void imageLoaded(BaseToken token) {
            mStillNeedToLoad.remove(token);
            if (mStillNeedToLoad.isEmpty()) {
                imagesLoaded(mTokens);
            }
        }

        void setTokens(Collection<BaseToken> tokens) {
            mTokens = tokens;
            mStillNeedToLoad = new HashSet<BaseToken>(mTokens);
        }
    }

    Queue<TokenImageWrapper> mRecycledImages = new SynchronousQueue<TokenImageWrapper>();
    Map<String, TokenImageWrapper> mCurrentImages = new HashMap<String, TokenImageWrapper>();
    private Context mContext;

    private TokenImageManager(Context context) {
        mContext = context;
    }

    public synchronized void requireTokenImages(Collection<BaseToken> tokens, Loader loader, MultiLoadCallback callback) {
        callback.setTokens(tokens);
        for (BaseToken t: tokens) {
            requireTokenImage(t, loader, callback);
        }
    }

    public synchronized void requireTokenImage(BaseToken token, Loader loader, Callback callback) {
        if (!token.needsLoad()) {
            callback.imageLoaded(token);
        }
        String tokenId = token.getTokenId();
        if (mCurrentImages.containsKey(tokenId)) {
            TokenImageWrapper image = mCurrentImages.get(tokenId);
            image.mReferenceCount++;
            callback.imageLoaded(token);
        } else {
            loader.queueTokenLoad(token, callback);
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
            initializePool((int)(mCurrentImages.size() * POOL_EXPANSION_RATIO) + 1);
            return mRecycledImages.remove();
        } else {
            assert newImageWrapper.mReferenceCount == 0;

            mCurrentImages.remove(newImageWrapper.mToken.getTokenId());
            newImageWrapper.mToken = null;
            return newImageWrapper;
        }
    }
}
