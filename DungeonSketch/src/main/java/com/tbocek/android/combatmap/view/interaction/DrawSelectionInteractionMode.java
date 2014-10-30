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


    /**
     * Constructor.
     *
     * @param view The CombatView that this interaction mode manipulates.
     */
    public DrawSelectionInteractionMode(CombatView view) {
        super(view);

    }

    @Override
    public boolean onDown(final MotionEvent e) {
        super.onDown(e);
        return true;
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        if (this.getNumberOfFingers() == 1) {
            mSelectionScreenSpace = new RectF(e1.getX(), e1.getY(), e2.getX(), e2.getY());
            getView().setSelectedRegion(getData().getWorldSpaceTransformer().screenSpaceToWorldSpace(mSelectionScreenSpace));
            return true;
        } else {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    public void onUp(final MotionEvent event) {
        getView().requestSelectRegion(
                getData().getWorldSpaceTransformer().screenSpaceToWorldSpace(mSelectionScreenSpace));
    }
}
