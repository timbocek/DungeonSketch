package com.tbocek.android.combatmap.model.primitives;

import android.graphics.Path;

import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a single vector-drawn line.
 * 
 * @author Tim
 * 
 */
public final class FreehandLine extends Shape {

    /**
     * Short character string that is the type of the shape.
     */
    public static final String SHAPE_TYPE = "fh";

    /**
     * When a segment of this freehand line has only a portion erased, the
     * resulting new line segments are placed in this array.
     */
    private transient List<StraightLine> mPartiallyErasedLineSegments =
            new ArrayList<StraightLine>();

    /**
     * The points that comprise this line.
     */
    private final List<PointF> mPoints = new ArrayList<PointF>();

    /**
     * Whether each point in the line should be drawn. This allows us to
     * temporarily suppress drawing the points when the line is being erased.
     * However, it's only a temporary fix; the line should later be optimized so
     * that points that shouldn't draw get removed instead.
     */
    private final List<Boolean> mShouldDraw = new ArrayList<Boolean>();

    /**
     * Constructor.
     * 
     * @param color
     *            Line color.
     * @param newLineStrokeWidth
     *            Line stroke width.
     */
    public FreehandLine(final int color, final float newLineStrokeWidth) {
        this.setColor(color);
        this.setWidth(newLineStrokeWidth);
    }

    /**
     * Adds the given point to the line.
     * 
     * @param p
     *            The point to add.
     */
    @Override
    public void addPoint(final PointF p) {
        this.mPoints.add(p);
        this.mShouldDraw.add(true);
        this.getBoundingRectangle().updateBounds(p);
        this.invalidatePath();
    }

    /**
     * Checks whether this point falls in the polygon created by closing this
     * path.
     * 
     * @param p
     *            The point to test
     * @return True if the polygon contains the point.
     */
    @Override
    public boolean contains(PointF p) {
        // First, check whether the bounding rectangle contains the point so
        // we can efficiently and quickly get rid of easy cases.
        if (!this.getBoundingRectangle().contains(p)) {
            return false;
        }

        // This algorithm adapted from http://alienryderflex.com/polygon/
        // The algorithm tests whether a horizontal line drawn through the test
        // point intersects an odd number of polygon sides to the left of the
        // point.

        // i and j store consecutive points, so they define a line segment.
        // Start with the line segment from the last point to the first point.
        int j = this.mPoints.size() - 1;
        boolean oddNodes = false;
        for (int i = 0; i < this.mPoints.size(); ++i) {
            PointF pj = this.mPoints.get(j);
            PointF pi = this.mPoints.get(i);

            // Check if the test point is in between the y coordinates of the
            // two points that make up this line segment. This checks two
            // conditions: whether the horizontal line has an intersection
            // (avoids division by 0), and whether the intersection between
            // the extruded line and horizontal line occurs on the line segment.
            if (pi.y < p.y && pj.y >= p.y || pj.y < p.y && pi.y >= p.y) {
                // Check if the horizontal line/line segment intersectino occurs
                // to the left of the test point.
                if (pi.x + (p.y - pi.y) / (pj.y - pi.y) * (pj.x - pi.x) < p.x) {
                    oddNodes = !oddNodes;
                }
            }
            j = i;
        }
        return oddNodes;
    }

    /**
     * Creates a new Path object that draws this shape.
     * 
     * @return The created path;
     */
    @Override
    protected Path createPath() {
        // Do not try to draw a line with too few points.
        if (this.mPoints.size() < 2) {
            return null;
        }

        Path path = new Path();
        boolean penDown = false;
        for (int i = 0; i < this.mPoints.size(); ++i) {
            PointF p1 = this.mPoints.get(i);
            if (penDown) {
                path.lineTo(p1.x, p1.y);
            } else {
                path.moveTo(p1.x, p1.y);
            }
            penDown = this.mShouldDraw.get(i);
        }

        if (this.mPartiallyErasedLineSegments == null) {
            this.mPartiallyErasedLineSegments = new ArrayList<StraightLine>();
        }

        for (StraightLine l : this.mPartiallyErasedLineSegments) {
            path.addPath(l.createPath());
        }

        return path;
    }

