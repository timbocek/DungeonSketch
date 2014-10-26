package com.tbocek.android.combatmap;

import android.app.Application;
import android.content.Context;

/**
 * Extends android.app.Application to provide static access to the application
 * context - used to clean up some instances where we would need to otherwise
 * pass this deep into a class hierarchy.
 * 
 * Approach described in:
 *     http://stackoverflow.com/questions/6589797/how-to-get-package-name-from-anywhere
 * WARNING: May fall prey to:
 *     https://code.google.com/p/android/issues/detail?id=8727
 * @author Tim
 *
 */
public class DungeonSketchApp extends Application {
    private static DungeonSketchApp instance;

    public static DungeonSketchApp getInstance() {
        return instance;
    }

    public static Context getContext(){
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
    }
}
