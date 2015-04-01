package com.tbocek.android.combatmap.view.interaction;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.view.MotionEvent;

import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.model.primitives.Units;
import com.tbocek.android.combatmap.model.primitives.Util;
import com.tbocek.android.combatmap.view.CombatView;

import java.text.NumberFormat;

public class MeasuringTapeInteractionMode extends BaseDrawInteractionMode {

	private float mStartPointX;
	private float mStartPointY;
	private boolean measuring = false;
	private float mLastPointX;
	private float mLastPointY;
	private final Paint mPaint;
	public MeasuringTapeInteractionMode(CombatView view) {
		super(view);
		mPaint = new Paint();
		mPaint.setColor(Color.RED);
		mPaint.setStrokeWidth(3.0f);
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setTextSize(Units.ptToPx(16));
	}
    @Override
    public boolean onDown(final MotionEvent e) {
        PointF p = this.getScreenSpacePoint(e);
        this.mStartPointX = p.x;
        this.mStartPointY = p.y;
        measuring = true;
        return true;
    }
    
    public void onUp(final MotionEvent e) {
        if (this.getNumberOfFingers() == 0) {
            this.measuring =  false;
        }
    }
    
    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
            final float distanceX, final float distanceY) {
    	PointF p = this.getScreenSpacePoint(e2);
    	this.mLastPointX = p.x;
    	this.mLastPointY = p.y;
    	getView().refreshMap();
		return true;
    }
    
    @Override
    public void draw(final Canvas c) {
    	if (!measuring) return;
    	c.drawLine(this.mStartPointX, this.mStartPointY, this.mLastPointX, mLastPointY, mPaint);
    	CoordinateTransformer transformer = getView().getData().getGrid().gridSpaceToScreenSpaceTransformer(getView().getData().getWorldSpaceTransformer());
        Float dist = transformer.screenSpaceToWorldSpace(
                Util.distance(mStartPointX, mStartPointY, mLastPointX, mLastPointY))
                * getView().getData().getGrid().getScale();

        NumberFormat fmt = NumberFormat.getInstance();
        fmt.setMaximumFractionDigits(2);
        String s = String.format("%s %s",
                fmt.format(dist),
                getView().getData().getGrid().getUnits());

        int height = (int) Units.ptToPx(20);

    	c.drawText(s, this.getView().getWidth() / 2, height,
                   this.mPaint);
    }
}
