package com.tbocek.android.combatmap.view.interaction;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.MotionEvent;

import com.tbocek.android.combatmap.model.primitives.BackgroundImage;
import com.tbocek.android.combatmap.model.primitives.BoundingRectangle;
import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.model.primitives.Util;
import com.tbocek.android.combatmap.view.CombatView;

/**
 * Provides an interaction mode that allows importing and creating images.
 * 
 * @author Tim
 * 
 */
public class BackgroundImageInteractionMode extends BaseDrawInteractionMode {

    // TODO: dp
    private static final int HANDLE_CIRCLE_RADIUS_DP = 12;

    private HandleMode mHandleMode;

    private PointF mLastDragPoint;

    public BackgroundImageInteractionMode(CombatView view) {
        super(view);
    }

    @Override
    public void draw(Canvas c) {
        BackgroundImage selectedImage =
                this.getView().getSelectedBackgroundImage();
        // Draw border and handles on the selected image.
        if (selectedImage != null &&
                this.getData().getBackgroundImages().contains(selectedImage)) {
            BoundingRectangle r = selectedImage.getBoundingRectangle();

            // Convert bounding rectangle bounds to screen space.
            PointF upperLeft =
                    this.getData()
                    .getWorldSpaceTransformer()
                    .worldSpaceToScreenSpace(
                            new PointF(r.getXMin(), r.getYMin()));
            PointF lowerRight =
                    this.getData()
                    .getWorldSpaceTransformer()
                    .worldSpaceToScreenSpace(
                            new PointF(r.getXMax(), r.getYMax()));
            float xmin = upperLeft.x;
            float xmax = lowerRight.x;
            float ymin = upperLeft.y;
            float ymax = lowerRight.y;

            Paint borderHandlePaint = new Paint();
            borderHandlePaint.setColor(Util.ICS_BLUE);
            borderHandlePaint.setStrokeWidth(2);
            borderHandlePaint.setStyle(Paint.Style.STROKE);

            HandleSet handles = new HandleSet(xmin, xmax, ymin, ymax);
            this.drawHandle(c, handles.getLeft(), borderHandlePaint);
            this.drawBorderSegment(c, handles.getLeft(),
                    handles.getUpperLeft(), borderHandlePaint);
            this.drawHandle(c, handles.getUpperLeft(), borderHandlePaint);
            this.drawBorderSegment(c, handles.getUpperLeft(), handles.getTop(),
                    borderHandlePaint);
            this.drawHandle(c, handles.getTop(), borderHandlePaint);
            this.drawBorderSegment(c, handles.getTop(),
                    handles.getUpperRight(), borderHandlePaint);
            this.drawHandle(c, handles.getUpperRight(), borderHandlePaint);
            this.drawBorderSegment(c, handles.getUpperRight(),
                    handles.getRight(), borderHandlePaint);
            this.drawHandle(c, handles.getRight(), borderHandlePaint);
            this.drawBorderSegment(c, handles.getRight(),
                    handles.getLowerRight(), borderHandlePaint);
            this.drawHandle(c, handles.getLowerRight(), borderHandlePaint);
            this.drawBorderSegment(c, handles.getLowerRight(),
                    handles.getBottom(), borderHandlePaint);
            this.drawHandle(c, handles.getBottom(), borderHandlePaint);
            this.drawBorderSegment(c, handles.getBottom(),
                    handles.getLowerLeft(), borderHandlePaint);
            this.drawHandle(c, handles.getLowerLeft(), borderHandlePaint);
            this.drawBorderSegment(c, handles.getLowerLeft(),
                    handles.getLeft(), borderHandlePaint);

        }
    }

    @Override
    public void onEndMode() {
        // When ending this interaction mode, clear whatever image is selected.
        getView().setSelectedBackgroundImage(null);
    }

