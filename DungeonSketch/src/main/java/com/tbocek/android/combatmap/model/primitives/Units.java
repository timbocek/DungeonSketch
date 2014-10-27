package com.tbocek.android.combatmap.model.primitives;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * Created by tbocek on 10/21/14.
 */
public class Units {
    private static DisplayMetrics sDisplayMetrics;

    public static void initialize(Context context) {
        sDisplayMetrics = context.getResources().getDisplayMetrics();
    }

    public static float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, sDisplayMetrics);
    }

    public static float ptToPx(float pt) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PT, pt, sDisplayMetrics);
    }
}
