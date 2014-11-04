package com.tbocek.android.combatmap.model;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.model.primitives.Units;

public class MapDrawer {
    private boolean mAreTokensManipulable;

    private FogOfWarMode mBackgroundFogOfWar;
    private boolean mDrawAnnotations;
    private boolean mDrawGmNotes;
    private boolean mDrawGridLines;
    private boolean mDrawTokens;
    private FogOfWarMode mGmNoteFogOfWar;
    private CoordinateTransformer mTransformer;

    private boolean mApplyMaskToTokens;
    private Selection mSelection;

    public MapDrawer areTokensManipulable(boolean val) {
        this.mAreTokensManipulable = val;
        return this;
    }

    public MapDrawer backgroundFogOfWar(FogOfWarMode val) {
        this.mBackgroundFogOfWar = val;
        return this;
    }

    public void draw(Canvas canvas, MapData m, Rect bounds) {
        if (mTransformer == null) {
            mTransformer = m.getWorldSpaceTransformer();
        }

    	PointF wsOrigin = mTransformer.screenSpaceToWorldSpace(bounds.left, bounds.top);
    	float wsWidth = mTransformer.screenSpaceToWorldSpace(bounds.width());
    	float wsHeight = mTransformer.screenSpaceToWorldSpace(bounds.height());
    	RectF worldSpaceBounds = new RectF(wsOrigin.x, wsOrigin.y, wsOrigin.x + wsWidth, wsOrigin.y + wsHeight);
        
    	m.getGrid().drawBackground(canvas);

        canvas.save();
        mTransformer.setMatrix(canvas);
        if (this.mBackgroundFogOfWar == FogOfWarMode.CLIP
                && !m.getBackgroundFogOfWar().isEmpty()) {
            m.getBackgroundFogOfWar().clipFogOfWar(canvas);
        }
        m.getBackgroundLines().drawAllLinesBelowGrid(canvas);
        m.getBackgroundImages().draw(canvas, mTransformer, worldSpaceBounds);
        canvas.restore();

        if (this.mDrawGridLines) {
            m.getGrid().draw(canvas, mTransformer);
        }

        canvas.save();
        mTransformer.setMatrix(canvas);
        if (this.mBackgroundFogOfWar == FogOfWarMode.CLIP
                && !m.getBackgroundFogOfWar().isEmpty()) {
            m.getBackgroundFogOfWar().clipFogOfWar(canvas);
        }
        m.getBackgroundLines().drawAllLinesAboveGrid(canvas);
        if (this.mBackgroundFogOfWar == FogOfWarMode.DRAW) {
            m.getBackgroundFogOfWar().drawFogOfWar(canvas);
        }
        canvas.restore();

        canvas.save();
        mTransformer.setMatrix(canvas);

        if (this.mDrawGmNotes) {
            canvas.save();
            if (this.mGmNoteFogOfWar == FogOfWarMode.CLIP) {
                m.getGmNotesFogOfWar().clipFogOfWar(canvas);
            }
            m.getGmNoteLines().drawAllLines(canvas);
            if (this.mGmNoteFogOfWar == FogOfWarMode.DRAW) {
                m.getGmNotesFogOfWar().drawFogOfWar(canvas);
            }
            canvas.restore();
            
        }

        if (this.mDrawAnnotations) {
            m.getAnnotationLines().drawAllLines(canvas);
        }
        canvas.restore();

        if (this.mSelection != null) {
            this.mSelection.draw(canvas, m.getWorldSpaceTransformer());
        }

        canvas.save();
        if (this.mBackgroundFogOfWar == FogOfWarMode.CLIP
                && !m.getBackgroundFogOfWar().isEmpty()
                && this.mApplyMaskToTokens) {
            mTransformer.setMatrix(canvas);
            m.getBackgroundFogOfWar().clipFogOfWar(canvas);
            mTransformer.setInverseMatrix(canvas);
        }
        CoordinateTransformer gridSpace =
                m.getGrid().gridSpaceToScreenSpaceTransformer(mTransformer);
        if (this.mDrawTokens) {
            m.getTokens().drawAllTokens(canvas, gridSpace,
                    m.getGrid().isDark(), this.mAreTokensManipulable);
        }
        canvas.restore();
    }

    public MapDrawer drawAnnotations(boolean val) {
        this.mDrawAnnotations = val;
        return this;
    }

    public MapDrawer drawGmNotes(boolean val) {
        this.mDrawGmNotes = val;
        return this;
    }

    public MapDrawer drawGridLines(boolean val) {
        this.mDrawGridLines = val;
        return this;
    }

    public MapDrawer drawTokens(boolean val) {
        this.mDrawTokens = val;
        return this;
    }

    public MapDrawer gmNotesFogOfWar(FogOfWarMode val) {
        this.mGmNoteFogOfWar = val;
        return this;
    }

    public MapDrawer applyMaskToTokens(boolean applyMaskToTokens) {
        mApplyMaskToTokens = applyMaskToTokens;
        return this;
    }

    /**
     * Sets the coordinate transformer to use instead of the default coordinate transformer that
     * is present in the map data.
     *
     * @param transformer The transformer to use for world space.
     * @return this instance for chaining calls.
     */
    public MapDrawer useCustomWorldSpaceTransformer(CoordinateTransformer transformer) {
        mTransformer = transformer;
        return this;
    }

    public MapDrawer drawSelection(Selection selection) {
        mSelection = selection;
        return this;
    }

    /**
     * Options for what to do with the fog of war.
     * 
     * @author Tim
     * 
     */
    public enum FogOfWarMode {
        /**
         * Use the fog of war to clip the background.
         */
        CLIP,

        /**
         * Draw the fog of war as an overlay.
         */
        DRAW,

        /**
         * Ignore the fog of war.
         */
        NOTHING
    }



}
