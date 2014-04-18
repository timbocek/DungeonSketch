package com.tbocek.android.combatmap.view;

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.tbocek.android.combatmap.TokenImageManager;
import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.android.combatmap.model.primitives.BaseToken;

/**
 * Provides a horizontally scrolling list of tokens, allowing the user to pick
 * one.
 * 
 * @author Tim Bocek
 * 
 */
public final class TokenSelectorView extends ListView {
    private static final int TOKEN_HEIGHT = 48;


    private OnTokenSelectedListener mOnTokenSelectedListener;
    private OnClickListener mOnClickTokenManagerListener;
    private OnClickListener mOnClickGroupSelectorListener;
    private boolean mShouldDrawDark;

    public void setOnTokenSelectedListener(OnTokenSelectedListener onTokenSelectedListener) {
        mOnTokenSelectedListener = onTokenSelectedListener;
    }

    public OnTokenSelectedListener getOnTokenSelectedListener() {
        return mOnTokenSelectedListener;
    }

    public void setOnClickTokenManagerListener(OnClickListener onClickTokenManagerListener) {
        mOnClickTokenManagerListener = onClickTokenManagerListener;
    }

    public OnClickListener getOnClickTokenManagerListener() {
        return mOnClickTokenManagerListener;
    }

    public void setOnClickGroupSelectorListener(OnClickListener onClickGroupSelectorListener) {
        mOnClickGroupSelectorListener = onClickGroupSelectorListener;
    }

    public OnClickListener getOnClickGroupSelectorListener() {
        return mOnClickGroupSelectorListener;
    }

    public void setShouldDrawDark(boolean shouldDrawDark) {
        mShouldDrawDark = shouldDrawDark;
    }

    public boolean isShouldDrawDark() {
        return mShouldDrawDark;
    }

    public void setLoader(TokenImageManager.Loader loader) {
        mLoader = loader;
    }

    public interface OnTokenSelectedListener {
        void onTokenSelected(final BaseToken t);
    }

    private class TokenListArrayAdapter extends ArrayAdapter<BaseToken> {

        public TokenListArrayAdapter(List<BaseToken> objects) {
            super(TokenSelectorView.this.getContext(), 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BaseToken prototype = this.getItem(position);
            TokenImageManager mgr = TokenImageManager.getInstance(getContext());
            if (convertView != null) {
                TokenButton oldTokenButton = ((TokenButton)convertView);
                if (oldTokenButton.loadedTokenImage()) {
                    String oldTokenId = oldTokenButton.getTokenId();
                    mgr.releaseTokenImage(oldTokenId);
                }
            }

            final TokenButton newTokenButton = (convertView!=null) ? (TokenButton)convertView : new TokenButton(getContext(), null);
            newTokenButton.setPrototype(prototype);

            mgr.requireTokenImage(prototype, mLoader, new TokenImageManager.Callback() {

                @Override
                public void imageLoaded(BaseToken token) {
                    newTokenButton.setLoadedTokenImage(true);
                    newTokenButton.invalidate();
                }
            });

            // TODO: dp.
            newTokenButton.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 48));

            return newTokenButton;
        }
    }

    private TokenImageManager.Loader mLoader;

    public TokenSelectorView(Context context) {
        this(context, null);
    }

    public TokenSelectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSelectedTag(String path, CombatView combatView) {
        TokenDatabase db = TokenDatabase.getInstanceOrNull();
        if (db == null) return;

        List<BaseToken> tokens = db.getTokensForTag(path);

        this.setAdapter(new TokenListArrayAdapter(tokens));
    }
}
