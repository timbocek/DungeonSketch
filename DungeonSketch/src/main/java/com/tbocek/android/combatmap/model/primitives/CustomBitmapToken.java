package com.tbocek.android.combatmap.model.primitives;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.tbocek.android.combatmap.DataManager;

import java.io.IOException;
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
        try {
            return dataManager.loadTokenImage(this.mFilename, existingBuffer);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Drawable createDrawable() {
        if (dataManager == null) {
            return null;
        }

        Bitmap b;
        try {
            b = dataManager.loadTokenImage(this.mFilename);
            return new BitmapDrawable(b);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
    public boolean maybeDeletePermanently() throws IOException {
        dataManager.deleteTokenImage(this.mFilename);
        return true;
    }

}
