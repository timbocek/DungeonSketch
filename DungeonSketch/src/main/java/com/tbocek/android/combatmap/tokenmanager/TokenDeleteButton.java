package com.tbocek.android.combatmap.tokenmanager;

import java.util.Collection;

import android.content.Context;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.ImageView;

import com.tbocek.dungeonsketch.R;
import com.tbocek.android.combatmap.model.primitives.BaseToken;

/**
 * This view defines a region that tokens can be dragged onto to delete them or
 * remove tags.
 * 
 * @author Tim Bocek
 * 
 */
public final class TokenDeleteButton extends ImageView {

    /**
     * The token that was last dropped onto the button.
     */
    private Collection<BaseToken> mManagedTokens;

    /**
     * On drag listener that manages changing the color of the button and
     * opening the context menu.
     */
    private OnDragListener mOnDragToTrashCanListener = new OnDragListener() {
        @Override
        public boolean onDrag(final View view, final DragEvent event) {
            Log.d("DRAG", Integer.toString(event.getAction()));
            ImageView iv = (ImageView) view;
            if (event.getAction() == DragEvent.ACTION_DROP) {
                @SuppressWarnings("unchecked")
                Collection<BaseToken> t =
                        (Collection<BaseToken>) event.getLocalState();
                TokenDeleteButton.this.mManagedTokens = t;
                iv.showContextMenu();
                iv.setImageResource(R.drawable.trashcan);
            } else if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
                iv.setImageResource(R.drawable.trashcan_hover_over);
                return true;
            } else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED) {
                iv.setImageResource(R.drawable.trashcan);
                return true;
            } else if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                return true;
            }
            return true;
        }
    };

    /**
     * Constructor.
     * 
     * @param context
     *            The context to construct in.
     */
    public TokenDeleteButton(final Context context) {
        super(context);
        this.setImageResource(R.drawable.trashcan);
        this.setOnDragListener(this.mOnDragToTrashCanListener);
    }

    /**
     * @return The managed tokens.
     */
    public Collection<BaseToken> getManagedTokens() {
        return this.mManagedTokens;
    }

    /**
     * @param tokens
     *            The tokens to manage.
     */
    public void setManagedTokens(final Collection<BaseToken> tokens) {
        this.mManagedTokens = tokens;
    }

}
