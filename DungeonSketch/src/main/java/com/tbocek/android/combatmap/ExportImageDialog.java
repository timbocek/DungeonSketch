package com.tbocek.android.combatmap;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.tbocek.android.combatmap.model.MapData;
import com.tbocek.android.combatmap.model.MapDrawer;
import com.tbocek.android.combatmap.model.MapDrawer.FogOfWarMode;
import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.dungeonsketch.R;

import java.io.IOException;
import java.util.List;

/**
 * Provides a dialog for the user to export an image.
 * 
 * @author Tim
 * 
 */
public class ExportImageDialog extends Dialog {

    private static final int WHOLE_IMAGE_MARGIN_PX = 30;

    private static final int MAX_IMAGE_WIDTH = 2048;
    private static final int MAX_IMAGE_HEIGHT = 2048;

    CheckBox mCheckAnnotations;
    CheckBox mCheckFogOfWar;
    CheckBox mCheckGmNotes;
    CheckBox mCheckGridLines;
    CheckBox mCheckTokens;
    private MapData mData;
    EditText mEditExportName;
    Button mExportButton;
    private TextView mExportSizeText;
    private int mExportHeight;
    private TextView mExportRowsText;
    private TextView mExportColsText;

    private int mExportWidth;
    RadioButton mRadioExportCurrentView;
    RadioButton mRadioExportFullMap;

