package com.tbocek.android.combatmap.view;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.os.AsyncTask;
import android.view.View;

import com.tbocek.android.combatmap.model.primitives.BaseToken;

/**
 * This task loads custom token images on a separate thread. This allows faster
 * startup times for activities that need to load the entire token library.
 * 
 * @author Tim
 * @deprecated Use TokenLoadManager instead!
 * 
 */
@Deprecated
public class TokenLoadTask extends AsyncTask<Void, String, Void> {

    /**
     * Combat view in which these tokens are being drawn.
     */
    private CombatView mCombatView;

    /**
     * Map from token ID to the token button to show once that token ID loads.
     */
    private Map<String, TokenButton> mTokenButtonMap;

    /**
     * The list of token buttons that are being loaded.
     */
    private Collection<TokenButton> mTokenButtons;

    /**
     * Constructor.
     * 
     * @param buttons
     *            The list of token buttons to load.
     */
    public TokenLoadTask(Collection<TokenButton> buttons) {
        super();
        this.mTokenButtons = buttons;
    }

    /**
     * Constructor.
     * 
     * @param buttons
     *            The list of token buttons to load.
     * @param combatView
     *            CombatView to refresh when a token is loaded.
     */
    public TokenLoadTask(Collection<TokenButton> buttons, CombatView combatView) {
        super();
        this.mTokenButtons = buttons;
        this.mCombatView = combatView;
    }

    @Override
    protected Void doInBackground(Void... args) {
        for (TokenButton b : this.mTokenButtonMap.values()) {
            BaseToken t = b.getClone();
            if (t.needsLoad()) {
                t.load();
            }
            this.publishProgress(b.getTokenId());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (this.mCombatView != null) {
            this.mCombatView.refreshMap();
        }
    }

    @Override
    protected void onPreExecute() {
        this.mTokenButtonMap = new HashMap<String, TokenButton>();
        for (TokenButton b : this.mTokenButtons) {
            if (b.getClone().needsLoad()) {
                b.setVisibility(View.INVISIBLE);
                this.mTokenButtonMap.put(b.getTokenId(), b);
            }
        }
    }

    @Override
    protected void onProgressUpdate(String... progress) {
        String tokenId = progress[0];
        TokenButton b = this.mTokenButtonMap.get(tokenId);
        if (b != null) {
            b.setVisibility(View.VISIBLE);
            b.invalidate();
        }
    }

}
