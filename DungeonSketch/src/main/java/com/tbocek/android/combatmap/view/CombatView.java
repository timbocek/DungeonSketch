package com.tbocek.android.combatmap.view;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.os.Build;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.google.common.collect.Sets;
import com.tbocek.android.combatmap.DeveloperMode;
import com.tbocek.android.combatmap.ScrollBuffer;
import com.tbocek.android.combatmap.ScrollBuffer.DrawRequest;
import com.tbocek.android.combatmap.TokenImageManager;
import com.tbocek.android.combatmap.model.LineCollection;
import com.tbocek.android.combatmap.model.MapData;
import com.tbocek.android.combatmap.model.MapDrawer;
import com.tbocek.android.combatmap.model.MapDrawer.FogOfWarMode;
import com.tbocek.android.combatmap.model.MultiSelectManager;
import com.tbocek.android.combatmap.model.TokenCollection;
import com.tbocek.android.combatmap.model.UndoRedoTarget;
import com.tbocek.android.combatmap.model.primitives.BackgroundImage;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.model.primitives.Information;
import com.tbocek.android.combatmap.model.primitives.OnScreenText;
import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.model.primitives.Shape;
import com.tbocek.android.combatmap.model.primitives.Util;
import com.tbocek.android.combatmap.view.interaction.BackgroundImageInteractionMode;
import com.tbocek.android.combatmap.view.interaction.CombatViewInteractionMode;
import com.tbocek.android.combatmap.view.interaction.CreateInfoInteractionMode;
import com.tbocek.android.combatmap.view.interaction.DrawTextInteractionMode;
import com.tbocek.android.combatmap.view.interaction.EraserInteractionMode;
import com.tbocek.android.combatmap.view.interaction.FingerDrawInteractionMode;
import com.tbocek.android.combatmap.view.interaction.GridRepositioningInteractionMode;
import com.tbocek.android.combatmap.view.interaction.MaskDrawInteractionMode;
import com.tbocek.android.combatmap.view.interaction.MaskEraseInteractionMode;
import com.tbocek.android.combatmap.view.interaction.MeasuringTapeInteractionMode;
import com.tbocek.android.combatmap.view.interaction.TokenManipulationInteractionMode;
import com.tbocek.android.combatmap.view.interaction.ZoomPanInteractionMode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This view is the main canvas on which the map and combat tokens are drawn and
 * manipulated.
 * 
 * @author Tim Bocek
 * 
 */
public final class CombatView extends SurfaceView {
    private static final String TAG = "CombatView";
    private static final float INFO_POINT_SIZE_DP = 32;

    /**
	 * A simple 3-state machine to make sure that full-screen draws performed during
	 * input processing are batched, and performed once at the very end of the draw.
	 * @author Tim
	 *
	 */
	private enum FullscreenDrawLatch {
		NOT_BATCHING, // Not attempting to batch full-screen draw operations.
		BATCHING, // Attempting to batch full-screen draws, none have been requested.
		BATCHED // A full-screen draw has been requested.
	}
	
	/**
	 * For framerate tracking.  Number of seconds to use when finding the
	 * framerate
	 */
	private static final int FRAMERATE_INTVL = 500;

    /**
     * For the explanatory mask text, Y location of the first line in density-
     * Independent pixels.
     */
    private static final int EXPLANATORY_TEXT_INITIAL_Y_DP = 16;

    /**
     * For the explanatory mask text, height of each line in density-
     * Independent pixels.
     */
    private static final int EXPLANATORY_TEXT_LINE_HEIGHT_DP = 20;

    /**
     * Reference to the collection of lines that are actively being drawn.
     */
    private LineCollection mActiveLines;

    /**
     * Whether tokens should be drawn as manipulatable.
     */
    private boolean mAreTokensManipulatable = true;

    /**
     * The current map.
     */
    private MapData mData;

    private boolean mEditingMask = false;

    private Paint mExplanatoryTextPaint;

    /**
     * What to do with the fog of war when drawing.
     */
    private MapDrawer.FogOfWarMode mFogOfWarMode;

    /**
     * Detector object to detect regular gestures.
     */
    private GestureDetector mGestureDetector;

    /**
     * Interaction mode, defining how the view should currently respond to user
     * input.
     */
    private CombatViewInteractionMode mInteractionMode;
    
    private FullscreenDrawLatch mDrawLatch = FullscreenDrawLatch.NOT_BATCHING;

    /**
     * The color to use when creating a new line.
     */
    private int mNewLineColor = Color.BLACK;

    /**
     * The stroke width to use when creating a new line.
     */
    private float mNewLineStrokeWidth;

