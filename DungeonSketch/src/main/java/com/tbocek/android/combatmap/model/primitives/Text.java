package com.tbocek.android.combatmap.model.primitives;

import android.graphics.Path;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class representing a Shape that stores text at a certain location.
 * Created by Tim on 4/27/2014.
 */
public abstract class Text extends Shape {
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

    public Text() {
        this(new PointF(0,0), "");
    }

    public Text(PointF location, String text) {
        super();
        this.getBoundingRectangle().updateBounds(location);
        this.mText = text;
        this.mLocation = location;
    }


    /**
     * Copy constructor.
     *
     * @param copyFrom
     *            Text object to copy parameters from.
     */
    public Text(Text copyFrom) {
        this.mText = copyFrom.mText;
        this.mLocation = new PointF(copyFrom.mLocation.x, copyFrom.mLocation.y);
        this.getBoundingRectangle().updateBounds(copyFrom.getBoundingRectangle());
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

    /**
     * @return The contents of the text object.
     */
    public String getText() {
        return this.mText;
    }

    public PointF getLocation() {
        return mLocation;
    }

    public void setLocation(PointF location) {
        mLocation = location;
    }
}
