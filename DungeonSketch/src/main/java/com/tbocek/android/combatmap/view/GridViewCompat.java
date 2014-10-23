package com.tbocek.android.combatmap.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.GridView;

import java.lang.reflect.Field;

/**
 * Extends GridView with backwards compatible implementations of getColumnWidth.
 * Created by tbocek on 10/23/14.
 */
public class GridViewCompat extends GridView {
    public GridViewCompat(Context context) {
        super(context);
    }

    public GridViewCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GridViewCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("NewApi")
    @Override
    public int getColumnWidth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            return super.getColumnWidth();
        else {
            try {
                Field field = GridView.class.getDeclaredField("mColumnWidth");
                field.setAccessible(true);
                Integer value = (Integer) field.get(this);
                field.setAccessible(false);

                return value.intValue();
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
