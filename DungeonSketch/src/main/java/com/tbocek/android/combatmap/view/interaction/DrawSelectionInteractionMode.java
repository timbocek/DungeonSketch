package com.tbocek.android.combatmap.view.interaction;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.tbocek.android.combatmap.model.primitives.Units;
import com.tbocek.android.combatmap.view.CombatView;

/**
 * Created by tbocek on 10/29/14.
 */
public class DrawSelectionInteractionMode extends CombatViewInteractionMode {
    private RectF mSelectionScreenSpace;

    private Paint mSelectionInteriorPaint;
    private Paint mSelectionExteriorPaint;

    /**
     * Constructor.
     *
     * @param view The CombatView that this interaction mode manipulates.
     */
    public DrawSelectionInteractionMode(CombatView view) {
        super(view);

        mSelectionInteriorPaint = new Paint();
        mSelectionInteriorPaint.setColor(Color.argb(64, 128, 128, 255));
        mSelectionInteriorPaint.setStyle(Paint.Style.FILL);

        mSelectionExteriorPaint = new Paint();
        mSelectionExteriorPaint.setColor(Color.argb(255, 64, 64, 255));
        mSelectionExteriorPaint.setStyle(Paint.Style.STROKE);
        mSelectionExteriorPaint.setStrokeWidth(Units.dpToPx(2));
        mSelectionExteriorPaint.setPathEffect(
                new DashPathEffect(new float[]{Units.dpToPx(4.0f), Units.dpToPx(8.0f)}, 0));
    }

    @Override
    public boolean onDown(final MotionEvent e) {
        return true;
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        if (this.getNumberOfFingers() == 1) {
            mSelectionScreenSpace = new RectF(e1.getX(), e1.getY(), e2.getX(), e2.getY());
            this.getView().refreshMap(); // TODO: smarter invalidation.
        }
        return true;
    }

    @Override
    public void draw(final Canvas c) {
        if (mSelectionScreenSpace != null) {
            c.drawRect(mSelectionScreenSpace, mSelectionInteriorPaint);
            c.drawRect(mSelectionScreenSpace, mSelectionExteriorPaint);
        }
    }
}
