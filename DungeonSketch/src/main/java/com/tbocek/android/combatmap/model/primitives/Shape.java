package com.tbocek.android.combatmap.model.primitives;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region.Op;

import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;

import java.io.IOException;
import java.util.List;

/**
 * Abstract base class representing a shape.
 * 
 * @author Tim
 * 
 */
public abstract class Shape implements Cloneable {

    /**
     * Value in [0, 255] to use for the alpha channel when drawing shapes as fog
     * of war regions.
     */
    private static final int FOG_OF_WAR_ALPHA = 128;

    /**
     * Paint object that is used when drawing fog of war regions for the fog of
     * war editor.
     */
    private static Paint fogOfWarPaint;

    /**
     * Cached rectangle that bounds all the points in this line. This could be
     * computed on demand, but it is easy enough to update every time a point is
     * added.
     */
    private BoundingRectangle mBoundingRectangle = new BoundingRectangle();

    /**
     * The color to draw this line with.
     */
    private int mColor = Color.BLACK;

    /**
     * X component of the pending move operation.
     */
    private float mDrawOffsetDeltaX = Float.NaN;

    /**
     * Y component of the pending move operation.
     */
    private float mDrawOffsetDeltaY = Float.NaN;

    /**
     * The paint object that will be used to draw this line.
     */
    private transient Paint mPaint;

    /**
     * Cached path that represents this line.
     */
    private transient Path mPath;

    /**
     * The stroke width to draw this line with. +Infinity will use a fill
     * instead (to ensure that it draws beneath all lines).
     */
    private float mWidth;

    /**
     * Deserializes and returns a shape.
     *
     * @param s The stream to read from.
     * @return The created shape.
     * @throws IOException On deserialization error.
     */
    public static Shape deserialize(MapDataDeserializer s) throws IOException {
        String shapeType = s.readString();
        s.expectObjectStart();
        int color = s.readInt();
        float width = s.readFloat();
        BoundingRectangle r = BoundingRectangle.deserialize(s);
        s.expectObjectEnd();

        Shape shape;

        if (shapeType.equals(FreehandLine.SHAPE_TYPE)) {
            shape = new FreehandLine(color, width);
        } else if (shapeType.equals(StraightLine.SHAPE_TYPE)) {
            shape = new StraightLine(color, width);
        } else if (shapeType.equals(Circle.SHAPE_TYPE)) {
            shape = new Circle(color, width);
        } else if (shapeType.equals(OnScreenText.SHAPE_TYPE)) {
            shape = new OnScreenText(color, width);
        } else if (shapeType.equals(Rectangle.SHAPE_TYPE)) {
            shape = new Rectangle(color, width);
        } else if (shapeType.equals(Information.SHAPE_TYPE)) {
            shape = new Information();
        } else {
            throw new IOException("Unrecognized shape type: " + shapeType);
        }

        shape.mBoundingRectangle = r;
        shape.shapeSpecificDeserialize(s);
        return shape;
    }

    /**
     * Adds a point to this shape. This is used when dragging, so depending on
     * implementation, this may either add a point or may modify the
     * size/position of the shape.
     *
     * @param p The point to add.
     */
    public abstract void addPoint(final PointF p);

    /**
     * Changes the given canvas's transformation to apply this draw offset.
     *
     * @param c The canvas to modify.
     */
    public void applyDrawOffsetToCanvas(Canvas c) {
        if (this.hasOffset()) {
            c.save();
            c.translate(this.mDrawOffsetDeltaX, this.mDrawOffsetDeltaY);
        }
    }

    /**
     * Removes the pending move operation.
     */
    private void clearDrawOffset() {
        this.mDrawOffsetDeltaX = Float.NaN;
        this.mDrawOffsetDeltaY = Float.NaN;
    }

