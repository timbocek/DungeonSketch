package com.tbocek.android.combatmap.model.primitives;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.Path;

import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;

/**
 * Shape class that represents a single straight line segment. Contains methods
 * for manipulating portions of the line segment based on a parameterization of
 * the segment.
 * 
 * @author Tim
 * 
 */
public class StraightLine extends Shape {

    /**
     * Short character string that is the type of the shape.
     */
    public static final String SHAPE_TYPE = "sl";

    /**
     * Second endpoint on the line. X coordinate guaranteed to be greater than x
     * coordinate of mStart.
     */
    private PointF mEnd;

    /**
     * Where to toggle the line on and off, for erasing purposes. These values
     * are parameterized by the length of the line, so that all values fall in
     * the range [0,1].
     */
    private List<Float> mLineToggleParameterization;

    /**
     * First endpoint on the line. X coordinate guaranteed to be less than x
     * coordinate of mEnd.
     */
    private PointF mStart;

    /**
     * Constructor.
     * 
     * @param color
     *            Color of the line.
     * @param newLineStrokeWidth
     *            Stroke width of the line.
     */
    public StraightLine(int color, float newLineStrokeWidth) {
        this.setColor(color);
        this.setWidth(newLineStrokeWidth);
    }

    @Override
    public void addPoint(PointF p) {
        if (this.mStart == null) {
            this.mStart = p;
        } else {
            this.mEnd = p;
            // Re-create the bounding rectangle every time this is done.
            this.getBoundingRectangle().clear();
            this.getBoundingRectangle().updateBounds(this.mStart);
            this.getBoundingRectangle().updateBounds(this.mEnd);
            this.invalidatePath();
        }
    }

    /**
     * Makes sure that start point's X coordinate occurs before the end point's
     * X coordinate. This assumption is used elsewhere.
     */
    private void canonicalizePointOrder() {
        if (this.mEnd.x < this.mStart.x) {
            PointF tmp;
            tmp = this.mStart;
            this.mStart = this.mEnd;
            this.mEnd = tmp;
        }
    }

    @Override
    public boolean contains(PointF p) {
        // Cannot define a region.
        return false;
    }

    @Override
    public Path createPath() {
        if (this.mStart == null || this.mEnd == null) {
            return null;
        }
        Path path = new Path();

        if (this.mLineToggleParameterization != null) {
            // Erasing has happened, follow erasing instructions.
            boolean on = false;
            for (float toggleT : this.mLineToggleParameterization) {
                PointF togglePoint = this.parameterizationToPoint(toggleT);
                if (on) {
                    path.lineTo(togglePoint.x, togglePoint.y);
                } else {
                    path.moveTo(togglePoint.x, togglePoint.y);
                }
                on = !on;
            }
        } else {
            path.moveTo(this.mStart.x, this.mStart.y);
            path.lineTo(this.mEnd.x, this.mEnd.y);
        }

        return path;
    }

    @Override
    public void erase(PointF center, float radius) {
        if (this.mStart == null
                || this.mEnd == null
                || !this.getBoundingRectangle().intersectsWithCircle(center,
                        radius)) {
            return;
        }

        this.canonicalizePointOrder();

        // Special case - if we have only two points, this is probably
        // a large straight line and we want to erase the line if the
        // eraser intersects with it. However, this is an expensive
        // test, so we don't want to do it for all line segments when
        // they are generally small enough for the eraser to enclose.
        Util.IntersectionPair intersection =
                Util.lineCircleIntersection(this.mStart, this.mEnd, center,
                        radius);

        if (intersection != null) {
            float intersect1T =
                    this.pointToParameterization(intersection
                            .getIntersection1());
            float intersect2T =
                    this.pointToParameterization(intersection
                            .getIntersection2());

            this.insertErasedSegment(intersect1T, intersect2T);
            this.invalidatePath();
        }
    }

