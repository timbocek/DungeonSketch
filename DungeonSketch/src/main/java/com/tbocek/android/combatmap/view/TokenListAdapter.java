package com.tbocek.android.combatmap.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.tbocek.android.combatmap.TokenImageManager;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.model.primitives.Util;

import java.util.List;

/**
 * Adapts a list of BaseTokens for use in a ListView or GridView.
 * This is an abstract class that requires subclasses to define how to actually create
 * the tokens.
 * Created by Tim on 4/18/2014.
 */

public abstract class TokenListAdapter extends ArrayAdapter<BaseToken> {

    private final TokenImageManager.Loader mLoader;

    public TokenListAdapter(Context context, List<BaseToken> objects,
            TokenImageManager.Loader imageLoader) {
        super(context, 0, objects);
        mLoader = imageLoader;
    }

    protected abstract TokenButton createTokenButton();

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final BaseToken prototype = this.getItem(position);
        TokenImageManager mgr = TokenImageManager.getInstance(getContext());
        if (convertView != null) {
            TokenButton oldTokenButton = ((TokenButton)convertView);
            if (oldTokenButton.loadedTokenImage()) {
                String oldTokenId = oldTokenButton.getTokenId();
                mgr.releaseTokenImage(oldTokenId);
            }
        }

        final TokenButton newTokenButton = (convertView!=null) ? (TokenButton)convertView : createTokenButton();
        newTokenButton.setPrototype(prototype);

        mgr.requireTokenImage(prototype.getTokenId(), mLoader, new TokenImageManager.Callback() {

            @Override
            public void imageLoaded(String tokenId) {
                newTokenButton.setLoadedTokenImage(true);
                newTokenButton.invalidate();
            }
        });

        return newTokenButton;
    }
}