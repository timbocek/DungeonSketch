package com.tbocek.android.combatmap.cast;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

/**
 * Created by tbocek on 4/29/14.
 */
public class CastManager {
    private static CastManager sInstance;
    public static CastManager getInstance(Context c) {
        if (sInstance == null) {
            sInstance = new CastManager(c);
        }
        return sInstance;
    }

    private static final String TAG = "CastManager";
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private CastDevice mSelectedDevice;
    private Context mContext;
    private GoogleApiClient mApiClient;
    private boolean mWaitingForReconnect;

    private CastFileServer mCastServer;

    private CastManager(Context context) {
        mContext = context.getApplicationContext();
    }

    public void onCreate() {
        mCastServer = new CastFileServer(mContext);
        mMediaRouter = android.support.v7.media.MediaRouter.getInstance(mContext);
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(
                        CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                ))
                .build();
    }

    public void detachCallbacks() {
        mMediaRouter.removeCallback(mMediaRouterCallback);
    }

    public void attachCallbacks() {
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    public MediaRouteSelector getMediaRouteSelector() {
        return mMediaRouteSelector;
    }

    private MediaRouter.Callback mMediaRouterCallback = new MediaRouter.Callback() {

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
            String routeId = info.getId();

            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                    .builder(mSelectedDevice, mCastClientListener);

            mApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mConnectionFailedListener)
                    .build();
            mApiClient.connect();

        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            mSelectedDevice = null;
            teardown();
        }
    };

    private GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new
            GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle connectionHint) {
            if (mWaitingForReconnect) {
                mWaitingForReconnect = false;
                reconnectChannels();
            } else {
                try {
                    Cast.CastApi.launchApplication(mApiClient, "YOUR_APPLICATION_ID", false)
                            .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                        @Override
                                        public void onResult(Cast.ApplicationConnectionResult result) {
                                            Status status = result.getStatus();
                                            if (status.isSuccess()) {
                                                ApplicationMetadata applicationMetadata =
                                                        result.getApplicationMetadata();
                                                String sessionId = result.getSessionId();
                                                String applicationStatus = result.getApplicationStatus();
                                                boolean wasLaunched = result.getWasLaunched();
                                            } else {
                                                teardown();
                                            }
                                        }
                                    });

                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch application", e);
                }
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            mWaitingForReconnect = true;
        }
    };

    private GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener = new
    GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            teardown();
        }
    };

    Cast.Listener mCastClientListener = new Cast.Listener() {
        @Override
        public void onApplicationStatusChanged() {
            if (mApiClient != null) {
                Log.d(TAG, "onApplicationStatusChanged: "
                        + Cast.CastApi.getApplicationStatus(mApiClient));
            }
        }

        @Override
        public void onVolumeChanged() {
            if (mApiClient != null) {
                Log.d(TAG, "onVolumeChanged: " + Cast.CastApi.getVolume(mApiClient));
            }
        }

        @Override
        public void onApplicationDisconnected(int errorCode) {
            teardown();
        }
    };

    private void teardown() {

    }

    private void reconnectChannels() {

    }
}