    /**
     * Clips out the region defined by this path on the fog of war.
     *
     * @param c Canvas to draw on.
     */
    public void clipFogOfWar(final Canvas c) {
        this.ensurePathCreated();
        if (this.mPath != null) {
            c.clipPath(this.mPath, Op.UNION);
        }
    }

    /**
     * Commits the pending move operation by returning a copy of this shape with
     * the offset applied. The offset is cleared from this shape. Calling code
     * should set up the proper undo/redo operation to actually implement the
     * move.
     *
     * @return Moved copy of the shape.
     */
    public Shape commitDrawOffset() {
        if (!this.hasOffset()) {
            return null;
        }

        Shape s =
                this.getMovedShape(this.mDrawOffsetDeltaX,
                        this.mDrawOffsetDeltaY);
        this.clearDrawOffset();
        return s;
    }

    /**
     * Checks whether this shape contains the given point.
     *
     * @param p The point to check.
     * @return True if that point falls within this shape.
     */
    public abstract boolean contains(PointF p);

    /**
     * Creates the Android graphics Path object used to draw this shape.
     *
     * @return The created path.
     */
    protected abstract Path createPath();

    /**
     * Draws the line on the given canvas.
     *
     * @param c Canvas to draw on.
     */
    public void draw(final Canvas c) {
        this.ensurePaintCreated();
        this.ensurePathCreated();
        if (this.mPath != null) {
            c.drawPath(this.mPath, this.mPaint);
        }
    }

    /**
     * Draws this path specifically as a fog of war region.
     *
     * @param c Canvas to draw on.
     */
    public void drawFogOfWar(final Canvas c) {
        // Ensure the static fog of war pen is created.
        if (fogOfWarPaint == null) {
            Paint p = new Paint();
            p.setColor(Color.RED);
            p.setAlpha(FOG_OF_WAR_ALPHA);
            p.setStyle(Paint.Style.FILL);
            fogOfWarPaint = p;
        }

        this.ensurePathCreated();
        if (this.mPath != null) {
            c.drawPath(this.mPath, fogOfWarPaint);
        }
    }

    /**
     * If there is no Paint object cached for this line, create one and set the
     * appropriate color and stroke width.
     */
    protected void ensurePaintCreated() {
        if (this.mPaint == null) {
            this.mPaint = new Paint();
            this.mPaint.setColor(this.mColor);
            if (this.getWidth() == Float.POSITIVE_INFINITY) {
                this.mPaint.setStyle(Paint.Style.FILL);
            } else {
                this.mPaint.setStrokeWidth(this.getWidth());
                this.mPaint.setStyle(Paint.Style.STROKE);
            }
        }
    }

    /**
     * Creates the path if it is currently invalid.
     */
    private void ensurePathCreated() {
        if (this.mPath == null) {
            this.mPath = this.createPath();
        }
    }

    /**
     * Erases the portion of this shape that falls within the given circle.
     *
     * @param center Center of the circle.
     * @param radius Radius of the circle.
     */
    public abstract void erase(final PointF center, final float radius);

    /**
     * Gets the smallest rectangle needed to fully enclose the line.
     *
     * @return The bounding rectangle.
     */
    public BoundingRectangle getBoundingRectangle() {
        return this.mBoundingRectangle;
    }

    /**
     * @return This shape's color.
     */
    public int getColor() {
        return this.mColor;
    }

    /**
     * @param deltaX Amount to move by in x dimension.
     * @param deltaY Amount to move by in Y dimension.
     * @return A *copy* of this shape that is moved by the given offset in world
     * space.
     */
    protected Shape getMovedShape(float deltaX, float deltaY) {
        // TODO: Implement this for each subclass, and make this abstract.
        throw new RuntimeException(
                "This shape does not support the move operation.");
    }

    /**
     * @return The paint object that should be used to draw this shape.
     */
    protected Paint getPaint() {
        this.ensurePaintCreated();
        return this.mPaint;
    }

    /**
     * @return This line's stroke width.
     */
    public float getStrokeWidth() {
        return this.getWidth();
    }

