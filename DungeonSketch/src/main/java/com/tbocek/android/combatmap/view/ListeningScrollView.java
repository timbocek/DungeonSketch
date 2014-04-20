package com.tbocek.android.combatmap.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by Tim on 4/20/2014.
 */
public class ListeningScrollView extends ScrollView {

    public interface OnScrollChangedListener {
        public void OnScrollChanged(ScrollView view, int x, int y, int oldx, int oldy);
    }

    private OnScrollChangedListener mOnScrollChangedListener;

    public ListeningScrollView(Context context) {
        super(context);
    }
    public ListeningScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        mOnScrollChangedListener = onScrollChangedListener;
    }

    public OnScrollChangedListener getOnScrollChangedListener() {
        return mOnScrollChangedListener;
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        if (mOnScrollChangedListener != null)
            mOnScrollChangedListener.OnScrollChanged(this, x, y, oldx, oldy);
    }
}
