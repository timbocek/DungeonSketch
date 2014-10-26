package com.tbocek.android.combatmap.model.primitives;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.tbocek.android.combatmap.TokenDatabase;

import java.util.Set;

/**
 * This token is meant to be replaced by another token after the token database
 * loads.
 * 
 * @author Tim
 * 
 */
public class PlaceholderToken extends BaseToken {

    /**
     * The ID of the token that this is a placeholder for.
     */
    private final String mReplaceWith;

    /**
     * Constructor.
     * 
     * @param tokenId
     *            The ID of the token that this is a placeholder for.
     */
    public PlaceholderToken(String tokenId) {
        this.mReplaceWith = tokenId;
    }

    @Override
    public BaseToken clone() throws CloneNotSupportedException {
        return new PlaceholderToken(this.mReplaceWith);
    }

    @Override
    public BaseToken deplaceholderize(TokenDatabase database) {
        return database.createToken(this.mReplaceWith.replace(this.getClass()
                .getSimpleName(), ""));
    }

    @Override
    protected void drawBloodiedImpl(Canvas c, float x, float y, float radius,
            boolean isManipulable) {
        this.drawImpl(c, x, y, radius, true, true);

    }

    @Override
    protected void drawGhost(Canvas c, float x, float y, float radius) {
        this.drawImpl(c, x, y, radius, true, true);

    }

    @Override
    protected void drawImpl(Canvas c, float x, float y, float radius,
            boolean darkBackground, boolean isManipulable) {
        Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setColor(Color.BLACK);
        p.setStrokeWidth(1.0f);

        c.drawCircle(x, y, radius, p);
    }

    @Override
    public Set<String> getDefaultTags() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getTokenClassSpecificId() {
        return this.mReplaceWith;
    }
}
