package com.tbocek.android.combatmap;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.StrictMode;

import com.tbocek.dungeonsketch.BuildConfig;

/**
 * Central location to enable debug options.
 * 
 * @author Tim
 * 
 */
public final class DeveloperMode {

    /**
     * Whether developer/debug mode is enabled.
     */
    public static final boolean DEVELOPER_MODE = BuildConfig.DEBUG;

    //public static final int MAX_BUILTIN_TOKENS = BuildConfig.DEBUG ? 10 : Integer.MAX_VALUE;
    public static final int MAX_BUILTIN_TOKENS = Integer.MAX_VALUE;
    /**
     * Starts the profiler only if developer mode is active.
     * 
     * @param name
     *            The name of the profiler to write to.
     */
    public static void startProfiler(String name) {
        if (DEVELOPER_MODE) {
            android.os.Debug.startMethodTracing(name);
        }
    }

    /**
     * Stops profiling.
     */
    public static void stopProfiler() {
        if (DEVELOPER_MODE) {
            android.os.Debug.stopMethodTracing();
        }
    }

    /**
     * If in developer mode and the SDK supports it, run in strict mode.
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void strictMode() {
        if (false && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD
                && DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
            .detectAll().penaltyLog().penaltyDeath().build());
        }
    }

	public static boolean shouldDisplayFramerate() {
		return DEVELOPER_MODE;
	}

    /**
     * Private constructor because this is a utility class.
     */
    private DeveloperMode() {
    }
}
