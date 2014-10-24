package com.tbocek.android.combatmap.tokenmanager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.dungeonsketch.R;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;


public class NewTagDialog extends RoboActivity {
	public static final String SELECTED_TAG_PATH = "SELECTED_TAG_PATH";
    public static final String TOKENS_TO_ADD = "TOKENS_TO_ADD";
    @InjectView(tag="new_tag_name") TextView tagName;
	@InjectView(tag="new_tag_under_top_level") RadioButton underTopLevel;
	@InjectView(tag="new_tag_under_selected") RadioButton underSelected;
	@InjectView(tag="new_tag_create") Button create;
	
	private TokenDatabase mDatabase;
	private TokenDatabase.TagTreeNode mSelectedTag;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.new_tag_layout);
        
    	mDatabase = TokenDatabase.getInstance(this);
    	String tagPath = this.getIntent().getStringExtra(SELECTED_TAG_PATH);
    	if (tagPath.equals(TokenDatabase.ALL)){
    		mSelectedTag = mDatabase.getRootNode();
    		underTopLevel.setVisibility(View.INVISIBLE);
    		underSelected.setVisibility(View.INVISIBLE);
    		underTopLevel.setChecked(true);
    	} else {
    		mSelectedTag = mDatabase.getRootNode().getNamedChild(tagPath, false);
    		underTopLevel.setVisibility(View.VISIBLE);
    		underSelected.setVisibility(View.VISIBLE);
    		underSelected.setText("Underneath " + mSelectedTag.getName());
    		underTopLevel.setChecked(false);
    		underSelected.setChecked(true);
    	}
    	
    	create.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				createTagClicked();
				
			}
		});
    }
    
    protected void createTagClicked() {
        TokenDatabase.TagTreeNode tag;

    	if (underTopLevel.isChecked()) {
    		tag = mDatabase.getRootNode().getNamedChild(tagName.getText().toString(), true);
    	} else {
    		tag = mSelectedTag.getNamedChild(tagName.getText().toString(), true);
    	}

        if (this.getIntent().hasExtra(TOKENS_TO_ADD)) {
            String[] tokens = this.getIntent().getStringArrayExtra(TOKENS_TO_ADD);
            for (String token : tokens) {
                tag.addToken(token);
            }
        }
    	finish();
    }
 }

