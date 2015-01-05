package com.tbocek.android.combatmap.view.interaction;

import android.view.MotionEvent;

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
        return true;
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        if (this.getNumberOfFingers() == 1) {
            return true;
        } else {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    public void onUp(final MotionEvent event) {

    }
}