    /**
     * 
     * @param segmentStart
     *            Start of the erased segment, parameterized by the length of
     *            the line.
     * @param segmentEnd
     *            End of the erased segment, parameterized by the length of the
     *            line.
     */
    void insertErasedSegment(float segmentStart, float segmentEnd) {
        // Make sure first intersections are ordered
        float tmp;
        if (segmentStart > segmentEnd) {
            tmp = segmentStart;
            segmentStart = segmentEnd;
            segmentEnd = tmp;
        }

        if (this.mLineToggleParameterization == null) {
            this.mLineToggleParameterization = new ArrayList<Float>();
            this.mLineToggleParameterization.add(0f);
            this.mLineToggleParameterization.add(1f);
        }

        // Location in the array before which to insert the first segment
        int segmentStartInsertion =
                Collections.binarySearch(this.mLineToggleParameterization,
                        segmentStart);
        if (segmentStartInsertion < 0) {
            segmentStartInsertion = -segmentStartInsertion - 1;
        }
        boolean startInDrawnRegion = segmentStartInsertion % 2 != 0;

        // Location in the array before which to insert the last segment.
        int segmentEndInsertion =
                -Collections.binarySearch(this.mLineToggleParameterization,
                        segmentEnd) - 1;
        if (segmentEndInsertion < 0) {
            segmentEndInsertion = -segmentEndInsertion - 1;
        }
        boolean endInDrawnRegion = segmentEndInsertion % 2 != 0;

        // Remove all segment starts or ends between the insertion points.
        // If we were to run the binary search again, segmentStartInsertion
        // should
        // remain unchanged and segmentEndInsertion should be equal to
        // segmentStartInsertion.
        // Guard this by making sure we don't try to remove from the end of the
        // list.
        if (segmentStartInsertion != this.mLineToggleParameterization.size()) {
            for (int i = 0; i < segmentEndInsertion - segmentStartInsertion; ++i) {
                this.mLineToggleParameterization.remove(segmentStartInsertion);
            }
        }

        if (endInDrawnRegion) {
            this.mLineToggleParameterization.add(segmentStartInsertion,
                    segmentEnd);
        }

        if (startInDrawnRegion) {
            this.mLineToggleParameterization.add(segmentStartInsertion,
                    segmentStart);
        }
    }

    @Override
    public boolean isValid() {
        return this.mStart != null && this.mEnd != null;
    }

    @Override
    public boolean needsOptimization() {
        return this.mLineToggleParameterization != null;
    }

    /**
     * Given a float in the range [0,1] that represents a distance along this
     * line scaled to the length of the line, returns a the coordinates where
     * that distance occurs on the line.
     * 
     * @param t
     *            The parameterized distance.
     * @return The point where that parameterization occurs on the line.
     */
    private PointF parameterizationToPoint(float t) {
        return new PointF(this.mStart.x + t * (this.mEnd.x - this.mStart.x),
                this.mStart.y + t * (this.mEnd.y - this.mStart.y));
    }

    /**
     * Given a point, gives a distance scaled to the length of the line where
     * that point falls on the line.
     * 
     * @param p
     *            The point to parameterize. Must fall on the line.
     * @return Distance along the line segment where the point falls, scaled to
     *         the range [0,1].
     */
    private float pointToParameterization(PointF p) {
        if (Math.abs(this.mEnd.y - this.mStart.y) > Math.abs(this.mEnd.x
                - this.mStart.x)) {
            return (p.y - this.mStart.y) / (this.mEnd.y - this.mStart.y);
        } else {
            return (p.x - this.mStart.x) / (this.mEnd.x - this.mStart.x);
        }
    }

    @Override
    public List<Shape> removeErasedPoints() {
        List<Shape> shapes = new ArrayList<Shape>();

        if (this.mLineToggleParameterization.size() > 0) {
            for (int i = 0; i < this.mLineToggleParameterization.size(); i += 2) {
                float startT = this.mLineToggleParameterization.get(i);
                float endT = this.mLineToggleParameterization.get(i + 1);

                StraightLine l =
                        new StraightLine(this.getColor(), this.getWidth());
                l.addPoint(this.parameterizationToPoint(startT));
                l.addPoint(this.parameterizationToPoint(endT));
                shapes.add(l);
            }
        }
        this.mLineToggleParameterization = null;
        this.invalidatePath();
        return shapes;
    }

    @Override
    public void serialize(MapDataSerializer s) throws IOException {
        this.serializeBase(s, SHAPE_TYPE);

        s.startObject();
        s.serializeFloat(this.mStart.x);
        s.serializeFloat(this.mStart.y);
        s.serializeFloat(this.mEnd.x);
        s.serializeFloat(this.mEnd.y);
        s.endObject();
    }

    @Override
    protected void shapeSpecificDeserialize(MapDataDeserializer s)
            throws IOException {
        s.expectObjectStart();
        this.mStart = new PointF();
        this.mStart.x = s.readFloat();
        this.mStart.y = s.readFloat();
        this.mEnd = new PointF();
        this.mEnd.x = s.readFloat();
        this.mEnd.y = s.readFloat();
        s.expectObjectEnd();
    }
}
