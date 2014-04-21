package com.tbocek.android.combatmap.view;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;
import com.tbocek.dungeonsketch.R;
import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.android.combatmap.TokenDatabase.TagTreeNode;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.model.primitives.Util;
import com.tbocek.android.combatmap.view.interaction.CombatViewInteractionMode;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

public class TagNavigator extends ScrollView {
	
    /**
     * Default text size to use in child views.
     */
    private static final int DEFAULT_TEXT_SIZE = 20;

    /**
     * Number of pixels to pad the top and bottom of each text view with.
     */
    private static final int VERTICAL_PADDING = 8;
	


    /**
     * The number of text views to create along with this parent view.
     * We create an initial text view pool because creating and destroying
     * objects constantly does not work with dragging and dropping; 
     * newly created views during a drag are not registered as drag
     * targets.
     */
    private static final int INITIAL_TEXT_VIEW_POOL = 40;
    
    
	private LinearLayout mChildTagList;
	private ImageButton mBackButton;
	private TextView mCurrentTag;
	private TokenDatabase mTokenDatabase;
	private TokenDatabase.TagTreeNode mCurrentTagTreeNode;
	
	/**
	 * Whether the tag navigator should show tags that were marked as inactive.
	 */
	private boolean showInactiveTags = true;

	/**
	 * Whether the tag navigator should show tags that were marked as "system".
	 */
	private boolean showSystemTags = true;
	
	private List<TagTreeLineItem> mTagItems = Lists.newArrayList();
	
	/**
	 * The tag that was selected when a drag and drop operation started.
	 * Used to return the user to that tag when the D&D operation ends.
	 */
	private TokenDatabase.TagTreeNode mTagOnDragStart = null;
	
	private int mTextSize;
	private boolean mAllowContextMenu;



	public TagNavigator(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		constructorImpl(context);
	}
	
	public TagNavigator(Context context, AttributeSet attrs) {
		super(context, attrs);
		constructorImpl(context);
	}
	
	public TagNavigator(Context context) {
		super(context);
		constructorImpl(context);
	}
	
	private void constructorImpl(Context context) {
		LayoutInflater.from(context).inflate(R.layout.tagnavigator, this);
		
		mChildTagList = (LinearLayout)this.findViewById(R.id.tagnavigator_current_tag_list);
		mBackButton = (ImageButton)this.findViewById(R.id.tagnavigator_back);
		mCurrentTag = (TextView)this.findViewById(R.id.tagnavigator_current_tag);
		this.setTextSize(DEFAULT_TEXT_SIZE);
		
		for (int i = 0; i < INITIAL_TEXT_VIEW_POOL; ++i) {
			TagTreeLineItem tv = createTextView();
			mTagItems.add(tv);
			mChildTagList.addView(tv);
		}
	}
	
	public void setTextSize(int size) {
		this.mTextSize = size;
		this.mCurrentTag.setTextSize(size);
		for (TagTreeLineItem existingView : mTagItems) {
			existingView.setTextSize(mTextSize);
		}
	}
	
	public void setAllowContextMenu(boolean allowContextMenu) {
		mAllowContextMenu = allowContextMenu;
		
		Activity activity = (Activity) this.getContext();
        activity.registerForContextMenu(mCurrentTag);
	}

	public void setTokenDatabase(TokenDatabase database) {
		TagTreeNode node = null;
		if (mCurrentTagTreeNode != null) {
			node = database.getRootNode().getNamedChild(mCurrentTagTreeNode.getPath(), false);
		}
		if (node == null) {
			node = database.getRootNode();
		}
		selectTag(node, true);
		mTokenDatabase = database;
	}
	
	public TagTreeNode getCurrentTagNode() {
		return mCurrentTagTreeNode;
	}
	

	public String getCurrentTag() {
		return mCurrentTagTreeNode.getName();
	}
	
	public String getCurrentTagPath() {
		return mCurrentTagTreeNode.getPath();
	}
	
	public void setShowInactiveTags(boolean show) {
		this.showInactiveTags = show;
	}

	public void setShowSystemTags(boolean show) {
		this.showSystemTags  = show;
	}
	
	private boolean shouldShowTag(TagTreeNode node) {
		return (node.isActive() || showInactiveTags) && (!node.isSystemTag() || showSystemTags);
	}
	
	private boolean hasTagRestrictions() {
		return !this.showInactiveTags || !this.showSystemTags;
	}
	
