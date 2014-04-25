package com.tbocek.android.combatmap.view.interaction;

import android.view.MotionEvent;

import com.tbocek.android.combatmap.model.primitives.Information;
import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.model.primitives.Shape;
import com.tbocek.android.combatmap.model.primitives.Text;
import com.tbocek.android.combatmap.view.CombatView;

/**
 * Interaction mode for drawing and manipulating text objects.
 * 
 * @author Tim
 * 
 */
public class DrawTextInteractionMode extends CreateInfoInteractionMode {

    /**
     * Constructor.
     *
     * @param view The view to manipulate.
     */
    public DrawTextInteractionMode(CombatView view) {
        super(view);
    }

    @Override
    public void onLongPress(final MotionEvent e) {
        PointF p =
                this.getView()
                        .getWorldSpaceTransformer()
                        .screenSpaceToWorldSpace(new PointF(e.getX(), e.getY()));

        Shape t = findShape(p);
        if (t != null) {
            this.getView().requestEditTextObject((Text) t);
        }
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {
        PointF p = new PointF(e.getX(), e.getY());

        this.getView().requestNewTextEntry(
                this.getView().getWorldSpaceTransformer()
                        .screenSpaceToWorldSpace(p));

        return true;
    }

    protected Shape findShape(PointF location) {
        return this.getView().getActiveLines().findShape(location, Text.class);
    }
}
