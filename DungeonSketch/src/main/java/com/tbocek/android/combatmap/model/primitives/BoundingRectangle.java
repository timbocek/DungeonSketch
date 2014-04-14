package com.tbocek.android.combatmap.model.primitives;

import java.io.IOException;
import java.util.Collection;

import android.graphics.RectF;

import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;

/**
 * Represents a recangle that bounds a number of drawable Dungeon Sketch
 * objects.
 * 
 * @author Tim
 * 
 */
public final class BoundingRectangle {

    /**
     * Current right bounds.
     */
    private float mXMax = -Float.MAX_VALUE;

    /**
     * Current left bounds.
     */
    private float mXMin = Float.MAX_VALUE;

    /**
     * Current bottom bounds.
     */
    private float mYMax = -Float.MAX_VALUE;

    /**
     * Current top bounds.
     */
    private float mYMin = Float.MAX_VALUE;

    /**
     * Creates a new BoundingRectangle by reading from the given stream.
     * 
     * @param s
     *            The deserialization object to load from.
     * @return The loaded BoundingRectangle.
     * @throws IOException
     *             On deserialization error.
     */
    public static BoundingRectangle deserialize(MapDataDeserializer s)
            throws IOException {
        BoundingRectangle r = new BoundingRectangle();
        r.mXMin = s.readFloat();
        r.mXMax = s.readFloat();
        r.mYMin = s.readFloat();
        r.mYMax = s.readFloat();
        return r;
    }

    /**
     * Default ctor.
     */
    public BoundingRectangle() {
    }

    /**
     * Constructs a bounding rectangle that uses the given points as bounds.
     * 
     * @param p1
     *            First point.
     * @param p2
     *            Second point.
     */
    public BoundingRectangle(PointF p1, PointF p2) {
        this.mXMin = p1.x;
        this.mXMax = p1.x;
        this.mYMin = p1.y;
        this.mYMax = p1.y;
        this.updateBounds(p2);
    }

    /**
     * Returns the bounding rectangle to its initial state.
     */
    public void clear() {
        this.mXMin = Float.MAX_VALUE;
        this.mXMax = Float.MIN_NORMAL;
        this.mYMin = Float.MAX_VALUE;
        this.mYMax = Float.MIN_NORMAL;
    }

    /**
     * Checks if this bounding rectangle contains a point.
     * 
     * @param p
     *            The point to check.
     * @return True if the point lies within this rectangle.
     */
    public boolean contains(final PointF p) {
        return p.x >= this.mXMin && p.x <= this.mXMax && p.y >= this.mYMin
                && p.y <= this.mYMax;
    }

    /**
     * @return The height.
     */
    public float getHeight() {
        return this.mYMax - this.mYMin;
    }

    /**
     * @return The width.
     */
    public float getWidth() {
        return this.mXMax - this.mXMin;
    }

    /**
     * @return The right bounds.
     */
    public float getXMax() {
        return this.mXMax;
    }

    /**
     * @return The left bounds.
     */
    public float getXMin() {
        return this.mXMin;
    }

    /**
     * @return The bottom bounds.
     */
    public float getYMax() {
        return this.mYMax;
    }

    /**
     * @return The top bounds.
     */
    public float getYMin() {
        return this.mYMin;
    }

    /**
     * Checks whether this bounding rectangle contains part of a circle. (Note
     * that this is an approximation and checks the circle's bounding rectangle,
     * as intersecting a line and a circle is a much slower operation).
     * 
     * @param center
     *            The center of the circle to check.
     * @param radius
     *            The radius of the circle to check.
     * @return True if the bounding rectangle contains part of this circle.
     */
    public boolean
            intersectsWithCircle(final PointF center, final float radius) {
        return center.x + radius >= this.mXMin
                && center.x - radius <= this.mXMax
                && center.y + radius >= this.mYMin
                && center.y - radius <= this.mYMax;
    }

    /**
     * Translates the rectangle in place by the specified amount.
     * 
     * @param deltaX
     *            Amount to move in X direction.
     * @param deltaY
     *            Amount to move in Y direction.
     */
    public void move(float deltaX, float deltaY) {
        this.mXMin += deltaX;
        this.mXMax += deltaX;
        this.mYMin += deltaY;
        this.mYMax += deltaY;
    }

    /**
     * Saves the rectangle to the given serialization stream.
     * 
     * @param s
     *            Serialization object to save to.
     * @throws IOException
     *             On serialization error.
     */
    public void serialize(MapDataSerializer s) throws IOException {
        s.serializeFloat(this.mXMin);
        s.serializeFloat(this.mXMax);
        s.serializeFloat(this.mYMin);
        s.serializeFloat(this.mYMax);
    }

    /**
     * Converts to as Android RectF.
     * 
     * @return The RectF.
     */
    public RectF toRectF() {
        return new RectF(this.mXMin, this.mYMin, this.mXMax, this.mYMax);
    }

    /**
     * Updates the bounds of the rectangle so that the given rectangle is fully
     * included as well.
     * 
     * @param other
     *            Other bounding rectangle to include.
     */
    public void updateBounds(final BoundingRectangle other) {
        this.mXMin = Math.min(this.mXMin, other.mXMin);
        this.mXMax = Math.max(this.mXMax, other.mXMax);
        this.mYMin = Math.min(this.mYMin, other.mYMin);
        this.mYMax = Math.max(this.mYMax, other.mYMax);
    }

    /**
     * Updates the bounds for an entire collection of points.
     * 
     * @param points
     *            The points to update with.
     */
    public void updateBounds(final Collection<PointF> points) {
        for (PointF p : points) {
            this.updateBounds(p);
        }
    }

    /**
     * Updates the bounds of the rectangle so that the given point is also
     * included.
     * 
     * @param p
     *            The point to include.
     */
    public void updateBounds(final PointF p) {
        this.mXMin = Math.min(this.mXMin, p.x);
        this.mXMax = Math.max(this.mXMax, p.x);
        this.mYMin = Math.min(this.mYMin, p.y);
        this.mYMax = Math.max(this.mYMax, p.y);
    }
    
    
    
    /**
     * Tests whether this rectangle partially falls within the given rectangle.
     * @param clipRegion The rectangle to test against.
     */
    public boolean testClip(final RectF clipRegion) {
    	return !((this.mXMin < clipRegion.left && this.mXMax < clipRegion.left) || 
    		     (this.mXMin > clipRegion.right && this.mXMax > clipRegion.right) ||
    		     (this.mYMin < clipRegion.top && this.mYMax < clipRegion.top) ||
    		     (this.mYMin > clipRegion.bottom && this.mYMax > clipRegion.bottom));
    }

	public void expand(float margin) {
		this.mXMin -= margin;
		this.mXMax += margin;
		this.mYMin -= margin;
		this.mYMax += margin;
	}

}