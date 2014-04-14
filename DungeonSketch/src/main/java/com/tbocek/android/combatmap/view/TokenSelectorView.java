package com.tbocek.android.combatmap.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.tbocek.android.combatmap.R;
import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.android.combatmap.TokenLoadManager;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.tokenmanager.TokenStackDragShadow;

/**
 * Provides a horizontally scrolling list of tokens, allowing the user to pick
 * one.
 * 
 * @author Tim Bocek
 * 
 */
public final class TokenSelectorView extends LinearLayout {

    /**
     * Percentage to scale the radius.
     */
    private static final float RADIUS_SCALE = .9f;

    /**
     * Number of tokens each block should contain. Since these blocks are
     * presented side by side, this should have no user-visible effect. It is
     * only a performance tweak (e.g. more tokens per row means fewer image
     * allocations but longer load times per block of tokens.
     */
    private static final int TOKENS_PER_BLOCK = 50;

    /**
     * Whether this control is being superimposed over a dark background.
     */
    private boolean mDrawDark;

    /**
     * Button that represents opening the tag selector.
     */
    private Button mGroupSelector;

    /**
     * Whether this token selector has changed absolute position. We will poll
     * every fraction of a second.
     */
    private boolean mIsPositionDirty;

    /**
     * Listener that fires when a token is selected.
     */
    private OnTokenSelectedListener mOnTokenSelectedListener;

    /**
     * Database to load tokens from.
     */
    private TokenDatabase mTokenDatabase;

    /**
     * The inner layout that contains the tokens.
     */
    private LinearLayout mTokenLayout;

    /**
     * Button that represents opening the token manager.
     */
    private ImageButton mTokenManager;

    /**
     * The current list of tokens. Must correspond to the order they appear in
     * mBitmap.
     */
    private List<BaseToken> mTokens;

    /**
     * Constructor.
     * 
     * @param context
     *            The context to create this view in.
     */
    @SuppressLint("NewApi")
    public TokenSelectorView(final Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.token_selector, this);
        // Create and add the child layout, which will be a linear layout of
        // tokens.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        this.mTokenLayout = new LinearLayout(context);
        ((HorizontalScrollView) this.findViewById(R.id.token_scroll_view))
                .addView(this.mTokenLayout);

