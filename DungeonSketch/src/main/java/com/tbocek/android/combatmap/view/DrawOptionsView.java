package com.tbocek.android.combatmap.view;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.tbocek.android.combatmap.R;
import com.tbocek.android.combatmap.model.primitives.Util;

/**
 * Provides a tool and color selector for drawing.
 * 
 * @author Tim Bocek
 * 
 */
public final class DrawOptionsView extends LinearLayout {

    /**
     * The width on each side of the buttons used to pick a new color.
     */
    private static final int COLOR_BUTTON_SIZE = 48;

    /**
     * Stroke width to use for the ink tube tool.
     */
    private static final float INKTUBE_STROKE_WIDTH = 2.0f;

    /**
     * Stroke width to use for the paintbrush tool.
     */
    private static final float PAINTBRUSH_STROKE_WIDTH = 0.5f;

    /**
     * Stroke width to use for the pen tool.
     */
    private static final float PEN_STROKE_WIDTH = 0.1f;

    /**
     * Stroke width to use for the pencil tool.
     */
    private static final float PENCIL_STROKE_WIDTH = 0.05f;

    /**
     * The width of the space that seperates tools and colors.
     */
    private static final int SEPERATOR_WIDTH = 32;

    /**
     * Button that loads images onto the background. Should only show in
     * background mode.
     */
    private ImageToggleButton mBackgroundImageButton;

    /**
     * A list of all buttons that select a color, so that they can be modified
     * as a group.
     */
    private ToggleButtonGroup mColorGroup = new ToggleButtonGroup();

    /**
     * View that contains controls that should be scrolled (this is not all
     * controls because we want the pan control to "stick" and be always
     * visible).
     */
    private HorizontalScrollView mInnerView;

    /**
     * The layout that will hold drawing buttons.
     */
    private LinearLayout mLayout;

    /**
     * List of all buttons that select line widths, so that they can be modified
     * as a group.
     */
    private ToggleButtonGroup mLineWidthGroup = new ToggleButtonGroup();

    /**
     * Line widths that do not make sense when using tools that don't support
     * drawing a region (like straight lines).
     */
    private ToggleButtonGroup mLineWidthRegionGroup = new ToggleButtonGroup();

    /**
     * The button used to select the mask control. Needs to be stored because it
     * needs to be conditionally hidden.
     */
    private ImageToggleButton mMaskButton;

    /**
     * The listener that is called when something about the current draw tool
     * changes.
     */
    private OnChangeDrawToolListener mOnChangeDrawToolListener =
            new NullChangeDrawToolListener();

    /**
     * A list of all buttons that select a drawing tool, so that they can be
     * modified as a group.
     */
    private ToggleButtonGroup mToolsGroup = new ToggleButtonGroup();

    /**
     * A list of all drawing tools that are applicable when drawing masks.
     */
    private ToggleButtonGroup mToolsInMaskGroup = new ToggleButtonGroup();

    /**
     * Constructs a new DrawOptionsView.
     * 
     * @param context
     *            The context to construct in.
     */
    public DrawOptionsView(final Context context) {
        super(context);
        this.createAndAddPanButton();

        this.mLayout = new LinearLayout(context);
        this.mInnerView = new HorizontalScrollView(context);
        this.mInnerView.addView(this.mLayout);
        this.addView(this.mInnerView);

        this.createAndAddSeperator();
        this.mMaskButton = this.createAndAddMaskButton();
        this.createAndAddSeperator();

        this.createAndAddEraserButton();
        this.createAndAddStraightLineButton();
        this.createAndAddFreehandLineButton();
        this.createAndAddRectangleButton();
        this.createAndAddCircleButton();
        this.createAndAddTextButton();

        this.mBackgroundImageButton = this.createAndAddBackgroundImageButton();
        this.createAndAddSeperator();
        
        this.createAndAddMoveTokenButton();
        this.createAndAddSeperator();

        this.addStrokeWidthButton(PENCIL_STROKE_WIDTH, R.drawable.pencil);
        this.addStrokeWidthButton(PEN_STROKE_WIDTH, R.drawable.pen);
        this.addStrokeWidthButton(PAINTBRUSH_STROKE_WIDTH,
                R.drawable.paintbrush);
        this.addStrokeWidthButton(INKTUBE_STROKE_WIDTH, R.drawable.inktube);
        this.createAndAddFillButton();

        this.createAndAddSeperator();

        for (int color : Util.getStandardColorPalette()) {
            this.addColorButton(color);
        }
    }