    /**
     * @return This shape's line width
     */
    public float getWidth() {
        return this.mWidth;
    }

    /**
     * @return Whether this shape has a temporary pending move operation.
     */
    public boolean hasOffset() {
        return this.mDrawOffsetDeltaX == this.mDrawOffsetDeltaX;
    }

    /**
     * Invalidates the path so that it is recreated on the next draw operation.
     */
    protected void invalidatePath() {
        this.mPath = null;
    }

    /**
     * Whether the shape is in a valid state. Subclasses should override this
     * with their own checks. If returns false, the shape may be: - Removed from
     * the line collection at any time. - Stopped from serializing.
     *
     * @return True if the shape is in a valid state, False otherwise.
     */
    public boolean isValid() {
        return true;
    }

    /**
     * @return True if this shape can be optimized.
     */
    public abstract boolean needsOptimization();

    /**
     * Optimizes this shape by removing erased points.
     *
     * @return A list of shapes that this shape optimizes to, since removing
     * erased points may create disjoint line segments.
     */
    public abstract List<Shape> removeErasedPoints();

    /**
     * Changes the given canvas's transformation to remove this draw offset.
     *
     * @param c The canvas to modify.
     */
    public void revertDrawOffsetFromCanvas(Canvas c) {
        if (this.hasOffset()) {
            c.restore();
        }
    }

    /**
     * Serializes this shape to the given stream. Must call serializeBase()
     *
     * @param s The stream to serialize to.
     * @throws IOException On serialization error.
     */
    public abstract void serialize(MapDataSerializer s) throws IOException;

    /**
     * Serializes shared attributes from the Shape base class.
     *
     * @param s         The shape to serialize.
     * @param shapeType Tag indicating the type of shape being serialized.
     * @throws IOException On serialization error.
     */
    protected void serializeBase(MapDataSerializer s, String shapeType)
            throws IOException {
        s.serializeString(shapeType);
        s.startObject();
        s.serializeInt(this.mColor);
        s.serializeFloat(this.getWidth());
        this.mBoundingRectangle.serialize(s);
        s.endObject();
    }

    /**
     * Sets the current shape's color.
     *
     * @param color The new color.
     */
    public void setColor(int color) {
        this.mColor = color;
    }

    /**
     * Sets a temporary offset for drawing this shape, which can be thought of
     * as a pending move operation. This will cause the shape to change the
     * tranformation until the operation is committed, which wipes the offset
     * data and returns a copy of the shape that is permanently modified with
     * the new offset. We do not directly modify this shape so that we can
     * support undo/redo.
     *
     * @param deltaX Amount to move the shape in X dimension.
     * @param deltaY Amount to move the shape in Y dimension.
     */
    public void setDrawOffset(float deltaX, float deltaY) {
        this.mDrawOffsetDeltaX = deltaX;
        this.mDrawOffsetDeltaY = deltaY;
    }

    /**
     * Sets the width of the current line.
     *
     * @param width The line width.
     */
    public void setWidth(float width) {
        this.mWidth = width;
    }

    /**
     * Template method that loads shape-specific data from the deserialization
     * stream.
     *
     * @param s Stream to read from.
     * @throws IOException On deserialization error.
     */
    protected abstract void shapeSpecificDeserialize(MapDataDeserializer s)
            throws IOException;

    /**
     * @return Whether this shape should be drawn below or above the grid.  By default, thin lines
     *     draw above, thick lines and filled shapes draw below.
     */
    public boolean shouldDrawBelowGrid() {
        return this.getWidth() > 1.0f;
    }

    public void setBoundingRectangle(PointF p1, PointF p2) {
        this.mBoundingRectangle = new BoundingRectangle(p1, p2);
    }


    /**
     * @return A copy of this token.
     */
    @Override
    public Shape clone() throws CloneNotSupportedException{
        return (Shape) super.clone();
    }
}