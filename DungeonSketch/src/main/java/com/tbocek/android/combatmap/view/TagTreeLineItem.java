package com.tbocek.android.combatmap.view;

import com.tbocek.dungeonsketch.R;
import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.android.combatmap.model.primitives.Util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class TagTreeLineItem extends LinearLayout {
	public static final int COLOR_DEFAULT = Color.GRAY;
	public static final int COLOR_SELECTED = Color.WHITE;
	public static final int COLOR_DRAG_TARGET = Util.ICS_BLUE;
	
	TextView mName;
	ImageView mHasChildren;
	ImageView mIsSystem;
	
	TokenDatabase.TagTreeNode mTag;
	
	public TagTreeLineItem(Context context) {
		super(context);
		LayoutInflater.from(context).inflate(R.layout.tag_widget_layout, this);
		
		mName = (TextView) this.findViewById(R.id.tag_widget_name);
		mHasChildren = (ImageView) this.findViewById(R.id.tag_widget_has_children);
		mIsSystem = (ImageView) this.findViewById(R.id.tag_widget_system);
	}
	
	public void setTagNode(TokenDatabase.TagTreeNode tag) {
		mName.setText(tag.getName());
		mHasChildren.setVisibility(tag.hasChildren() ? View.VISIBLE : View.GONE);
		mIsSystem.setVisibility(tag.isSystemTag() ? View.VISIBLE : View.INVISIBLE);
		if (tag.isActive()) {
			mName.setPaintFlags(mName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
		} else {
			mName.setPaintFlags(mName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
		}
		mTag = tag;
	}
	
	public TokenDatabase.TagTreeNode getTagNode() {
		return mTag;
	}
	
	public void setTextColor(int color) {
		mName.setTextColor(color);
		
		PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
		
		mHasChildren.setColorFilter(colorFilter);
		mIsSystem.setColorFilter(colorFilter);
	}
	
	public void setTextSize(int size) {
		mName.setTextSize(size);
	}
}