	private void loadTokenData(TagTreeNode node) {
		mCurrentTagTreeNode = node;
		boolean root = node.getParent() == null;
		mBackButton.setVisibility(root ? View.GONE : View.VISIBLE);
		mCurrentTag.setText(node.getName());
		
		//TODO: should only have to set these once.
		mCurrentTag.setOnClickListener(new TagLabelClickedListener());
		mBackButton.setOnClickListener(new TagLabelClickedListener());

		mCurrentTag.setTag(node);
		mBackButton.setTag(node.getParent());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mCurrentTag.setOnDragListener(new OnDragListener());
            mBackButton.setOnDragListener(new OnDragListener());
        }

		List<String> tagNames = Lists.newArrayList(node.getTagNames());
		if (hasTagRestrictions()) {
			List<String> activeTagNames = Lists.newArrayList();
			for (String tag: tagNames) {
				if (shouldShowTag(node.getNamedChild(tag, false))) {
					activeTagNames.add(tag);
				}
			}
			tagNames = activeTagNames;
		}
		
		
		Collections.sort(tagNames, new Comparator<String>() {
		    @Override
		    public int compare(String o1, String o2) {
		    	// Make sure system tags sort to the bottom
		    	boolean o1System = TokenDatabase.isSystemTag(o1);
		    	boolean o2System = TokenDatabase.isSystemTag(o2);
		    	if (o1System && !o2System) {
		    		return 1;
		    	}
		    	if (o2System && !o1System) {
		    		return -1;
		    	}
		        return o1.compareToIgnoreCase(o2);
		    }});
		
		
		
		// Make sure there are enough text views to go around.
		for (int i = mTagItems.size(); i < tagNames.size(); ++i) {
			TagTreeLineItem tv = createTextView();
			mTagItems.add(tv);
			mChildTagList.addView(tv);
		}
		
