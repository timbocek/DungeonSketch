package com.tbocek.android.combatmap.view.interaction;

import android.view.MotionEvent;

import com.tbocek.android.combatmap.model.primitives.Information;
import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.model.primitives.Shape;
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


    @Override
    /**
     * Long-pressing information points should open them regardless of the interaction mode used.
     */
    public void onLongPress(final MotionEvent e) {
        // TODO(deupe)
        PointF p =
                this.getView()
                        .getWorldSpaceTransformer()
                        .screenSpaceToWorldSpace(new PointF(e.getX(), e.getY()));

        Shape t = this.getView().getActiveLines().findShape(p, Information.class);
        if (t != null) {
            this.getView().requestEditInfoObject((Information) t);
        } else {
            super.onLongPress(e);
        }
    }
}