    /**
     * The style that new lines should have.
     */
    private NewLineStyle mNewLineStyle = NewLineStyle.FREEHAND;

    /**
     * Callback that the parent activity registers to listen to requests for
     * new/edited text objects. This indirection is needed because this
     * operation requires a dialog, which the activity needs to open.
     */
    private ActivityRequestListener mActivityRequestListener;

    /**
     * Listener to publish refresh requests to.
     */
    private OnRefreshListener mOnRefreshListener;

    /**
     * Listener for background image selection changes.
     */
    private ImageSelectionListener mImageSelectionListener;

    /**
     * Detector object to detect pinch zoom.
     */
    private ScaleGestureDetector mScaleDetector;

    /**
     * Whether to draw the annotation layer.
     */
    private boolean mShouldDrawAnnotations;

    /**
     * Whether to draw private GM notes.
     */
    private boolean mShouldDrawGmNotes;

    /**
     * Whether tokens being moved should snap to the grid.
     */
    private boolean mSnapToGrid;

    /**
     * Whether the mask applies to tokens.
     */
    private boolean mApplyMaskToTokens;

    /**
     * Callback for the Android graphics surface management system.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private SurfaceHolder.Callback mSurfaceHolderCallback =
            new SurfaceHolder.Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder arg0, int arg1,
                int arg2, int arg3) {
            CombatView.this.refreshMap();
        }

        @Override
        public void surfaceCreated(SurfaceHolder arg0) {
            CombatView.this.mSurfaceReady = true;
            CombatView.this.refreshMap();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder arg0) {
            CombatView.this.mSurfaceReady = false;

        }
    };

    /**
     * Whether the surface is ready to draw.
     */
    private boolean mSurfaceReady = false;

    /**
     * Object to manage a selection of multiple tokens.
     */
    private MultiSelectManager mTokenSelection = new MultiSelectManager();

    /**
     * Whether tokens snap to the intersections of grid lines rather than the
     * spaces between them.
     */
    private boolean mTokensSnapToIntersections;

    /**
     * The current map data object that undo/redo actions will affect.
     */
    private UndoRedoTarget mUndoRedoTarget;

    /**
     * The currently selected background image.
     */
    private BackgroundImage mSelectedBackgroundImage;
    
    Paint mFrameratePaint;
    
    Paint mDrawRectDebugPaint;
    
    /**
     * Frames since the framerate was last computed
     */
    private int mFrameCount;
    
    private long mLastFramerateComputeTime;
    
    private float mFramerate;
    
    private ScrollBuffer mScrollBuffer = new ScrollBuffer();

    private TokenImageManager.Loader mLoader;

    private Set<String> mVisibleTokenImages = new HashSet<String>();
    
