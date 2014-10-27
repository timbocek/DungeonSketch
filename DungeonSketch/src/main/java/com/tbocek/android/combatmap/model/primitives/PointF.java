package com.tbocek.android.combatmap.model.primitives;

import java.io.IOException;
import java.io.Serializable;

/**
 * Serializable version of Android's PointF.
 * 
 * @author Tim Bocek
 * 
 */
public class PointF extends android.graphics.PointF implements Serializable {

    /**
     * ID for serialization.
     */
    private static final long serialVersionUID = -8072053114782117778L;

    /**
     * Default constructor.
     */
    public PointF() {
        super();
    }

    /**
     * Constructor from coordinates.
     * 
     * @param x
     *            X coordinate.
     * @param y
     *            Y coordinate.
     */
    public PointF(final float x, final float y) {
        super(x, y);
    }

    /**
     * Deserializes the PointF.
     * 
     * @param in
     *            Stream to deserialize from.
     * @throws IOException
     *             On input error.
     */
    private void readObject(final java.io.ObjectInputStream in)
            throws IOException {
        this.x = in.readFloat();
        this.y = in.readFloat();
    }

    /**
     * Serializes the PointF.
     * 
     * @param out
     *            Stream to serialize to.
     * @throws IOException
     *             On output error.
     */
    private void writeObject(final java.io.ObjectOutputStream out)
            throws IOException {
        out.writeFloat(this.x);
        out.writeFloat(this.y);
    }
}
