package com.tbocek.android.combatmap.view.interaction;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.tbocek.android.combatmap.model.primitives.BoundingRectangle;
import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.model.primitives.Shape;
import com.tbocek.android.combatmap.model.primitives.Units;
import com.tbocek.android.combatmap.model.primitives.Util;
import com.tbocek.android.combatmap.view.CombatView;

/**
 * Interaction mode that allows the user to draw a line on the CombatView. The
 * color and width of the line are selected and stored elsewhere in the
 * CombatView.
 * 
 * @author Tim
 * 
 */
public class FingerDrawInteractionMode extends BaseDrawInteractionMode {

    /**
     * Distance between successive points on a line in screen space.
     */
    private static final float POINT_RATE_LIMIT = 3;

    /**
     * The line that the user is actively drawing. New points will be added to
     * this line.
     */
    private Shape mCurrentLine;

    /**
     * Whether a draw operation is in progress.
     */
    private boolean mDrawing;

    /**
     * The last x coordinate at which a point was added to the current line.
     */
    private float mLastPointX;

    /**
     * The last y coordinate at which a point was added to the current line.
     */
    private float mLastPointY;

    /**
     * Whether a zoom operation is in progress.
     */
    private boolean mZooming;

    /**
     * Constructor.
     * 
     * @param view
     *            The CombatView to manipulate.
     */
    public FingerDrawInteractionMode(final CombatView view) {
        super(view);
    }

    /**
     * Adds the location of the given motion event to the line.
     * 
     * @param e
     *            The motion event containing the point to add.
     */
    private void addLinePoint(final MotionEvent e) {
        PointF p = this.getScreenSpacePoint(e);
        
        // Get the bounding box both before and after the draw op, since
        // if the shape gets smaller we want to redraw the part of the screen
        // that the shape is no longer in.
        BoundingRectangle redrawRect = new BoundingRectangle();
        redrawRect.updateBounds(mCurrentLine.getBoundingRectangle());
        
        // Need to transform to world space.
        this.mCurrentLine.addPoint(this.getView().getWorldSpaceTransformer()
                .screenSpaceToWorldSpace(p));

        redrawRect.updateBounds(mCurrentLine.getBoundingRectangle());
        if (mCurrentLine.getStrokeWidth() != Float.POSITIVE_INFINITY) {
	        redrawRect.expand(Units.dpToPx(
                    this.getView().getWorldSpaceTransformer().worldSpaceToScreenSpace(
                            mCurrentLine.getStrokeWidth())));
        }
        this.getView().refreshMap(
        		redrawRect.toRectF(),
        		this.getView().getWorldSpaceTransformer()); 
        this.mLastPointX = p.x;
        this.mLastPointY = p.y;
    }

    /**
     * Creates a line in the data tied to the manipulated view, and returns it.
     * 
     * @return The created line.
     */
    protected Shape createLine() {
        return this.getView().createLine();
    }

    @Override
    public boolean onDown(final MotionEvent e) {
        this.mCurrentLine = this.createLine();
        PointF p = this.getScreenSpacePoint(e);
        this.mLastPointX = p.x;
        this.mLastPointY = p.y;
        return true;
    }

    @Override
    public boolean onScale(final ScaleGestureDetector detector) {
        this.mZooming = true;
        if (!this.mDrawing) {
            return super.onScale(detector);
        } else {
            return true;
        }
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
            final float distanceX, final float distanceY) {
        if (this.mZooming) {
            this.getView().getWorldSpaceTransformer()
                    .moveOrigin(-distanceX, -distanceY);
            this.getView().refreshMap();
            return true;
        }

        if (this.mCurrentLine == null) {
            return true;
        }

        this.mDrawing = true;

        if (this.shouldAddPoint(e2.getX(), e2.getY())) {
            this.addLinePoint(e2);
        }
        return true;
    }

    @Override
    public void onUp(final MotionEvent e) {
        if (this.getNumberOfFingers() == 0) {
            this.mZooming = false;
            this.mDrawing = false;
        }
    }

    /**
     * Returns True if the proposed point is far enough away from the previously
     * drawn point to add to the line.
     * 
     * @param newPointX
     *            X coordinate of the new point.
     * @param newPointY
     *            Y coordinate of the new point.
     * @return True if the point should be added
     */
    private boolean
            shouldAddPoint(final float newPointX, final float newPointY) {
        return Util.distance(this.mLastPointX, this.mLastPointY, newPointX,
                newPointY) > POINT_RATE_LIMIT;
    }
}
