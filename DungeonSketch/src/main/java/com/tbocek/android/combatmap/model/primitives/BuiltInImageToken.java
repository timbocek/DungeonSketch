package com.tbocek.android.combatmap.model.primitives;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.tbocek.android.combatmap.DungeonSketchApp;

/**
 * Creates a token for one of the built-in images.
 * 
 * @author Tim Bocek
 * 
 */
public final class BuiltInImageToken extends DrawableToken {
    /**
     * Format string that pads the sort order with 0s.
     */
    private static final DecimalFormat SORT_ORDER_FORMAT = new DecimalFormat(
            "#0000.###");

    /**
     * Tags loaded for this built-in token.
     */
    private Set<String> mDefaultTags;

    /**
     * The name of the resource to load for this token.
     */
    private String mResourceName;

    /**
     * Relative order to sort this token in.
     */
    private int mSortOrder;

    /**
     * Constructor from resource ID.
     * 
     * @param resourceName
     *            The resource to load for this token.
     * @param sortOrder
     *            Integer that will be used to specify a sort order for this
     *            class.
     * @param defaultTags
     *            Set of tags that this built in token loads with.
     */
    public BuiltInImageToken(final String resourceName, final int sortOrder,
            final Set<String> defaultTags) {
        this.mResourceName = resourceName;
        this.mSortOrder = sortOrder;
        this.mDefaultTags = defaultTags;
    }

    @Override
    public BaseToken clone() {
        return this.copyAttributesTo(new BuiltInImageToken(this.mResourceName,
                this.mSortOrder, this.mDefaultTags));
    }

    @Override
    public Bitmap loadBitmap() {
        int id =
                DungeonSketchApp.getContext().getResources().getIdentifier(
                        this.mResourceName, "drawable",
                        DungeonSketchApp.getContext().getPackageName());
        if (id == 0) {
            return null;
        }
        return ((BitmapDrawable)DungeonSketchApp.getContext().getResources().getDrawable(id)).getBitmap();
    }

    @Override
    protected Drawable createDrawable() {
        int id =
                DungeonSketchApp.getContext().getResources().getIdentifier(
                        this.mResourceName, "drawable",
                        DungeonSketchApp.getContext().getPackageName());
        if (id == 0) {
            return null;
        }
        return DungeonSketchApp.getContext().getResources().getDrawable(id);
    }

    @Override
    public Set<String> getDefaultTags() {
        Set<String> s = new HashSet<String>();
        s.add("built-in");
        s.add("image");
        s.addAll(this.mDefaultTags);
        return s;
    }

    @Override
    protected String getTokenClassSpecificId() {
        return this.mResourceName;
    }

    @Override
    protected String getTokenClassSpecificSortOrder() {
        SORT_ORDER_FORMAT.setDecimalSeparatorAlwaysShown(false);
        return SORT_ORDER_FORMAT.format(this.mSortOrder);

    }
}
