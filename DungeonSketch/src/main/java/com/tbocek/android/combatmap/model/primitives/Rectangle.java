package com.tbocek.android.combatmap.model.primitives;

import android.graphics.Path;

import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a drawn rectangle.
 * 
 * @author Tim
 * 
 */
public class Rectangle extends Shape {
    /**
     * Short character string that is the type of the shape.
     */
    public static final String SHAPE_TYPE = "rct";

    /**
     * Line to use when erasing portions of the rectangle.
     */
    private FreehandLine mLineForErasing;

    /**
     * Lower left corner of the rectangle.
     */
    private PointF mP1;

    /**
     * Upper right corner of the rectangle.
     */
    private PointF mP2;

    /**
     * Constructor.
     * 
     * @param color
     *            Line color.
     * @param width
     *            Stroke width.
     */
    public Rectangle(int color, float width) {
        this.setColor(color);
        this.setWidth(width);
    }

    @Override
    public void addPoint(PointF p) {
        if (this.mP1 == null) {
            this.mP1 = p;
        } else {
            this.mP2 = p;
            // Re-create the bounding rectangle every time this is done.
            this.getBoundingRectangle().clear();
            this.getBoundingRectangle().updateBounds(this.mP1);
            this.getBoundingRectangle().updateBounds(this.mP2);
            this.invalidatePath();
        }

    }

    @Override
    public boolean contains(PointF p) {
        return this.getBoundingRectangle().contains(p);
    }

    @Override
    protected Path createPath() {
        if (!this.isValid()) {
            return null;
        }
        if (this.mLineForErasing != null) {
            return this.mLineForErasing.createPath();
        } else {
            Path p = new Path();

            p.addRect(Math.min(this.mP1.x, this.mP2.x),
                    Math.min(this.mP1.y, this.mP2.y),
                    Math.max(this.mP1.x, this.mP2.x),
                    Math.max(this.mP1.y, this.mP2.y), Path.Direction.CW);
            return p;
        }
    }

    @Override
    public void erase(PointF center, float radius) {
        if (this.getBoundingRectangle().intersectsWithCircle(center, radius)) {
            if (this.mLineForErasing == null) {
                float xmin = Math.min(this.mP1.x, this.mP2.x);
                float ymin = Math.min(this.mP1.y, this.mP2.y);
                float xmax = Math.max(this.mP1.x, this.mP2.x);
                float ymax = Math.max(this.mP1.y, this.mP2.y);
                this.mLineForErasing =
                        new FreehandLine(this.getColor(), this.getStrokeWidth());
                this.mLineForErasing.addPoint(new PointF(xmin, ymin));
                this.mLineForErasing.addPoint(new PointF(xmin, ymax));
                this.mLineForErasing.addPoint(new PointF(xmax, ymax));
                this.mLineForErasing.addPoint(new PointF(xmax, ymin));
                this.mLineForErasing.addPoint(new PointF(xmin, ymin));
            }
            this.mLineForErasing.erase(center, radius);
            this.invalidatePath();
        }

    }

    @Override
    public boolean isValid() {
        return this.mP2 != null && this.mP1 != null;
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
        s.serializeFloat(this.mP1.x);
        s.serializeFloat(this.mP1.y);
        s.serializeFloat(this.mP2.x);
        s.serializeFloat(this.mP2.y);
        s.endObject();

    }

    @Override
    protected void shapeSpecificDeserialize(MapDataDeserializer s)
            throws IOException {
        s.expectObjectStart();
        this.mP1 = new PointF();
        this.mP1.x = s.readFloat();
        this.mP1.y = s.readFloat();
        this.mP2 = new PointF();
        this.mP2.x = s.readFloat();
        this.mP2.y = s.readFloat();
        s.expectObjectEnd();
    }


    protected Shape getMovedShape(float deltaX, float deltaY) {
        Rectangle r = new Rectangle(getColor(), this.getStrokeWidth());

        r.mP1 = new PointF(this.mP1.x + deltaX, this.mP1.y + deltaY);
        r.mP2 = new PointF(this.mP2.x + deltaX, this.mP2.y + deltaY);
        r.getBoundingRectangle().updateBounds(this.getBoundingRectangle());
        r.getBoundingRectangle().move(deltaX, deltaY);
        return r;
    }
}
