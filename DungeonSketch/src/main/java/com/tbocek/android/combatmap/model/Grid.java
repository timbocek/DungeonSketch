package com.tbocek.android.combatmap.model;

import android.graphics.Canvas;

import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;
import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.model.primitives.PointF;

import java.io.IOException;

/**
 * Abstract class for the grid lines that are drawn on the main combat canvas.
 * 
 * @author Tim Bocek
 * 
 */
public class Grid {

    /**
     * The color scheme to use when drawing this grid.
     */
    private GridColorScheme mColorScheme = GridColorScheme.GRAPH_PAPER;

    private GridDrawStrategy mDrawStrategy = new RectangularGridStrategy();

    private String mUnits = "ft";
    private float mScale = 5;

    /**
     * The transformation from grid space to world space. We track this
     * seperately as a property of the grid so that the grid can easily be
     * resized to fit a drawing.
     */
    private CoordinateTransformer mGridToWorldTransformer =
            new CoordinateTransformer(0, 0, 1);

    /**
     * Default constructor
     */
    public Grid() {}

    /**
     * Copy constructor.
     * @param copyFrom Grid to copy from.
     */
    public Grid(Grid copyFrom) {
        mColorScheme = copyFrom.mColorScheme;
        mDrawStrategy = copyFrom.mDrawStrategy;
        mUnits = copyFrom.mUnits;
        mScale = copyFrom.mScale;
    }

    /**
     * Factory method that creates a grid with the given parameters.
     * 
     * @param gridStyle
     *            The style of the grid, either "hex" or "rectangular".
     * @param colorScheme
     *            The GridColorScheme object to use.
     * @param transformer
     *            A grid space to world space transformation to use in this
     *            grid.
     * @return The created grid.
     */
    public static Grid createGrid(final String gridStyle,
            final GridColorScheme colorScheme,
            final CoordinateTransformer transformer) {
        Grid g = new Grid();
        g.setDrawStrategy(gridStyle.equals(new HexGridStrategy()
                .getTypeString()) ? new HexGridStrategy()
                : new RectangularGridStrategy());
        g.mColorScheme = colorScheme;
        g.mGridToWorldTransformer = transformer;
        return g;
    }

    /**
     * Loads and returns a Grid object from the given deserialization stream.
     * 
     * @param s
     *            Stream to read from.
     * @return The loaded token object.
     * @throws IOException
     *             On deserialization error.
     */
    public static Grid deserialize(MapDataDeserializer s) throws IOException {
        s.expectObjectStart();
        String type = s.readString();
        GridColorScheme colorScheme = GridColorScheme.deserialize(s);
        CoordinateTransformer transform = CoordinateTransformer.deserialize(s);

        Grid g = createGrid(type, colorScheme, transform);
        if (!s.isObjectEnd()) {
            g.mScale = s.readFloat();
            g.mUnits = s.readString();
        }

        s.expectObjectEnd();
        return g;
    }

    /**
     * Draws the grid on the given canvas.
     * 
     * @param canvas
     *            The canvas to draw on.
     * @param transformer
     *            World space to screen space transformer (not grid to screen,
     *            since grid to world is defined in this class).
     */
    public final void draw(final Canvas canvas,
            final CoordinateTransformer transformer) {
        CoordinateTransformer transformer2 =
                this.gridSpaceToScreenSpaceTransformer(transformer);
        this.mDrawStrategy.drawGrid(canvas, transformer2, this.mColorScheme);
    }

    /**
     * Fills the canvas with the background color.
     * 
     * @param canvas
     *            The canvas to draw on.
     */
    public final void drawBackground(final Canvas canvas) {
        canvas.drawColor(this.getBackgroundColor());
    }

    /**
     * @return The color to use when drawing the background.
     */
    protected final int getBackgroundColor() {
        return this.mColorScheme.getBackgroundColor();
    }

    public GridColorScheme getColorScheme() {
        return this.mColorScheme;
    }

    public GridDrawStrategy getDrawStrategy() {
        return this.mDrawStrategy;
    }

    /**
     * Given a point, returns a the point nearest to that point that will draw a
     * circle of the given diameter snapped to the grid.
     * 
     * @param currentLocation
     *            The candidate point in grid space.
     * @param tokenDiameter
     *            Diameter of the token that will be drawn.
     * @return A point that is snapped to the grid.
     */
    public PointF getNearestSnapPoint(final PointF currentLocation,
            final float tokenDiameter) {
        return this.mDrawStrategy.getNearestSnapPoint(currentLocation,
                tokenDiameter);
    }

    /**
     * Gets a transformation between grid space and screen space, by composing
     * the known grid --> world transformation with the given world --> screen
     * transformation.
     * 
     * @param worldToScreen
     *            Transformation from world space to screen space.
     * @return The grid space to screen space transformation.
     */
    public final CoordinateTransformer gridSpaceToScreenSpaceTransformer(
            final CoordinateTransformer worldToScreen) {
        return this.mGridToWorldTransformer.compose(worldToScreen);
    }

    /**
     * Returns the stored transformation from grid space to world space.
     * 
     * @return The grid space to world space transformation.
     */
    public final CoordinateTransformer gridSpaceToWorldSpaceTransformer() {
        return this.mGridToWorldTransformer;
    }

    /**
     * @return Whether the grid has a dark background.
     */
    public final boolean isDark() {
        return this.mColorScheme.isDark();
    }

    /**
     * Writes this Grid object to the given serialization stream.
     * 
     * @param s
     *            Stream to write to.
     * @throws IOException
     *             On serialization error.
     */
    public void serialize(MapDataSerializer s) throws IOException {
        s.startObject();
        s.serializeString(this.mDrawStrategy.getTypeString());
        this.mColorScheme.serialize(s);
        this.mGridToWorldTransformer.serialize(s);
        s.serializeFloat(this.mScale);
        s.serializeString(this.mUnits);
        s.endObject();
    }

    public void setColorScheme(GridColorScheme scheme) {
        this.mColorScheme = scheme;

    }

    public String getUnits() {
        return mUnits;
    }

    public void setUnits(String units) {
        mUnits = units;
    }

    public float getScale() {
        return mScale;
    }

    public void setScale(float scale) {
        mScale = scale;
    }

    public void setDrawStrategy(GridDrawStrategy s) {
        this.mDrawStrategy = s;
    }

}