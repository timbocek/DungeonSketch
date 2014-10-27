package com.tbocek.android.combatmap.model.primitives;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.tbocek.android.combatmap.DataManager;
import com.tbocek.android.combatmap.TokenImageManager;

/**
 * Base class for tokens that display some sort of drawable. Provides standard
 * drawing methods and a caching scheme. Subclasses need mainly to specify how
 * to load the drawable.
 * 
 * @author Tim Bocek
 * 
 */
public abstract class DrawableToken extends BaseToken {
    protected static final int LOAD_DIM_DP = 96;

    /**
     * The data manager that is used to load custom images.
     */
    protected static transient DataManager dataManager = null;
    /**
     * Sets the data manager that will be used to load images.
     *
     * @param manager
     *            The data manager.
     */
    public static void registerDataManager(final DataManager manager) {
        CustomBitmapToken.dataManager = manager;
    }

    /**
     * Color transformation matrix used to place a red tint on bloodied tokens.
     */
    // @formatter:off
    private static final float[] BLOODIED_COLOR_MATRIX = new float[] 
            {1, 0,    0,    0, 0, 
             0, .25f, 0,    0, 0, 
             0, 0,    .25f, 0, 0, 
             0, 0,    0,    1, 0};
    // @formatter:on

    /**
     * Filter created from the bloodied transformation matrix.
     */
    private static final ColorMatrixColorFilter BLOODIED_FILTER =
            new ColorMatrixColorFilter(new ColorMatrix(BLOODIED_COLOR_MATRIX));

    /**
     * Alpha value that will draw at full opacity.
     */
    private static final int FULL_OPACITY = 255;

    /**
     * Alpha value that will draw at half opacity.
     */
    private static final int HALF_OPACITY = 128;

    /**
     * Sets the clip of the given canvas to a circle centered at (x,y) with
     * radius r.
     * 
     * @param c
     *            The canvas we are drawing on.
     * @param x
     *            X coordinate of the circle to clip to.
     * @param y
     *            Y coordinate of the circle to clip to.
     * @param radius
     *            Radius of the circle to clip to.
     */
    private void clipToCircle(final Canvas c, final float x, final float y,
            final float radius) {
        Path p = new Path();
        p.addCircle(x, y, radius, Path.Direction.CW);
        c.clipPath(p);
    }

    @Override
    public final void drawBloodiedImpl(final Canvas c, final float x,
            final float y, final float radius, final boolean isManipulable) {
        Drawable d = this.getDrawable();
        if (d != null) {
            d.setColorFilter(BLOODIED_FILTER);
            this.drawImpl(c, x, y, radius, false, isManipulable);
            d.setColorFilter(null);
        } else {
            this.drawPlaceholder(c, x, y, radius);
        }

    }

    @Override
    protected final void drawGhost(final Canvas c, final float x,
            final float y, final float radius) {
        Drawable d = this.getDrawable();
        if (d != null) {
            d.setAlpha(HALF_OPACITY);
            this.drawImpl(c, x, y, radius, false, true);
            d.setAlpha(FULL_OPACITY);
        }
    }

    @Override
    public final void drawImpl(final Canvas c, final float x, final float y,
            final float radius, final boolean darkBackground,
            final boolean isManipulable) {
        Drawable d = this.getDrawable();
        if (d != null) {
            c.save(Canvas.CLIP_SAVE_FLAG);
            this.clipToCircle(c, x, y, radius);
            d.setBounds(new Rect((int) (x - radius), (int) (y - radius),
                    (int) (x + radius), (int) (y + radius)));
            if (!isManipulable) {
                d.setAlpha(HALF_OPACITY);
            }
            d.draw(c);
            if (!isManipulable) {
                d.setAlpha(FULL_OPACITY);
            }
            c.restore();
        } else {
            this.drawPlaceholder(c, x, y, radius);
        }
    }

    /**
     * Draws a placeholder where this token should be. Used for when the token
     * hasn't loaded yet.
     * 
     * @param c
     *            Canvas
     * @param x
     *            center x
     * @param y
     *            center y
     * @param radius
     *            circle radius
     */
    private void drawPlaceholder(final Canvas c, final float x, final float y,
            final float radius) {
        Paint p = new Paint();
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.0f);
        c.drawCircle(x, y, radius, p);
    }

    /**
     * Returns the drawable associated with this token. This may cause the
     * drawable to be loaded.
     * 
     * @return The drawable.
     */
    private Drawable getDrawable() {
        TokenImageManager mgr = TokenImageManager.getInstanceOrNull();
        if (mgr == null) return null;
        return mgr.getTokenDrawable(this.getTokenId());
    }

    public abstract Bitmap loadBitmap(Bitmap image);

    @Override
    public final boolean needsLoad() {
        return true;
    }

}
