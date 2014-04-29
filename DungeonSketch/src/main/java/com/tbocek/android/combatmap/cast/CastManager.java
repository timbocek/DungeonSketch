package com.tbocek.android.combatmap.cast;

import android.content.Context;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;

/**
 * Created by tbocek on 4/29/14.
 */
public class CastManager {
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private CastDevice mSelectedDevice;
    private Context mContext;

    public CastManager(Context context) {
        mContext = context.getApplicationContext();
    }

    public void onCreate() {
        mMediaRouter = android.support.v7.media.MediaRouter.getInstance(mContext);
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast("YOUR_APPLICATION_ID"))
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
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            mSelectedDevice = null;
        }
    };
}
