package com.tbocek.android.combatmap.tokenmanager;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tbocek.android.combatmap.DeveloperMode;
import com.tbocek.android.combatmap.Help;
import com.tbocek.android.combatmap.TokenImageManager;
import com.tbocek.android.combatmap.view.TokenListAdapter;
import com.tbocek.dungeonsketch.R;
import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.android.combatmap.TokenDatabase.TagTreeNode;
import com.tbocek.android.combatmap.model.MultiSelectManager;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.view.GridLayout;
import com.tbocek.android.combatmap.view.TagNavigator;
import com.tbocek.android.combatmap.view.TokenButton;
import com.tbocek.android.combatmap.view.TokenLoadTask;

/**
 * This activity lets the user view their library of tokens and manage which
 * tags apply to which tokens.
 * 
 * @author Tim Bocek
 * 
 */
public final class TokenManager extends ActionBarActivity {

    /**
     * The minimum number of tokens to display in the grid across the smallest
     * screen dimension.
     */
    private static final int MINIMUM_TOKENS_SHOWN = 3;

    /**
     * The width and height of each token button.
     */
    private static final int TOKEN_BUTTON_SIZE = 150;

    /**
     * The list of token buttons that are managed by this activity.
     */
    private Collection<TokenButton> mButtons = Lists.newArrayList();

    /**
     * When a context menu for a tag is opened, stores the tag that the menu
     * opened on.
     */
    private String mContextMenuTag;

    private MenuItem mDeleteTagMenuItem;

    private MultiSelectManager mMultiSelectManager;

    /**
     * The action mode that was started to manage the selection.
     */
    private ActionMode mMultiSelectActionMode;
    
    private TagNavigator.TagSelectedListener mTagSelectedListener = 
    		new TagNavigator.TagSelectedListener() {
				@Override
				public void onTagSelected(TagTreeNode selectedTag) {
					TokenManager.this.setScrollViewTag(selectedTag.getPath());
					TokenManager.this.getSupportActionBar().setTitle(selectedTag.getName());
				}

				@Override
				public void onDragTokensToTag(Collection<BaseToken> tokens,
						TagTreeNode tag) {
		            for (BaseToken t : tokens) {
		            	if (!tag.isSystemTag()) {
			                TokenManager.this.mTokenDatabase.tagToken(
			                        t.getTokenId(), tag.getPath());
			                TokenManager.this.setScrollViewTag(tag.getPath());
		            	} else {
		            		Toast toast = Toast.makeText(
		            				TokenManager.this, 
		            				"Cannot add token to tag " + tag.getName(), 
		            				Toast.LENGTH_LONG);
		            		toast.show();
		            	}
		            }
					
				}
			};

    private TagNavigator mTagNavigator;

    /**
     * The database to load tags and tokens from.
     */
    private TokenDatabase mTokenDatabase;

    /**
     * Drag target to delete tokens.
     */
    private TokenDeleteButton mTrashButton;

    private String tagFromActionBar = null;

	private MenuItem mTagActiveMenuItem;

	private DrawerLayout drawer;

	private Adapter adapter;

	private FrameLayout drawerList;

	private ActionBarDrawerToggle actionBarDrawerToggle;
	
	private Button enableTagButton;
	
	private TextView disabledTagExplanation;

    private TokenImageManager.Loader mLoader;

    private GridView mGridView;

    /**
     * Deletes the given list of tokens.
     * 
     * @param tokens
     *            The tokens to delete.
     */
    private void deleteTokens(Collection<BaseToken> tokens) {
        for (BaseToken token : tokens) {
            this.mTokenDatabase.removeToken(token);
            try {
                token.maybeDeletePermanently();
            } catch (IOException e) {
                Toast toast =
                        Toast.makeText(
                                this.getApplicationContext(),
                                "Did not delete the token, probably because "
                                        + "the external storage isn't writable."
                                        + e.toString(), Toast.LENGTH_LONG);
                toast.show();
            }
        }
        this.setScrollViewTag(this.mTagNavigator.getCurrentTag());
    }

    /**
     * @return
     */
    private String getActiveTag() {
    	return this.mTagNavigator.getCurrentTag();
    }
    
    private String getActiveTagPath() {
        return this.mTagNavigator.getCurrentTagPath();
    }

