package com.tbocek.android.combatmap.view.interaction;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.MotionEvent;

import com.google.common.collect.Lists;
import com.tbocek.android.combatmap.DeveloperMode;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.model.primitives.BoundingRectangle;
import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.model.primitives.Util;
import com.tbocek.android.combatmap.view.CombatView;
import com.tbocek.dungeonsketch.R;

import java.util.ArrayList;
import java.util.Collection;

/**
 * An interaction mode that allows the user to drag tokens around the canvas.
 * 
 * @author Tim Bocek
 * 
 */
public final class TokenManipulationInteractionMode extends
        ZoomPanInteractionMode {
    /**
     * Distance in screen space that a token needs to be away from a snap point
     * until it will snap.
     */
    private static final int GRID_SNAP_THRESHOLD = 20;

    /**
     * Rectangle in which to draw the trash can.
     */
    private Rect mTrashCanRect;

    /**
     * Length of the trash can fade in, in ms.
     */
    private static final int TRASH_FADE_IN_DURATION = 1000;

    /**
     * Length of the trash can fade out, in ms.
     */
    private static final int TRASH_FADE_OUT_DURATION = 250;

    PointF debugSnapPoint = null;

    /**
     * True if the token is being hovered over the trash can.
     */
    private boolean mAboutToTrash;

    /**
     * Cached value of whether drawing against a dark background. Used to detect
     * theme changes.
     */
    private boolean mCachedDark;

    /**
     * The token that the user clicked on to start a drag operation. Will be
     * used to determine snapping to grid.
     */
    private BaseToken mCurrentToken;
    
    /**
     * Whether the use is currently dragging a token.
     */
    private boolean mDown;

    /**
     * Whether the current token has been moved.
     */
    private boolean mMoved;
    
    /**
     * When moving tokens with snap to grid enabled, this is the last point that
     * was snapped to.
     */
    private PointF mLastSnappedLocation;
    
    /**
     * The tokens being moved.
     */
    private Collection<BaseToken> mMovedTokens = Lists.newArrayList();
    
    /**
     * Collection of ghost tokens to draw when moving a group of tokens.
     */
    private Collection<BaseToken> mUnmovedTokens = Lists.newArrayList();

    /**
     * Animated alpha value to use for the trash can; allows it to fade in and
     * out.
     */
    private int mTrashCanAlpha;

    /**
     * Animation object to fade the trash can.
     */
    private ValueAnimator mTrashCanAnimator;
    
    /**
     * 
     */

    /**
     * Animation update handler that changes the alpha value of the trash can.
     */
    private ValueAnimator.AnimatorUpdateListener mTrashCanFadeListener = null;

    /**
     * Cached drawable for a trash can to move tokens to.
     */
    private Drawable mTrashDrawable;

    /**
     * Cached drawable for the trash can image when a token is dragged to it.
     */
    private Drawable mTrashHoverDrawable;

    /**
     * Constructor.
     * 
     * @param view
     *            The CombatView that this mode will interact with.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public TokenManipulationInteractionMode(final CombatView view) {
        super(view);

        // Get the size of the trash can
        Resources res = view.getResources();
        int trashX = res.getDimensionPixelSize(R.dimen.trash_can_left);
        int trashY = res.getDimensionPixelSize(R.dimen.trash_can_top);
        int trashWidth = res.getDimensionPixelSize(R.dimen.trash_can_width);
        int trashHeight = res.getDimensionPixelSize(R.dimen.trash_can_height);

        mTrashCanRect = new Rect(trashX, trashY, trashX + trashWidth, trashY + trashHeight);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mTrashCanFadeListener =  new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    TokenManipulationInteractionMode.this.mTrashCanAlpha =
                            (Integer) animation.getAnimatedValue();
                    // TODO: See if it is better to lump this in with the token move refresh.
                    TokenManipulationInteractionMode.this.getView()
                            .refreshMap(mTrashCanRect);
                }
            };
        }
    }

    @Override
    public boolean useDefaultLongPressLogic() { return false; }
    
    @Override
    public void draw(final Canvas c) {
        for (BaseToken t : this.mUnmovedTokens) {
            t.drawGhost(c, this.getView().getGridSpaceTransformer(),
                    t.getLocation());
        }

        if (this.mTrashCanAlpha != 0) {
            // Draw a trash can to drag tokens to.
            this.ensureTrashCanDrawablesCreated();

            if (this.mAboutToTrash) {
                this.mTrashHoverDrawable.setAlpha(this.mTrashCanAlpha);
                this.mTrashHoverDrawable.draw(c);
            } else {
                this.mTrashDrawable.setAlpha(this.mTrashCanAlpha);
                this.mTrashDrawable.draw(c);
            }
        }

        if (this.debugSnapPoint != null && this.mDown) {
            Paint p = new Paint();
            p.setColor(this.getData().getGrid().getColorScheme().getLineColor());
            p.setStyle(Paint.Style.STROKE);

            c.drawCircle(this.debugSnapPoint.x, this.debugSnapPoint.y, 3, p);

        }
    }

    /**
     * Checks whether trash can drawables need to be created, and creates them
     * if needed.
     */
    private void ensureTrashCanDrawablesCreated() {
        if (this.mTrashDrawable == null
                || this.mCachedDark != this.getData().getGrid().isDark()) {
            if (this.getData().getGrid().isDark()) {
                this.mTrashDrawable =
                        this.getView().getResources()
                                .getDrawable(R.drawable.trashcan);
            } else {
                this.mTrashDrawable =
                        this.getView().getResources()
                                .getDrawable(R.drawable.trashcan_dark);
            }

            this.mCachedDark = this.getData().getGrid().isDark();
            this.mTrashDrawable.setBounds(mTrashCanRect);
            this.mTrashHoverDrawable =
                    this.getView().getResources()
                            .getDrawable(R.drawable.trashcan_hover_over);
            this.mTrashHoverDrawable.setBounds(mTrashCanRect);
        }
    }

    /**
     * Begins an animation to fade the trash can in.
     */
    private void fadeTrashCanIn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (this.mTrashCanAnimator != null
                    && this.mTrashCanAnimator.isRunning()) {
                this.mTrashCanAnimator.cancel();
            }
            this.mTrashCanAnimator = ValueAnimator.ofInt(0, Util.FULL_OPACITY);
            this.mTrashCanAnimator.setDuration(TRASH_FADE_IN_DURATION);
            this.mTrashCanAnimator.addUpdateListener(this.mTrashCanFadeListener);
            this.mTrashCanAnimator.start();
        }
    }

    /**
     * Begins an animation to fade the trash can out.
     */
    private void fadeTrashCanOut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (this.mTrashCanAlpha == 0) {
                return;
            }

            if (this.mTrashCanAnimator != null
                    && this.mTrashCanAnimator.isRunning()) {
                this.mTrashCanAnimator.cancel();
            }
            this.mTrashCanAnimator = ValueAnimator.ofInt(this.mTrashCanAlpha, 0);
            this.mTrashCanAnimator.setDuration(TRASH_FADE_OUT_DURATION);
            this.mTrashCanAnimator.addUpdateListener(this.mTrashCanFadeListener);
            this.mTrashCanAnimator.start();
        }
    }

    @Override
    public boolean onDoubleTap(final MotionEvent e) {
        if (this.mCurrentToken != null) {
            this.getView().getTokens().checkpointToken(this.mCurrentToken);
            this.mCurrentToken.setBloodied(!this.mCurrentToken.isBloodied());
            this.getView().getTokens().createCommandHistory();
            this.getView().refreshMap(this.mCurrentToken.getBoundingRectangle().toRectF(),
            		                  this.getView().getGridSpaceTransformer());
        }
        return true;
    }

    @Override
    public boolean onDown(final MotionEvent e) {
    	DeveloperMode.startProfiler("token_manip_down");
        this.mCurrentToken =
                this.getView()
                        .getTokens()
                        .getTokenUnderPoint(new PointF(e.getX(), e.getY()),
                                this.getView().getGridSpaceTransformer());

        if (this.mCurrentToken != null) {
        	this.mMovedTokens.clear();
        	if (this.getView().getMultiSelect().isActive()) {
        		this.mMovedTokens.addAll(this.getView().getMultiSelect().getSelectedTokens());
        		if (!this.mMovedTokens.contains(this.mCurrentToken)) {
        			this.mMovedTokens.add(this.mCurrentToken);
        		}
        	} else {
        		this.mMovedTokens.add(mCurrentToken);
        	}
            this.mMoved = false;
            this.getView().getTokens().checkpointTokens(this.mMovedTokens);
            this.fadeTrashCanIn();
            
            for (BaseToken t: this.mMovedTokens) {
            	this.mUnmovedTokens.add(t.clone());
            }
            this.mLastSnappedLocation = this.mCurrentToken.getLocation();
        } else {
        	super.onDown(e);
        }

        this.mDown = true;
        this.customLongPressDetector.onDown(e);
        return true;
    }

    @Override
    public void onLongPress(final MotionEvent e) {
        if (this.mCurrentToken != null) {
            this.getView().getMultiSelect().addToken(this.mCurrentToken);
            BoundingRectangle redrawRect = this.mCurrentToken.getBoundingRectangle();
            redrawRect.expand(Util.convertDpToPixel(
            		BaseToken.SELECTION_STROKE_WIDTH,
            		getView().getContext()));
            this.getView().refreshMap(redrawRect.toRectF(),
	                  this.getView().getGridSpaceTransformer());
        } else {
            super.onLongPress(e);
        }
    }
    
    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {
    	if (this.customLongPressDetector.isLongPress()) { return true; }
    	
    	if (this.getView().getMultiSelect().isActive()) {
	        BaseToken t =
	                this.getView()
	                        .getTokens()
	                        .getTokenUnderPoint(new PointF(e.getX(), e.getY()),
	                                this.getView().getGridSpaceTransformer());
	        if (t != null) {
	            this.getView().getMultiSelect().toggleToken(t);
	            BoundingRectangle redrawRect = t.getBoundingRectangle();
	            redrawRect.expand(Util.convertDpToPixel(
	            		BaseToken.SELECTION_STROKE_WIDTH,
	            		getView().getContext()));
	            this.getView().refreshMap(redrawRect.toRectF(),
		                  this.getView().getGridSpaceTransformer());
	        }
    	}
        return true;
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
            final float distanceX, final float distanceY) {
        if (this.mCurrentToken != null) {
            this.mMoved = true;
            CoordinateTransformer transformer =
                    this.getView().getGridSpaceTransformer();
            float deltaX;
            float deltaY;

            // If snap to grid is enabled, change the world space movement
            // deltas to compensate for distance between the real point and
            // the snap to grid point.
            if (this.getView().shouldSnapToGrid()) {
                PointF currentPointScreenSpace =
                        new PointF(e2.getX(), e2.getY());
                PointF currentPointWorldSpace =
                        transformer
                                .screenSpaceToWorldSpace(currentPointScreenSpace);
                PointF nearestSnapPointWorldSpace =
                        this.getData()
                                .getGrid()
                                .getNearestSnapPoint(
                                        currentPointWorldSpace,
                                        this.getView()
                                                .tokensSnapToIntersections()
                                                ? 0
                                                : this.mCurrentToken.getSize());

                // Snap to that point if it is less than a threshold
                float distanceToSnapPoint =
                        Util.distance(
                                transformer
                                        .worldSpaceToScreenSpace(nearestSnapPointWorldSpace),
                                currentPointScreenSpace);

                PointF newLocationWorldSpace =
                        distanceToSnapPoint < GRID_SNAP_THRESHOLD
                                ? nearestSnapPointWorldSpace
                                : currentPointWorldSpace;

                deltaX = this.mLastSnappedLocation.x - newLocationWorldSpace.x;
                deltaY = this.mLastSnappedLocation.y - newLocationWorldSpace.y;
                this.mLastSnappedLocation = newLocationWorldSpace;

            } else {
                deltaX = transformer.screenSpaceToWorldSpace(distanceX);
                deltaY = transformer.screenSpaceToWorldSpace(distanceY);
            }
            BoundingRectangle redrawRect = new BoundingRectangle();
            for (BaseToken t : this.mMovedTokens) {
            	// Update the redraw bounds with both the before and after state
            	// of this token.
            	redrawRect.updateBounds(t.getBoundingRectangle());
                t.move(deltaX, deltaY);
                redrawRect.updateBounds(t.getBoundingRectangle());
            }
            this.mAboutToTrash =
                    mTrashCanRect.contains((int) e2.getX(), (int) e2.getY());
            
            // If tokens have been highlighted with a selection, we need a few more
            // pixels around the refresh area because the select indicator draws around the
            // edge of the tokens.
            if (this.getView().getMultiSelect().isActive()) {
	            redrawRect.expand(Util.convertDpToPixel(
	            		BaseToken.SELECTION_STROKE_WIDTH,
	            		getView().getContext()));
            }
            this.getView().refreshMap(redrawRect.toRectF(), this.getView().getGridSpaceTransformer());
        } else {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
        this.customLongPressDetector.onScroll(e1, e2, distanceX, distanceY);
        return true;
    }

    @Override
    public void onUp(final MotionEvent ev) {
        if (this.mMoved) {
            if (this.mAboutToTrash) {
                this.getView().getTokens().restoreCheckpointedTokens();
                this.getView().getTokens().removeAll(new ArrayList<BaseToken>(this.mMovedTokens));
                this.getView().getMultiSelect().selectNone();
            } else {
                this.getView().getTokens().createCommandHistory();
            }
        }
        
        this.mDown = false;
        this.debugSnapPoint = null;
        
        // Set up a bounding rectangle that will clear out any ghost tokens. 
	    if (!mUnmovedTokens.isEmpty()) {
	        BoundingRectangle redrawRect = new BoundingRectangle();
	        for (BaseToken t : this.mUnmovedTokens) {
	            redrawRect.updateBounds(t.getBoundingRectangle());
	        }
	        this.mUnmovedTokens.clear();
	        this.getView().refreshMap(redrawRect.toRectF(), this.getView().getGridSpaceTransformer());
	    }
	        
        this.mMovedTokens.clear();
        
        this.fadeTrashCanOut();
        this.mAboutToTrash = false;
        this.customLongPressDetector.onUp(ev);
        super.onUp(ev);
        DeveloperMode.stopProfiler();
    }

}
