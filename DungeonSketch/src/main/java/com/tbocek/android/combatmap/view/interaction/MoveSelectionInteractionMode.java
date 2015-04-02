package com.tbocek.android.combatmap.view.interaction;

import android.view.MotionEvent;

import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.view.CombatView;

/**
 * Created by tbocek on 10/29/14.
 */
public class MoveSelectionInteractionMode extends BaseDrawInteractionMode {

    private PointF mLastSnappedPoint;

    /**
     * Constructor.
     *
     * @param view The CombatView that this interaction mode manipulates.
     */
    public MoveSelectionInteractionMode(CombatView view) {
        super(view);

    }

    @Override
    public boolean onDown(final MotionEvent e) {
        super.onDown(e);
        this.getView().beginBatchMove();
        mLastSnappedPoint = getScreenSpacePoint(e);
        return true;
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        CoordinateTransformer t = this.getView().getWorldSpaceTransformer();

        PointF startPoint = this.getScreenSpacePoint(e1);
        PointF newPoint = this.getScreenSpacePoint(e2);

        float snappedDistanceX = mLastSnappedPoint.x - newPoint.x;
        float snappedDistanceY = mLastSnappedPoint.y - newPoint.y;
        mLastSnappedPoint = newPoint;

        if (this.getNumberOfFingers() == 1) {
            getView().getSelection().setTemporaryOffset(
                    t.screenSpaceToWorldSpace(newPoint.x - startPoint.x),
                    t.screenSpaceToWorldSpace(newPoint.y - startPoint.y),
                    t.screenSpaceToWorldSpace(snappedDistanceX),
                    t.screenSpaceToWorldSpace(snappedDistanceY));
            getView().refreshMap();
            return true;
        } else {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    public void onUp(final MotionEvent event) {
        this.getView().endBatchMove();
    }
}
