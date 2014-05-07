package com.tbocek.android.combatmap.view;

import java.util.List;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

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

    private class Adapter extends TokenListAdapter {

        public Adapter(List<BaseToken> objects) {
            super(TokenSelectorView.this.getContext(), objects, mLoader);
        }

        @Override
        protected TokenButton createTokenButton() {
            TokenButton b = new TokenButton(getContext(), null);

            b.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
            b.setMinimumWidth((int) Util.convertDpToPixel(80, TokenSelectorView.this.getContext()));
            b.setMinimumHeight((int) Util.convertDpToPixel(80, TokenSelectorView.this.getContext()));
            b.allowDrag(false); // Will be handling dragging in the horizontal scroll view.

            return b;
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

        this.mTokenLayout.setOnItemSelectedListener(new HorizontalListView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                BaseToken token = (BaseToken) parent.getItemAtPosition(position);
                getOnTokenSelectedListener().onTokenSelected(token.clone());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        this.mTokenLayout.setOnItemLongClickListener(new HorizontalListView.OnItemLongClickListener(){

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                    ((TokenButton)view).onStartDrag();
                    return true;
                }
                return false;
            }
        });

    }

    public void setSelectedTag(String path, CombatView combatView) {
        TokenDatabase db = TokenDatabase.getInstanceOrNull();
        if (db == null) return;

        List<BaseToken> tokens = db.getTokensForTag(path);

        this.mTokenLayout.setAdapter(new Adapter(tokens));
    }
}
