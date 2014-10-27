package com.tbocek.android.combatmap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.tbocek.android.combatmap.model.GridColorScheme;
import com.tbocek.android.combatmap.model.HexGridStrategy;
import com.tbocek.android.combatmap.model.MapData;
import com.tbocek.android.combatmap.model.RectangularGridStrategy;
import com.tbocek.android.combatmap.model.primitives.Units;
import com.tbocek.android.combatmap.view.ImageToggleButton;
import com.tbocek.android.combatmap.view.ToggleButtonGroup;
import com.tbocek.dungeonsketch.R;

import net.margaritov.preference.colorpicker.ColorPickerDialog;

/**
 * A dialog that allows the user to edit the grid geometry and color.
 * 
 * @author Tim
 * 
 */
public class GridPropertiesDialog extends Dialog {

    /**
     * Size of the preview bitmaps on each edge.
     */
    private static final int PREVIEW_BITMAP_SIZE_DP = 30;

    /**
     * Button that previews and allows the user to change the background color.
     */
    private final ImageButton mBackgroundColor;
    private ArrayAdapter<CharSequence> mUnitAdapter;

    /**
     * The map data whose grid properties are being edited.
     */
    private MapData mData;

    /**
     * Button that previews and allows the user to change the foreground color.
     */
    private final ImageButton mForegroundColor;

    /**
     * ToggleButtonGroup that ensures that the rect/hex grid buttons behave like
     * a set of radio buttons (i.e. only one is toggled "on" at a time).
     */
    private final ToggleButtonGroup mGridTypeToggles = new ToggleButtonGroup();

    /**
     * Button used to switch to a hexagonal grid geometry.
     */
    private final ImageToggleButton mHexGridButton;
    /**
     * Color picker dialog to open when we want to select a color directly.
     */
    private ColorPickerDialog mPickerDialog;

    /**
     * ArrayAdapter of grid theme presets.
     */
    private final ArrayAdapter<CharSequence> mPresetAdapter;

    /**
     * Listener to call when the grid properties change.
     */
    private PropertiesChangedListener mPropertyListener;

    /**
     * Button used to switch to a rectangular grid geometry.
     */
    private final ImageToggleButton mRectGridButton;

    private TextView mScaleText;

    private Spinner mUnitSpinner;

