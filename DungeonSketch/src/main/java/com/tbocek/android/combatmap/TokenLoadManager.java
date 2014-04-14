package com.tbocek.android.combatmap;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.os.Handler;

import com.tbocek.android.combatmap.model.primitives.BaseToken;

/**
 * This class maintains a queue of tokens that need to be loaded. Each entry in
 * the queue comes with a callback that is called when the token is loaded. This
 * callback is called in the UI thread so that UI elements can update with the
 * newly loaded token.
 * 
 * Token loads can be batched, so that the callback is only called when every
 * token in the batch is loaded.
 * 
 * Registering a token to load provides a handle that allows the load action to
 * be cancelled at any time.
 * 
 * @author Tim
 * 
 */
public class TokenLoadManager {
    private static TokenLoadManager mInstance = null;

    private boolean isStarted;

    BlockingQueue<JobHandle> mQueue = new LinkedBlockingQueue<JobHandle>();

    public static TokenLoadManager getInstance() {
        if (mInstance == null) {
            mInstance = new TokenLoadManager();
        }
        return mInstance;
    }

    private TokenLoadManager() {
    }

    private void handleJob(JobHandle job) {
        for (BaseToken t : job.mTokensToLoad) {
            t.load();
            if (job.isCancelled()) {
                return;
            }
        }
    }

    public JobHandle startJob(List<BaseToken> tokensToLoad,
            JobCallback callback, Handler uiThreadHandler) {
        JobHandle handle =
                new JobHandle(tokensToLoad, callback, uiThreadHandler);
        try {
            this.mQueue.put(handle);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return handle;
    }

    public void startThread() {
        synchronized (this) {
            if (!this.isStarted) {
                new TokenLoadJobThread().start();
            }
            this.isStarted = true;
        }
    }

    public interface JobCallback {
        void onJobComplete(List<BaseToken> loadedTokens);
    }

    private final class JobCallbackRunnableWrapper implements Runnable {
        private JobCallback mCallback;
        private List<BaseToken> mLoadedTokens;

        private JobCallbackRunnableWrapper(JobCallback callback,
                List<BaseToken> loadedTokens) {
            this.mCallback = callback;
            this.mLoadedTokens = loadedTokens;
        }

        @Override
        public void run() {
            this.mCallback.onJobComplete(this.mLoadedTokens);
        }

    }

    /**
     * Contains information pertaining to a single token load job. This class is
     * passed back to the client that requested the job, allowing it to be
     * cancelled.
     * 
     * @author Tim
     * 
     */
    public final class JobHandle {
        private JobCallback mCallback;
        private boolean mIsCancelled = false;
        private List<BaseToken> mTokensToLoad;

        private Handler mUiThreadHandler;

        private JobHandle(List<BaseToken> tokensToLoad, JobCallback callback,
                Handler uiThreadHandler) {
            this.mTokensToLoad = tokensToLoad;
            this.mCallback = callback;
            this.mUiThreadHandler = uiThreadHandler;
        }

        public void cancel() {
            synchronized (this) {
                this.mIsCancelled = true;
            }
        }

        private boolean isCancelled() {
            synchronized (this) {
                return this.mIsCancelled;
            }
        }

        private void postResult() {
            this.mUiThreadHandler.post(new JobCallbackRunnableWrapper(
                    this.mCallback, this.mTokensToLoad));
        }
    }

    private class TokenLoadJobThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    JobHandle currentJob = TokenLoadManager.this.mQueue.take();
                    TokenLoadManager.this.handleJob(currentJob);
                    currentJob.postResult();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
