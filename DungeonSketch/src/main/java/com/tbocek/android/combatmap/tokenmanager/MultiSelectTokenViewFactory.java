package com.tbocek.android.combatmap.tokenmanager;

import android.content.Context;

import com.tbocek.android.combatmap.model.MultiSelectManager;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.view.TokenButton;
import com.tbocek.android.combatmap.view.TokenViewFactory;

/**
 * Subclasses the TokenViewFactory to override the kind of TokenButton created.
 * This creates TokenButtons that allow easy multiple selection of tokens.
 * 
 * @author Tim Bocek
 * 
 */
public final class MultiSelectTokenViewFactory extends TokenViewFactory {
    /**
     * Shared object for storing the tokens that are currently selected, so that
     * each token button can use that information.
     */
    private MultiSelectManager mMultiSelect = new MultiSelectManager();

    /**
     * Constructor.
     * 
     * @param context
     *            The context to use when creating tokens.
     */
    public MultiSelectTokenViewFactory(final Context context) {
        super(context);
    }

    /**
     * 
     * @return The multi-select manager used by token views created by this
     *         view.
     */
    public MultiSelectManager getMultiSelectManager() {
        return this.mMultiSelect;
    }

    @Override
    public TokenButton getTokenView(final BaseToken prototype) {
        MultiSelectTokenButton mstb =
                (MultiSelectTokenButton) super.getTokenView(prototype);
        mstb.refreshSelectedState();
        return mstb;
    }

    @Override
    protected TokenButton newTokenView(final BaseToken prototype) {
        MultiSelectTokenButton b =
                new MultiSelectTokenButton(this.getContext(), prototype,
                        this.mMultiSelect);
        b.refreshSelectedState();
        return b;
    }

}
