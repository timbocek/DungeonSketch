package com.tbocek.android.combatmap.model.primitives;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import com.google.common.collect.Lists;
import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;

import java.io.IOException;
import java.util.List;

/**
 * Defines a transformation from one 2D coordinate system to another coordinate
 * system. The class is written in terms of a world space/screen space
 * conversion.
 * 
 * @author Tim Bocek
 * 
 */
public final class CoordinateTransformer {

    /**
     * Upper-left corner's x coordinate in screen space.
     */
    private float mOriginX = 0.0f;

    /**
     * Upper-left corner's y coordinate in screen space.
     */
    private float mOriginY = 0.0f;

    /**
     * Conversion of lengths in world space to lengths in screen space.
     */
    private float mZoomLevel = 1.0f;

    /**
     * Creates and loads a coordinate transform object from the given stream.
     * 
     * @param s
     *            Stream to load from.
     * @return The loaded CoordinateTransformer object.
     * @throws IOException
     *             On deserialization error.
     */
    public static CoordinateTransformer deserialize(MapDataDeserializer s)
            throws IOException {
        s.expectObjectStart();
        float x = s.readFloat();
        float y = s.readFloat();
        float zoom = s.readFloat();
        s.expectObjectEnd();
        return new CoordinateTransformer(x, y, zoom);
    }

    /**
     * Constructor.
     * 
     * @param originX
     *            X coordinate of the screen space origin.
     * @param originY
     *            Y coordinate of the screen space origin.
     * @param zoomLevel
     *            Conversion of world space to screen space lengths.
     */
    public CoordinateTransformer(final float originX, final float originY,
            final float zoomLevel) {
        this.mOriginX = originX;
        this.mOriginY = originY;
        this.mZoomLevel = zoomLevel;
    }

    /**
     * Copy Constructor
     */
    public CoordinateTransformer(CoordinateTransformer other) {
        this.mOriginX = other.mOriginX;
        this.mOriginY = other.mOriginY;
        this.mZoomLevel = other.mZoomLevel;
    }

    /**
     * Given a mapping between (x, y) -> (x', y') and a mapping between (x', y')
     * -> (x'', y''), returns a CoordinateTransformer that maps (x, y) -> (x'',
     * y'').
     * 
     * @param second
     *            The other transformation to compose this transformation with.
     * @return The composed transformation.
     */
    public CoordinateTransformer compose(final CoordinateTransformer second) {
        return new CoordinateTransformer(
                second.worldSpaceToScreenSpace(this.mOriginX) + second.mOriginX,
                second.worldSpaceToScreenSpace(this.mOriginY) + second.mOriginY,
                this.mZoomLevel * second.mZoomLevel);
    }

    /**
     * Returns the upper-left-hand corner of the screen in screen space.
     * 
     * @return The origin.
     */
    public PointF getOrigin() {
        return new PointF(this.mOriginX, this.mOriginY);
    }

    /**
     * Moves the origin by the specified amount.
     * 
     * @param dx
     *            Amount to move in x dimension.
     * @param dy
     *            Amount to move in y dimension.
     */
    public void moveOrigin(final float dx, final float dy) {
        this.mOriginX += dx;
        this.mOriginY += dy;
    }

    /**
     * Converts the given distance in screen space to world space.
     * 
     * @param d
     *            Distance in screen space.
     * @return Distance in world space.
     */
    public float screenSpaceToWorldSpace(final float d) {
        return d / this.mZoomLevel;
    }

    /**
     * Converts the given point in screen space to world space.
     * 
     * @param x
     *            X coordinate in screen space.
     * @param y
     *            Y coordinate in screen space.
     * @return The coordinate in world space.
     */
    public PointF screenSpaceToWorldSpace(final float x, final float y) {
        return new PointF((x - this.mOriginX) / this.mZoomLevel,
                (y - this.mOriginY) / this.mZoomLevel);
    }
    

    /**
     * Converts the given point in screen space to world space.
     * 
     * @param sscoord
     *            The coordinate in screen space.
     * @return The coordinate in world space.
     */
    public PointF screenSpaceToWorldSpace(final PointF sscoord) {
        return this.screenSpaceToWorldSpace(sscoord.x, sscoord.y);
    }
   

