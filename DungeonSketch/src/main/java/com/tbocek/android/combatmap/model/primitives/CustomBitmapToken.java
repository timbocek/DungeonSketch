package com.tbocek.android.combatmap.model.primitives;

import android.graphics.Bitmap;

import java.util.HashSet;
import java.util.Set;

/**
 * A token type that is loaded from an external file and displays the image.
 * 
 * @author Tim Bocek
 * 
 */
public final class CustomBitmapToken extends DrawableToken {


    /**
     * The filename to load.
     */
    private String mFilename = null;

    /**
     * Constructor.
     * 
     * @param filename
     *            The filename to load.
     */
    public CustomBitmapToken(final String filename) {
        this.mFilename = filename;
    }

    @Override
    public BaseToken clone() {
        return this.copyAttributesTo(new CustomBitmapToken(this.mFilename));
    }

    @Override
    public Bitmap loadBitmap(Bitmap existingBuffer) {
        return dataManager.loadTokenImage(this.mFilename, existingBuffer, LOAD_DIM_DP, LOAD_DIM_DP);
    }

    @Override
    public Set<String> getDefaultTags() {
        Set<String> s = new HashSet<String>();
        s.add("custom");
        s.add("image");
        return s;
    }

    @Override
    protected String getTokenClassSpecificId() {
        return this.mFilename;
    }

    @Override
    public boolean isBuiltIn() {
        return false;
    }

    @Override
    public boolean maybeDeletePermanently() {
        dataManager.deleteTokenImage(this.mFilename);
        return true;
    }

}
