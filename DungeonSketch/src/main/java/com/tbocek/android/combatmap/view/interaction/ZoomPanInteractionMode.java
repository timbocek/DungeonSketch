package com.tbocek.android.combatmap.view.interaction;

import android.util.Log;
import android.view.MotionEvent;

import com.tbocek.android.combatmap.view.CombatView;

/**
 * Extends the default combat view gesture behavior to allow a single finger to
 * scroll around the map area.
 * 
 * @author Tim Bocek
 */
public class ZoomPanInteractionMode extends BaseDrawInteractionMode {
    /**
     * Constructor.
     * 
     * @param view
     *            The CombatView that this interaction mode interacts with.
     */
    public ZoomPanInteractionMode(final CombatView view) {
        super(view);
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
            final float distanceX, final float distanceY) {
    	if (e2.getPointerCount() > 1) {
    		// In this case, since we are probably interleaving scale and scroll operations,
    		// we should request a full screen refresh in each case.
            this.getView().getWorldSpaceTransformer()
            	.moveOrigin(-distanceX, -distanceY);
            this.getView().refreshMap();
    	} else {
    		this.getView().scroll(-distanceX, -distanceY);
    	}
        return true;
    }
}