    /**
     * Constructor.
     * 
     * @param context
     *            The context to create this view in.
     */
    @SuppressLint("NewApi")
    public CombatView(final Context context) {
        super(context);

        this.mExplanatoryTextPaint = new Paint();
        this.mExplanatoryTextPaint.setTextAlign(Align.CENTER);
        this.mExplanatoryTextPaint.setTextSize(24);
        this.mExplanatoryTextPaint.setColor(Color.RED);
        
        this.mFrameratePaint = new Paint();
        this.mFrameratePaint.setTextAlign(Align.LEFT);
        this.mFrameratePaint.setTextSize(24);
        this.mFrameratePaint.setColor(Color.BLACK);
        
        this.mDrawRectDebugPaint = new Paint();
        this.mDrawRectDebugPaint.setColor(Color.RED);
        this.mDrawRectDebugPaint.setStyle(Style.STROKE);
        this.mDrawRectDebugPaint.setStrokeWidth(3.0f);

        this.setFocusable(true);
        this.setFocusableInTouchMode(true);

        this.setTokenManipulationMode();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            OnDragListener onDrag = new OnDragListener() {
                @Override
                public boolean onDrag(final View view, final DragEvent event) {
                    Log.d("DRAG", Integer.toString(event.getAction()));
                    if (event.getAction() == DragEvent.ACTION_DROP) {
                        BaseToken toAdd = (BaseToken) event.getLocalState();
                        PointF location =
                                CombatView.this.getGridSpaceTransformer()
                                        .screenSpaceToWorldSpace(
                                                new PointF(event.getX(), event
                                                        .getY())
                                        );
                        if (CombatView.this.mSnapToGrid) {
                            location =
                                    CombatView.this
                                            .getData()
                                            .getGrid()
                                            .getNearestSnapPoint(location,
                                                    toAdd.getSize());
                        }
                        toAdd.setLocation(location);
                        CombatView.this.getData().getTokens().addToken(toAdd);
                        CombatView.this.alertTokensChanged();
                        return true;
                    } else if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                        return true;
                    }
                    return false;
                }
            };
            this.setOnDragListener(onDrag);
        }

        this.getHolder().addCallback(this.mSurfaceHolderCallback);
        // setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    /**
     * Creates a new region in the fog of war.
     * 
     * @return The new region.
     */
    public Shape createFogOfWarRegion() {
        return this.createLine(this.getActiveFogOfWar());
    }

    /**
     * Creates a new line on whatever line set is currently active, using the
     * currently set color, stroke width, and type.
     * 
     * @return The new line.
     */
    public Shape createLine() {
        return this.createLine(this.mActiveLines);
    }

    /**
     * Creates a line in the given line collection with the currently set color,
     * stroke width, and type.
     * 
     * @param lines
     *            The line collection to create the line in.
     * @return The created line.
     */
    protected Shape createLine(LineCollection lines) {
        switch (this.mNewLineStyle) {
        case FREEHAND:
            return lines.createFreehandLine(this.mNewLineColor,
                    this.mNewLineStrokeWidth);
        case STRAIGHT:
            return lines.createStraightLine(this.mNewLineColor,
                    this.mNewLineStrokeWidth);
        case CIRCLE:
            return lines.createCircle(this.mNewLineColor,
                    this.mNewLineStrokeWidth);
        case RECTANGLE:
            return lines.createRectangle(this.mNewLineColor,
                    this.mNewLineStrokeWidth);
        default:
            throw new IllegalArgumentException("Invalid new line type.");
        }
    }

    /**
     * Creates a new text object with the given location, text, and font size.
     * This is called by the parent activity after being alerted to the request
     * for a new text object and after the resulting dialog is accepted.
     * 
     * @param newTextLocationWorldSpace
     *            Location to place the new text object at.
     * @param text
     *            Contents of the new text object.
     * @param size
     *            Font size of the new text object.
     */
    public void createNewText(PointF newTextLocationWorldSpace, String text,
            float size) {
        // Compute the text size as being one grid cell large.
        float textSize =
                this.getData().getGrid().gridSpaceToWorldSpaceTransformer()
                .worldSpaceToScreenSpace(size);
        this.mActiveLines.createText(text, textSize, this.mNewLineColor,
                Float.POSITIVE_INFINITY, newTextLocationWorldSpace,
                this.getWorldSpaceTransformer());
        this.refreshMap();
    }

    public void createNewInfo(PointF newObjectLocationWorldSpace, String text, int iconId) {
        this.mActiveLines.createInfo(text, newObjectLocationWorldSpace, iconId);
        this.refreshMap();
    }


    /**
     * Draws the contents of the view to the given canvas.
     * 
     * @param canvas
     *            The canvas to draw on.
     */
    private void drawOnCanvas(final Canvas canvas, final Rect dirty) {
        new MapDrawer()
        .drawGridLines(true)
        .drawGmNotes(this.mShouldDrawGmNotes)
        .drawTokens(true)
        .areTokensManipulable(this.mAreTokensManipulatable)
        .drawAnnotations(this.mShouldDrawAnnotations)
        .gmNotesFogOfWar(
                this.mActiveLines == this.getData().getGmNoteLines()
                ? FogOfWarMode.DRAW
                        : FogOfWarMode.CLIP)
                        .applyMaskToTokens(mApplyMaskToTokens)
                        .backgroundFogOfWar(this.mFogOfWarMode)
                        .draw(canvas, this.getData(), dirty);

        this.mInteractionMode.draw(canvas);
        
	    if (DeveloperMode.shouldDisplayFramerate()) {
	    	long time = System.currentTimeMillis();
	    	if (time - mLastFramerateComputeTime >= CombatView.FRAMERATE_INTVL) {
	    		mFramerate = ((float)mFrameCount) / (((float)(time - mLastFramerateComputeTime))/1000);
	    		mFrameCount = 0;
	    		mLastFramerateComputeTime = time;
	    	}
	        mFrameCount++;
	    }
	    
	    if (DeveloperMode.shouldDisplayDrawRects()) {
	    	canvas.drawRect(dirty, this.mDrawRectDebugPaint);
	    }
    }
    
    /**
     * Draws any overlay/UI elements that are not part of the map.
     * @param canvas
     */
    private void drawOverlays(Canvas canvas) {
        if (this.mEditingMask) {
            String explanatoryText = this.getMaskExplanatoryText();

            int i = EXPLANATORY_TEXT_INITIAL_Y_DP;
            for (String s : explanatoryText.split("\n")) {
                float scaledDensity =
                        this.getContext().getResources().getDisplayMetrics().scaledDensity;
                canvas.drawText(s, this.getWidth() / 2, i * scaledDensity,
                        this.mExplanatoryTextPaint);
                i += EXPLANATORY_TEXT_LINE_HEIGHT_DP;
            }
        }

    	if (DeveloperMode.shouldDisplayFramerate()) {
    		canvas.drawText("Framerate: " + Float.toString(mFramerate) + " fps", 4, 16, this.mFrameratePaint);
    	}
    }

    /**
     * @return The fog of war layer associated with the current active lines.
     */
    public LineCollection getActiveFogOfWar() {
        if (this.mActiveLines == this.getData().getBackgroundLines()) {
            return this.getData().getBackgroundFogOfWar();
        } else if (this.mActiveLines == this.getData().getGmNoteLines()) {
            return this.getData().getGmNotesFogOfWar();
        } else {
            return null;
        }
    }

    /**
     * Gets the currently active line collection.
     * 
     * @return The active lines.
     */
    public LineCollection getActiveLines() {
        return this.mActiveLines;
    }

    /**
     * Gets the current map data.
     * 
     * @return data The map data.
     */
    public MapData getData() {
        return this.mData;
    }

    /**
     * 
     * @return The fog of war mode.
     */
    public FogOfWarMode getFogOfWarMode() {
        return this.mFogOfWarMode;
    }

    /**
     * Gets a transformer from grid space to screen space, by composing the grid
     * to world and the world to screen transformers.
     * 
     * @return The composed transformation.
     */
    public CoordinateTransformer getGridSpaceTransformer() {
        return this
                .getData()
                .getGrid()
                .gridSpaceToScreenSpaceTransformer(
                        this.getData().getWorldSpaceTransformer());
    }

    private String getMaskExplanatoryText() {
        if (this.getActiveFogOfWar() == null) {
            return "";
        }
        String explanatoryText =
                "Editing layer mask - Only selected regions will be visible";

        boolean visibleByDefault =
                this.getActiveFogOfWar() == this.mData.getBackgroundFogOfWar();
        if (visibleByDefault && this.getActiveFogOfWar().isEmpty()) {
            explanatoryText +=
                    "\n\nBy default, the entire background is shown\nuntil a mask region is added";

        }
        return explanatoryText;
    }

    /**
     * @return The multi-select manager used to select multiple tokens in this
     *         view.
     */
    public MultiSelectManager getMultiSelect() {
        return this.mTokenSelection;
    }

    /**
     * @return the newLineColor
     */
    public int getNewLineColor() {
        return this.mNewLineColor;
    }

    /**
     * @return the newLineStrokeWidth
     */
    public float getNewLineStrokeWidth() {
        return this.mNewLineStrokeWidth;
    }

    /**
     * @return the newLineStyle
     */
    public NewLineStyle getNewLineStyle() {
        return this.mNewLineStyle;
    }

    /**
     * Gets a preview image of the map currently displayed in the view.
     * 
     * @return A bitmap containing the preview image.
     */
    public Bitmap getPreview() {
        if (this.getWidth() == 0 || this.getHeight() == 0) {
            return null;
        }
        Bitmap bitmap =
                Bitmap.createBitmap(this.getWidth(), this.getHeight(),
                        Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        new MapDrawer().drawGridLines(false).drawGmNotes(false)
        .drawTokens(true).areTokensManipulable(true)
        .drawAnnotations(false).gmNotesFogOfWar(FogOfWarMode.NOTHING)
        .backgroundFogOfWar(this.mFogOfWarMode)
        .draw(canvas, this.getData(), canvas.getClipBounds());

        return bitmap;
    }

    /**
     * Returns the current token collection.
     * 
     * @return The tokens.
     */
    public TokenCollection getTokens() {
        return this.getData().getTokens();
    }

    /**
     * 
     * @return The current target for undo and redo operations.
     */
    public UndoRedoTarget getUndoRedoTarget() {
        return this.mUndoRedoTarget;
    }

    /**
     * Returns the world space to screen space transformer used by the view.
     * 
     * @return The transformation object.
     */
    public CoordinateTransformer getWorldSpaceTransformer() {
        return this.getData().getWorldSpaceTransformer();
    }

    /**
     * 
     * @return Whether one of the fog of war layers is visible.
     */
    public boolean isAFogOfWarLayerVisible() {
        return this.getFogOfWarMode() == FogOfWarMode.DRAW
                || this.mShouldDrawGmNotes;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            this.mInteractionMode.addFinger();
        }

        startBatchingDraws();
        this.mGestureDetector.onTouchEvent(ev);
        this.mScaleDetector.onTouchEvent(ev);

        // If a finger was removed, optimize the lines by removing unused
        // points.
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            this.mInteractionMode.removeFinger();
            this.mInteractionMode.onUp(ev);

            // If the end of a gesture, load any newly required token images.
            if (this.mInteractionMode.getNumberOfFingers() == 0) {
               this.loadNewTokenImages();
            }
        }
        
        // If one or more fullscreen draws was requested, do so now, and either
        // way leave us open to non-touch-event-driven draw requests.
        stopBatchingDraws();
        return true;
    }

    private void startBatchingDraws() {
        if (this.mDrawLatch == FullscreenDrawLatch.NOT_BATCHING)
            this.mDrawLatch = FullscreenDrawLatch.BATCHING;
    }

    private void stopBatchingDraws() {
        if (this.mDrawLatch == FullscreenDrawLatch.BATCHED) {
            this.mDrawLatch = FullscreenDrawLatch.NOT_BATCHING;
            this.refreshMap();
        } else {
            this.mDrawLatch = FullscreenDrawLatch.NOT_BATCHING;
        }
    }

    /**
     * Removes all erased points from the currently active set of lines.
     */
    public void optimizeActiveLines() {
        this.mActiveLines.optimize();
    }

    /**
     * Places a token on the screen, at a location chosen by the view.
     * 
     * @param t
     *            The token to place.
     */
    public void placeToken(final BaseToken t) {
        PointF attemptedLocationScreenSpace =
                new PointF(this.getWidth() / 2.0f, this.getHeight() / 2.0f);
        PointF attemptedLocationGridSpace =
                this.getGridSpaceTransformer().screenSpaceToWorldSpace(
                        attemptedLocationScreenSpace);

        this.getData()
        .getTokens()
        .placeTokenNearby(t, attemptedLocationGridSpace,
                this.getData().getGrid(),
                this.mTokensSnapToIntersections);
        this.getData().getTokens().addToken(t);
        this.refreshMap(t.getBoundingRectangle().toRectF(), this.getGridSpaceTransformer());
    }

    /**
     * Redraws the contents of the map.
     * @param invalidBounds Screen space portion to redraw.s
     */
    public void refreshMap(Rect invalidBounds) {
    	// Make sure the refresh being requested is sane.
    	// (This could be violated when starting to draw for a shape that was
    	// just now created).
    	if (invalidBounds.left > invalidBounds.right || invalidBounds.top > invalidBounds.bottom){
    		return;
    	}

    	// If we already need a full screen refresh as part of this draw,
    	// be smart and don't redraw just part of the screen!
    	if (this.mDrawLatch == FullscreenDrawLatch.BATCHED) {
    		return;
    	}
    	
        if (!this.mSurfaceReady) {
            return;
        }

        // Make sure that any scale changes are reflected in the way that scale-independent sprites
        // (such as info points) are drawn.
        float infoWidthScreenSpace = Util.convertDpToPixel(INFO_POINT_SIZE_DP, this.getContext());
        Information.setSizeWorldSpace(
                getData().getWorldSpaceTransformer().screenSpaceToWorldSpace(infoWidthScreenSpace));

        SurfaceHolder holder = this.getHolder();
        Canvas canvas = holder.lockCanvas(invalidBounds);
        if (canvas != null) {
        	canvas.clipRect(invalidBounds);
            this.drawOnCanvas(canvas, invalidBounds);
            this.drawOverlays(canvas);
            holder.unlockCanvasAndPost(canvas);
        }

        if (this.mOnRefreshListener != null) {
            this.mOnRefreshListener.onRefresh(this.mInteractionMode.getNumberOfFingers() == 0);
        }
        
        // If we called this, then a non-scroll operation triggered a map refresh.
        // This means the scroll buffer will contain out-of-date info.
        this.mScrollBuffer.invalidateBuffers();
    }
    
    /**
     * Refreshes the entire map.
     */
    public void refreshMap() {
    	// If we are batching full screen draw operations, defer this operation
    	// until we are ready for the batch.
    	if (this.mDrawLatch != FullscreenDrawLatch.NOT_BATCHING) {
    		this.mDrawLatch = FullscreenDrawLatch.BATCHED;
    	} else {
    		refreshMap(new Rect(0,0,this.getWidth(),this.getHeight()));
    	}
    }
    
    /**
     * Refreshes the portion of the map, using the given transformer to transform to screen space.
     */
    public void refreshMap(RectF invalidBounds, CoordinateTransformer transformer) {
    	refreshMap(transformer.worldSpaceToScreenSpace(invalidBounds));
    }

    /**
     * Forwards a request to edit a text object to the parent activity.
     * 
     * @param t
     *            The text object to edit.
     */
    public void requestEditTextObject(OnScreenText t) {
        this.mActivityRequestListener.requestEditTextObject(t);
    }


    public void requestEditInfoObject(Information t) {
        this.mActivityRequestListener.requestEditInfoObject(t);
    }


    /**
     * Forwards a request to create a text object to the parent activity.
     * 
     * @param newTextLocationWorldSpace
     *            Location to place the new text object in world space.
     */
    public void requestNewTextEntry(PointF newTextLocationWorldSpace) {
        if (this.mActivityRequestListener != null) {
            this.mActivityRequestListener
            .requestNewTextEntry(newTextLocationWorldSpace);
        }
    }


    public void requestNewInfoEntry(PointF pointF) {
        if (this.mActivityRequestListener != null) {
            this.mActivityRequestListener
                    .requestNewInfoEntry(pointF);
        }
    }


    /**
     * Sets whether tokens are manipulatable.
     * 
     * @param manip
     *            Value to set.
     */
    public void setAreTokensManipulatable(boolean manip) {
        this.mAreTokensManipulatable = manip;
    }

    /**
     * Sets the interaction mode to creating and manipulating background images.
     */
    public void setBackgroundImageMode() {
        this.setInteractionMode(new BackgroundImageInteractionMode(this));
    }

    /**
     * Sets the map data displayed. Forces a redraw.
     * 
     * @param data
     *            The new map data.
     */
    public void setData(final MapData data) {

        boolean useBackgroundLines =
                (this.mData == null)
                || this.mActiveLines == this.mData.getBackgroundLines();
        this.mData = data;
        this.mActiveLines =
                useBackgroundLines
                ? this.mData.getBackgroundLines()
                        : this.mData.getAnnotationLines();
                this.alertTokensChanged();
    }

    /**
     * Sets the interaction mode to drawing lines.
     */
    public void setDrawMode() {
        this.setInteractionMode(new FingerDrawInteractionMode(this));
    }

    public void setEditingLayerMask(boolean editingLayerMask) {
        this.mEditingMask = editingLayerMask;
        this.refreshMap();
    }

    /**
     * Sets the interaction mode to erasing lines.
     */
    public void setEraseMode() {
        this.setInteractionMode(new EraserInteractionMode(this));
    }

    /**
     * Sets the interaction mode to drawing fog of war regions.
     */
    public void setFogOfWarDrawMode() {
        this.setInteractionMode(new MaskDrawInteractionMode(this));
    }

    /**
     * Sets the interaction mode to erase mask regions.
     */
    public void setFogOfWarEraseMode() {
        this.setInteractionMode(new MaskEraseInteractionMode(this));
    }

    /**
     * Sets what to do with the fog of war layer.
     * 
     * @param mode
     *            The fog of war mode to use.
     */
    public void setFogOfWarMode(final FogOfWarMode mode) {
        this.mFogOfWarMode = mode;
        this.refreshMap();
    }

    /**
     * Sets the interaction mode to the given listener.
     * 
     * @param mode
     *            The interaction mode to use.
     */
    private void setInteractionMode(final CombatViewInteractionMode mode, boolean areTokensManipulatable) {
    	this.setAreTokensManipulatable(areTokensManipulatable);
    	if (this.mInteractionMode != null) {
            this.mInteractionMode.onEndMode();
        }

        this.mGestureDetector = new GestureDetector(this.getContext(), mode);
        this.mGestureDetector.setOnDoubleTapListener(mode);
        this.mGestureDetector.setIsLongpressEnabled(mode.useDefaultLongPressLogic());
        this.mScaleDetector = new ScaleGestureDetector(this.getContext(), mode);
        this.mInteractionMode = mode;

        this.mInteractionMode.onStartMode();

        this.refreshMap();
    }
    
    
    private void setInteractionMode(final CombatViewInteractionMode mode) {
    	this.setInteractionMode(mode, false);
    }

    /**
     * @param newLineColor
     *            the newLineColor to set
     */
    public void setNewLineColor(final int newLineColor) {
        this.mNewLineColor = newLineColor;
    }

    /**
     * @param width
     *            the newLineStrokeWidth to set
     */
    public void setNewLineStrokeWidth(final float width) {
        this.mNewLineStrokeWidth = width;
    }

    /**
     * @param newLineStyle
     *            the newLineStyle to set
     */
    public void setNewLineStyle(NewLineStyle newLineStyle) {
        this.mNewLineStyle = newLineStyle;
    }

    /**
     * Sets the listener for requests to change or create text objects.
     * 
     * @param newTextEntryListener
     *            The listener to use.
     */
    public void setNewTextEntryListener(
            ActivityRequestListener newTextEntryListener) {
        this.mActivityRequestListener = newTextEntryListener;
    }

    /**
     * Sets the listener to publish refresh requests to.
     * 
     * @param onRefreshListener
     *            The listener to use.
     */
    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.mOnRefreshListener = onRefreshListener;
    }

    /**
     * Sets the interaction mode to resizing the grid independent of anything
     * already drawn.
     */
    public void setResizeGridMode() {
        this.setInteractionMode(new GridRepositioningInteractionMode(this));
    }

    /**
     * @param shouldSnapToGrid
     *            the shouldSnapToGrid to set
     */
    public void setShouldSnapToGrid(final boolean shouldSnapToGrid) {
        this.mSnapToGrid = shouldSnapToGrid;
    }

    /**
     * Sets the interaction mode to creating text objects.
     */
    public void setTextMode() {
        this.setInteractionMode(new DrawTextInteractionMode(this));
    }


    public void setInfoMode() { this.setInteractionMode(new CreateInfoInteractionMode(this)); }

    /**
     * Sets the interaction mode to dragging tokens; this will zoom and pan when
     * not on a token. Note that annotations should always draw in this mode.
     */
    public void setTokenManipulationMode() {
        this.setInteractionMode(new TokenManipulationInteractionMode(this), true);
        this.mShouldDrawAnnotations = true;
        this.mShouldDrawGmNotes = true;
        if (this.mData != null) {
            this.mUndoRedoTarget = this.mData.getTokens();
        }
    }

    /**
     * Sets whether tokens snap to the intersections of grid lines rather than
     * the spaces between them.
     * 
     * @param shouldSnap
     *            True if should snap to intersections.
     */
    public void setTokensSnapToIntersections(boolean shouldSnap) {
        this.mTokensSnapToIntersections = shouldSnap;

    }

    /**
     * Sets the interaction mode to simple zooming and panning.
     */
    public void setZoomPanMode() {
        this.setInteractionMode(new ZoomPanInteractionMode(this));
    }

    /**
     * @return the shouldSnapToGrid
     */
    public boolean shouldSnapToGrid() {
        return this.mSnapToGrid;
    }

    /**
     * Kicks off the process of adding a background image to this map.  Note
     * that this method doesn't actually add the background image, since opening
     * another activity to select the image is necessary.
     * @param locationWorldSpace The location to add the background image, in
     * world space.
     */
    public void startAddingBackgroundImage(PointF locationWorldSpace) {
        if (mActivityRequestListener != null) {
            mActivityRequestListener.requestNewBackgroundImage(
                    locationWorldSpace);
        }
    }

    /**
     * @return Whether tokens snap to the intersections of grid lines rather
     *         than the spaces between them.
     */
    public boolean tokensSnapToIntersections() {
        return this.mTokensSnapToIntersections;
    }

    /**
     * Sets the annotation layer as the active layer, so that any draw commands
     * will draw on the annotations.
     */
    public void useAnnotationLayer() {
        this.mActiveLines = this.getData().getAnnotationLines();
        this.mShouldDrawAnnotations = true;
        this.mShouldDrawGmNotes = false;
        this.mUndoRedoTarget = this.mActiveLines;
    }

    /**
     * Sets the background layer as the active layer, so that any draw commands
     * will draw on the background.
     */
    public void useBackgroundLayer() {
        this.mActiveLines = this.getData().getBackgroundLines();
        this.mShouldDrawAnnotations = false;
        this.mShouldDrawGmNotes = false;
        this.mUndoRedoTarget = this.mActiveLines;
    }

    /**
     * Sets the gm note layer as the active layer, so that any draw commands
     * will draw on the annotations.
     */
    public void useGmNotesLayer() {
        this.mActiveLines = this.getData().getGmNoteLines();
        this.mShouldDrawAnnotations = false;
        this.mShouldDrawGmNotes = true;
        this.mUndoRedoTarget = this.mActiveLines;
    }

    /**
     * The style that new lines should have.
     * 
     * @author Tim
     * 
     */
    public enum NewLineStyle {
        /**
         * Draw a circle.
         */
        CIRCLE,

        /**
         * Draw freehand lines.
         */
        FREEHAND,

        /**
         * Draw rectangle.
         */
        RECTANGLE,

        /**
         * Draw straight lines.
         */
        STRAIGHT,

        /**
         * Draw text.
         */
        TEXT
    }

    /**
     * Callback interface for the activity to listen for requests to create or
     * edit text objects, since these actions require the activity to open a
     * dialog box.
     * 
     * @author Tim
     * 
     */
    public interface ActivityRequestListener {
        /**
         * Called when an edit is requested on an existing text object.
         * 
         * @param t
         *            The text object to edit.
         */
        void requestEditTextObject(OnScreenText t);

        /**
         * Called when a new text object is created.
         * 
         * @param locationWorldSpace
         *            Location to place the new text object in world space.
         */
        void requestNewTextEntry(PointF locationWorldSpace);

        /**
         * Called when a new background image is created.
         * 
         * @param locationWorldSpace
         *            Location to place the new image in world space.
         */
        void requestNewBackgroundImage(PointF locationWorldSpace);

        /**
         * Called when a new information location is requested.
         * @param locationWorldSpace
         */
        void requestNewInfoEntry(PointF locationWorldSpace);

        void requestEditInfoObject(Information information);
    }

    /**
     * Called when this combat view is refreshed.
     * 
     * @author Tim
     * 
     */
    public interface OnRefreshListener {
        /**
         * Called when this combat view is refreshed.
         */
        void onRefresh(boolean interactionDone);
    }

    /**
     * Sets the currently selected background image, and bubbles this
     * information up to the activity via the ImageSelectionListener.
     * @param selectedImage The image that is now selected.
     */
    public void reportCurrentlySelectedImage(BackgroundImage selectedImage) {
        if (mImageSelectionListener != null) {
            mImageSelectionListener.onSelectBackgroundImage(selectedImage);
        }
    }

    /**
     * Listener to allow us to report to the activity whenever a new background
     * image has been selected.
     * @author Tim
     *
     */
    public interface ImageSelectionListener {
        /**
         * Reports that the given image has been selected.
         * @param selectedImage The image selected.
         */
        void onSelectBackgroundImage(BackgroundImage selectedImage);

        /**
         * Reports that no image is currently selected.  This may be called
         * extraneously.
         */
        void onSelectNoBackgroundImage();
    }


    /**
     * Sets the listener for changes in image selection.
     * @param listener The listener to use.
     */
    public void setImageSeletionListener(ImageSelectionListener listener) {
        mImageSelectionListener = listener;
    }

    /**
     * Sets whether the mask applies to tokens.
     * @param applyMask Whether the mask applies to tokens.
     */
    public void setMaskAppliesToTokens(boolean applyMask) {
        mApplyMaskToTokens = applyMask;
    }

    public BackgroundImage getSelectedBackgroundImage() {
        return mSelectedBackgroundImage;
    }


    public void setSelectedBackgroundImage(BackgroundImage selectedImage) {
        mSelectedBackgroundImage = selectedImage;
        if (selectedImage != null) {
            this.mImageSelectionListener.onSelectBackgroundImage(selectedImage);
        } else {
            this.mImageSelectionListener.onSelectNoBackgroundImage();
        }
    }

	public void setMeasuringTapeMode() {
		this.setInteractionMode(new MeasuringTapeInteractionMode(this));
	}
	
	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		mScrollBuffer.allocateBitmaps(w, h, this.getContext());
	}
	
	public void scroll(float deltaXF, float deltaYF) {
		DrawRequest req = mScrollBuffer.scroll(deltaXF, deltaYF);
		if (req == null) return;
		
        getWorldSpaceTransformer()
        	.moveOrigin(req.deltaX, req.deltaY);
		
		for (Rect r: req.invalidRegions) {
			req.canvas.clipRect(r, Op.REPLACE);
			this.drawOnCanvas(req.canvas, r);
		}

        SurfaceHolder holder = this.getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            canvas.drawBitmap(mScrollBuffer.getActiveBuffer(), 0, 0, null);
            this.drawOverlays(canvas);
            holder.unlockCanvasAndPost(canvas);
        }
	}

    public void setLoader(TokenImageManager.Loader loader) {
        mLoader = loader;
    }

    private void loadNewTokenImages() {
        Set<String> newTokenImages = mData.getVisibleTokenIds(this.getWidth(), this.getHeight());

        Set<String> toLoad = Sets.difference(newTokenImages, mVisibleTokenImages);
        Set<String> toDiscard = Sets.difference(mVisibleTokenImages, newTokenImages);

        for (String id : toDiscard) {
            mLoader.discardOrCancelTokenLoad(id);
        }

        if (!toLoad.isEmpty()) {
            TokenImageManager.getInstance(getContext()).requireTokenImages(toLoad, mLoader,
                    new TokenImageManager.MultiLoadCallback() {
                        @Override
                        protected void imagesLoaded(Collection<String> tokenIds) {
                            refreshMap();
                        }
                    });
        }
    }

    /**
     * Should be called when tokens are added to or removed from the map instead of just refrshing.
     */
    public void alertTokensChanged() {
        // LoadNewTokenImages and RefreshMap could each trigger a redraw, so batch full screen
        // draws in this method to avoid that.
        startBatchingDraws();
        loadNewTokenImages();
        refreshMap();
        stopBatchingDraws();
    }
}