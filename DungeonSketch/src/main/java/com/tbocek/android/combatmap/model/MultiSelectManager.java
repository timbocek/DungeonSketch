package com.tbocek.android.combatmap.model;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.model.primitives.BoundingRectangle;

/**
 * This class tracks a selection of multiple tokens in a way that each token
 * button can contribute to and make use of the selections.
 * 
 * @author Tim Bocek
 * 
 */
public final class MultiSelectManager {
    /**
     * The selected tokens. Maps original object hash code to token (the default
     * token implementation hashes on TokenID, which isn't good enough for us).
     */
    private Set<BaseToken> mSelection = Sets.newHashSet();

    /**
     * The callback to use when the selection is modified.
     */
    private SelectionChangedListener mSelectionChangedListener;

    /**
     * Adds a token to the current selection.
     * 
     * @param t
     *            The token to add. Should be a unique clone.
     */
    public void addToken(final BaseToken t) {
        if (this.mSelection.isEmpty() && this.mSelectionChangedListener != null) {
            this.mSelectionChangedListener.selectionStarted();
        }
        this.mSelection.add(t);
        t.setSelected(true);
        if (this.mSelectionChangedListener != null) {
            this.mSelectionChangedListener.selectionChanged();
        }
    }

    /**
     * Returns the selected tokens, in no particular order.
     * 
     * @return The tokens.
     */
    public Collection<BaseToken> getSelectedTokens() {
        return this.mSelection;
    }

    /**
     * Removes the token with the given ID from the current selection.
     * 
     * @param token
     *            The token to remove.
     */
    public void removeToken(final BaseToken token) {
        token.setSelected(false);
        this.mSelection.remove(token);
        if (this.mSelectionChangedListener != null) {
            this.mSelectionChangedListener.selectionChanged();
            if (this.mSelection.isEmpty()) {
                this.mSelectionChangedListener.selectionEnded();
            }
        }
    }

    /**
     * Clears the selection.
     */
    public void selectNone() {
        for (BaseToken t : this.mSelection) {
            t.setSelected(false);
        }
        this.mSelection.clear();
        if (this.mSelectionChangedListener != null) {
            this.mSelectionChangedListener.selectionEnded();
        }
    }

    /**
     * Changes the callback to use when the selection changes.
     * 
     * @param listener
     *            The new listener.
     */
    public void setSelectionChangedListener(SelectionChangedListener listener) {
        this.mSelectionChangedListener = listener;
    }

    /**
     * Toggles the selected state of the given token.
     * 
     * @param token
     *            The token to toggle.
     */
    public void toggleToken(BaseToken token) {
        if (token.isSelected()) {
            this.removeToken(token);
        } else {
            this.addToken(token);
        }
    }

    /**
     * Callback to define actions to take when a selection has changed.
     * 
     * @author Tim
     * 
     */
    public interface SelectionChangedListener {

        /**
         * Called when a single token is added or removed from the collection.
         */
        void selectionChanged();

        /**
         * Called when a selection is cleared.
         */
        void selectionEnded();

        /**
         * Called when a new selection is started.
         */
        void selectionStarted();
    }

	public boolean isActive() {
		return this.getSelectedTokens().size() != 0;
	}
	
	public BoundingRectangle getSelectionBoundingRect() {
		BoundingRectangle rect = new BoundingRectangle();
		for (BaseToken t: this.mSelection){
			rect.updateBounds(t.getBoundingRectangle());
		}
		return rect;
	}
}