        this.mGroupSelector =
                (Button) this.findViewById(R.id.token_category_selector_button);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.mGroupSelector.setAlpha(1.0f);
            this.mTokenManager =
                    (ImageButton) this.findViewById(R.id.token_manager_button);
            this.mTokenManager.setAlpha(1.0f);
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (h != oldh && this.mTokens != null) {
            new CreateImageTask(h, this.mDrawDark).execute(this.mTokens);
        }
    }

    /**
     * Sets an OnClickListener for the tag selector button.
     * 
     * @param listener
     *            The listener to use when the tag selector is clicked.
     */
    public void setOnClickGroupSelectorListener(
            final View.OnClickListener listener) {
        this.mGroupSelector.setOnClickListener(listener);
    }

    /**
     * Sets an OnClickLIstener for the token manager button.
     * 
     * @param listener
     *            The listener to use when the token manager button is clicked.
     */
    public void setOnClickTokenManagerListener(
            final View.OnClickListener listener) {
        this.mTokenManager.setOnClickListener(listener);
    }

    /**
     * Sets the listener to fire when a token is selected.
     * 
     * @param onTokenSelectedListener
     *            The listener.
     */
    public void setOnTokenSelectedListener(
            final OnTokenSelectedListener onTokenSelectedListener) {
        this.mOnTokenSelectedListener = onTokenSelectedListener;
    }

    /**
     * Sets the tag currently being displayed.
     * 
     * @param checkedTag
     *            The tag currently being displayed.
     * @param combatView
     *            The CombatView to refresh when tokens are loaded.
     */
    public void setSelectedTag(final String checkedTag,
            final CombatView combatView) {
        this.setTokenList(this.mTokenDatabase.getTokensForTag(checkedTag));
    }

    /**
     * @param drawDark
     *            Whether tokens are drawn on a dark background.
     */
    public void setShouldDrawDark(boolean drawDark) {
        this.mDrawDark = drawDark;
        this.mGroupSelector.setTextColor(drawDark ? Color.WHITE : Color.BLACK);
    }

    /**
     * Sets the token database to query for tokens.
     * 
     * @param database
     *            The token database.
     */
    public void setTokenDatabase(final TokenDatabase database) {
        this.mTokenDatabase = database;
        this.setTokenList(this.mTokenDatabase.getAllTokens());
    }

    /**
     * Sets the list of tokens displayed to the given collection.
     * 
     * @param tokens
     *            Tokens to offer in the selector.
     */
    @SuppressWarnings("unchecked")
    private void setTokenList(final Collection<BaseToken> tokens) {
        if (tokens instanceof List<?>) {
            this.mTokens = (List<BaseToken>) tokens;
        } else {
            this.mTokens = new ArrayList<BaseToken>(tokens);
        }
        // TODO: Make this work if height is then changed.
        new CreateImageTask(this.getHeight(), this.mDrawDark)
                .execute(this.mTokens);
    }

    /**
     * @return Whether tokens are drawn on a dark background.
     */
    public boolean shouldDrawDark() {
        return this.mDrawDark;
    }

    /**
     * Async task to create the image used.
     * 
     * @author Tim
     * 
     */
    private class CreateImageTask extends
            AsyncTask<List<BaseToken>, Integer, List<TokenImageResult>> {

        /**
         * Whether drawn on a dark background.
         */
        private boolean mDrawDark;

        /**
         * Height of the view.
         */
        private int mHeight;

        /**
         * Progress bar to show when loading.
         */
        private ProgressBar mProgressBar;

        /**
         * Constructor.
         * 
         * @param height
         *            Height of the control (width will be determined by the
         *            number of tokens loaded).
         * @param drawDark
         *            Whether there is a dark background.
         */
        public CreateImageTask(int height, boolean drawDark) {
            this.mHeight = height;
            this.mDrawDark = drawDark;
        }

        private Bitmap createBitmapFromTokens(List<BaseToken> tokens) {
            Bitmap b =
                    this.mHeight > 0 ? Bitmap.createBitmap(this.mHeight
                            * tokens.size(), this.mHeight,
                            Bitmap.Config.ARGB_4444) : null;

            int drawY = this.mHeight / 2;
            int drawX = this.mHeight / 2;

            if (b != null) {
                b.eraseColor(Color.TRANSPARENT);
            }

            Canvas c = b != null ? new Canvas(b) : null;
            /*
             * int i = 0; for (BaseToken t: tokens) { t.load(); if (b != null) {
             * t.draw(c, drawX, drawY, RADIUS_SCALE * mHeight / 2, mDrawDark,
             * true); drawX += mHeight; } i += 1; publishProgress(i,
             * tokens.size()); }
             */
            return b;
        }

        @Override
        protected List<TokenImageResult>
                doInBackground(List<BaseToken>... args) {
            List<BaseToken> tokens = args[0];
            List<TokenImageResult> res = Lists.newArrayList();

            for (List<BaseToken> partition : this.partitionTokens(tokens,
                    TOKENS_PER_BLOCK)) {
                res.add(new TokenImageResult(this
                        .createBitmapFromTokens(partition), partition));
            }

            return res;
        }

        @Override
        protected void onPostExecute(List<TokenImageResult> results) {
            TokenSelectorView.this.mTokenLayout.removeAllViews();
            for (TokenImageResult r : results) {
                TokenSelectorViewRow v =
                        new TokenSelectorViewRow(
                                TokenSelectorView.this.getContext(), r.bitmap,
                                r.tokenList);
                v.startLoadingTokenImages();
                TokenSelectorView.this.mTokenLayout.addView(v);
            }
        }

        @Override
        protected void onPreExecute() {
            TokenSelectorView.this.mTokenLayout.removeAllViews();

            TextView explanation =
                    new TextView(TokenSelectorView.this.getContext());
            explanation.setText("Loading Tokens");
            TokenSelectorView.this.mTokenLayout.addView(explanation);

            this.mProgressBar =
                    new ProgressBar(TokenSelectorView.this.getContext());
            TokenSelectorView.this.mTokenLayout.addView(this.mProgressBar);
        }

        @Override
        protected void onProgressUpdate(Integer... args) {
            int progress = args[0];
            int max = args[1];

            this.mProgressBar.setMax(max);
            this.mProgressBar.setProgress(progress);
        }

        /**
         * Divide the given list of tokens into sublists of no greater than the
         * given size.
         * 
         * @param tokens
         *            The list of tokens to partition.
         * @param maxSize
         *            The maximum size of each sublist.
         * @return A list of lists of tokens, each sublist of which contains no
         *         more than maxSize tokens.
         */
        private List<List<BaseToken>> partitionTokens(List<BaseToken> tokens,
                int maxSize) {
            List<List<BaseToken>> ret = Lists.newArrayList();

            for (int i = 0; i < tokens.size(); i += maxSize) {
                ret.add(tokens.subList(i, Math.min(tokens.size(), i + maxSize)));
            }

            return ret;
        }
    }

    /**
     * Listener that fires when a token is selected.
     * 
     * @author Tim Bocek
     * 
     */
    public interface OnTokenSelectedListener {
        /**
         * 
         * @param t
         *            The selected token. This is already a unique clone.
         */
        void onTokenSelected(BaseToken t);
    }

    private class TokenImageResult {
        public Bitmap bitmap;

        public List<BaseToken> tokenList;

        public TokenImageResult(Bitmap b, List<BaseToken> tokens) {
            this.bitmap = b;
            this.tokenList = tokens;
        }
    }

    private class TokenSelectorViewRow extends ImageView {
        Bitmap mBitmap;

        /**
         * Gesture detector for tapping or long pressing the list of tokens.
         */
        private GestureDetector mGestureDetector;
        private List<BaseToken> mTokens;

        public TokenSelectorViewRow(Context context, Bitmap tokenRowImage,
                List<BaseToken> tokens) {
            super(context);
            this.setImageBitmap(tokenRowImage);
            this.mBitmap = tokenRowImage;
            this.mTokens = tokens;
            this.mGestureDetector =
                    new GestureDetector(this.getContext(),
                            new TouchTokenListener());
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            this.mGestureDetector.onTouchEvent(event);
            return true;
        }

        public void startLoadingTokenImages() {
            // Load all the tokens that are currently placed on the map.
            if (this.mBitmap == null) {
                return;
            }

            TokenLoadManager.getInstance().startJob(this.mTokens,
                    new TokenLoadManager.JobCallback() {
                        @Override
                        public void onJobComplete(List<BaseToken> loadedTokens) {
                            Canvas c =
                                    new Canvas(
                                            TokenSelectorViewRow.this.mBitmap);

                            int radius =
                                    TokenSelectorViewRow.this.getHeight() / 2;
                            int drawX = radius;

                            int i = 0;
                            for (BaseToken t : loadedTokens) {
                                t.load();
                                t.draw(c, drawX, radius, RADIUS_SCALE * radius,
                                        TokenSelectorView.this.mDrawDark, true);
                                drawX += radius * 2;
                                i += 1;
                            }
                            TokenSelectorViewRow.this
                                    .setImageBitmap(TokenSelectorViewRow.this.mBitmap);
                        }
                    }, new Handler());
        }

        /**
         * Gesture listener for tapping and long pressing the token list.
         * 
         * @author Tim
         * 
         */
        private class TouchTokenListener extends
                GestureDetector.SimpleOnGestureListener {
            /**
             * Returns the token touched in the token list given an X
             * coordinate.
             * 
             * @param x
             *            The coordinate touched.
             * @return Clone of the token touched.
             */
            private BaseToken getTouchedToken(float x) {
                int tokenIndex =
                        ((int) x) / TokenSelectorViewRow.this.getHeight();

                return TokenSelectorViewRow.this.mTokens.get(tokenIndex)
                        .clone();
            }

            @SuppressLint("NewApi")
            @Override
            public void onLongPress(MotionEvent e) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                    BaseToken t = this.getTouchedToken(e.getX());
                    List<BaseToken> stack = new ArrayList<BaseToken>();
                    stack.add(t);
                    TokenSelectorViewRow.this.startDrag(
                            null,
                            new TokenStackDragShadow(stack,
                                    (int) (RADIUS_SCALE
                                            * TokenSelectorViewRow.this
                                                    .getHeight() / 2)), t, 0);
                }
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (TokenSelectorView.this.mOnTokenSelectedListener != null) {
                    TokenSelectorView.this.mOnTokenSelectedListener
                            .onTokenSelected(this.getTouchedToken(e.getX()));
                }
                return true;
            }
        }
    }
}
