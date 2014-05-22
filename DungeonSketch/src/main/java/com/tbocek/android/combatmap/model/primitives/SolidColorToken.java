package com.tbocek.android.combatmap.model.primitives;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

/**
 * A built-in token type that draws as a solid color.
 * 
 * @author Tim Bocek
 * 
 */
public final class SolidColorToken extends BaseToken {

    /**
     * Stroke width to use for drawing a red border around bloodied tokens.
     */
    private static final int BLOODIED_BORDER_STROKE_WIDTH = 8;

    /**
     * Alpha channel value to use for ghost tokens.
     */
    private static final int GHOST_ALPHA = 128;

    /**
     * Color to use when bloodying a token that is already colored red.
     */
    private static final int RED_TOKEN_BLOODIED_BORDER_COLOR = Color.rgb(127,
            0, 0);

    /**
     * Format string that pads the sort order with 0s.
     */
    private static final DecimalFormat SORT_ORDER_FORMAT = new DecimalFormat(
            "#0000.###");

    /**
     * This token's color.
     */
    private final int mColor;

    /**
     * A sort order for the token, since sorting on color does not produce a
     * pleasing ordering to the tokens.
     */
    private final int mSortOrder;

    /**
     * Constructor.
     * 
     * @param c
     *            The color to draw this token with.
     * @param sortOrder
     *            Ordering for this token. We manually specify this because
     *            sorting on color doesn't produce good-looking results.
     */
    public SolidColorToken(final int c, final int sortOrder) {
        this.mColor = c;
        this.mSortOrder = sortOrder;
    }

    @Override
    public BaseToken clone() {
        return this.copyAttributesTo(new SolidColorToken(this.mColor,
                this.mSortOrder));
    }

    @Override
    public void drawBloodiedImpl(final Canvas c, final float x, final float y,
            final float radius, final boolean isManipulable) {
        this.drawImpl(c, x, y, radius, false, isManipulable);

        Paint p = new Paint();
        // If token is already colored red, use a dark red border so it's
        // visible
        p.setColor(this.mColor != Color.RED
                ? Color.RED
                : RED_TOKEN_BLOODIED_BORDER_COLOR);
        p.setStyle(Style.STROKE);
        p.setStrokeWidth(BLOODIED_BORDER_STROKE_WIDTH);
        // CHECKSTYLE:OFF
        c.drawCircle(x, y, radius - 4, p);
        // CHECKSTYLE:ON
    }

    /**
     * Draws an indication of a past location of the token.
     * 
     * @param c
     *            Canvas to draw on.
     * @param x
     *            X location of the center of the ghost token.
     * @param y
     *            Y location of the center of the ghost token.
     * @param radius
     *            Radius of the ghost token.
     */
    @Override
    public void drawGhost(final Canvas c, final float x, final float y,
            final float radius) {
        Paint p = new Paint();
        p.setColor(this.mColor);
        p.setAlpha(GHOST_ALPHA);
        c.drawCircle(x, y, radius, p);
    }

    @Override
    public void drawImpl(final Canvas c, final float x, final float y,
            final float radius, final boolean darkBackground,
            final boolean isManipulable) {
        Paint p = new Paint();
        p.setColor(this.mColor);
        if (!isManipulable) {
            p.setAlpha(GHOST_ALPHA);
        }
        c.drawCircle(x, y, radius, p);
    }

    @Override
    public Set<String> getDefaultTags() {
        Set<String> s = new HashSet<String>();
        s.add("built-in");
        s.add("solid color");
        return s;
    }

    @Override
    protected String getTokenClassSpecificId() {
        SORT_ORDER_FORMAT.setDecimalSeparatorAlwaysShown(false);
        return SORT_ORDER_FORMAT.format(this.mSortOrder) + '_'
                + Integer.toString(this.mColor);
    }
}
