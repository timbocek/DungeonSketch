package com.tbocek.android.combatmap.model.primitives;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;

import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;
import com.tbocek.dungeonsketch.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This primitive allows the GM to place more informative text on the map.  Unlike the "text"
 * primitive, this will display only a marker to signify the presence of the text.  Clicking on
 * the marker will allow entry in a more free-form field.
 *
 * This is intended to be able to store and quickly recall large blocks of text, e.g. area
 * descriptions, monster stats, etc.
 * Created by Tim on 4/22/2014.
 */
public class Information extends Shape {

    private static Bitmap sInfoBitmap;
    public static void loadInfoBitmap(Context c) {
        sInfoBitmap = BitmapFactory.decodeResource(c.getResources(), R.drawable.info);
    }

    public static final String SHAPE_TYPE = "inf";
    /**
     * Whether this text object has a pending erase operation.
     */
    protected boolean mErased;
    /**
     * Location of the lower left hand corner of the text.
     */
    protected PointF mLocation;
    /**
     * Contents of the text field.
     */
    protected String mText;

    public Information() {
       this(new PointF(0,0), "");
    }

    public Information(PointF location, String text) {
        this.getBoundingRectangle().updateBounds(location);
        this.getBoundingRectangle().updateBounds(new PointF(
                location.x + 1, location.y + 1));
        this.mText = text;
    }

    /**
     * Copy constructor.
     *
     * @param copyFrom
     *            Text object to copy parameters from.
     */
    public Information(Information copyFrom) {
        this.mText = copyFrom.mText;
        this.getBoundingRectangle().clear();
        this.getBoundingRectangle().updateBounds(
                copyFrom.getBoundingRectangle());
        this.mLocation = new PointF(copyFrom.mLocation.x, copyFrom.mLocation.y);
    }


    @Override
    public void serialize(MapDataSerializer s) throws IOException {
        this.serializeBase(s, SHAPE_TYPE);

        s.startObject();
        s.serializeString(this.mText);
        s.serializeFloat(this.mLocation.x);
        s.serializeFloat(this.mLocation.y);
        s.endObject();
    }

    @Override
    protected void shapeSpecificDeserialize(MapDataDeserializer s)
            throws IOException {
        s.expectObjectStart();
        this.mText = s.readString();
        this.mLocation = new PointF();
        this.mLocation.x = s.readFloat();
        this.mLocation.y = s.readFloat();
        s.expectObjectEnd();
    }

    @Override
    public void addPoint(PointF p) {
        throw new RuntimeException("Adding point to text not supported.");
    }

    @Override
    public boolean contains(PointF p) {
        return this.getBoundingRectangle().contains(p);
    }

    @Override
    protected Path createPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void erase(PointF center, float radius) {
        if (this.getBoundingRectangle().intersectsWithCircle(center, radius)) {
            this.mErased = true;
        }
    }

    /**
     * @return The location of the lower left hand corner of the text.
     */
    public PointF getLocation() {
        return this.mLocation;
    }


    @Override
    public boolean needsOptimization() {
        // TODO Auto-generated method stub
        return this.mErased;
    }

    @Override
    public List<Shape> removeErasedPoints() {
        List<Shape> ret = new ArrayList<Shape>();
        if (!this.mErased) {
            ret.add(this);
        } else {
            this.mErased = false;
        }
        return ret;
    }

    @Override
    public boolean shouldDrawBelowGrid() {
        return false; // Text should never draw below the grid.
    }


    @Override
    protected Shape getMovedShape(float deltaX, float deltaY) {
        Information t = new Information(this);

        t.mLocation.x += deltaX;
        t.mLocation.y += deltaY;
        t.getBoundingRectangle().move(deltaX, deltaY);

        return t;
    }

    /**
     * @return The contents of the text object.
     */
    public String getText() {
        return this.mText;
    }

    @Override
    public void draw(Canvas c) {
        if (sInfoBitmap != null) {
            c.drawBitmap(sInfoBitmap, new Rect(0,0,sInfoBitmap.getWidth(), sInfoBitmap.getHeight()),
                    new Rect((int)this.getBoundingRectangle().getXMin(), (int)this.getBoundingRectangle().getYMin(),
                             (int)this.getBoundingRectangle().getXMax(), (int)this.getBoundingRectangle().getYMax()), null);
        }
    }
}