    private boolean isLargeScreen() {
        int layout = this.getResources().getConfiguration().screenLayout;
        int layoutSize = layout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return layoutSize == Configuration.SCREENLAYOUT_SIZE_LARGE
                || layoutSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.token_delete_entire_token) {
            this.deleteTokens(this.mTrashButton.getManagedTokens());
            return true;
        } else if (itemId == R.id.token_delete_from_tag) {
            this.removeTagFromTokens(this.mTrashButton.getManagedTokens(),
                    this.mTagNavigator.getCurrentTagPath());
            return true;
        } else if (itemId == R.id.tag_context_menu_delete) {
            if (this.mTagNavigator.getCurrentTag().equals(this.mContextMenuTag)) {
                this.mTagNavigator.selectRoot();
            }
            TagTreeNode parent = this.mTokenDatabase.deleteTag(this.mContextMenuTag);
            String path = parent.getPath();
            this.updateTagList();
            this.mTagNavigator.setTagPath(path);
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        DeveloperMode.strictMode();
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.token_manager_layout);

        mMultiSelectManager = new MultiSelectManager();
        
        this.mTagNavigator = new TagNavigator(this);
        this.mTagNavigator.setTagSelectedListener(this.mTagSelectedListener);
        this.mTagNavigator.setAllowContextMenu(true);

        this.mGridView = (GridView) findViewById(R.id.token_manager_grid_view);

        mLoader = new TokenImageManager.Loader(new Handler());
        mLoader.start();
        mLoader.getLooper(); // Make sure loader thread is ready to go.

        // On large screens, set up a seperate column of token tags and possibly
        // set up a trash can to drag tokens too if drag&drop is an option on
        // the platform.
        // Otherwise, use tabs in the action bar to display & select token tags.
        FrameLayout tagListFrame;
        if (this.isLargeScreen()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                this.mTrashButton = new TokenDeleteButton(this);
                this.registerForContextMenu(this.mTrashButton);
                ((FrameLayout) this
                        .findViewById(R.id.token_manager_delete_button_frame))
                        .addView(this.mTrashButton);
            } else {
                // If not Honeycomb, no need for the trash button.
                this.findViewById(R.id.token_manager_delete_button_frame)
                .setVisibility(View.GONE);
            }