    /**
     * Constructor.
     * 
     * @param context
     *            The application context this dialog is constructed in.
     */
    public GridPropertiesDialog(Context context) {
        super(context);
        this.setTitle("Grid Properties");
        this.setContentView(R.layout.grid_background_properties);
        this.mForegroundColor =
                (ImageButton) this.findViewById(R.id.button_foreground_color);
        this.mBackgroundColor =
                (ImageButton) this.findViewById(R.id.button_background_color);
        this.mHexGridButton =
                (ImageToggleButton) this
                        .findViewById(R.id.button_toggle_hex_grid);
        this.mRectGridButton =
                (ImageToggleButton) this
                        .findViewById(R.id.button_toggle_rect_grid);
        this.mUnitSpinner = (Spinner) this.findViewById(R.id.background_props_square_unit);
        this.mScaleText = (TextView) this.findViewById(R.id.background_props_square_size);

        this.mGridTypeToggles.add(this.mHexGridButton);
        this.mGridTypeToggles.add(this.mRectGridButton);

        this.mUnitAdapter = ArrayAdapter.createFromResource(
                this.getContext(),
                R.array.units, android.R.layout.simple_spinner_item);
        this.mUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mUnitSpinner.setAdapter(this.mUnitAdapter);


        this.mHexGridButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                GridPropertiesDialog.this.hexGridButtonClick();

            }

        });
        this.mRectGridButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                GridPropertiesDialog.this.rectGridButtonClick();
            }

        });
        this.mForegroundColor.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                GridPropertiesDialog.this.foregroundColorClick();
            }

        });
        this.mBackgroundColor.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                GridPropertiesDialog.this.backgroundColorClick();
            }

        });
        this.mScaleText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                GridPropertiesDialog.this.scaleTextChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        this.mUnitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GridPropertiesDialog.this.unitTextChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        this.mPresetAdapter =
                new GridPropertiesDialog.MapThemeArrayAdapter(this.getContext());

        Button themePresetButton = (Button) this.findViewById(R.id.button_theme_presets);
        themePresetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GridPropertiesDialog.this.presetButtonClick();
            }
        });

        Button resizeGridButton = (Button) this.findViewById(R.id.button_resize_grid);
        resizeGridButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GridPropertiesDialog.this.dismiss();
                if (mPropertyListener != null) {
                    mPropertyListener.onResizeGridRequested();
                }
            }
        });
    }

    /**
     * Opens a color picker dialog to select the background color.
     */
    void backgroundColorClick() {
        this.mPickerDialog =
                new ColorPickerDialog(this.getContext(), this.mData.getGrid()
                        .getColorScheme().getBackgroundColor());
        this.mPickerDialog
                .setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {

                    @Override
                    public void onColorChanged(int color) {
                        GridPropertiesDialog.this.mBackgroundColor
                                .setImageBitmap(GridPropertiesDialog.this
                                        .createColorPreviewBitmap(color));
                        GridPropertiesDialog.this.mData
                                .getGrid()
                                .setColorScheme(
                                        new GridColorScheme(color,
                                                GridPropertiesDialog.this.mData
                                                        .getGrid()
                                                        .getColorScheme()
                                                        .getLineColor(), false));
                        GridPropertiesDialog.this.mPickerDialog.dismiss();
                        GridPropertiesDialog.this.propertiesChanged();
                    }
                });
        this.mPickerDialog.show();
    }

    /**
     * Sets color theme in the map data to the given theme. Also updates the
     * color swatches in the dialog.
     * 
     * @param colorTheme
     *            The new theme.
     */
    private void colorThemeSelected(String colorTheme) {
        GridColorScheme scheme = GridColorScheme.fromNamedScheme(colorTheme);
        this.mForegroundColor.setImageBitmap(this
                .createColorPreviewBitmap(scheme.getLineColor()));
        this.mBackgroundColor.setImageBitmap(this
                .createColorPreviewBitmap(scheme.getBackgroundColor()));
        this.mData.getGrid().setColorScheme(scheme);
        this.propertiesChanged();
    }

    /**
     * Generates a bitmap previewing the given color. The bitmap will be a solid
     * color with a grey border.
     * 
     * 
     * @param color
     *            The color to preview.
     * @return The generated bitmap.
     */
    private Bitmap createColorPreviewBitmap(int color) {
        int d = (int) Units.dpToPx(PREVIEW_BITMAP_SIZE_DP);
        Bitmap bm = Bitmap.createBitmap(d, d, Config.ARGB_8888);
        int w = bm.getWidth();
        int h = bm.getHeight();
        for (int i = 0; i < w; i++) {
            for (int j = i; j < h; j++) {
                int c =
                        (i <= 1 || j <= 1 || i >= w - 2 || j >= h - 2)
                                ? Color.GRAY
                                : color;
                bm.setPixel(i, j, c);
                if (i != j) {
                    bm.setPixel(j, i, c);
                }
            }
        }

        return bm;
    }

    /**
     * Opens a color picker dialog to select the grid line color.
     */
    void foregroundColorClick() {
        this.mPickerDialog =
                new ColorPickerDialog(this.getContext(), this.mData.getGrid()
                        .getColorScheme().getLineColor());
        this.mPickerDialog
                .setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {

                    @Override
                    public void onColorChanged(int color) {
                        GridPropertiesDialog.this.mForegroundColor
                                .setImageBitmap(GridPropertiesDialog.this
                                        .createColorPreviewBitmap(color));
                        GridPropertiesDialog.this.mData.getGrid()
                                .setColorScheme(
                                        new GridColorScheme(
                                                GridPropertiesDialog.this.mData
                                                        .getGrid()
                                                        .getColorScheme()
                                                        .getBackgroundColor(),
                                                color, false));
                        GridPropertiesDialog.this.mPickerDialog.dismiss();
                        GridPropertiesDialog.this.propertiesChanged();
                    }
                });
        this.mPickerDialog.show();
    }

    /**
     * Sets the grid type to hex.
     */
    private void hexGridButtonClick() {
        this.mGridTypeToggles.untoggle();
        this.mHexGridButton.setToggled(true);
        this.mData.getGrid().setDrawStrategy(new HexGridStrategy());
        this.propertiesChanged();
    }

    /**
     * Opens the list of map theme presets for selection.
     */
    private void presetButtonClick() {

        new AlertDialog.Builder(this.getContext())
                .setTitle("Grid Theme Presets")
                .setAdapter(this.mPresetAdapter,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                GridPropertiesDialog.this
                                        .colorThemeSelected(GridPropertiesDialog.this.mPresetAdapter
                                                .getItem(which).toString());
                                dialog.dismiss();
                                GridPropertiesDialog.this.propertiesChanged();
                            }
                        }).create().show();
    }

    private void scaleTextChanged() {
        if (mScaleText.getText().length() > 0) {
            this.mData.getGrid().setScale(Float.parseFloat(this.mScaleText.getText().toString()));
        }
    }

    private void unitTextChanged() {
        this.mData.getGrid().setUnits(this.mUnitSpinner.getSelectedItem().toString());
    }

    /**
     * Calls the property changed listener to notify of new grid properties, if
     * one has been specified.
     */
    private void propertiesChanged() {
        if (this.mPropertyListener != null) {
            this.mPropertyListener.onPropertiesChanged();
        }
    }

    /**
     * Sets the grid type to rectangular.
     */
    private void rectGridButtonClick() {
        this.mGridTypeToggles.untoggle();
        this.mRectGridButton.setToggled(true);
        this.mData.getGrid().setDrawStrategy(new RectangularGridStrategy());
        this.propertiesChanged();
    }

    /**
     * Reads the current grid properties from the given map data, and stores
     * that map data instance so that it can be updated with new selections.
     * 
     * @param data
     *            The map data to read/write grid properties to.
     */
    public void setMapData(MapData data) {
        this.mBackgroundColor.setImageBitmap(this.createColorPreviewBitmap(data
                .getGrid().getColorScheme().getBackgroundColor()));
        this.mForegroundColor.setImageBitmap(this.createColorPreviewBitmap(data
                .getGrid().getColorScheme().getLineColor()));
        this.mGridTypeToggles.untoggle();
        if (data.getGrid().getDrawStrategy() instanceof HexGridStrategy) {
            this.mHexGridButton.setToggled(true);
        } else {
            this.mRectGridButton.setToggled(true);
        }
        this.mData = data;
        int currentUnitPosition = this.mUnitAdapter.getPosition(mData.getGrid().getUnits());
        if (currentUnitPosition >= 0) {
            mUnitSpinner.setSelection(currentUnitPosition);
        }
        mScaleText.setText(String.format("%.2f", mData.getGrid().getScale()));
    }

    /**
     * Sets the listener that will be notified when the grid property selections
     * change.
     * 
     * @param propertiesChangedListener
     *            The listener to notify.
     */
    public void setOnPropertiesChangedListener(
            PropertiesChangedListener propertiesChangedListener) {
        this.mPropertyListener = propertiesChangedListener;

    }

    /**
     * Extends ArrayAdapter to be an adapter specific to map themes. Will theme
     * the list items to preview what the map will look like with those colors.
     * 
     * @author Tim
     * 
     */
    private class MapThemeArrayAdapter extends ArrayAdapter<CharSequence> {

        /**
         * The padding to use around the views previewing the different theme
         * options.
         */
        private static final int THEME_PREVIEW_PADDING = 16;
        private static final int THEME_PREVIEW_FONT_SIZE = 12;

        /**
         * Constructor.
         * 
         * @param context
         *            The application context used to construct this array
         *            adapter.
         */
        public MapThemeArrayAdapter(Context context) {
            super(context, R.layout.selection_dialog_text_view, context
                    .getResources().getTextArray(R.array.themeNames));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView v =
                    (TextView) super.getView(position, convertView, parent);
            GridColorScheme scheme =
                    GridColorScheme.fromNamedScheme(v.getText().toString());
            v.setTextColor(scheme.getLineColor());
            v.setBackgroundColor(scheme.getBackgroundColor());

            // With all these different colors, changing the padding works
            // wonders!
            // TODO: DO this in the XML!!!!
            float padding = Units.dpToPx(THEME_PREVIEW_PADDING);
            float font_size = Units.ptToPx(THEME_PREVIEW_FONT_SIZE);
            v.setPadding(0, (int) padding, 0, (int) padding);
            v.setTextSize(font_size);

            return v;
        }

    }

    /**
     * Listener interface so that this dialog can notifiy other UI elements when
     * the options have changed. This allows us to provide a live update of the
     * grid options in the combat view, rather than waiting until the dialog is
     * confirmed.
     * 
     * The new properties can be found by querying the MapData instance being
     * managed by this dialog.
     * 
     * @author Tim
     * 
     */
    public interface PropertiesChangedListener {

        /**
         * Called when a property of the grid has been changed.
         */
        void onPropertiesChanged();

        /**
         * Called when the resize operation has been requested.
         */
        void onResizeGridRequested();
    }

}
