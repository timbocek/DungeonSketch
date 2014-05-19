package com.tbocek.android.combatmap.model.primitives;

import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.tbocek.android.combatmap.DataManager;
import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;

public class BackgroundImage implements Cloneable{

    /**
     * The data manager that is used to load custom images.
     */
    private static transient DataManager dataManager = null;

    /**
     * Copy Constructor
     * @param copyFrom BackgroundImage to copy from
     */
    public BackgroundImage(BackgroundImage copyFrom) {
        this.mPath = copyFrom.mPath;
        this.mDrawable = copyFrom.mDrawable;
        this.mOriginWorldSpace = copyFrom.mOriginWorldSpace;
        this.mWidthWorldSpace = copyFrom.mWidthWorldSpace;
        this.mHeightWorldSpace = copyFrom.mHeightWorldSpace;
        this.mKeepAspectRatio = copyFrom.mKeepAspectRatio;
        this.mOriginalAspectRatio = copyFrom.mOriginalAspectRatio;
    }

    public static void registerDataManager(DataManager dataManager) {
        BackgroundImage.dataManager = dataManager;
    }

    /**
     * Path that this image should load from.
     */
    private final String mPath;

    /**
     * Drawable containing the background image.
     */
    private transient Drawable mDrawable = null;
    private transient boolean mTriedToLoadDrawable = false;

    /**
     * Height of the image's containing rectangle, in world space.
     */
    private float mHeightWorldSpace = 0;

    /**
     * Width of the image's containing rectangle, in world space.
     */
    private float mWidthWorldSpace = 0;

    /**
     * Location of the upper left corner of the image's containing rectangle, in
     * world space.
     */
    private PointF mOriginWorldSpace;

    private boolean mKeepAspectRatio = true;
    private float mOriginalAspectRatio = 1;

    /**
     * Constructor.
     * @param path Path to the resource to load.
     * @param originWorldSpace Initial position of the background image, in
     *     world space.
     */
    public BackgroundImage(String path, PointF originWorldSpace) {
        this.mPath = path;
        this.mOriginWorldSpace = originWorldSpace;
    }