		for (int i = 0; i < mTagItems.size(); ++i) {
			TagTreeLineItem tv = mTagItems.get(i);
			if (i < tagNames.size()) {
				TagTreeNode child = node.getNamedChild(tagNames.get(i), false);
				tv.setTagNode(child);
				tv.setVisibility(View.VISIBLE);
			} else {
				tv.setVisibility(View.GONE);
			}
		}
		resetTextViewColors();
	}
	
	private TagTreeLineItem createTextView() {
		TagTreeLineItem view = new TagTreeLineItem(this.getContext());
		view.setOnClickListener(new TagLineItemClickedListener());
		view.setTextSize(this.mTextSize);
		view.setPadding(0, VERTICAL_PADDING, 0, VERTICAL_PADDING);
		if (mAllowContextMenu) {
			Activity activity = (Activity) this.getContext();
	        activity.registerForContextMenu(view);
		}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            view.setOnDragListener(new OnDragListener());
        }
		view.setVisibility(View.GONE);
		
		return view;
	}
	
	public void resetTextViewColors() {
		for (TagTreeLineItem v: this.mTagItems) {
			v.setTextColor(v.getTagNode() == mCurrentTagTreeNode 
			               ? TagTreeLineItem.COLOR_SELECTED 
						   : TagTreeLineItem.COLOR_DEFAULT);
		}
		this.mCurrentTag.setTextColor(
				mCurrentTag.getText().equals(this.mCurrentTagTreeNode.getName()) 
				? TagTreeLineItem.COLOR_SELECTED 
				: TagTreeLineItem.COLOR_DEFAULT);
	}
	
	private void selectTag(TagTreeNode node, boolean updateColors) {
		if (mTagSelectedListener != null) {
			mTagSelectedListener.onTagSelected(node);
		}
		mCurrentTagTreeNode = node;
		if (node.hasChildren()) {
			loadTokenData(node);
		} else {
			if (updateColors) {
				resetTextViewColors();
			}
		}
		
	}
	
	private class TagLabelClickedListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			selectTag((TagTreeNode) v.getTag(), true);
		}
	}
	
	private class TagLineItemClickedListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			selectTag(((TagTreeLineItem)v).getTagNode(), true);
		}
	}

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private class OnDragListener implements View.OnDragListener {
        private Handler mLongDragHandler = new Handler();
        
        
        private class LongDragRunnable implements Runnable {
        	public TagTreeNode mNode;
			@Override
			public void run() {
				selectTag(mNode, false);
			}
        	
        }
        private LongDragRunnable mLongDragRunnable = new LongDragRunnable();
        
		@Override
		public boolean onDrag(View v, DragEvent event) {
            if (event.getAction() == DragEvent.ACTION_DROP) {
                @SuppressWarnings("unchecked")
                Collection<BaseToken> toAdd =
                        (Collection<BaseToken>) event.getLocalState();
                if (TagNavigator.this.mTagSelectedListener != null) {
                    TagNavigator.this.mTagSelectedListener
                            .onDragTokensToTag(toAdd, ((TagTreeLineItem)v).getTagNode());
                }
                resetTextViewColors();
                mLongDragHandler.removeCallbacks(this.mLongDragRunnable);
                return true;
            } else if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
            	TagTreeNode node = null;
            	try {
	            	node = ((TagTreeLineItem)v).getTagNode();
	            	if (node.isSystemTag()) {
	            		return true;
	            	}
	            	((TagTreeLineItem)v).setTextColor(TagTreeLineItem.COLOR_DRAG_TARGET);
	            	
            	} catch (Exception e) {
            		// Ignore - bad cast expected here
            	}
            	
            	try {
            		((TextView)v).setTextColor(TagTreeLineItem.COLOR_DRAG_TARGET);
            	} catch (Exception e) {
            		// Ignore - bad cast expected here
            	}
        		if (node == null) {
        			node = (TagTreeNode) v.getTag();
        		}
        		
            	if (node != null) {
            		mLongDragRunnable.mNode = node;
            		mLongDragHandler.postDelayed(mLongDragRunnable, ViewConfiguration.getLongPressTimeout());
            	}
                return true;
            } else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED) {
            	resetTextViewColors();
                mLongDragHandler.removeCallbacks(this.mLongDragRunnable);
                return true;
            } else if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
            	if (mTagOnDragStart == null) {
            		TagNavigator.this.mTagOnDragStart = TagNavigator.this.mCurrentTagTreeNode;
            	}
            	return true;
            } else if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
            	if (mTagOnDragStart != null) {
            		TagNavigator.this.selectTag(TagNavigator.this.mTagOnDragStart, true);
            		mTagOnDragStart = null;
            	}
            	resetTextViewColors();
            	return true;
            }
            return true;
		}
		
	}
	
	
	public interface TagSelectedListener {
		
        /**
         * Called when the user clicks on a tag.
         * 
         * @param selectedTag
         *            The tag clicked on.
         */
		void onTagSelected(TagTreeNode selectedTag);

        /**
         * Called when the user drags a token onto a tag.
         * 
         * @param token
         *            The token that was dragged.
         * @param tag
         *            The tag that the token was dragged to.
         */
        void onDragTokensToTag(Collection<BaseToken> token, TagTreeNode tag);
	}
	private TagSelectedListener mTagSelectedListener = null;
	public void setTagSelectedListener(TagSelectedListener listener) {
		mTagSelectedListener = listener;
	}

	public boolean isViewAChild(View v) {
		return (v == this.mCurrentTag) || this.mTagItems.contains(v);
	}

	public void selectRoot() {
		TagTreeNode n = this.mCurrentTagTreeNode;
		while (n.getParent() != null) {
			n = n.getParent();
		}
		selectTag(n, true);
	}

	public void setCurrentTagIsActive(boolean active) {
		this.getCurrentTagNode().setIsActive(active);
		selectTag(this.getCurrentTagNode(), true);
		for (TagTreeLineItem tv: mTagItems) {
			if (tv.getTagNode() == this.getCurrentTagNode()) {
				// Force reload of tag properties.
				tv.setTagNode(this.getCurrentTagNode());
			}
				
		}
	}

	public void setDragStyleOnCurrentTag() {
		// TODO Auto-generated method stub
		for (TagTreeLineItem view: this.mTagItems){
			if (view.getTagNode() == this.mCurrentTagTreeNode) {
				view.setTextColor(TagTreeLineItem.COLOR_DRAG_TARGET);
			}
		}
	}
	
	public boolean setTagPath(String path) {
		TokenDatabase.TagTreeNode node = null;
		try {
			if (path == TokenDatabase.ALL) {
				node = this.mTokenDatabase.getRootNode();
			} else {
				node = this.mTokenDatabase.getRootNode().getNamedChild(path, false);
			}
			selectTag(node, true);
		} catch (Exception e) { 
			return false;
		}
		return true;
	}

}
