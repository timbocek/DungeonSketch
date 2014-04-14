package com.tbocek.android.combatmap.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * This composite view lays out controls in a grid. It is given a size for the
 * cells in the grid, and lays controls out, one per cell, in rows. The control
 * grows as much as needed vertically to accommodate the child controls. It does
 * not guarantee a particular relative layout; the row in which a child appears
 * will depend on the width of the control.
 * 
 * @author Tim
 * 
 */
public class GridLayout extends ViewGroup {

    /**
     * The height of each cell.
     */
    private int mCellHeight;

    /**
     * The width of each cell.
     */
    private int mCellWidth;

    /**
     * Constructor.
     * 
     * @param context
     *            Context that this view uses.
     */
    public GridLayout(Context context) {
        super(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int cellsPerRow = (r - l) / this.mCellWidth;

        for (int i = 0; i < this.getChildCount(); ++i) {
            int row = i / cellsPerRow;
            int col = i % cellsPerRow;

            int childLeft = col * this.mCellWidth;
            int childTop = row * this.mCellHeight;

            this.getChildAt(i).layout(childLeft, childTop,
                    childLeft + this.mCellWidth, childTop + this.mCellHeight);
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);

        int cellsPerRow = width / this.mCellWidth;
        int numRows =
                (int) Math.ceil(((float) this.getChildCount())
                        / ((float) cellsPerRow));

        int height = numRows * this.mCellHeight;

        this.setMeasuredDimension(width, height);

        // Measure children to give them the dimensions allowed.
        int childWidthSpec =
                View.MeasureSpec.makeMeasureSpec(this.mCellWidth,
                        View.MeasureSpec.EXACTLY);
        int childHeightSpec =
                View.MeasureSpec.makeMeasureSpec(this.mCellHeight,
                        View.MeasureSpec.EXACTLY);
        for (int i = 0; i < this.getChildCount(); ++i) {
            this.getChildAt(i).measure(childWidthSpec, childHeightSpec);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    /**
     * Sets the width and height for each cell.
     * 
     * @param width
     *            The width of each cell.
     * @param height
     *            The height of each cell.
     */
    public void setCellDimensions(int width, int height) {
        this.mCellWidth = width;
        this.mCellHeight = height;
    }

}