    /**
     * Adds a button that selects the given color as the current color.
     * 
     * @param color
     *            The color that this button will select.
     */
    private void addColorButton(final int color) {
        ImageToggleButton b = new ImageToggleButton(this.getContext());
        b.setOnClickListener(new ColorListener(color));
        b.setLayoutParams(new LinearLayout.LayoutParams(
                (int) (COLOR_BUTTON_SIZE * this.getResources()
                        .getDisplayMetrics().density),
                (int) (COLOR_BUTTON_SIZE * this.getResources()
                        .getDisplayMetrics().density)));
        Drawable pencil =
                this.getContext().getResources()
                        .getDrawable(R.drawable.pencilbw);
        b.setImageDrawable(pencil);
        b.setColorFilter(new PorterDuffColorFilter(color,
                PorterDuff.Mode.LIGHTEN));
        this.mLayout.addView(b);
        this.mColorGroup.add(b);
    }

    /**
     * Adds a button that changes the stroke width to the given width, and that
     * uses the given resource ID to represent its self.
     * 
     * @param f
     *            The width that this button will change the stroke width to.
     * @param resourceId
     *            ID of the image to draw on this button.
     */
    private void addStrokeWidthButton(final float f, final int resourceId) {
        ImageToggleButton b = new ImageToggleButton(this.getContext());
        b.setImageResource(resourceId);
        b.setOnClickListener(new StrokeWidthListener(f));
        this.mLayout.addView(b);
        this.mLineWidthGroup.add(b);
    }

