package com.tbocek.android.combatmap.view;

import android.content.Context;
import android.widget.ImageButton;

/**
 * A toggle button that switches between two images as a toggle representation.
 * 
 * @author Tim
 * 
 */
public final class TwoImageToggleButton extends ImageButton {

    /**
     * Resource ID of the image to use when the button is toggled.
     */
    private final int mToggledResourceId;

    /**
     * Resource ID of the image to use when the button is not toggled.
     */
    private final int mNotToggledResourceId;

    /**
     * Whether to draw the button toggled.
     */
    private boolean mToggled;


    /**
     * Constructor.
     * 
     * @param context
     *            Context to construct in.
     * @param notToggledResourceId
     *            ID of the resource to draw when the toggle state is false.
     * @param toggledResourceId
     *            ID of the resource to draw when the toggle state is true.
     */
    public TwoImageToggleButton(final Context context,
            final int notToggledResourceId, final int toggledResourceId) {
        super(context);
        this.mNotToggledResourceId = notToggledResourceId;
        this.mToggledResourceId = toggledResourceId;
        this.setImageResource(this.mNotToggledResourceId);
    }

    /**
     * @return Whether the button is toggled.
     */
    public boolean isToggled() {
        return this.mToggled;
    }

    /**
     * Sets the toggled state.
     * 
     * @param toggled
     *            Whether the button should be toggled.
     */
    public void setToggled(final boolean toggled) {
        this.mToggled = toggled;
        this.setImageResource(this.mToggled
                ? this.mToggledResourceId
                : this.mNotToggledResourceId);
    }

}
