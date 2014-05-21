package com.tbocek.android.combatmap.cast;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.format.Formatter;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Encapsulates a small HTTP server used to make a snapshot of the current state available to the
 * cast receiver.
 * Created by tbocek on 4/29/14.
 */
public class CastFileServer extends NanoHTTPD {
    private final static String TAG = "CastFileServer";

    public static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final int JPEG_COMPRESSION = 60;
    private final Context mContext;

    // TODO: need to cache this on disk for memory concerns, or is it OK to keep this jpg in
    // memory?
    private byte[] mImageJpg;

    public interface Listener {
        void onNewImageAvailable();
        void onImageFetched();
    }

    private final Listener mNullListener = new Listener() {
        @Override
        public void onNewImageAvailable() { }

        @Override
        public void onImageFetched() { }
    };

    Listener mListener = mNullListener;

    public CastFileServer(Context context) {
        // TODO: Allow port selection in advanced options.
        super(8000);
        mContext = context;
    }

    public void saveImage(Bitmap bitmap) {
        new SaveBitmapTask().execute(bitmap);
    }

    @Override
    public Response serve(String uri, Method method,
                          Map<String, String> header,
                          Map<String, String> parameters,
                          Map<String, String> files) {

        mListener.onImageFetched();
        synchronized(this) {
            return new Response(
                    Response.Status.OK, JPEG_MIME_TYPE,
                    new ByteArrayInputStream(mImageJpg.clone()));
        }
    }

    public String getImageAddress() {
        WifiManager wifiMgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        String ipAddress = Formatter.formatIpAddress(ip);
        return "http://" + ipAddress + ":" + Integer.toString(getListeningPort());
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private class SaveBitmapTask extends AsyncTask<Bitmap, Void, Void> {

        @Override
        protected Void doInBackground(Bitmap... bitmaps) {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            BufferedOutputStream buf = new BufferedOutputStream(s);
            bitmaps[0].compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION, buf);
            synchronized(CastFileServer.this) {
                mImageJpg = s.toByteArray();
            }
            try {
                buf.close();
                s.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing bitmap writer", e);
            }

            mListener.onNewImageAvailable();
            return null;
        }
    }
}
