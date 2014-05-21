package com.tbocek.android.combatmap;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
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
    private static final String TAG = "ExportImageDialog";

    final CheckBox mCheckAnnotations;
    final CheckBox mCheckFogOfWar;
    final CheckBox mCheckGmNotes;
    final CheckBox mCheckGridLines;
    final CheckBox mCheckTokens;
    private MapData mData;
    final EditText mEditExportName;
    final Button mExportButton;
    private final TextView mExportSizeText;
    private int mExportHeight;

    private int mExportWidth;
    final RadioButton mRadioExportCurrentView;
    final RadioButton mRadioExportFullMap;

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
        return getExportedImageSize(wholeMap, this.mData);
    }

    private Point getExportedImageSize(boolean wholeMap, MapData data) {
        if (wholeMap) {
            RectF wholeMapRect = data.getScreenSpaceBoundingRect(WHOLE_IMAGE_MARGIN_PX);
            return new Point((int)wholeMapRect.width(), (int)wholeMapRect.height());
        } else {
            return new Point(this.mExportWidth, this.mExportHeight);
        }
    }

    /**
     * Exports the image using the settings set up in this activity.
     * @throws IOException if the export failed.
     */
    private void export() {
        final boolean exportCurrentView = mRadioExportCurrentView.isChecked();
        final boolean gridLines = mCheckGridLines.isChecked();
        final boolean gmNotes = mCheckGmNotes.isChecked();
        final boolean tokens = mCheckTokens.isChecked();
        final boolean annotations = mCheckAnnotations.isChecked();
        final boolean fogOfWar = mCheckFogOfWar.isChecked();
        final Context context = getContext().getApplicationContext();
        final String exportName = this.mEditExportName.getText().toString();
        final MapData data = MapData.getCopy();
        final Point numExportedImages = this.getNumExportImages();

        if (context != null) {
            Toast.makeText(context, "Exporting image", Toast.LENGTH_LONG).show();
        }
        AsyncTask<Void, Void, Boolean> exportImageTask = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                Point exportSize = getExportedImageSize(mRadioExportFullMap.isChecked(), data);
                int width = exportSize.x;
                int height = exportSize.y;
                RectF wholeMapRect = data.getScreenSpaceBoundingRect(WHOLE_IMAGE_MARGIN_PX);

                Bitmap bitmap =
                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                List<CoordinateTransformer> transformers = data.getWorldSpaceTransformer().splitMap(
                        width, height, numExportedImages.x, numExportedImages.y);


                if (exportCurrentView) {
                    data.getWorldSpaceTransformer().moveOrigin(
                            -wholeMapRect.left, -wholeMapRect.top);
                }

                int i = 1;
                for (CoordinateTransformer transformer : transformers) {
                    Log.d(TAG, "Writing image with origin = " + Float.toString(transformer.getOrigin().x) +
                            ", " + Float.toString(transformer.getOrigin().y));
                    new MapDrawer()
                            .drawGridLines(gridLines)
                            .drawGmNotes(gmNotes)
                            .drawTokens(tokens)
                            .areTokensManipulable(true)
                            .drawAnnotations(annotations)
                            .gmNotesFogOfWar(FogOfWarMode.NOTHING)
                            .backgroundFogOfWar(
                                    fogOfWar ? FogOfWarMode.CLIP : FogOfWarMode.NOTHING)
                            .useCustomWorldSpaceTransformer(transformer)
                            .draw(canvas, data, canvas.getClipBounds());

                    String thisExportName = exportName;
                    if (transformers.size() > 1) {
                        thisExportName += "_" + Integer.toString(i);
                    }

                    try {
                        Log.d(TAG, "Exporting to " + thisExportName);
                        new DataManager(context).exportImage(
                                thisExportName, bitmap, Bitmap.CompressFormat.PNG);
                    } catch (IOException e) {
                        Log.d(TAG, "Export image failed", e);
                        return false;
                    }
                    i++;
                }
                return true;
            }

            protected void onPostExecute(Boolean result) {
                if (context != null) {
                    Toast.makeText(
                            context,
                            result ? "Export image successful" : "Export image failed",
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        exportImageTask.execute();
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
        final String mPreference;

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
