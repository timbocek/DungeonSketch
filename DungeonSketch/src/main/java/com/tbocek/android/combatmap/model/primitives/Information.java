package com.tbocek.android.combatmap.model.primitives;

import android.graphics.Path;

import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;

import java.io.IOException;
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

    @Override
    public void addPoint(PointF p) {
        throw new RuntimeException("Adding point to text not supported.");
    }

    @Override
    public boolean contains(PointF p) {
        return false;
    }

    @Override
    protected Path createPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void erase(PointF center, float radius) {

    }

    @Override
    public boolean needsOptimization() {
        return false;
    }

    @Override
    public List<Shape> removeErasedPoints() {
        return null;
    }

    @Override
    public void serialize(MapDataSerializer s) throws IOException {

    }

    @Override
    protected void shapeSpecificDeserialize(MapDataDeserializer s) throws IOException {

    }
}
