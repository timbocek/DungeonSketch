package com.tbocek.android.combatmap;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Help {
    private static final String HELP_URL =
            "https://sites.google.com/site/dungeonsketchhelp/help";

    public static void openHelp(Context c) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(HELP_URL));
        c.startActivity(i);
    }
}