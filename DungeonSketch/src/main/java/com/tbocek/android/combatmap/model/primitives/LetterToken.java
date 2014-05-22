package com.tbocek.android.combatmap.model.primitives;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a token that draws as a capital letter inside a circle.
 * 
 * @author Tim Bocek
 * 
 */
public final class LetterToken extends BaseToken {

    /**
     * Color to use when a token is both non-manipulable and bloodied.
     */
    private static final int NON_MANIPULABLE_BLOODIED_COLOR = Color.rgb(255,
            128, 128);

    /**
     * The stroke width to use when drawing this token.
     */
    private static final int STROKE_WIDTH = 3;

    /**
     * The letter to draw in the circle. While this could be anything, it should
     * really only be a single character.
     */
    private final String mLetter;

    /**
     * Constructor.
     * 
     * @param letter
     *            The single character to draw in the circle.
     */
    public LetterToken(final String letter) {
        this.mLetter = letter;
    }

    @Override
    public BaseToken clone() {
        return this.copyAttributesTo(new LetterToken(this.mLetter));
    }

    /**
     * Draws the token with the given paint style.
     * 
     * @param c
     *            Canvas to draw on.
     * @param x
     *            X coordinate of the token center, in screen space.
     * @param y
     *            Y coordinate of the token center, in screen space.
     * @param radius
     *            Radius of the token, in screen space.
     * @param paint
     *            Paint object to use when drawing the circle and text.
     */
    private void draw(final Canvas c, final float x, final float y,
            final float radius, final Paint paint) {
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setStyle(Style.STROKE);
        c.drawCircle(x, y, radius, paint);
        paint.setTextSize(radius);
        paint.setStrokeWidth(2);
        paint.setStyle(Style.FILL);
        // CHECKSTYLE:OFF
        c.drawText(this.mLetter, x - radius / 4, y + radius / 4, paint);
        // CHECKSTYLE:ON
    }

    @Override
    public void drawBloodiedImpl(final Canvas c, final float x, final float y,
            final float radius, final boolean isManipulable) {
        Paint p = new Paint();
        p.setColor(isManipulable
                ? Color.RED
                : NON_MANIPULABLE_BLOODIED_COLOR);
        this.draw(c, x, y, radius, p);
    }

    @Override
    public void drawGhost(final Canvas c, final float x, final float y,
            final float radius) {
        Paint p = new Paint();
        p.setColor(Color.GRAY);
        this.draw(c, x, y, radius, p);
    }

    @Override
    public void drawImpl(final Canvas c, final float x, final float y,
            final float radius, final boolean darkBackground,
            final boolean isManipulable) {
        Paint p = new Paint();
        p.setColor(isManipulable ? (darkBackground
                ? Color.WHITE
                : Color.BLACK) : Color.GRAY);
        this.draw(c, x, y, radius, p);
    }

    @Override
    public Set<String> getDefaultTags() {
        Set<String> s = new HashSet<String>();
        s.add("built-in");
        s.add("letter");
        return s;
    }

    @Override
    protected String getTokenClassSpecificId() {
        return this.mLetter;
    }
}
