package com.tbocek.android.combatmap.view.interaction;

import android.view.MotionEvent;

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
public class DrawTextInteractionMode extends BaseDrawInteractionMode {

    /**
     * Constructor.
     * 
     * @param view
     *            The view to manipulate.
     */
    public DrawTextInteractionMode(CombatView view) {
        super(view);
    }

    @Override
    public void onEndMode() {
        Text.shouldDrawBoundingBoxes(false);
    }

    @Override
    public void onLongPress(final MotionEvent e) {
        PointF p =
                this.getView()
                        .getWorldSpaceTransformer()
                        .screenSpaceToWorldSpace(new PointF(e.getX(), e.getY()));

        Shape t = this.getView().getActiveLines().findShape(p, Text.class);
        if (t != null) {
            this.getView().requestEditTextObject((Text) t);
        }
    }

    // Drag to move text.
    @Override
    public boolean onScroll(final MotionEvent arg0, final MotionEvent arg1,
            final float arg2, final float arg3) {
        PointF p =
                this.getView()
                        .getWorldSpaceTransformer()
                        .screenSpaceToWorldSpace(
                                new PointF(arg0.getX(), arg0.getY()));
        Shape t = this.getView().getActiveLines().findShape(p, Text.class);
        if (t != null) {
            t.setDrawOffset(this.getView().getWorldSpaceTransformer()
                    .screenSpaceToWorldSpace(arg1.getX() - arg0.getX()), this
                    .getView().getWorldSpaceTransformer()
                    .screenSpaceToWorldSpace(arg1.getY() - arg0.getY()));
            this.getView().refreshMap();
            return true;
        } else {
            return super.onScroll(arg0, arg1, arg2, arg3);
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

    @Override
    public void onStartMode() {
        Text.shouldDrawBoundingBoxes(true);
    }

    @Override
    public void onUp(final MotionEvent event) {
        this.getView().getActiveLines().optimize();
        this.getView().refreshMap();
    }
}
