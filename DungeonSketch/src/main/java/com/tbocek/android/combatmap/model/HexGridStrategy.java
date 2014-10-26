package com.tbocek.android.combatmap.model;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.model.primitives.Util;

/**
 * A grid based on tesselated hexagons.
 * 
 * @author Tim Bocek
 */
public final class HexGridStrategy extends GridDrawStrategy {
    /**
     * Cached result for the cos(30 degrees).
     */
    private static final float COSINE_30_DEGREES =
    // CHECKSTYLE:OFF
            (float) Math.cos(30 * Math.PI / 180);
    // CHECKSTYLE:ON

    /**
     * Cached result for the cos(30 degrees).
     */
    private static final float COSINE_60_DEGREES =
    // CHECKSTYLE:OFF
            (float) Math.cos(60 * Math.PI / 180);
    // CHECKSTYLE:ON

    /**
     * Square size at which grid elements will stop being drawn.
     */
    private static final float MIN_SQUARE_SIZE = 15;

    @Override
    public void drawGrid(final Canvas canvas,
            final CoordinateTransformer transformer,
            final GridColorScheme colorScheme) {
        Paint paint = new Paint();
        paint.setColor(colorScheme.getLineColor());

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // The height of each hexagonal element.
        float h = transformer.worldSpaceToScreenSpace(1.0f);

        // Length of any line segment of the hexagon.
        float l = .5f * h / COSINE_30_DEGREES;

        float innerOffset = l * COSINE_60_DEGREES;

        if (h < MIN_SQUARE_SIZE) {
            return;
        }

        float numSquaresHorizontal = (float) width / (l + innerOffset);
        float numSquaresVertical =
                numSquaresHorizontal * ((float) height) / ((float) width);

        PointF origin = transformer.getOrigin();

        float offsetX = origin.x % (l + innerOffset);
        float offsetY = origin.y % h;

        int innerPointStartX =
                (int) ((origin.x % ((l + innerOffset) * 2)) / (l + innerOffset));

        // Draw the vertical undulating "lines". We want to start slightly
        // offscreen
        for (int j = -1; j <= numSquaresHorizontal + 1; ++j) {
            float x = j * (l + innerOffset) + offsetX;
            float innerX = x + innerOffset;
            for (int i = -1; i <= numSquaresVertical + 1; ++i) {
                float y =
                        i * h + offsetY - (j - innerPointStartX) % 2 * .5f * h;
                canvas.drawLine(x, y, innerX, y + h / 2, paint);
                canvas.drawLine(innerX, y + h / 2, x, y + h, paint);
                canvas.drawLine(innerX, y + h / 2, innerX + l, y + h / 2, paint);
            }
        }
    }

    private PointF getNearestHexCorner(float centroidX, float centroidY,
            float candidateX, float candidateY) {
        float r = .5f / COSINE_30_DEGREES;
        float minDistance = Float.MAX_VALUE;
        PointF nearestPoint = null;
        for (float theta = 0; theta < 2 * Math.PI; theta += Math.PI / 3) {
            float cornerX = (float) (centroidX + r * Math.cos(theta));
            float cornerY = (float) (centroidY + r * Math.sin(theta));
            float distance =
                    Util.distance(candidateX, candidateY, cornerX, cornerY);
            if (distance < minDistance) {
                minDistance = distance;
                nearestPoint = new PointF(cornerX, cornerY);
            }
        }
        return nearestPoint;
    }

    @Override
    public PointF getNearestSnapPoint(final PointF currentLocation,
            final float tokenDiameter) {
        // The height of each hexagonal element.
        float h = 1.0f;

        // Length of any line segment of the hexagon.
        float l = .5f * h / COSINE_30_DEGREES;

        float innerOffset = l * COSINE_60_DEGREES;

        // Rescale x coordinate so that instead of hexes being length
        // (l + innerOffset) they are length 1.
        float rescaledX = currentLocation.x / (l + innerOffset);

        float previousGridLineX =
                (float) Math.floor((double) rescaledX) + h / 2;

        float previousGridLineY;
        if ((previousGridLineX > 0) == (((int) previousGridLineX) % 2 == 0)) {
            previousGridLineY =
                    (float) Math.floor((double) currentLocation.y + .5) - .5f;
        } else {
            previousGridLineY = (float) Math.floor((double) currentLocation.y);
        }

        // Special case for tokenDiameter == 0: these should snap to corners
        // instead of centroids. Now that we identified the centroid, we want
        // to find the nearest corner.
        if (tokenDiameter == 0) {
            return this.getNearestHexCorner(previousGridLineX
                    * (l + innerOffset) + innerOffset / 2,
                    previousGridLineY + .5f, currentLocation.x,
                    currentLocation.y);
        }

        return new PointF(previousGridLineX * (l + innerOffset) + innerOffset
                / 2, previousGridLineY + .5f);
    }

    @Override
    public String getTypeString() {
        return "hex";
    }

}
