package com.tbocek.android.combatmap.tokenmanager;

import com.tbocek.dungeonsketch.R;
import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.android.combatmap.view.TagNavigator;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.ImageButton;

public class SelectTagDialog extends Dialog {

	private TagNavigator mTagNavigator;
	private ImageButton mAccept;
	private ImageButton mReject;
	
	public SelectTagDialog(Context context) {
		super(context);
		this.setContentView(R.layout.select_tag_dialog);
		this.setTitle("Select a Tag");
		
		mAccept = (ImageButton) this.findViewById(R.id.select_tag_dialog_accept);
		mReject = (ImageButton) this.findViewById(R.id.select_tag_dialog_cancel);
		mTagNavigator = (TagNavigator) this.findViewById(R.id.select_tag_dialog_tag_navigator);
		mTagNavigator.setShowSystemTags(false);
		mTagNavigator.setShowInactiveTags(false);
		
		mAccept.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (listener != null) {
					listener.onTagSelected(mTagNavigator.getCurrentTagPath());
				}
				SelectTagDialog.this.dismiss();
			}
		});
		
		mReject.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SelectTagDialog.this.cancel();
			}
		});
		
		this.mTagNavigator.setTokenDatabase(TokenDatabase.getInstance(this.getContext()));
	}
	
	public void setOnTagSelectedListener(TagSelectedListener l) {
		listener = l;
	}
	
	public interface TagSelectedListener {
		void onTagSelected(String tagPath);
	}
	private TagSelectedListener listener;

}