    /**
     * Erases all points in the line that fall in the circle specified by the
     * given center and radius. This does not delete the points, just marks them
     * as erased. removeErasedPoints() needs to be called afterward to get the
     * true result of the erase operation.
     * 
     * @param center
     *            Center of the circle to erase.
     * @param radius
     *            Radius of the circle to erase.
     */
    @Override
    public void erase(final PointF center, final float radius) {
        if (this.getBoundingRectangle().intersectsWithCircle(center, radius)) {
            if (this.mPartiallyErasedLineSegments == null) {
                this.mPartiallyErasedLineSegments =
                        new ArrayList<StraightLine>();
            }

            for (StraightLine sl : this.mPartiallyErasedLineSegments) {
                sl.erase(center, radius);
            }

            for (int i = 0; i < this.mPoints.size() - 1; ++i) {
                if (this.mShouldDraw.get(i)) {
                    PointF p1 = this.mPoints.get(i);
                    PointF p2 = this.mPoints.get(i + 1);
                    Util.IntersectionPair intersection =
                            Util.lineCircleIntersection(p1, p2, center, radius);
                    if (intersection != null) {
                        this.mShouldDraw.set(i, false);
                        StraightLine sl =
                                new StraightLine(this.getColor(),
                                        this.getWidth());
                        sl.addPoint(p1);
                        sl.addPoint(p2);
                        sl.erase(center, radius);
                        this.mPartiallyErasedLineSegments.add(sl);
                    }
                }
            }
        }
        this.invalidatePath();
    }

    /**
     * @return True if an optimization pass is needed on this line (i.e. if it
     *         was just erased).
     */
    @Override
    public boolean needsOptimization() {
        for (boolean b : this.mShouldDraw) {
            if (!b) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list of lines that are created by removing any erased points
     * from this line. There will often be more than one line returned, as it is
     * a common case to erase the middle of the line. The current line is set to
     * draw all points again.
     * 
     * @return A list of lines that results from removing erased points.
     */
    @Override
    public List<Shape> removeErasedPoints() {
        List<Shape> optimizedLines = new ArrayList<Shape>();
        FreehandLine l = new FreehandLine(this.getColor(), this.getWidth());
        optimizedLines.add(l);
        for (int i = 0; i < this.mPoints.size(); ++i) {
            l.addPoint(this.mPoints.get(i));
            if (!this.mShouldDraw.get(i)) {
                // Do not add a line with only one point in it, those are
                // useless
                if (l.mPoints.size() <= 1) {
                    optimizedLines.remove(l);
                }
                l = new FreehandLine(this.getColor(), this.getWidth());
                optimizedLines.add(l);
            }
            this.mShouldDraw.set(i, true);
        }

        for (StraightLine sl : this.mPartiallyErasedLineSegments) {
            if (sl.needsOptimization()) {
                optimizedLines.addAll(sl.removeErasedPoints());
            } else {
                optimizedLines.add(sl);
            }
        }
        this.mPartiallyErasedLineSegments = new ArrayList<StraightLine>();

        // shouldDraw was reset, path is invalid
        this.invalidatePath();

        return optimizedLines;
    }

    @Override
    public void serialize(MapDataSerializer s) throws IOException {
        this.serializeBase(s, SHAPE_TYPE);
        s.startObject();
        s.startArray();
        for (PointF p : this.mPoints) {
            s.serializeFloat(p.x);
            s.serializeFloat(p.y);
        }
        s.endArray();
        s.endObject();
    }

    @Override
    protected void shapeSpecificDeserialize(MapDataDeserializer s)
            throws IOException {
        s.expectObjectStart();
        int arrayLevel = s.expectArrayStart();
        while (s.hasMoreArrayItems(arrayLevel)) {
            PointF p = new PointF();
            p.x = s.readFloat();
            p.y = s.readFloat();
            this.mPoints.add(p);
            this.mShouldDraw.add(true);
        }
        s.expectArrayEnd();
        s.expectObjectEnd();
    }
}