    /**
     * Saves this coordinate transform to the given stream.
     * 
     * @param s
     *            The serialization stream to save to.
     * @throws IOException
     *             On serialization error.
     */
    public void serialize(MapDataSerializer s) throws IOException {
        s.startObject();
        s.serializeFloat(this.mOriginX);
        s.serializeFloat(this.mOriginY);
        s.serializeFloat(this.mZoomLevel);
        s.endObject();
    }

    /**
     * Uses an inverse of this coordinate transform object to modify the given
     * canvas's draw transformation matrix.
     * 
     * @param c
     *            The canvas to modify
     */
    public void setInverseMatrix(Canvas c) {
        c.scale(1 / this.mZoomLevel, 1 / this.mZoomLevel);
        c.translate(-this.mOriginX, -this.mOriginY);
    }

    /**
     * Uses this coordinate transform object to modify the given canvas's draw
     * transformation matrix.
     * 
     * @param c
     *            The canvas to modify
     */
    public void setMatrix(Canvas c) {
        c.translate(this.mOriginX, this.mOriginY);
        c.scale(this.mZoomLevel, this.mZoomLevel);
    }

    /**
     * Converts the given distance in world space to screen space.
     * 
     * @param d
     *            Distance in world space.
     * @return Distance in screen space.
     */
    public float worldSpaceToScreenSpace(final float d) {
        return d * this.mZoomLevel;
    }

    /**
     * Converts the given point in world space to screen space.
     * 
     * @param x
     *            X coordinate in world space.
     * @param y
     *            Y coordinate in world space.
     * @return The coordinate in screen space.
     */
    public PointF worldSpaceToScreenSpace(final float x, final float y) {
        return new PointF(this.mZoomLevel * x + this.mOriginX, this.mZoomLevel
                * y + this.mOriginY);
    }

    /**
     * Converts the given point in world space to screen space.
     * 
     * @param wscoord
     *            The coordinate in world space.
     * @return The coordinate in screen space.
     */
    public PointF worldSpaceToScreenSpace(final PointF wscoord) {
        return this.worldSpaceToScreenSpace(wscoord.x, wscoord.y);
    }
    
    public Rect worldSpaceToScreenSpace(final RectF wsrect) {
    	PointF topLeft = worldSpaceToScreenSpace(wsrect.left, wsrect.top);
    	PointF bottomRight = worldSpaceToScreenSpace(wsrect.right, wsrect.bottom);
    	
    	return new Rect((int)topLeft.x, (int)topLeft.y, (int)bottomRight.x, (int)bottomRight.y);
    }

    /**
     * Changes the scale of the transformation.
     * 
     * @param scaleFactor
     *            Amount to change the zoom level by
     * @param invariant
     *            Screen space point that should map to the same world space
     *            point before and after the transformation.
     */
    public void zoom(final float scaleFactor, final PointF invariant) {
        float lastZoomLevel = this.mZoomLevel;
        float lastOriginX = this.mOriginX;
        float lastOriginY = this.mOriginY;

        this.mZoomLevel *= scaleFactor;

        // Change the origin so that we zoom around the focus point.
        // Derived by assuming that the focus point should map to the same point
        // in world space before and after the zoom.
        this.mOriginX =
                invariant.x - (invariant.x - lastOriginX) * this.mZoomLevel
                        / lastZoomLevel;
        this.mOriginY =
                invariant.y - (invariant.y - lastOriginY) * this.mZoomLevel
                        / lastZoomLevel;

    }

    /**
     * Creates a list of coordinate transformers that splits the map into a grid of sub-maps.
     * @param width The width of the map in world space.
     * @param height The height of the map in world space.
     * @param numHorizontal The number of horizontal submaps.
     * @param numVertical The number of vertical submaps.
     * @return The list of coordinate transformers.
     */
    public List<CoordinateTransformer> splitMap(
            float width, float height, int numHorizontal, int numVertical) {
        List<CoordinateTransformer> transformers = Lists.newArrayList();

        float submapWidth = (width / numHorizontal);
        float submapHeight = (height / numVertical);

        for (int j = 0; j < numVertical; ++j) {
            for (int i = 0; i < numHorizontal; ++i) {
                transformers.add(new CoordinateTransformer(
                        this.mOriginX - submapWidth * i,
                        this.mOriginY - submapHeight * j,
                        this.mZoomLevel));
            }
        }

        return transformers;
    }
}