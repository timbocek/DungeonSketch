package com.tbocek.android.combatmap.model;

import android.graphics.Canvas;

import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.model.primitives.PointF;

/**
 * Base class for grid types (e.g. rectangular or hex). This has been seperated
 * out from the Grid class using the Strategy pattern to allow run-time changing
 * of grid type.
 * 
 * @author Tim
 */
public abstract class GridDrawStrategy {
    /**
     * Draws the grid lines.
     * 
     * @param canvas
     *            Canvas to draw on.
     * @param transformer
     *            Transformation from world space to screen space.
     * @param colorScheme
     *            The color scheme used to draw the grid.
     */
    public abstract void drawGrid(final Canvas canvas,
            final CoordinateTransformer transformer,
            final GridColorScheme colorScheme);

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
    public abstract PointF getNearestSnapPoint(final PointF currentLocation,
            final float tokenDiameter);

    /**
     * @return A string that can identify this draw strategy when saving and
     *         loading maps.
     */
    public abstract String getTypeString();
}
