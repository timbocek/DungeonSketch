package com.tbocek.android.combatmap.tokenmanager;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.android.combatmap.view.TagNavigator;
import com.tbocek.dungeonsketch.R;

public class SelectTagDialog extends Dialog {

	private final TagNavigator mTagNavigator;

    private Button mNewTagButton;

    public SelectTagDialog(Context context) {
		super(context);
		this.setContentView(R.layout.select_tag_dialog);
		this.setTitle("Select a Tag");

        ImageButton accept = (ImageButton) this.findViewById(R.id.select_tag_dialog_accept);
        ImageButton reject = (ImageButton) this.findViewById(R.id.select_tag_dialog_cancel);
		mNewTagButton = (Button) this.findViewById(R.id.select_tag_dialog_new);
        mTagNavigator = (TagNavigator) this.findViewById(R.id.select_tag_dialog_tag_navigator);
		mTagNavigator.setShowSystemTags(false);
		mTagNavigator.setShowInactiveTags(false);
		
		accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (listener != null) {
                    listener.onTagSelected(mTagNavigator.getCurrentTagPath());
                }
                SelectTagDialog.this.dismiss();
            }
        });
		
		reject.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                SelectTagDialog.this.cancel();
            }
        });

        mNewTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onNewTagRequested(mTagNavigator.getCurrentTagPath());
                }
                SelectTagDialog.this.dismiss();
            }
        });

		this.mTagNavigator.setTokenDatabase(TokenDatabase.getInstance(this.getContext()));
	}
	
	public void setOnTagSelectedListener(TagSelectedListener l) {
		listener = l;
	}

    public void setAllowNewTag(boolean allowed) {
        mNewTagButton.setVisibility(allowed ? View.VISIBLE : View.GONE);
    }
	
	public interface TagSelectedListener {
		void onTagSelected(String tagPath);
        void onNewTagRequested(String currentTagPath);
    }
	private TagSelectedListener listener;

}
