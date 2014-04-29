package com.tbocek.android.combatmap.model.primitives;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
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
public class Information extends Text {

    /**
     * The size of an info point in world space.  Done so that we can adjust this size to make
     * this appear a constant size in screen space.
     */
    private static float sSizeWorldSpace = 1.0f;
    public static void setSizeWorldSpace(float size) {
        sSizeWorldSpace = size;
    }

    private static Bitmap sInfoBitmap;
    public static void loadInfoBitmap(Context c) {
        sInfoBitmap = BitmapFactory.decodeResource(c.getResources(), R.drawable.info);
    }

    public static final String SHAPE_TYPE = "inf";


    public Information() {
       this(new PointF(0,0), "");
    }

    public Information(PointF location, String text) {
        this.mText = text;
        this.mLocation = location;
        this.setBoundingRectangle(
          new PointF(mLocation.x, mLocation.y),
          new PointF(mLocation.x + 1, mLocation.y + 1));
    }



    /**
     * Copy constructor.
     *
     * @param copyFrom
     *            Text object to copy parameters from.
     */
    public Information(Information copyFrom) {
        this.mText = copyFrom.mText;
        this.mLocation = new PointF(copyFrom.mLocation.x, copyFrom.mLocation.y);
        this.setBoundingRectangle(
                new PointF(mLocation.x, mLocation.y),
                new PointF(mLocation.x + 1, mLocation.y + 1));
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
    protected Shape getMovedShape(float deltaX, float deltaY) {
        Information t = new Information(this);

        t.mLocation.x += deltaX;
        t.mLocation.y += deltaY;
        t.getBoundingRectangle().move(deltaX, deltaY);

        return t;
    }

    @Override
    public void draw(Canvas c) {
        if (sInfoBitmap != null) {
            c.drawBitmap(sInfoBitmap, new Rect(0,0,sInfoBitmap.getWidth(), sInfoBitmap.getHeight()), this.getBoundingRectangle().toRectF(), null);
        }
    }

    @Override
    public BoundingRectangle getBoundingRectangle() {
        if (this.mLocation == null) return new BoundingRectangle();
        return new BoundingRectangle(
                this.mLocation,
                new PointF(this.mLocation.x + sSizeWorldSpace, this.mLocation.y + sSizeWorldSpace));
    }
}
