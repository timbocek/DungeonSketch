package com.tbocek.android.combatmap.cast;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by tbocek on 4/29/14.
 */
public class CastFileServer extends NanoHTTPD {
    private boolean mFrameNeeded;
    public static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final int JPEG_COMPRESSION = 60;
    private Context mContext;

    // TODO: need to cache this on disk for memory concerns, or is it OK to keep this jpg in
    // memory?
    private byte[] mImageJpg;

    public CastFileServer(Context context) {
        // TODO: Allow port selection in advanced options.
        super(8000);
        mContext = context;
    }

    public void saveImage(Bitmap bitmap) throws IOException {
        mFrameNeeded = false;
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        BufferedOutputStream buf = new BufferedOutputStream(s);
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION, buf);
        mImageJpg = s.toByteArray();
        buf.close();
        s.close();
    }

    @Override
    public Response serve(String uri, Method method,
                          Map<String, String> header,
                          Map<String, String> parameters,
                          Map<String, String> files) {

        mFrameNeeded = true;
        return new Response(
                Response.Status.OK, JPEG_MIME_TYPE, new ByteArrayInputStream(mImageJpg));
    }

    public String getImageAddress() {
        WifiManager wifiMgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        String ipAddress = Formatter.formatIpAddress(ip);
        return "http://" + ipAddress + ":" + Integer.toString(getListeningPort());
    }

    public boolean needsNewFrame() {
        return mFrameNeeded;
    }
}