    /**
     * Helper function to draw a segment of the selection border. Accounts for
     * the presence of a circle handle and doesn't overlap it.
     * 
     * @param c Canvas to draw on.
     * @param p1 First point of the line segment.
     * @param p2 Second point on the line segment.
     * @param p Paint object to draw with.
     */
    private void drawBorderSegment(Canvas c, PointF p1, PointF p2, Paint p) {
        float horizontalClip =
                Math.abs(p1.x - p2.x) > Math.abs(p1.y - p2.y) ? this
                        .handleCircleRadiusPx() : 0;
                        float verticalClip =
                                Math.abs(p1.x - p2.x) < Math.abs(p1.y - p2.y) ? this
                                        .handleCircleRadiusPx() : 0;

                                        c.drawLine(Math.min(p1.x, p2.x) + horizontalClip, Math.min(p1.y, p2.y)
                                                + verticalClip, Math.max(p1.x, p2.x) - horizontalClip,
                                                Math.max(p1.y, p2.y) - verticalClip, p);
    }

    private void drawHandle(Canvas c, PointF location, Paint p) {
        c.drawCircle(location.x, location.y, this.handleCircleRadiusPx(), p);
    }

    private int handleCircleRadiusPx() {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                HANDLE_CIRCLE_RADIUS_DP, this.getView().getResources()
                .getDisplayMetrics());
    }

    @Override
    public boolean onDown(final MotionEvent e) {
        PointF locationWorldSpace =
                this.getData()
                .getWorldSpaceTransformer()
                .screenSpaceToWorldSpace(new PointF(e.getX(), e.getY()));

        BackgroundImage selectedImage =
                this.getData()
                .getBackgroundImages()
                .getImageOnPoint(
                        locationWorldSpace,
                        this.getData()
                        .getWorldSpaceTransformer()
                        .screenSpaceToWorldSpace(
                                this.handleCircleRadiusPx()));

        this.mLastDragPoint = new PointF(e.getX(), e.getY());

        if (selectedImage != null) {
            this.getView().setSelectedBackgroundImage(selectedImage);
            // Select a handle mode based on what part of the image was touched.
            BoundingRectangle r = selectedImage.getBoundingRectangle();

            // Convert bounding rectangle bounds to screen space.
            PointF upperLeft =
                    this.getData()
                    .getWorldSpaceTransformer()
                    .worldSpaceToScreenSpace(
                            new PointF(r.getXMin(), r.getYMin()));
            PointF lowerRight =
                    this.getData()
                    .getWorldSpaceTransformer()
                    .worldSpaceToScreenSpace(
                            new PointF(r.getXMax(), r.getYMax()));
            float xmin = upperLeft.x;
            float xmax = lowerRight.x;
            float ymin = upperLeft.y;
            float ymax = lowerRight.y;

            HandleSet handles = new HandleSet(xmin, xmax, ymin, ymax);
            this.mHandleMode = handles.getHandleMode(this.mLastDragPoint);
            if (this.getView().shouldSnapToGrid()) {
                this.mLastDragPoint =
                        handles.getClickedHandleCenter(this.mLastDragPoint);
            }
            this.getView().reportCurrentlySelectedImage(selectedImage);
        }

        this.getView().refreshMap();
        return true;
    }

    @Override
    public void onUp(final MotionEvent event) {
        this.getData().getBackgroundImages().checkpointImageAfter();
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
            final float distanceXignored, final float distanceYignored) {

        PointF snapped = this.getScreenSpacePoint(e2);

        float distanceX = snapped.x - this.mLastDragPoint.x;
        float distanceY = snapped.y - this.mLastDragPoint.y;
        this.mLastDragPoint = snapped;

        BackgroundImage selectedImage =
                this.getView().getSelectedBackgroundImage();

        if (selectedImage != null &&
                this.getData().getBackgroundImages().contains(selectedImage)) {
            // Now that we are for sure modifying the image, checkpoint it if
            // it hasn't been checkpointed already.
            this.getData().getBackgroundImages().checkpointImageBefore(
                    selectedImage);

            float wsDistX =
                    this.getData().getWorldSpaceTransformer()
                    .screenSpaceToWorldSpace(distanceX);
            float wsDistY =
                    this.getData().getWorldSpaceTransformer()
                    .screenSpaceToWorldSpace(distanceY);

            switch (this.mHandleMode) {
            case LEFT:
                selectedImage.moveLeft(wsDistX);
                if (selectedImage.shouldMaintainAspectRatio()) {
                    selectedImage.recomputeHeight();
                }
                break;
            case RIGHT:
                selectedImage.moveRight(wsDistX);
                if (selectedImage.shouldMaintainAspectRatio()) {
                    selectedImage.recomputeHeight();
                }
                break;
            case TOP:
                selectedImage.moveTop(wsDistY);
                if (selectedImage.shouldMaintainAspectRatio()) {
                    selectedImage.recomputeWidth();
                }
                break;
            case BOTTOM:
                selectedImage.moveBottom(wsDistY);
                if (selectedImage.shouldMaintainAspectRatio()) {
                    selectedImage.recomputeWidth();
                }
                break;
            case UPPER_LEFT:
                selectedImage.moveLeft(wsDistX);
                selectedImage.moveTop(wsDistY);
                if (selectedImage.shouldMaintainAspectRatio()) {
                    if (Math.abs(wsDistX) > Math.abs(wsDistY)) {
                        selectedImage.recomputeHeight();
                    } else {
                        selectedImage.recomputeWidth();
                    }
                }
                break;
            case LOWER_LEFT:
                selectedImage.moveLeft(wsDistX);
                selectedImage.moveBottom(wsDistY);
                if (selectedImage.shouldMaintainAspectRatio()) {
                    if (Math.abs(wsDistX) > Math.abs(wsDistY)) {
                        selectedImage.recomputeHeight();
                    } else {
                        selectedImage.recomputeWidth();
                    }
                }
                break;
            case UPPER_RIGHT:
                selectedImage.moveRight(wsDistX);
                selectedImage.moveTop(wsDistY);
                if (selectedImage.shouldMaintainAspectRatio()) {
                    if (Math.abs(wsDistX) > Math.abs(wsDistY)) {
                        selectedImage.recomputeHeight();
                    } else {
                        selectedImage.recomputeWidth();
                    }
                }
                break;
            case LOWER_RIGHT:
                selectedImage.moveRight(wsDistX);
                selectedImage.moveBottom(wsDistY);
                if (selectedImage.shouldMaintainAspectRatio()) {
                    if (Math.abs(wsDistX) > Math.abs(wsDistY)) {
                        selectedImage.recomputeHeight();
                    } else {
                        selectedImage.recomputeWidth();
                    }
                }
                break;
            default:
                selectedImage.moveImage(wsDistX, wsDistY);
                // Snap the upper left hand corner of the image to the grid if needed.
                if (this.getView().shouldSnapToGrid()) {
                    PointF pointWS = selectedImage.getBoundingRectangle().getUpperLeft();
                    PointF snapPointWS = this.getData().getGrid().getNearestSnapPoint(pointWS, 0);
                    selectedImage.moveImage(snapPointWS.x - pointWS.x, snapPointWS.y - pointWS.y);
                }
                break;
            }
            this.getView().refreshMap();
        } else {
            super.onScroll(e1, e2, distanceX, distanceY);
        }

        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {
        PointF locationWorldSpace =
                this.getData()
                .getWorldSpaceTransformer()
                .screenSpaceToWorldSpace(new PointF(e.getX(), e.getY()));

        BackgroundImage tappedImage =
                this.getData()
                .getBackgroundImages()
                .getImageOnPoint(
                        locationWorldSpace,
                        this.getData()
                        .getWorldSpaceTransformer()
                        .screenSpaceToWorldSpace(
                                this.handleCircleRadiusPx()));

        if (tappedImage == null) {
            this.getView().startAddingBackgroundImage(locationWorldSpace);
            /*
            BackgroundImage i =
                    new BackgroundImage(this.getView().getResources()
                            .getDrawable(R.drawable.add_image));
            i.setLocation(locationWorldSpace);
            this.getData().getBackgroundImages().addImage(i);
            this.mSelectedImage = i;*/
        }
        this.getView().refreshMap();
        return true;
    }

    /**
     * Enum for representing what aspect of an image is being dragged.
     * 
     * @author Tim
     * 
     */
    private enum HandleMode {
        BOTTOM, LEFT, LOWER_LEFT, LOWER_RIGHT, // CHECKSTYLE:OFF
        MOVE,
        RIGHT,
        TOP,
        UPPER_LEFT,
        UPPER_RIGHT,
        // CHECKSTYLE:ON
    }

    /**
     * Provides ability to specify a rectangle and query the locations of the
     * handles to manipulate parts of that rectangle.
     * 
     * @author Tim
     * 
     */
    private class HandleSet {
        final float mXmax;
        final float mXmin;
        final float mYmax;
        final float mYmin;

        public HandleSet(float xmin, float xmax, float ymin, float ymax) {
            this.mXmin = xmin;
            this.mXmax = xmax;
            this.mYmin = ymin;
            this.mYmax = ymax;
        }

        public PointF getBottom() {
            return new PointF((this.mXmin + this.mXmax) / 2, this.mYmax);
        }

        /**
         * Gets the center of the handle that was clicked on. This is used to
         * more accurately snap those handles to the grid. In the case of moving
         * the image rather than an edge, the upper left point is returned since
         * that is what will snap to the grid.
         * 
         * @param p Point that was clicked on.
         * @return Adjusted point corresponding to the correct handle, if a handle was clicked.
         */
        public PointF getClickedHandleCenter(PointF p) {
            int r = BackgroundImageInteractionMode.this.handleCircleRadiusPx();
            if (Util.distance(p, this.getLeft()) < r) {
                return this.getLeft();
            } else if (Util.distance(p, this.getRight()) < r) {
                return this.getRight();
            } else if (Util.distance(p, this.getTop()) < r) {
                return this.getTop();
            } else if (Util.distance(p, this.getBottom()) < r) {
                return this.getBottom();
            } else if (Util.distance(p, this.getUpperLeft()) < r) {
                return this.getUpperLeft();
            } else if (Util.distance(p, this.getLowerLeft()) < r) {
                return this.getLowerLeft();
            } else if (Util.distance(p, this.getUpperRight()) < r) {
                return this.getUpperRight();
            } else if (Util.distance(p, this.getLowerRight()) < r) {
                return this.getLowerRight();
            } else {
                return p;
            }
        }

        public HandleMode getHandleMode(PointF p) {
            int r = BackgroundImageInteractionMode.this.handleCircleRadiusPx();
            if (Util.distance(p, this.getLeft()) < r) {
                return HandleMode.LEFT;
            } else if (Util.distance(p, this.getRight()) < r) {
                return HandleMode.RIGHT;
            } else if (Util.distance(p, this.getTop()) < r) {
                return HandleMode.TOP;
            } else if (Util.distance(p, this.getBottom()) < r) {
                return HandleMode.BOTTOM;
            } else if (Util.distance(p, this.getUpperLeft()) < r) {
                return HandleMode.UPPER_LEFT;
            } else if (Util.distance(p, this.getLowerLeft()) < r) {
                return HandleMode.LOWER_LEFT;
            } else if (Util.distance(p, this.getUpperRight()) < r) {
                return HandleMode.UPPER_RIGHT;
            } else if (Util.distance(p, this.getLowerRight()) < r) {
                return HandleMode.LOWER_RIGHT;
            } else {
                return HandleMode.MOVE;
            }
        }

        public PointF getLeft() {
            return new PointF(this.mXmin, (this.mYmin + this.mYmax) / 2);
        }

        public PointF getLowerLeft() {
            return new PointF(this.mXmin, this.mYmax);
        }

        public PointF getLowerRight() {
            return new PointF(this.mXmax, this.mYmax);
        }

        public PointF getRight() {
            return new PointF(this.mXmax, (this.mYmin + this.mYmax) / 2);
        }

        public PointF getTop() {
            return new PointF((this.mXmin + this.mXmax) / 2, this.mYmin);
        }

        public PointF getUpperLeft() {
            return new PointF(this.mXmin, this.mYmin);
        }

        public PointF getUpperRight() {
            return new PointF(this.mXmax, this.mYmin);
        }
    }
}
