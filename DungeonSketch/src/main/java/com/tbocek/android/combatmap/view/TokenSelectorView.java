package com.tbocek.android.combatmap.view;

import java.util.List;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.tbocek.android.combatmap.TokenImageManager;
import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.model.primitives.Util;
import com.tbocek.dungeonsketch.R;

/**
 * Provides a horizontally scrolling list of tokens, allowing the user to pick
 * one.
 * 
 * @author Tim Bocek
 * 
 */
public final class TokenSelectorView extends LinearLayout {
    private static final int TOKEN_HEIGHT = 48;
    private final HorizontalListView mTokenLayout;
    private Button mGroupSelector;


    private OnTokenSelectedListener mOnTokenSelectedListener;
    private boolean mShouldDrawDark;

    public void setOnTokenSelectedListener(OnTokenSelectedListener onTokenSelectedListener) {
        mOnTokenSelectedListener = onTokenSelectedListener;
    }

    public OnTokenSelectedListener getOnTokenSelectedListener() {
        return mOnTokenSelectedListener;
    }

    public void setOnClickGroupSelectorListener(OnClickListener onClickGroupSelectorListener) {
        mGroupSelector.setOnClickListener(onClickGroupSelectorListener);
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
            final BaseToken prototype = this.getItem(position);
            TokenImageManager mgr = TokenImageManager.getInstance(getContext());
            if (convertView != null) {
                TokenButton oldTokenButton = ((TokenButton)convertView);
                if (oldTokenButton.loadedTokenImage()) {
                    String oldTokenId = oldTokenButton.getTokenId();
                    mgr.releaseTokenImage(oldTokenId);
                }
            }

            final TokenButton newTokenButton = (convertView!=null) ? (TokenButton)convertView : new TokenButton(getContext(), prototype);
            newTokenButton.setPrototype(prototype);

            newTokenButton.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
            newTokenButton.setMinimumWidth((int) Util.convertDpToPixel(80, TokenSelectorView.this.getContext()));
            newTokenButton.setMinimumHeight((int) Util.convertDpToPixel(80, TokenSelectorView.this.getContext()));

            mgr.requireTokenImage(prototype, mLoader, new TokenImageManager.Callback() {

                @Override
                public void imageLoaded(BaseToken token) {
                    newTokenButton.setLoadedTokenImage(true);
                    newTokenButton.invalidate();
                }
            });

            newTokenButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnTokenSelectedListener != null) {
                        mOnTokenSelectedListener.onTokenSelected(prototype.clone());
                    }
                }
            });

            return newTokenButton;
        }
    }

    private TokenImageManager.Loader mLoader;

    public TokenSelectorView(Context context) {
        this(context, null);
    }

    public TokenSelectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.token_selector, this);
        // Create and add the child layout, which will be a linear layout of
        // tokens.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        this.mTokenLayout = (HorizontalListView)findViewById(R.id.token_selector_list_view);

        this.mGroupSelector =
                (Button) this.findViewById(R.id.token_category_selector_button);

    }

    public void setSelectedTag(String path, CombatView combatView) {
        TokenDatabase db = TokenDatabase.getInstanceOrNull();
        if (db == null) return;

        List<BaseToken> tokens = db.getTokensForTag(path);

        this.mTokenLayout.setAdapter(new TokenListArrayAdapter(tokens));
        ((TokenListArrayAdapter)this.mTokenLayout.getAdapter()).notifyDataSetChanged();
    }
}
