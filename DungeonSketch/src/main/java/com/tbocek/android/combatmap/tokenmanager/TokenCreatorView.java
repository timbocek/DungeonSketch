package com.tbocek.android.combatmap.tokenmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;

import com.tbocek.android.combatmap.model.primitives.PointF;

/**
 * This view is the main rendering and manipulation logic for the token creator
 * activity.
 * 
 * @author Tim Bocek
 * 
 */
public final class TokenCreatorView extends View {
    /**
     * The maximum dimension to output for a square token. If a token is too big
     * it will be scaled down to this when saving.
     */
    private static final int MAX_TOKEN_SIZE = 150;

    /**
     * X coordinate of the center of the circle drawn on the candidate image, in
     * screen space.
     */
    private float mCircleCenterX;

    /**
     * Y coordinate of the center of the circle drawn on the candidate image, in
     * screen space.
     */
    private float mCircleCenterY;

    /**
     * Radius of the circle drawn on the candidate image, in screen space.
     */
    private float mCircleRadius;

    /**
     * The current image that is being cropped.
     */
    private Drawable mCurrentImage;

    /**
     * Detector that will allow the user to move the candidate circle.
     */
    private GestureDetector mGestureDetector;

    /**
     * Whether a circle has been drawn on the loaded token yet.
     */
    private boolean mHasCircle;

