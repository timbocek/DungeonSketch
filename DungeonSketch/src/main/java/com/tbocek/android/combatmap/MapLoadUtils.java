package com.tbocek.android.combatmap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.tbocek.dungeonsketch.R;

import java.io.File;

/**
 * Created by tbocek on 4/1/15.
 */
public class MapLoadUtils {
    public interface LoadFinishedCallback {
        void loadFinished(boolean success);
    }

    public static void loadMap(Context context, String name, LoadFinishedCallback callback) {
        String errorString = new DataManager(context.getApplicationContext()).loadMapName(name);
        if (errorString != null && !errorString.isEmpty()) {
            if (context.getApplicationContext() != null) {
                Toast toast = Toast.makeText(context,
                        "Could not load file.  Reason: " + errorString,
                        Toast.LENGTH_LONG);
                toast.show();
            }
            setFilenamePreference(context, null);
            // Open map error reporting dialog.
            reportBadMap(context, name, errorString, callback);
        } else {
            setFilenamePreference(context, name);
            if (callback != null) callback.loadFinished(true);
        }
    }

    /**
     * Gives the user the opportunity to report that a map failed to load.
     * @param mapName
     * @param errorString
     */
    private static void reportBadMap(final Context context, String mapName, final String errorString, final LoadFinishedCallback callback) {
        final File mapFile = new DataManager(context).getSavedMapFile(mapName);
        new AlertDialog.Builder(context)
                .setPositiveButton(R.string.report_via_email,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                                intent.setType("text/plain");
                                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{context.getString(R.string.error_email_destination)} );
                                intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.error_email_subject));
                                intent.putExtra(Intent.EXTRA_TEXT, errorString);
                                intent.setType("text/plain");

                                if (!mapFile.exists() || !mapFile.canRead()) {
                                    Toast.makeText(
                                            context, context.getString(R.string.attachment_issue),
                                            Toast.LENGTH_SHORT).show();
                                }
                                Uri uri = Uri.parse("file://" + mapFile.getAbsolutePath());
                                intent.putExtra(Intent.EXTRA_STREAM, uri);
                                context.startActivity(Intent.createChooser(
                                        intent, context.getString(R.string.report_via_email)));
                                if (callback != null) callback.loadFinished(false);
                            }
                        })
                .setNegativeButton(R.string.dont_report,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (callback != null) callback.loadFinished(false);
                            }
                        })
                .setMessage(context.getString(R.string.map_error_report))
                .create().show();
    }


    public static void setFilenamePreference(Context context, final String newFilename) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        // Persist the filename that we saved to so that we can load from that
        // file again.
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("filename", newFilename);
        editor.commit();
    }
}


