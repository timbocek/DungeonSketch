package com.tbocek.android.combatmap.cast;

import android.content.Context;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by tbocek on 4/29/14.
 */
public class CastFileServer extends NanoHTTPD {
    private Context mContext;

    public CastFileServer(Context context) {
        // TODO: Allow port selection in advanced options.
        super(8000);
        mContext = context;
    }

    @Override
    public Response serve(String uri, Method method,
                          Map<String, String> header,
                          Map<String, String> parameters,
                          Map<String, String> files) {

    }
}
