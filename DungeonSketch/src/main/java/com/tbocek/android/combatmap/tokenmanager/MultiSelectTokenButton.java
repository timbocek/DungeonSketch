package com.tbocek.android.combatmap.tokenmanager;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.View;

import com.tbocek.android.combatmap.model.MultiSelectManager;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.view.TokenButton;

import java.util.ArrayList;

/**
 * Extends the behavior of TokenButton to allow multiple tokens to be selected
 * at once. Dragging from this button will drag a list of tokens, not a single
 * token.
 * 
 * @author Tim Bocek
 * 
 */
public final class MultiSelectTokenButton extends TokenButton {

    /**
     * The manager that tracks which tokens are selected across a group of
     * MultiSelectTokenButton instances.
     */
    private final MultiSelectManager mMultiSelect;

    /**
     * Whether this token is currently selected.
     */
    private boolean mSelected;

    /**
     * Constructor.
     * 
     * @param context
     *            Context to create this button in.
     * @param token
     *            The token represented by this button.
     * @param multiSelect
     *            The manager that tracks a group of selected tokens.
     */
    public MultiSelectTokenButton(final Context context, final BaseToken token,
            final MultiSelectManager multiSelect) {
        super(context, token);

        this.mMultiSelect = multiSelect;

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View arg0) {
                MultiSelectTokenButton.this.mSelected =
                        !MultiSelectTokenButton.this.mSelected;
                if (MultiSelectTokenButton.this.mSelected) {
                    MultiSelectTokenButton.this.mMultiSelect
                            .addToken(MultiSelectTokenButton.this
                                    .getPrototype());
                } else {
                    MultiSelectTokenButton.this.mMultiSelect
                            .removeToken(MultiSelectTokenButton.this
                                    .getPrototype());
                }
                MultiSelectTokenButton.this.invalidate();
            }
        });
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void onStartDrag() {
        // Add this token to the selection, so we are at least dragging it.
        ArrayList<BaseToken> tokens =
                new ArrayList<BaseToken>(this.mMultiSelect.getSelectedTokens());
        if (!this.mSelected) {
            tokens.add(0, this.getPrototype());
        }
        this.startDrag(null,
                new TokenStackDragShadow(tokens, (int) this.getTokenRadius()),
                tokens, 0);
    }

    /**
     * Sets whether the button is selected. This maintains consistent state, so
     * if a token is selected using this method it will be added to the
     * collection of selected tokens.
     * 
     * @param selected
     *            Whether the token should be selected.
     */
    @Override
    public void setSelected(boolean selected) {
        boolean oldSelected = this.mSelected;
        this.mSelected = selected;
        if (!oldSelected && selected) {
            this.mMultiSelect.addToken(this.getPrototype());
        } else if (oldSelected && !selected) {
            this.mMultiSelect.removeToken(this.getPrototype());
        }

        if (oldSelected != this.mSelected) {
            this.invalidate();
        }
    }
}
