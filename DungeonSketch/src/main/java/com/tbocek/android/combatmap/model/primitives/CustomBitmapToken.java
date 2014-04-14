package com.tbocek.android.combatmap.model.primitives;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.tbocek.android.combatmap.DataManager;

/**
 * A token type that is loaded from an external file and displays the image.
 * 
 * @author Tim Bocek
 * 
 */
public final class CustomBitmapToken extends DrawableToken {

    /**
     * The data manager that is used to load custom images.
     */
    private static transient DataManager dataManager = null;

    /**
     * The filename to load.
     */
    private String mFilename = null;

    /**
     * Sets the data manager that will be used to load images.
     * 
     * @param manager
     *            The data manager.
     */
    public static void registerDataManager(final DataManager manager) {
        CustomBitmapToken.dataManager = manager;
    }

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
