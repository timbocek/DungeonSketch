package com.tbocek.android.combatmap.view.interaction;

import android.view.MotionEvent;

import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.view.CombatView;

/**
 * Created by tbocek on 10/29/14.
 */
public class MoveSelectionInteractionMode extends CombatViewInteractionMode {

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
        return true;
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        CoordinateTransformer t = this.getView().getWorldSpaceTransformer();
        if (this.getNumberOfFingers() == 1) {
            getView().getSelection().setTemporaryOffset(
                    t.screenSpaceToWorldSpace(e2.getX() - e1.getX()),
                    t.screenSpaceToWorldSpace(e2.getY() - e1.getY()),
                    t.screenSpaceToWorldSpace(distanceX),
                    t.screenSpaceToWorldSpace(distanceY));
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
