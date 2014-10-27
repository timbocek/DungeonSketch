package com.tbocek.android.combatmap.model.primitives;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Path;

import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;

/**
 * Represents a circle drawn on the map.
 * 
 * @author Tim
 * 
 */
public class Circle extends Shape {

    /**
     * When converting the circle into a freehand line that is a polygon to
     * approximate a circle, the number of line segments to use.
     */
    private static final int FREEHAND_LINE_CONVERSION_SEGMENTS = 64;

    /**
     * Short character string that is the type of the shape.
     */
    public static final String SHAPE_TYPE = "cr";

    /**
     * Center of the circle, in world space.
     */
    private PointF mCenter;

    /**
     * When the user starts erasing a circle, it is converted into a freehand
     * line for easier erasing.
     */
    private FreehandLine mLineForErasing;

    /**
     * Radius of the circle, in world space.
     */
    private float mRadius;

    /**
     * A point on the edge of the circle, where the user first placed his
     * finger.
     */
    private PointF mStartPoint;

    /**
     * Constructor from line properties. Center and radius to be set later.
     * 
     * @param color
     *            Color of the new circle.
     * @param newLineStrokeWidth
     *            Stroke width of the new circle.
     */
    public Circle(int color, float newLineStrokeWidth) {
        this.setColor(color);
        this.setWidth(newLineStrokeWidth);
    }

    @Override
    public void addPoint(PointF p) {
        if (this.mStartPoint == null) {
            this.mStartPoint = p;
        } else {
            // Create a circle where the line from startPoint to P is a
            // diameter.
            this.mRadius = Util.distance(this.mStartPoint, p) / 2;
            this.mCenter =
                    new PointF((p.x + this.mStartPoint.x) / 2,
                            (p.y + this.mStartPoint.y) / 2);
            this.invalidatePath();
            this.getBoundingRectangle().clear();
            this.getBoundingRectangle().updateBounds(
                    new PointF(this.mCenter.x - this.mRadius, this.mCenter.y
                            - this.mRadius));
            this.getBoundingRectangle().updateBounds(
                    new PointF(this.mCenter.x + this.mRadius, this.mCenter.y
                            + this.mRadius));
        }
    }

    @Override
    public boolean contains(PointF p) {
        if (this.mCenter == null) {
            return false;
        }
        return Util.distance(p, this.mCenter) < this.mRadius;
    }

    /**
     * Converts this circle into a freehand line that approximates a circle, so
     * that we can then erase segments of the freehand line.
     */
    private void createLineForErasing() {
        this.mLineForErasing =
                new FreehandLine(this.getColor(), this.getWidth());
        for (float rad = 0; rad < 2 * Math.PI; rad +=
                2 * Math.PI / FREEHAND_LINE_CONVERSION_SEGMENTS) {
            this.mLineForErasing.addPoint(new PointF(this.mCenter.x
                    + this.mRadius * (float) Math.cos(rad), this.mCenter.y
                    + this.mRadius * (float) Math.sin(rad)));
        }
    }

    @Override
    protected Path createPath() {
        if (this.mCenter == null || Float.isNaN(this.mRadius)) {
            return null;
        }

        if (this.mLineForErasing != null) {
            return this.mLineForErasing.createPath();
        } else {
            Path p = new Path();
            p.addCircle(this.mCenter.x, this.mCenter.y, this.mRadius,
                    Path.Direction.CW);
            return p;
        }
    }

    @Override
    public void erase(PointF center, float radius) {
        if (this.mLineForErasing != null) {
            this.mLineForErasing.erase(center, radius);
            this.invalidatePath();
            return;
        }

        if (!this.getBoundingRectangle().intersectsWithCircle(center, radius)) {
            return;
        }

        float d = Util.distance(this.mCenter, center);

        if (d <= radius + this.mRadius && d >= Math.abs(radius - this.mRadius)) {
            this.createLineForErasing();
            this.mLineForErasing.erase(center, radius);
            this.invalidatePath();
        }
    }

    @Override
    public boolean isValid() {
        return this.mRadius == this.mRadius && this.mCenter != null;
    }

    @Override
    public boolean needsOptimization() {
        return this.mLineForErasing != null;
    }

    @Override
    public List<Shape> removeErasedPoints() {
        List<Shape> l = new ArrayList<Shape>();
        l.add(this.mLineForErasing);
        this.mLineForErasing = null;
        this.invalidatePath();
        return l;
    }

    @Override
    public void serialize(MapDataSerializer s) throws IOException {
        this.serializeBase(s, SHAPE_TYPE);
        s.startObject();
        s.serializeFloat(this.mRadius);
        s.serializeFloat(this.mCenter.x);
        s.serializeFloat(this.mCenter.y);
        s.endObject();
    }

    @Override
    protected void shapeSpecificDeserialize(MapDataDeserializer s)
            throws IOException {
        s.expectObjectStart();
        this.mRadius = s.readFloat();
        this.mCenter = new PointF();
        this.mCenter.x = s.readFloat();
        this.mCenter.y = s.readFloat();
        s.expectObjectEnd();
    }
}