    /**
     * Creates a button to enter background image mode.
     * 
     * @return The created button.
     */
    private ImageToggleButton createAndAddBackgroundImageButton() {
        final ImageToggleButton b = new ImageToggleButton(this.getContext());
        b.setImageResource(R.drawable.add_image);
        this.mLayout.addView(b);
        this.mToolsGroup.add(b);
        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                DrawOptionsView.this.mLineWidthGroup
                        .setGroupVisibility(View.GONE);
                DrawOptionsView.this.mColorGroup.setGroupVisibility(View.GONE);
                DrawOptionsView.this.mToolsGroup.untoggle();
                b.setToggled(true);
                DrawOptionsView.this.mOnChangeDrawToolListener
                        .onChooseImageTool();
            }
        });
        return b;
    }

    /**
     * Creates a button to switch to draw circle mode and adds it to the view.
     */
    protected void createAndAddCircleButton() {
        final ImageToggleButton button =
                new ImageToggleButton(this.getContext());
        button.setImageResource(R.drawable.circle);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                DrawOptionsView.this.mOnChangeDrawToolListener
                        .onChooseCircleTool();
                DrawOptionsView.this.mToolsGroup.untoggle();
                DrawOptionsView.this.mColorGroup
                        .setGroupVisibility(View.VISIBLE);
                DrawOptionsView.this.mLineWidthGroup
                        .setGroupVisibility(View.VISIBLE);
                button.setToggled(true);
                DrawOptionsView.this.mLineWidthGroup.maybeSelectDefault();
                DrawOptionsView.this.mColorGroup.maybeSelectDefault();
            }
        });
        this.mLayout.addView(button);
        this.mToolsGroup.add(button);
    }

    /**
     * Creates the eraser button and adds it to the view.
     */
    protected void createAndAddEraserButton() {
        final ImageToggleButton eraserButton =
                new ImageToggleButton(this.getContext());
        eraserButton.setImageResource(R.drawable.eraser);
        eraserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                DrawOptionsView.this.mToolsGroup.untoggle();
                if (DrawOptionsView.this.mMaskButton.isToggled()) {
                    DrawOptionsView.this.mOnChangeDrawToolListener
                            .onChooseMaskEraser();
                } else {
                    DrawOptionsView.this.mOnChangeDrawToolListener
                            .onChooseEraser();
                    DrawOptionsView.this.mColorGroup
                            .setGroupVisibility(View.GONE);
                    DrawOptionsView.this.mLineWidthGroup
                            .setGroupVisibility(View.GONE);
                }
                eraserButton.setToggled(true);

            }
        });
        this.mLayout.addView(eraserButton);
        this.mToolsGroup.add(eraserButton);
        this.mToolsInMaskGroup.add(eraserButton);
    }

    /**
	 *
	 */
    protected void createAndAddFillButton() {
        ImageToggleButton b = new ImageToggleButton(this.getContext());
        b.setImageResource(R.drawable.freehand_shape);
        b.setOnClickListener(new StrokeWidthListener(Float.POSITIVE_INFINITY));
        this.mLayout.addView(b);
        this.mLineWidthGroup.add(b);
        this.mLineWidthRegionGroup.add(b);
    }

    /**
     * Creates a button to switch to freehand drawing and adds it to the view.
     */
    protected void createAndAddFreehandLineButton() {
        final ImageToggleButton button =
                new ImageToggleButton(this.getContext());
        button.setImageResource(R.drawable.line_freehand);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                DrawOptionsView.this.mToolsGroup.untoggle();
                if (DrawOptionsView.this.mMaskButton.isToggled()) {
                    DrawOptionsView.this.mOnChangeDrawToolListener
                            .onChooseMaskTool();
                } else {
                    DrawOptionsView.this.mOnChangeDrawToolListener
                            .onChooseFreeHandTool();
                    DrawOptionsView.this.mColorGroup
                            .setGroupVisibility(View.VISIBLE);
                    DrawOptionsView.this.mLineWidthGroup
                            .setGroupVisibility(View.VISIBLE);
                    DrawOptionsView.this.mLineWidthGroup.maybeSelectDefault();
                    DrawOptionsView.this.mColorGroup.maybeSelectDefault();
                }
                button.setToggled(true);
            }
        });
        this.mLayout.addView(button);
        this.mToolsGroup.add(button);
        this.mToolsInMaskGroup.add(button);
    }

    /**
     * Creates a button to activate the mask tool and adds it to the layout.
     * 
     * @return The button to activate the mask tool.
     */
    private ImageToggleButton createAndAddMaskButton() {
        final ImageToggleButton maskButton =
                new ImageToggleButton(this.getContext());
        maskButton.setImageResource(R.drawable.mask);
        maskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View arg0) {
                // mOnChangeDrawToolListener.onChooseMaskTool();

                maskButton.setToggled(!maskButton.isToggled());

                if (maskButton.isToggled()) {
                    // Make sure only mask tools are visible.
                    DrawOptionsView.this.mToolsGroup
                            .setGroupVisibility(View.GONE);
                    DrawOptionsView.this.mToolsInMaskGroup
                            .setGroupVisibility(View.VISIBLE);

                    DrawOptionsView.this.mColorGroup
                            .setGroupVisibility(View.GONE);
                    DrawOptionsView.this.mLineWidthGroup
                            .setGroupVisibility(View.GONE);

                    DrawOptionsView.this.mToolsGroup.maybeSelectDefault();
                    DrawOptionsView.this.mOnChangeDrawToolListener
                            .onChangeMaskEditing(true);
                } else {
                    DrawOptionsView.this.returnToNonMaskState();
                }
            }
        });
        this.mLayout.addView(maskButton);
        return maskButton;
    }

    /**
     * Creates the pan button, adds it to the view, and sets it as the default.
     */
    protected void createAndAddPanButton() {
        final ImageToggleButton panButton =
                new ImageToggleButton(this.getContext());
        panButton.setImageResource(R.drawable.transform_move);
        panButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                DrawOptionsView.this.mOnChangeDrawToolListener
                        .onChoosePanTool();
                DrawOptionsView.this.mToolsGroup.untoggle();
                DrawOptionsView.this.mColorGroup.setGroupVisibility(View.GONE);
                DrawOptionsView.this.mLineWidthGroup
                        .setGroupVisibility(View.GONE);
                panButton.setToggled(true);
            }
        });
        this.addView(panButton);
        this.mToolsGroup.add(panButton);
        this.mToolsInMaskGroup.add(panButton);
    }

    /**
     * Creates a button to switch to draw rectangle mode and adds it to the
     * view.
     */
    protected void createAndAddRectangleButton() {
        final ImageToggleButton button =
                new ImageToggleButton(this.getContext());
        button.setImageResource(R.drawable.rectangle);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                DrawOptionsView.this.mOnChangeDrawToolListener
                        .onChooseRectangleTool();
                DrawOptionsView.this.mToolsGroup.untoggle();
                DrawOptionsView.this.mColorGroup
                        .setGroupVisibility(View.VISIBLE);
                DrawOptionsView.this.mLineWidthGroup
                        .setGroupVisibility(View.VISIBLE);
                button.setToggled(true);
                DrawOptionsView.this.mLineWidthGroup.maybeSelectDefault();
                DrawOptionsView.this.mColorGroup.maybeSelectDefault();
            }
        });
        this.mLayout.addView(button);
        this.mToolsGroup.add(button);
    }

    /**
	 *
	 */
    protected void createAndAddSeperator() {
        ImageView seperator = new ImageView(this.getContext());
        seperator.setLayoutParams(new LinearLayout.LayoutParams(
                SEPERATOR_WIDTH, LinearLayout.LayoutParams.MATCH_PARENT));
        this.mLayout.addView(seperator);
    }

    /**
     * Creates a button to switch to straight line drawing and adds it to the
     * view.
     */
    protected void createAndAddStraightLineButton() {
        final ImageToggleButton button =
                new ImageToggleButton(this.getContext());
        button.setImageResource(R.drawable.line_straight);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                DrawOptionsView.this.mOnChangeDrawToolListener
                        .onChooseStraightLineTool();
                DrawOptionsView.this.mToolsGroup.untoggle();
                DrawOptionsView.this.mColorGroup
                        .setGroupVisibility(View.VISIBLE);
                DrawOptionsView.this.mLineWidthGroup
                        .setGroupVisibility(View.VISIBLE);
                DrawOptionsView.this.mLineWidthRegionGroup
                        .setGroupVisibility(View.GONE);
                DrawOptionsView.this.mLineWidthGroup.maybeSelectDefault();
                DrawOptionsView.this.mColorGroup.maybeSelectDefault();
                button.setToggled(true);
            }
        });
        this.mLayout.addView(button);
        this.mToolsGroup.add(button);
    }

    /**
     * Creates a button to switch to draw text tool and adds it to the view.
     */
    protected void createAndAddTextButton() {
        final ImageToggleButton button =
                new ImageToggleButton(this.getContext());

        button.setImageResource(R.drawable.draw_text);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                DrawOptionsView.this.mOnChangeDrawToolListener
                        .onChooseTextTool();
                DrawOptionsView.this.mToolsGroup.untoggle();
                DrawOptionsView.this.mColorGroup
                        .setGroupVisibility(View.VISIBLE);
                DrawOptionsView.this.mLineWidthGroup
                        .setGroupVisibility(View.GONE);
                button.setToggled(true);
                DrawOptionsView.this.mLineWidthGroup.maybeSelectDefault();
                DrawOptionsView.this.mColorGroup.maybeSelectDefault();
            }
        });
        this.mLayout.addView(button);
        this.mToolsGroup.add(button);
    }

    /**
     * Creates a button to switch to the move token tool, and adds it to the view.
     */
    protected void createAndAddMoveTokenButton() {
        final ImageToggleButton button =
                new ImageToggleButton(this.getContext());

        button.setImageResource(R.drawable.move_token);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                DrawOptionsView.this.mOnChangeDrawToolListener
                        .onChooseMoveTokenTool();
                DrawOptionsView.this.mToolsGroup.untoggle();
                DrawOptionsView.this.mColorGroup
                        .setGroupVisibility(View.GONE);
                DrawOptionsView.this.mLineWidthGroup
                        .setGroupVisibility(View.GONE);
                button.setToggled(true);
                DrawOptionsView.this.mLineWidthGroup.maybeSelectDefault();
                DrawOptionsView.this.mColorGroup.maybeSelectDefault();
            }
        });
        this.mLayout.addView(button);
        this.mToolsGroup.add(button);
    }
    
    void returnToNonMaskState() {
        // Return to non-mask state.
        this.mToolsGroup.setGroupVisibility(View.VISIBLE);
        this.mToolsGroup.maybeSelectDefault();
        this.mOnChangeDrawToolListener.onChangeMaskEditing(false);
    }

    /**
     * Sets whether the image toggle button was visible.
     * 
     * @param visible
     *            Whether the button is visible.
     */
    public void setBackgroundImageButtonVisibility(boolean visible) {
        this.mBackgroundImageButton.setVisibility(visible
                ? View.VISIBLE
                : View.GONE);
        if (!visible && this.mBackgroundImageButton.isToggled()) {
            this.mToolsGroup.forceDefault();
        }
    }

    /**
     * Automatically loads the default tool. If a tool is already selected,
     * re-selects it.
     */
    public void setDefault() {
        // Start out with the pan button selected.
        this.mToolsGroup.maybeSelectDefault();
    }

    /**
     * Sets whether the mask tool should be visible.
     * 
     * @param visible
     *            True if visible.
     */
    public void setMaskToolVisibility(final boolean visible) {
        this.mMaskButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            // If mask tool was selected, we need to de-select it.
            if (this.mMaskButton.isToggled()) {
                this.mMaskButton.setToggled(false);
                this.returnToNonMaskState();
            }
        }
    }

    /**
     * Sets the listener to call when a new draw tool is selected.
     * 
     * @param listener
     *            The new listener
     */
    public void setOnChangeDrawToolListener(
            final OnChangeDrawToolListener listener) {
        this.mOnChangeDrawToolListener = listener;
    }
    
    public boolean isMaskToolSelected() {
    	return mMaskButton != null && mMaskButton.isToggled();
    }

    /**
     * OnClickListener for when a button representing a color is clicked.
     * 
     * @author Tim Bocek
     */
    private class ColorListener implements View.OnClickListener {
        /**
         * The color that this listener will pick when fired.
         */
        private int mColor;

        /**
         * Constructor.
         * 
         * @param color
         *            The color that the listener will pick when fired.
         */
        public ColorListener(final int color) {
            this.mColor = color;
        }

        @Override
        public void onClick(final View v) {
            DrawOptionsView.this.mOnChangeDrawToolListener
                    .onChooseColoredPen(this.mColor);
            DrawOptionsView.this.mColorGroup.untoggle();
            ((ImageToggleButton) v).setToggled(true);
        }			// TODO Auto-generated method stub
		
		
    }

    /**
     * Using the null class pattern; this implementation of
     * OnChangeDrawToolListener should do nothing. It is only here to avoid null
     * checkes whenever one of these methods is called.
     * 
     * @author Tim
     * 
     */
    public static class NullChangeDrawToolListener implements
            OnChangeDrawToolListener {
        @Override
        public void onChangeMaskEditing(boolean editingMask) {
        }

        @Override
        public void onChooseCircleTool() {
        }

        @Override
        public void onChooseColoredPen(final int color) {
        }

        @Override
        public void onChooseEraser() {
        }

        @Override
        public void onChooseFreeHandTool() {
        }

        @Override
        public void onChooseImageTool() {
        }

        @Override
        public void onChooseMaskEraser() {
        }

        @Override
        public void onChooseMaskTool() {
        }

        @Override
        public void onChoosePanTool() {
        }

        @Override
        public void onChooseRectangleTool() {
        }			// TODO Auto-generated method stub
		
		

        @Override
        public void onChooseStraightLineTool() {
        }

        @Override
        public void onChooseStrokeWidth(final float width) {
        }

        @Override
        public void onChooseTextTool() {
        }

		@Override
		public void onChooseMoveTokenTool() {
		}
    }

    /**
     * Listener that is called when different drawing tools are chosen.
     * 
     * @author Tim Bocek
     */
    public interface OnChangeDrawToolListener {
        void onChangeMaskEditing(boolean editingMask);

        void onChooseMoveTokenTool();

		/**
         * Called when the circle draw tool is chosen.
         */
        void onChooseCircleTool();

        /**
         * Fired when the color is changed.
         * 
         * @param color
         *            The new color.
         */
        void onChooseColoredPen(int color);

        /**
         * Fired when the eraser tool is selected.
         */
        void onChooseEraser();

        /**
         * Called when the freehand draw tool is chosen.
         */
        void onChooseFreeHandTool();

        /**
         * Fired when the image tool is selected.
         */
        void onChooseImageTool();

        void onChooseMaskEraser();

        /**
         * Fired when the mask took is selected.
         */
        void onChooseMaskTool();

        /**
         * Fired when the pan tool is selected.
         */
        void onChoosePanTool();

        /**
         * Called when the rectangle draw tool is chosen.
         */
        void onChooseRectangleTool();

        /**
         * Called when the straight line draw tool is chosen.
         */
        void onChooseStraightLineTool();

        /**
         * Fired when the stroke width is changed. This can also be thought of
         * as selecting a pen tool with the given stroke width.
         * 
         * @param width
         *            The new stroke width.
         */
        void onChooseStrokeWidth(float width);

        /**
         * Called when the text draw tool is chosen.
         */
        void onChooseTextTool();
    }

    /**
     * OnClickListener for when a button representing a drawing width is
     * clicked.
     * 
     * @author Tim Bocek
     */
    private class StrokeWidthListener implements View.OnClickListener {
        /**
         * The line width that the drawing tool will be changed to when this
         * listener fires.
         */
        private float mWidth;

        /**
         * Constructor.
         * 
         * @param f
         *            The line width that will be used when the listener fires.
         */
        public StrokeWidthListener(final float f) {
            this.mWidth = f;
        }

        @Override
        public void onClick(final View v) {
            DrawOptionsView.this.mOnChangeDrawToolListener
                    .onChooseStrokeWidth(this.mWidth);
            DrawOptionsView.this.mLineWidthGroup.untoggle();
            ((ImageToggleButton) v).setToggled(true);
            DrawOptionsView.this.mColorGroup.setGroupVisibility(View.VISIBLE);
        }
    }
}