    /**
     * Gesture listener that moves the candidate circle when the user scrolls.
     */
    private SimpleOnGestureListener mOnGesture = new SimpleOnGestureListener() {
        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                final float distanceX, final float distanceY) {
            TokenCreatorView.this.mCircleCenterX -= distanceX;
            TokenCreatorView.this.mCircleCenterY -= distanceY;
            TokenCreatorView.this.invalidate();
            return true;
        }
    };

    /**
     * Gesture listener that resizes the candidate circle when the user pinch
     * zooms.
     */
    private SimpleOnScaleGestureListener mOnScaleGesture =
            new SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(final ScaleGestureDetector detector) {
                    TokenCreatorView.this.mCircleCenterX = detector.getFocusX();
                    TokenCreatorView.this.mCircleCenterY = detector.getFocusY();
                    TokenCreatorView.this.mCircleRadius =
                            detector.getCurrentSpan() / 2;
                    TokenCreatorView.this.mHasCircle = true;
                    TokenCreatorView.this.invalidate();
                    return true;
                }
            };

    /**
     * Detector that will allow the user to pinch zoom to resize the candidate
     * circle.
     */
    private ScaleGestureDetector mScaleDetector;

    /**
     * Constructor.
     * 
     * @param context
     *            The context this view is being constructed in.
     */
    @SuppressLint("NewApi")
    public TokenCreatorView(final Context context) {
        super(context);
        this.setFocusable(true);
        this.setFocusableInTouchMode(true);
        this.mGestureDetector =
                new GestureDetector(this.getContext(), this.mOnGesture);
        this.mScaleDetector =
                new ScaleGestureDetector(this.getContext(),
                        this.mOnScaleGesture);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    /**
     * Draws the full brightness image only in the selected circle.
     * 
     * @param canvas
     *            The canvas to draw on.
     */
    public void drawFullBrightnessCircle(final Canvas canvas) {
        canvas.save(Canvas.CLIP_SAVE_FLAG);
        Path p = new Path();
        p.addCircle(this.mCircleCenterX, this.mCircleCenterY,
                this.mCircleRadius, Path.Direction.CW);
        canvas.clipPath(p);
        this.mCurrentImage.draw(canvas);
        canvas.restore();
    }

    /**
     * Draws the image at half brightness.
     * 
     * @param canvas
     *            The canvas to draw on.
     */
    public void drawHalfBrightnessImage(final Canvas canvas) {
        // TODO: Cache the result of the color filter
        this.mCurrentImage.setColorFilter(new PorterDuffColorFilter(Color.GRAY,
                PorterDuff.Mode.MULTIPLY));
        this.mCurrentImage.draw(canvas);
        this.mCurrentImage.setColorFilter(null);
    }

    /**
     * Transforms the center of the circle in screen space to the center of the
     * circle in image space.
     * 
     * @return The center in image space.
     */
    private PointF getCenterInImageSpace() {
        // First, compute where the center would be if there were no scale
        // factor but the origin was at the upper left hand corner of the clip
        // rectangle
        Rect clipRect = this.getImageRect();
        float imageClipSpaceX = this.mCircleCenterX - clipRect.left;
        float imageClipSpaceY = this.mCircleCenterY - clipRect.top;

        // Now, scale these coordinates using the scale factor
        float scaleFactor = this.getScaleFactor();
        return new PointF(imageClipSpaceX * scaleFactor, imageClipSpaceY
                * scaleFactor);
    }

    /**
     * Gets a bitmap that has the selected circle inscribed in it. The circle is
     * not actually drawn in the bitmap, but is exported such that other circle
     * drawing methods will draw it correctly. The bitmap may also be downscaled
     * if it is large.
     * 
     * @return The bitmap suitable for writing to the token database.
     */
    public Bitmap getClippedBitmap() {
        // TODO: Downscale in the case of a large image.
        if (this.mCurrentImage == null || !this.mHasCircle) {
            return null;
        }
        float scale = this.getScaleFactor();

        float squareSizeImageSpace = 2 * this.mCircleRadius * scale;
        int bitmapSquareSize =
                Math.min((int) squareSizeImageSpace, MAX_TOKEN_SIZE);

        Bitmap bitmap =
                Bitmap.createBitmap(bitmapSquareSize, bitmapSquareSize,
                        Bitmap.Config.ARGB_8888);

        // Compute a clipping rectangle that is intentionally larger than the
        // bitmap and allows the bitmap to sit such that the drawn circle is
        // inscribed in it.
        PointF centerImageSpace = this.getCenterInImageSpace();
        PointF upperLeftImageSpace =
                new PointF(centerImageSpace.x - squareSizeImageSpace / 2,
                        centerImageSpace.y - squareSizeImageSpace / 2);

        Rect clippingRect =
                new Rect((int) (-upperLeftImageSpace.x),
                        (int) (-upperLeftImageSpace.y),
                        (int) (-upperLeftImageSpace.x + this.mCurrentImage
                                .getIntrinsicWidth()),
                        (int) (-upperLeftImageSpace.y + this.mCurrentImage
                                .getIntrinsicHeight()));

        Canvas canvas = new Canvas(bitmap);
        float outputScale = ((float) bitmapSquareSize) / squareSizeImageSpace;
        canvas.scale(outputScale, outputScale);
        this.mCurrentImage.setBounds(clippingRect);
        this.mCurrentImage.draw(canvas);

        return bitmap;
    }

    /**
     * Returns a rectangle with the same aspect ratio as the image that
     * represents the area of this view that the image should occupy.
     * 
     * @return The rectangle.
     */
    private Rect getImageRect() {
        if (this.mCurrentImage == null) {
            return null;
        }

        float screenAspectRatio =
                ((float) this.getWidth()) / ((float) this.getHeight());
        float imageAspectRatio =
                ((float) this.mCurrentImage.getIntrinsicWidth())
                        / ((float) this.mCurrentImage.getIntrinsicHeight());

        float width;
        float height;
        if (imageAspectRatio > screenAspectRatio) {
            // Image is wider than the screen, fit width to screen and center
            // vertically
            width = this.getWidth();
            height = width / imageAspectRatio;
        } else {
            // Image is taller than the screen, fit height to screen and center
            // horizontally
            height = this.getHeight();
            width = height * imageAspectRatio;
        }
        float startX = (this.getWidth() - width) / 2;
        float startY = (this.getHeight() - height) / 2;
        return new Rect((int) startX, (int) startY, (int) (startX + width),
                (int) (startY + height));
    }

    /**
     * Returns the ratio of length in image space to length in screen space.
     * 
     * @return The scale factor.
     */
    private float getScaleFactor() {
        if (this.mCurrentImage == null) {
            return 1f;
        }

        float screenAspectRatio =
                ((float) this.getWidth()) / ((float) this.getHeight());
        float imageAspectRatio =
                ((float) this.mCurrentImage.getIntrinsicWidth())
                        / ((float) this.mCurrentImage.getIntrinsicHeight());

        if (imageAspectRatio > screenAspectRatio) {
            // Screen width is limiting factor
            return ((float) this.mCurrentImage.getIntrinsicWidth())
                    / ((float) this.getWidth());
        } else {
            // Screen height is limiting factor
            return ((float) this.mCurrentImage.getIntrinsicHeight())
                    / ((float) this.getHeight());
        }

    }

    @Override
    public void onDraw(final Canvas canvas) {
        if (this.mCurrentImage == null) {
            return;
        }

        this.mCurrentImage.setBounds(this.getImageRect());
        if (this.mHasCircle) {
            // If a circle is being drawn, draw a half-brightness version of
            // the full image followed by a full-brightness version of the
            // clipped region
            this.drawHalfBrightnessImage(canvas);
            this.drawFullBrightnessCircle(canvas);
        } else {
            this.mCurrentImage.draw(canvas);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (!this.mHasCircle && w > 0 && h > 0) {
            this.setCircleToDefault(w, h);
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        // Forward touch events to the gesture detectors.
        this.mGestureDetector.onTouchEvent(ev);
        this.mScaleDetector.onTouchEvent(ev);
        return true;
    }

    /**
     * Sets the currently cut out circle to a default.
     * 
     * @param width
     *            The width of the view
     * @param height
     *            The height of the view
     */
    private void setCircleToDefault(float width, float height) {
        if (width > 0 && height > 0 && this.mCurrentImage != null
                && this.mCurrentImage.getIntrinsicWidth() > 0) {
            this.mCircleCenterX = width / 2;
            this.mCircleCenterY = height / 2;

            float imgWidth = this.mCurrentImage.getIntrinsicWidth();
            float imgHeight = this.mCurrentImage.getIntrinsicHeight();

            // Check the aspect ratio to see if width or height will be
            // the limiting factor
            if (width / height > imgWidth / imgHeight) {
                // Image is thinner than the current view, height will be the
                // limiting factor.
                if (imgWidth / imgHeight < 1) {
                    this.mCircleRadius = .5f * imgWidth * (height / imgHeight);
                } else {
                    this.mCircleRadius = height / 2;
                }
            } else {
                if (imgWidth / imgHeight < 1) {
                    this.mCircleRadius = width / 2;
                } else {
                    this.mCircleRadius = .5f * imgHeight * (width / imgWidth);
                }
            }
            this.mHasCircle = true;
        }
    }

    /**
     * Sets the candidate image that the user will crop.
     * 
     * @param image
     *            The image to eventually use as a token.
     */
    public void setImage(final Drawable image) {
        this.mCurrentImage = image;
        // New image, so clear the circle.
        this.mHasCircle = false;
        if (this.getWidth() > 0) {
            this.setCircleToDefault(this.getWidth(), this.getHeight());
        } else {
            this.mHasCircle = false; // Defer until the widget is sized into the
            // layout.
        }

        this.invalidate();
    }
}
