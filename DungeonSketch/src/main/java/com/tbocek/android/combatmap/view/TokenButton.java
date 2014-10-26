package com.tbocek.android.combatmap.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.tbocek.android.combatmap.model.primitives.BaseToken;

import javax.annotation.Nonnull;

/**
 * Represents a button that contains a prototype for a token. Draws the button
 * based on the token's prototype, and provides a method to construct a copy.
 * 
 * @author Tim Bocek
 * 
 */
public class TokenButton extends ImageView {

    private static final String TAG = "TokenButton";

    private static Paint mNullTokenPaint = null;

    public interface TokenSelectedListener {
        void OnTokenSelected(BaseToken token);
    }

    /**
     * How much to scale the token by. 1.0 means it is completely inscribed in
     * the button element.
     */
    private static final float TOKEN_SCALE = 0.8f;

    /**
     * Whether this token button is allowed to initiate a drag action.
     */
    private boolean mAllowDrag = true;

    /**
     * Whether tokens should be drawn as if on a dark background.
     */
    private boolean mDrawDark = false;

    /**
     * A gesture detector used to detect long presses for drag and drop start.
     */
    private final GestureDetector mGestureDetector;


    private TokenSelectedListener mTokenSelectedListener = null;

    /**
     * A gesture listener used to start a drag and drop when a long press
     * occurs.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final SimpleOnGestureListener mGestureListener =
            new SimpleOnGestureListener() {
                @Override
                public void onLongPress(final MotionEvent e) {
                    if (TokenButton.this.mAllowDrag
                            && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                        TokenButton.this.onStartDrag();
                    }
                }

                @Override
                public boolean onSingleTapConfirmed(final MotionEvent e) {
                    if (mTokenSelectedListener != null) {
                        mTokenSelectedListener.OnTokenSelected(getClone());
                    }
                    return true;
                }
            };

    public void setPrototype(BaseToken prototype) {
        mPrototype = prototype;
    }

    /**
     * The token represented by this button.
     */
    private BaseToken mPrototype;

    /**
     * Constructor for tools
     * @param context The context to create this view in.
     */
    public TokenButton(final Context context) {
        this(context, (BaseToken)null);
    }


    /**
     * Constructor for tools
     * @param context The context to create this view in.
     */
    @SuppressWarnings("UnusedDeclaration") // Used by tools
    public TokenButton(final Context context, final AttributeSet attrs) {
        this(context);
    }


    /**
     * Constructor.
     * 
     * @param context
     *            The context to create this view in.
     * @param prototype
     *            The prototype token that this view represents.
     */
    public TokenButton(final Context context, final BaseToken prototype) {
        super(context);
        this.mPrototype = prototype;

        // Set up listener to see if a drag has started.
        this.mGestureDetector =
                new GestureDetector(this.getContext(), this.mGestureListener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        if (mNullTokenPaint == null) {
            mNullTokenPaint = new Paint();
            mNullTokenPaint.setColor(Color.BLACK);
        }
    }

    /**
     * @param allowDrag
     *            Whether to allow this token button to start a drag action.
     */
    public void allowDrag(boolean allowDrag) {
        this.mAllowDrag = allowDrag;
    }

    /**
     * Gets a new token that is a clone of the token specified here.
     * 
     * @return A clone of the token.
     */
    public final BaseToken getClone() {
        try {
            return this.mPrototype.clone();
        } catch (CloneNotSupportedException e) {
            Log.e(TAG, "Failed to clone token", e);
            return null;
        }
    }

    /**
     * @return The original prototype token.
     */
    public final BaseToken getPrototype() {
        return this.mPrototype;
    }

    /**
     * Gets the token ID of the managed token.
     * 
     * @return The token ID.
     */
    public final String getTokenId() {

        if (this.mPrototype != null) {
            return this.mPrototype.getTokenId();
        }
        else {
            return null;
        }
    }

    /**
     * @return The radius that should be used when drawing a token.
     */
    protected final float getTokenRadius() {
        return Math.min(this.getWidth(), this.getHeight()) * TOKEN_SCALE / 2;
    }

    @Override
    public void onDraw(final @Nonnull Canvas c) {
        if (mPrototype != null) {
            this.mPrototype.draw(c, (float) this.getWidth() / 2,
                    (float) this.getHeight() / 2, this.getTokenRadius(),
                    this.mDrawDark, true);
        } else {
            c.drawCircle((float) this.getWidth() / 2,
                    (float) this.getHeight() / 2, this.getTokenRadius(), mNullTokenPaint);
        }
    }

    /**
     * Called when a drag and drop operation should start.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void onStartDrag() {
        try {
            this.startDrag(null, new DragShadowBuilder(TokenButton.this),
                    this.mPrototype.clone(), 0);
        } catch (CloneNotSupportedException e) {
            Log.e(TAG, "Clone failed when starting drag", e);
        }
    }

    @Override
    public boolean onTouchEvent(final @Nonnull MotionEvent ev) {
        this.mGestureDetector.onTouchEvent(ev);
        return super.onTouchEvent(ev);
    }

    /**
     * @param drawDark
     *            Whether tokens are drawn on a dark background.
     */
    public void setShouldDrawDark(boolean drawDark) {
        this.mDrawDark = drawDark;
    }

    /**
     * @return Whether tokens are drawn on a dark background.
     */
    public boolean shouldDrawDark() {
        return this.mDrawDark;
    }

    private boolean mLoadedTokenImage;

    public boolean loadedTokenImage() {
        return mLoadedTokenImage;
    }

    public void setLoadedTokenImage(boolean loadedTokenImage) {
        mLoadedTokenImage = loadedTokenImage;
    }

    public void setTokenSelectedListener(TokenSelectedListener tokenSelectedListener) {
        mTokenSelectedListener = tokenSelectedListener;
    }
}
