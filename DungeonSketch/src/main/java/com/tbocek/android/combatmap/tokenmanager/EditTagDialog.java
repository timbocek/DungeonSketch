package com.tbocek.android.combatmap.tokenmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.dungeonsketch.R;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

public class EditTagDialog extends RoboActivity {

    public static final String NEW_TAG_PATH = "NEW_TAG_PATH";
    public static final String SELECTED_TAG_PATH = "SELECTED_TAG_PATH";

    @InjectView(tag="new_tag_name")
    TextView tagName;
    @InjectView(tag="new_tag_under_top_level")
    RadioButton underTopLevel;
    @InjectView(tag="new_tag_under_selected") RadioButton underSelected;
    @InjectView(tag="new_tag_create")
    Button create;

    private TokenDatabase mDatabase;
    private TokenDatabase.TagTreeNode mSelectedTag;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.new_tag_layout);

        mDatabase = TokenDatabase.getInstance(this);
        String tagPath = this.getIntent().getStringExtra(SELECTED_TAG_PATH);

        mSelectedTag = mDatabase.getRootNode().getNamedChild(tagPath, false);
        underTopLevel.setVisibility(View.INVISIBLE);
        underSelected.setVisibility(View.INVISIBLE);
        underTopLevel.setChecked(true);

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPath = mDatabase.renameTag
                        (mSelectedTag.getPath(), tagName.getText().toString());
                Intent resultData = new Intent();
                resultData.putExtra(NEW_TAG_PATH, newPath);
                setResult(RESULT_OK, resultData);
                finish();
            }
        });
        create.setText(R.string.rename_tag);
    }
}