    public void loadDrawable() {
        // Don't go any further if we've already tried and failed.
        if (mTriedToLoadDrawable) {
            return;
        }

        // Don't go any further if we don't have a data manager.
        if (dataManager == null) {
            return;
        }

        Bitmap b;
        try {
            b = dataManager.loadMapDataImage(this.mPath);
            this.mDrawable = new BitmapDrawable(dataManager.getContext().getResources(), b);
            // If no width and height yet, set height = 1, width according to
            // aspect ratio of the original image.
            if (this.mWidthWorldSpace < Util.FP_COMPARE_ERROR
                    && this.mHeightWorldSpace < Util.FP_COMPARE_ERROR) {
                this.mOriginalAspectRatio =
                        ((float) b.getWidth()) / b.getHeight();
                this.mHeightWorldSpace = 1;
                this.mWidthWorldSpace = mOriginalAspectRatio;
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.mTriedToLoadDrawable = true;
        }
    }

    /**
     * Draws the background image. Needs to assume an untransformed coordinate
     * space, UNLIKE other draw commands. The coordinate transformer is passed
     * in instead. This is because setBounds is retarded and takes integers.
     * 
     * @param c
     *            Canvas to draw on.
     * @param transformer
     *            The screen to world space transformer.
     */
    public void draw(Canvas c, CoordinateTransformer transformer) {
        if (this.mDrawable == null) {
            return;
        }

        // Convert bounding rectangle bounds to screen space.
        PointF upperLeft =
                transformer.worldSpaceToScreenSpace(new PointF(
                        this.mOriginWorldSpace.x, this.mOriginWorldSpace.y));
        PointF lowerRight =
                transformer.worldSpaceToScreenSpace(new PointF(
                        this.mOriginWorldSpace.x + this.mWidthWorldSpace,
                        this.mOriginWorldSpace.y + this.mHeightWorldSpace));
        int left = (int) Math.min(upperLeft.x, lowerRight.x);
        int right = (int) Math.max(upperLeft.x, lowerRight.x);
        int top = (int) Math.min(upperLeft.y, lowerRight.y);
        int bottom = (int) Math.max(upperLeft.y, lowerRight.y);

        this.mDrawable.setBounds(left, top, right, bottom);

        this.mDrawable.draw(c);
    }

    public BoundingRectangle getBoundingRectangle() {
        return this.getBoundingRectangle(0);
    }

    public BoundingRectangle getBoundingRectangle(float borderWorldSpace) {
        PointF p1 =
                new PointF(this.mOriginWorldSpace.x - borderWorldSpace,
                        this.mOriginWorldSpace.y - borderWorldSpace);
        PointF p2 =
                new PointF(this.mOriginWorldSpace.x + this.mWidthWorldSpace
                        + borderWorldSpace, this.mOriginWorldSpace.y
                        + this.mHeightWorldSpace + borderWorldSpace);
        return new BoundingRectangle(p1, p2);
    }

    public void moveBottom(float delta) {
        this.mHeightWorldSpace += delta;
    }

    public void moveImage(float deltaX, float deltaY) {
        this.mOriginWorldSpace =
                new PointF(this.mOriginWorldSpace.x + deltaX,
                        this.mOriginWorldSpace.y + deltaY);
    }

    public void moveLeft(float delta) {
        this.mOriginWorldSpace.x += delta;
        this.mWidthWorldSpace -= delta;
    }

    public void moveRight(float delta) {
        this.mWidthWorldSpace += delta;
    }

    public void moveTop(float delta) {
        this.mOriginWorldSpace.y += delta;
        this.mHeightWorldSpace -= delta;
    }

    public boolean shouldMaintainAspectRatio() {
        return this.mKeepAspectRatio;
    }

    public void setShouldMaintainAspectRatio(boolean keepAspectRatio) {
        this.mKeepAspectRatio = keepAspectRatio;
    }

    /**
     * Forces this image to recompute the width, given the height and the aspect
     * ratio.
     */
    public void recomputeWidth() {
        this.mWidthWorldSpace =
                this.mHeightWorldSpace * this.mOriginalAspectRatio;
    }


    /**
     * Forces this image to recompute the height, given the width and the aspect
     * ratio.
     */
    public void recomputeHeight() {
        this.mHeightWorldSpace =
                this.mWidthWorldSpace / this.mOriginalAspectRatio;
    }

    @Override
    public BackgroundImage clone() throws CloneNotSupportedException {
        return (BackgroundImage) super.clone();
    }

    public void serialize(MapDataSerializer s) throws IOException {
        s.startObject();
        s.serializeString(this.mPath);
        s.serializeFloat(this.mOriginWorldSpace.x);
        s.serializeFloat(this.mOriginWorldSpace.y);
        s.serializeFloat(this.mWidthWorldSpace);
        s.serializeFloat(this.mHeightWorldSpace);
        s.serializeFloat(this.mOriginalAspectRatio);
        s.serializeBoolean(this.mKeepAspectRatio);
        s.endObject();
    }

    public static BackgroundImage deserialize(MapDataDeserializer s)
            throws IOException {
        s.expectObjectStart();
        String imageName = s.readString();
        float imageX = s.readFloat();
        float imageY = s.readFloat();
        float width = s.readFloat();
        float height = s.readFloat();
        float originalAspectRatio = s.readFloat();
        boolean keepAspectRatio = s.readBoolean();
        s.expectObjectEnd();
        BackgroundImage i = new BackgroundImage(
                imageName, new PointF(imageX, imageY));
        i.mWidthWorldSpace = width;
        i.mHeightWorldSpace = height;
        i.mOriginalAspectRatio = originalAspectRatio;
        i.mKeepAspectRatio = keepAspectRatio;
        return i;
    }

    public void copyLocationDataFrom(BackgroundImage from) {
        mWidthWorldSpace = from.mWidthWorldSpace;
        mHeightWorldSpace = from.mHeightWorldSpace;
        mOriginalAspectRatio = from.mOriginalAspectRatio;
        mKeepAspectRatio = from.mKeepAspectRatio;
        mOriginWorldSpace = from.mOriginWorldSpace;
    }

}