    /**
     * Constructor.
     * 
     * @param context
     *            Application context to use.
     */
    public ExportImageDialog(Context context) {
        super(context);
        this.setTitle("Export Image");
        this.setContentView(R.layout.export_dialog);

        this.mRadioExportFullMap =
                (RadioButton) this.findViewById(R.id.radio_export_full_map);
        this.mRadioExportCurrentView =
                (RadioButton) this.findViewById(R.id.radio_export_current_view);
        this.mCheckGridLines =
                (CheckBox) this.findViewById(R.id.checkbox_export_grid_lines);
        this.mCheckGmNotes =
                (CheckBox) this.findViewById(R.id.checkbox_export_gm_notes);
        this.mCheckTokens =
                (CheckBox) this.findViewById(R.id.checkbox_export_tokens);
        this.mCheckAnnotations =
                (CheckBox) this.findViewById(R.id.checkbox_export_annotations);
        this.mCheckFogOfWar =
                (CheckBox) this.findViewById(R.id.checkbox_export_fog_of_war);
        this.mEditExportName =
                (EditText) this.findViewById(R.id.edit_export_name);
        this.mExportButton = (Button) this.findViewById(R.id.button_export);

        this.mExportSizeText =
                (TextView) this.findViewById(R.id.text_export_size);

        this.mExportRowsText =
                (TextView) this.findViewById(R.id.text_export_rows_advisory);
        this.mExportColsText =
                (TextView) this.findViewById(R.id.text_export_cols_advisory);

        this.associateControl(this.mRadioExportFullMap, "export_full_map", true);
        this.associateControl(this.mRadioExportCurrentView,
                "export_current_view", false);
        this.associateControl(this.mCheckGridLines, "export_grid_lines", true);
        this.associateControl(this.mCheckGmNotes, "export_gm_notes", false);
        this.associateControl(this.mCheckTokens, "export_tokens", true);
        this.associateControl(this.mCheckAnnotations, "export_annotations",
                false);
        this.associateControl(this.mCheckFogOfWar, "export_fog_of_war", false);

        this.mRadioExportFullMap.setOnCheckedChangeListener(
                new RadioButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        updateExportSizeText(checked);
                    }
                }
        );

        this.mExportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ExportImageDialog.this.export();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast toast =
                            Toast.makeText(
                                    ExportImageDialog.this.getContext(),
                                    "Could not export.  Reason: "
                                            + e.toString(), Toast.LENGTH_LONG
                            );
                    toast.show();
                }
                ExportImageDialog.this.dismiss();
            }
        });
    }

    private void updateExportSizeText(boolean wholeMap) {
        findViewById(R.id.text_export_size_advisory).setVisibility(
                wholeMap ? View.VISIBLE : View.GONE);

        Point exportSize = getExportedImageSize(wholeMap);
        mExportSizeText.setText(
                "Exported image will be " + Integer.toString(exportSize.x) +
                        " x " + Integer.toString(exportSize.y));

        Point numExportedImages = getNumExportImages();

        if (numExportedImages.x > 1) {
            mExportRowsText.setVisibility(View.VISIBLE);
            mExportRowsText.setText(
                    "Image will be split into " + Integer.toString(numExportedImages.x) + " rows");
        } else {
            mExportRowsText.setVisibility(View.GONE);
        }

        if (numExportedImages.y > 1) {
            mExportColsText.setVisibility(View.VISIBLE);
            mExportColsText.setText(
                    "Image will be split into " + Integer.toString(numExportedImages.y) +
                            " columns");
        } else {
            mExportColsText.setVisibility(View.GONE);
        }
    }

    /**
     * Associates the given control with the the given preference such that
     * modifying the control modifies the preference.
     * @param b The CompoundButton to associate with a preference.
     * @param pref Name of the preference to associate the control with.  Must
     *     be a Boolean value (not enforced in this method!)
     * @param defaultValue The default value to use if the preference has no
     *     setting yet.
     */
    private void associateControl(CompoundButton b, String pref,
            boolean defaultValue) {
        SharedPreferences prefs =
                PreferenceManager
                        .getDefaultSharedPreferences(this.getContext());
        b.setChecked(prefs.getBoolean(pref, defaultValue));
        b.setOnCheckedChangeListener(new SetBooleanPreferenceHandler(pref));
    }

    private Point getExportedImageSize(boolean wholeMap) {
        if (wholeMap) {
            RectF wholeMapRect = this.mData.getScreenSpaceBoundingRect(WHOLE_IMAGE_MARGIN_PX);
            return new Point((int)wholeMapRect.width(), (int)wholeMapRect.height());
        } else {
            return new Point(this.mExportWidth, this.mExportHeight);
        }
    }

    /**
     * Exports the image using the settings set up in this activity.
     * @throws IOException if the export failed.
     */
    private void export() throws IOException {
        Point exportSize = getExportedImageSize(mRadioExportFullMap.isChecked());
        int width = exportSize.x;
        int height = exportSize.y;
        RectF wholeMapRect = this.mData.getScreenSpaceBoundingRect(WHOLE_IMAGE_MARGIN_PX);

        if (!this.mRadioExportCurrentView.isChecked()) {
            this.mData.getWorldSpaceTransformer().moveOrigin(
                    -wholeMapRect.left, -wholeMapRect.top);
        }

        Point numExportedImages = this.getNumExportImages();

        List<CoordinateTransformer> transformers = mData.getWorldSpaceTransformer().splitMap(
                width, height, numExportedImages.x, numExportedImages.y);

        Bitmap bitmap =
                Bitmap.createBitmap(width / numExportedImages.x, height / numExportedImages.y,
                        Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        int i = 1;
        for (CoordinateTransformer transformer : transformers) {
            new MapDrawer()
                    .drawGridLines(this.mCheckGridLines.isChecked())
                    .drawGmNotes(this.mCheckGmNotes.isChecked())
                    .drawTokens(this.mCheckTokens.isChecked())
                    .areTokensManipulable(true)
                    .drawAnnotations(this.mCheckAnnotations.isChecked())
                    .gmNotesFogOfWar(FogOfWarMode.NOTHING)
                    .backgroundFogOfWar(
                            this.mCheckFogOfWar.isChecked() ? FogOfWarMode.CLIP
                                    : FogOfWarMode.NOTHING
                    )
                    .useCustomWorldSpaceTransformer(transformer)
                    .draw(canvas, this.mData, canvas.getClipBounds());

            String exportName = this.mEditExportName.getText().toString();
            if (transformers.size() > 1) {
                exportName += "_" + Integer.toString(i);
            }

            new DataManager(this.getContext()).exportImage(
                    exportName, bitmap, Bitmap.CompressFormat.PNG);
            i++;
        }

        if (!this.mRadioExportCurrentView.isChecked()) {
            this.mData.getWorldSpaceTransformer().moveOrigin(wholeMapRect.left,
                    wholeMapRect.top);
        }
    }

    public void prepare(String name, MapData mapData, int width, int height) {
        this.mEditExportName.setText(name);
        this.mData = mapData;
        this.mExportWidth = width;
        this.mExportHeight = height;

        updateExportSizeText(mRadioExportFullMap.isChecked());
    }

    private class SetBooleanPreferenceHandler implements
            CompoundButton.OnCheckedChangeListener {
        String mPreference;

        public SetBooleanPreferenceHandler(String preference) {
            this.mPreference = preference;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            SharedPreferences sharedPreferences =
                    PreferenceManager
                            .getDefaultSharedPreferences(ExportImageDialog.this
                                    .getContext());
            Editor editor = sharedPreferences.edit();
            editor.putBoolean(this.mPreference, isChecked);
            editor.commit();
        }
    }

    private Point getNumExportImages() {
        Point exportSize = getExportedImageSize(mRadioExportFullMap.isChecked());

        return new Point(
                this.roundUp(((float) exportSize.x) / MAX_IMAGE_WIDTH),
                this.roundUp(((float) exportSize.y) / MAX_IMAGE_HEIGHT));
    }

    /**
     * Rounds up to the nearest integer.
     * @param x Value to round.
     * @return The smallest integer greater than X.
     */
    private int roundUp(float x) {
        // Math.ceil returns a float; add 0.5 before truncating to an int in case the float is
        // equal to something like 1.999999999999998
        return (int)(Math.ceil(x) + 0.5);
    }
}