            tagListFrame =
                    (FrameLayout) this
                    .findViewById(R.id.token_manager_taglist_frame);
        } else {
            drawer = (DrawerLayout) findViewById(R.id.drawer_layout);     
            drawerList = (FrameLayout) findViewById(R.id.left_drawer);        
            actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawer, R.drawable.ic_drawer, R.string.drawer_open , R.string.drawer_close) {
                @Override
                public void onDrawerOpened(View drawerView) {
                    // TODO Auto-generated method stub
                    super.onDrawerOpened(drawerView);
                }
            };
            drawer.setDrawerListener(actionBarDrawerToggle);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            tagListFrame =
                    (FrameLayout) this
                    .findViewById(R.id.left_drawer);
        }
        
        tagListFrame.addView(this.mTagNavigator);

        // Set up a drag handler so that the user can drop tokens onto the token
        // view when it switches over after long-holding on a tag.
        this.mGridView.setOnDragListener(new View.OnDragListener() {
			
			@Override
			public boolean onDrag(View v, DragEvent event) {
				if (event.getAction() == DragEvent.ACTION_DROP) {
					TagTreeNode tag = mTagNavigator.getCurrentTagNode();
					Collection<BaseToken> tokens =
	                        (Collection<BaseToken>) event.getLocalState();
					
					// TODO: De-dupe this with the drag handler above.
		            for (BaseToken t : tokens) {
		            	if (!tag.isSystemTag()) {
			                TokenManager.this.mTokenDatabase.tagToken(
			                        t.getTokenId(), tag.getPath());
			                TokenManager.this.setScrollViewTag(tag.getPath());
		            	} else {
		            		Toast toast = Toast.makeText(
		            				TokenManager.this, 
		            				"Cannot add token to tag " + tag.getName(), 
		            				Toast.LENGTH_LONG);
		            		toast.show();
		            	}
		            }
				} else if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
					mTagNavigator.setDragStyleOnCurrentTag();
				} else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED) {
					mTagNavigator.resetTextViewColors();
				}
				return true;
			}
		});

        mMultiSelectManager.setSelectionChangedListener(
                new MultiSelectManager.SelectionChangedListener() {

                    @Override
                    public void selectionChanged() {
                        int numTokens =
                                mMultiSelectManager.getSelectedTokens().size();
                        if (TokenManager.this.mMultiSelectActionMode != null) {
                            TokenManager.this.mMultiSelectActionMode
                                    .setTitle(Integer
                                            .toString(numTokens)
                                            + (numTokens == 1
                                            ? " Token "
                                            : " Tokens ")
                                            + "Selected.");
                        }
                    }

                    @Override
                    public void selectionEnded() {
                        if (TokenManager.this.mMultiSelectActionMode != null) {
                            ActionMode m =
                                    TokenManager.this.mMultiSelectActionMode;
                            TokenManager.this.mMultiSelectActionMode =
                                    null;
                            m.finish();
                        }

                        for (TokenButton b : TokenManager.this.mButtons) {
                            MultiSelectTokenButton msb =
                                    (MultiSelectTokenButton) b;
                            msb.setSelected(false);
                        }
                    }

                    @Override
                    public void selectionStarted() {
                        TokenManager.this.mMultiSelectActionMode =
                                TokenManager.this
                                        .startActionMode(new TokenSelectionActionModeCallback());
                    }
                }
        );
        
        disabledTagExplanation = (TextView) this.findViewById(R.id.token_manager_disabled_explanation);
        enableTagButton = (Button) this.findViewById(R.id.token_manager_enable_button);
        
        enableTagButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mTagActiveMenuItem.setChecked(true);
	        	TokenManager.this.mTagNavigator.setCurrentTagIsActive(true);
			}
        });

    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenuInfo menuInfo) {
        if (this.mTagNavigator.isViewAChild(v)) {
            TextView tv = (TextView) v;
            this.mContextMenuTag = tv.getText().toString();
            this.getMenuInflater().inflate(R.menu.tag_context_menu, menu);
        }
        if (v == this.mTrashButton) {
            Collection<BaseToken> tokens = this.mTrashButton.getManagedTokens();
            if (tokens.size() > 0) {
                String deleteText = "";
                if (tokens.size() == 1) {
                    deleteText = this.getString(R.string.delete_token);
                } else {
                    deleteText =
                            "Delete " + Integer.toString(tokens.size())
                            + " Tokens";
                }

                menu.add(Menu.NONE, R.id.token_delete_entire_token, Menu.NONE,
                        deleteText);
            }

            if (!this.mTagNavigator.getCurrentTag().equals(TokenDatabase.ALL)) {
                String removeText =
                        "Remove '" + this.mTagNavigator.getCurrentTag() + "' Tag";

                if (tokens.size() > 1) {
                    removeText +=
                            " from " + Integer.toString(tokens.size())
                            + " tokens";
                }
                menu.add(Menu.NONE, R.id.token_delete_from_tag, Menu.NONE,
                        removeText);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.token_manager_menu, menu);
        this.mDeleteTagMenuItem = menu.findItem(R.id.token_manager_delete_tag);
        this.mTagActiveMenuItem = menu.findItem(R.id.token_manager_is_active);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.token_manager_new_token) {
        	Intent i = new Intent(this, TokenCreator.class);
        	if (!this.getActiveTag().equals(TokenDatabase.ALL)){ 
        		i.putExtra(TokenCreator.TAG_TO_ADD, this.getActiveTagPath());
        	}
            this.startActivity(i);
            return true;
        } else if (itemId == R.id.token_manager_new_tag) {
            Intent i = new Intent(this, NewTagDialog.class);
            i.putExtra(NewTagDialog.SELECTED_TAG_PATH, this.getActiveTagPath());
            this.startActivity(i);
            return true;
        } else if (itemId == R.id.token_manager_delete_tag) {
            // Confirm the tag deletion.
            new AlertDialog.Builder(this)
            .setMessage(
                    "Really delete the " + this.getActiveTag()
                    + " tag?  This won't delete any tokens.")
                    .setCancelable(false)
                    .setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog,
                                int which) {
                            TagTreeNode parent = TokenManager.this.mTokenDatabase.deleteTag(
                                    TokenManager.this.getActiveTagPath());
                            String path = parent.getPath();
                            TokenManager.this.updateTagList();
                            mTagNavigator.setTagPath(path);
                        }
                    })
                    .setNegativeButton("No",
                            new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog,
                                int which) {
                            dialog.cancel();

                        }
                    }).create().show();
            return true;
        } else if (itemId == R.id.token_manager_help) {
            Help.openHelp(this);
            return true;
        } else if (itemId == android.R.id.home) {
        	if (!this.isLargeScreen()) {
	        	if (drawer.isDrawerOpen(drawerList)) {
	        		drawer.closeDrawer(drawerList);
	        		} else {
	        		drawer.openDrawer(drawerList);
	        	}
        	}
            return true;
        } else if (itemId == R.id.token_manager_is_active) {
        	mTagActiveMenuItem.setChecked(!mTagActiveMenuItem.isChecked());
        	TokenManager.this.mTagNavigator.setCurrentTagIsActive(mTagActiveMenuItem.isChecked());
        	return true;
        } else {
            return false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            this.mTokenDatabase.save(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLoader.quit();
        mLoader.clearQueue();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mTokenDatabase =
                TokenDatabase.getInstance(this.getApplicationContext());
        this.updateTagList();
        Debug.stopMethodTracing();
    }

    /**
     * Removes the given tag from the given tokens.
     * 
     * @param tokens
     *            Tokens to remove the tag from.
     * @param tag
     *            The tag to remove.
     */
    private void removeTagFromTokens(Collection<BaseToken> tokens, String tag) {
        for (BaseToken token : tokens) {
            this.mTokenDatabase.removeTagFromToken(token.getTokenId(), tag);
            this.setScrollViewTag(tag);
        }
    }

    /**
     * Sets the tag that the scroll view will display tokens from.
     * 
     * @param tag
     *            The new tag to display tokens for.
     */
    private void setScrollViewTag(final String tag) {
        List<BaseToken> tokens = mTokenDatabase.getTokensForTag(tag);
	    this.mGridView.setAdapter(new Adapter(tokens));

		if (tag.equals(TokenDatabase.ALL) || mTokenDatabase.getRootNode().getNamedChild(tag, false).isActive()) {
			enableTagButton.setVisibility(View.GONE);
			disabledTagExplanation.setVisibility(View.GONE);
		} else {
			enableTagButton.setVisibility(View.VISIBLE);
			disabledTagExplanation.setVisibility(View.VISIBLE);
	    }
    }

    private void updateTagList() {
    	this.mTagNavigator.setTokenDatabase(this.mTokenDatabase);
    }

    /**
     * Callback defining an action mode for selecting multiple tokens.
     * 
     * @author Tim
     * 
     */
    private class TokenSelectionActionModeCallback implements
    ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            final Collection<BaseToken> tokens =
                    mMultiSelectManager.getSelectedTokens();
            int itemId = item.getItemId();
            if (itemId == R.id.token_manager_action_mode_remove_tag) {
                TokenManager.this.removeTagFromTokens(tokens,
                        TokenManager.this.getActiveTagPath());
                return true;
            } else if (itemId == R.id.token_manager_action_mode_delete) {
                TokenManager.this.deleteTokens(tokens);
                return true;
            } else if (itemId == R.id.token_manager_action_mode_add_to_tag) {
                List<String> tags = TokenManager.this.mTokenDatabase.getTags();
                final ArrayAdapter<String> adapter =
                        new ArrayAdapter<String>(TokenManager.this,
                                R.layout.selection_dialog_text_view);
                adapter.addAll(tags);
                SelectTagDialog selectTagDlg = new SelectTagDialog(TokenManager.this);
                selectTagDlg.setOnTagSelectedListener(new SelectTagDialog.TagSelectedListener() {	
					@Override
					public void onTagSelected(String tagPath) {
						for (BaseToken token : tokens) {
							Set<String> tags =
	                                Sets.newHashSet(tagPath);
                            TokenManager.this.mTokenDatabase
                            	.tagToken(token, tags);
                        }
					}
				});
                selectTagDlg.show();
                return true;
            } else {
            	
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.token_manager_action_mode,
                    menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mMultiSelectManager.selectNone();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            MenuItem removeTag =
                    menu.findItem(R.id.token_manager_action_mode_remove_tag);
            removeTag.setVisible(!TokenManager.this.getActiveTag().equals(
                    TokenDatabase.ALL) && 
                    !TokenDatabase.isSystemTag(getActiveTag()));
            removeTag.setTitle("Remove tag '"
                    + TokenManager.this.getActiveTag() + "'");
            return true;
        }

    }
    private class Adapter extends TokenListAdapter {

        public Adapter(List<BaseToken> objects) {
            super(TokenManager.this, objects, mLoader);
        }

        protected TokenButton createTokenButton() {
            MultiSelectTokenButton b = new MultiSelectTokenButton(getContext(), null,
                    mMultiSelectManager);

            b.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
            b.setMinimumWidth(mGridView.getColumnWidth());
            b.setMinimumHeight(mGridView.getColumnWidth());

            return b;
        }
    }
}
