package com.tbocek.android.combatmap;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.StrictMode;

import com.tbocek.dungeonsketch.BuildConfig;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

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
        //noinspection PointlessBooleanExpression
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD
                && DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
            .detectAll().penaltyLog().penaltyDeath().build());

            // Replace System.err with one that'll monitor for StrictMode
            // killing us and perform a hprof heap dump just before it does.
            System.setErr (new PrintStreamThatDumpsHprofWhenStrictModeKillsUs(
                    System.err));
        }
    }

	public static boolean shouldDisplayFramerate() {
		return false && DEVELOPER_MODE;
	}

    /**
     * Private constructor because this is a utility class.
     */
    private DeveloperMode() {
    }

    public static boolean shouldDisplayDrawRects() {
        return false;
    }

    private static class PrintStreamThatDumpsHprofWhenStrictModeKillsUs
            extends PrintStream {
        public PrintStreamThatDumpsHprofWhenStrictModeKillsUs(OutputStream outs) {
            super (outs);
        }

        @Override
        public synchronized void println(String str) {
            super.println(str);
            if (str.startsWith("StrictMode VmPolicy violation with POLICY_DEATH;")) {
                // StrictMode is about to terminate us... do a heap dump!
                try {
                    File dir = Environment.getExternalStorageDirectory();
                    File file = new File(dir, "strictmode-violation.hprof");
                    super.println("Dumping HPROF to: " + file);
                    Debug.dumpHprofData(file.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
