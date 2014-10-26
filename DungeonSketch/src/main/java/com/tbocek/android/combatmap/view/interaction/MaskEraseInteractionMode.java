package com.tbocek.android.combatmap.view.interaction;

import android.view.MotionEvent;

import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.model.primitives.Shape;
import com.tbocek.android.combatmap.view.CombatView;

/**
 * Provides an interaction mode for erasing mask (fog of war) elements.
 * @author Tim
 */
public class MaskEraseInteractionMode extends ZoomPanInteractionMode {

    /**
     * Constructor.
     * @param view The CombatView being controlled.
     */
    public MaskEraseInteractionMode(CombatView view) {
        super(view);
    }

    @Override
    public boolean onSingleTapUp(final MotionEvent ev) {
        PointF pt =
                this.getView().getWorldSpaceTransformer()
                        .screenSpaceToWorldSpace(ev.getX(), ev.getY());
        if (this.getView().isAFogOfWarLayerVisible()
                && this.getView().getActiveFogOfWar() != null) {
            Shape shapeUnderPress =
                    this.getView().getActiveFogOfWar().findShape(pt);
            if (shapeUnderPress != null) {
                this.getView().getActiveFogOfWar().deleteShape(shapeUnderPress);
                this.getView().refreshMap();
            }
        }
        return true;
    }
}
