package com.tbocek.android.combatmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.google.common.collect.Lists;
import com.tbocek.android.combatmap.model.primitives.Units;
import com.tbocek.android.combatmap.model.primitives.Util;

import java.util.List;

public class ScrollBuffer {
	private static final int MIN_DRAW_DIP = 3;
	
	public class DrawRequest {
		public Canvas canvas;
		public final List<Rect> invalidRegions = Lists.newArrayList();
		public int deltaX;
		public int deltaY;
		
	}
	private Bitmap primary;
	private Bitmap secondary;
    private int mMinDraw;
	
	private float deltaXAccumulator = 0;
	private float deltaYAccumulator = 0;
	
	private boolean invalidated = false;
	
	public void invalidateBuffers() {
		invalidated = true;
	}
	
	public void allocateBitmaps(int width, int height) {
		// TODO: Do we need to use ARGB_8888 instead?
		primary = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		secondary = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		invalidated = true;
		mMinDraw = (int) (Units.dpToPx(MIN_DRAW_DIP));
	}
	
	public DrawRequest scroll(float deltaX, float deltaY) {
		DrawRequest req = new DrawRequest();
		deltaXAccumulator += deltaX;
		deltaYAccumulator += deltaY;
		
		int mLastXScroll = (int) deltaX;
		int mLastYScroll = (int) deltaY;
		
		if (mLastXScroll == 0 && mLastYScroll == 0) {
			return null;
		}
		
		deltaXAccumulator -= mLastXScroll;
		deltaYAccumulator -= mLastYScroll;
		
		req.deltaX = mLastXScroll;
		req.deltaY = mLastYScroll;
		
		if (invalidated) {
			invalidated = false;
			req.canvas = new Canvas(primary);
			req.invalidRegions.add(new Rect(0,0, req.canvas.getWidth(), req.canvas.getHeight()));
			return req;
		}
		
		req.canvas = new Canvas(secondary);
		Rect dst = new Rect(mLastXScroll, mLastYScroll, req.canvas.getWidth() + mLastXScroll, req.canvas.getHeight() + mLastYScroll);
		req.canvas.drawBitmap(primary, null, dst, null);

		swapBuffers();
		
		// We want to draw a bit more than needed to avoid propegating artifacts around the edge.
		int redrawSizeX = enforceMinScroll(mLastXScroll);
		int redrawSizeY = enforceMinScroll(mLastYScroll);
		
		if (mLastXScroll > 0) {
			req.invalidRegions.add(new Rect(0, 0, redrawSizeX, req.canvas.getHeight()));
		} else if (mLastXScroll < 0) {
			req.invalidRegions.add(new Rect(req.canvas.getWidth() + redrawSizeX, 0, req.canvas.getWidth(), req.canvas.getHeight()));
		}
		
		if (mLastYScroll > 0) {
			req.invalidRegions.add((new Rect(0, 0, req.canvas.getWidth(), redrawSizeY)));
		} else if (mLastYScroll < 0) {
			req.invalidRegions.add(new Rect(0, req.canvas.getHeight() + redrawSizeY, req.canvas.getWidth(), req.canvas.getHeight()));
		}
		
		return req;
	}
	
	private int enforceMinScroll(int scrollAmount) {
		if (scrollAmount > 0) {
			return Math.max(scrollAmount, mMinDraw);
		} else if (scrollAmount < 0) {
			return Math.min(scrollAmount, -mMinDraw);
		} else {
			return 0;
		}
	}

    private void swapBuffers() {
		Bitmap tmp = primary;
		primary = secondary;
		secondary = tmp;
	}

	public Bitmap getActiveBuffer() {
		return primary;
	}
}
