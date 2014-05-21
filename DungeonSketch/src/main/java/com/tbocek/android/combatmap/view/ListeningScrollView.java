package com.tbocek.android.combatmap.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Extends ScrollView to report when the view has been scrolled.
 * Created by Tim on 4/20/2014.
 */
public class ListeningScrollView extends ScrollView {

    public interface OnScrollChangedListener {
        public void OnScrollChanged(ScrollView view, int x, int y, int oldx, int oldy);
    }

    private OnScrollChangedListener mOnScrollChangedListener;

    @SuppressWarnings("UnusedDeclaration")
    public ListeningScrollView(Context context) {
        super(context);
    }
    @SuppressWarnings("UnusedDeclaration")
    public ListeningScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        mOnScrollChangedListener = onScrollChangedListener;
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        if (mOnScrollChangedListener != null)
            mOnScrollChangedListener.OnScrollChanged(this, x, y, oldx, oldy);
    }
}
