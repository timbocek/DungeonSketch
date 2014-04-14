package com.tbocek.android.combatmap.view.interaction;

import android.view.MotionEvent;

import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.view.CombatView;

/**
 * Base class for drawing interaction modes that implements some common behavior
 * that should always happen when drawing.
 * 
 * @author Tim
 * 
 */
public class BaseDrawInteractionMode extends CombatViewInteractionMode {

    /**
     * The point in world space that was long-pressed to open the menu.
     */
    private PointF mLongPressPoint;

    /**
     * Constructor.
     * 
     * @param view
     *            The CombatView to manipulate.
     */
    public BaseDrawInteractionMode(CombatView view) {
        super(view);
    }

    /**
     * Gets the draw location in screen space. Snaps to the grid if necessary.
     * 
     * @param e
     *            The motion event to get the point from.
     * @return The point in screen space.
     */
    protected PointF getScreenSpacePoint(final MotionEvent e) {
        PointF p = new PointF(e.getX(), e.getY());
        if (this.getView().shouldSnapToGrid()) {
            CoordinateTransformer transformer =
                    this.getView().getGridSpaceTransformer();
            p =
                    transformer.worldSpaceToScreenSpace(this
                            .getView()
                            .getData()
                            .getGrid()
                            .getNearestSnapPoint(
                                    transformer.screenSpaceToWorldSpace(p), 0));
        }
        return p;
    }

}