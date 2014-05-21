package com.tbocek.android.combatmap.tokenmanager;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.ImageView;

import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.dungeonsketch.R;

import java.util.Collection;

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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private class DragListener implements OnDragListener {
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
    }

    /**
     * Constructor.
     * 
     * @param context
     *            The context to construct in.
     */
    public TokenDeleteButton(final Context context) {
        super(context);
        this.setImageResource(R.drawable.trashcan);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.setOnDragListener(new DragListener());
        }
    }

    /**
     * @return The managed tokens.
     */
    public Collection<BaseToken> getManagedTokens() {
        return this.mManagedTokens;
    }
}
