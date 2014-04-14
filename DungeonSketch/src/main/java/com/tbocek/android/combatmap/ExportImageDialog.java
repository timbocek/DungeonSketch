package com.tbocek.android.combatmap;

import java.io.IOException;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.tbocek.android.combatmap.model.MapData;
import com.tbocek.android.combatmap.model.MapDrawer;
import com.tbocek.android.combatmap.model.MapDrawer.FogOfWarMode;
import com.tbocek.dungeonsketch.R;

/**
 * Provides a dialog for the user to export an image.
 * 
 * @author Tim
 * 
 */
public class ExportImageDialog extends Dialog {

    CheckBox mCheckAnnotations;
    CheckBox mCheckFogOfWar;
    CheckBox mCheckGmNotes;
    CheckBox mCheckGridLines;
    CheckBox mCheckTokens;
    private MapData mData;
    EditText mEditExportName;
    Button mExportButton;
    private int mExportHeight;

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

        this.associateControl(this.mRadioExportFullMap, "export_full_map", true);
        this.associateControl(this.mRadioExportCurrentView,
                "export_current_view", false);
        this.associateControl(this.mCheckGridLines, "export_grid_lines", true);
        this.associateControl(this.mCheckGmNotes, "export_gm_notes", false);
        this.associateControl(this.mCheckTokens, "export_tokens", true);
        this.associateControl(this.mCheckAnnotations, "export_annotations",
                false);
        this.associateControl(this.mCheckFogOfWar, "export_fog_of_war", false);

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
                                            + e.toString(), Toast.LENGTH_LONG);
                    toast.show();
                }
                ExportImageDialog.this.dismiss();
            }
        });
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

    /**
     * Exports the image using the settings set up in this activity.
     * @throws IOException if the export failed.
     */
    private void export() throws IOException {
        int width;
        int height;

        RectF wholeMapRect = this.mData.getScreenSpaceBoundingRect(30);
        if (this.mRadioExportCurrentView.isChecked()) {
            width = this.mExportWidth;
            height = this.mExportHeight;
        } else {
            width = (int) wholeMapRect.width();
            height = (int) wholeMapRect.height();
        }
        Bitmap bitmap =
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        if (!this.mRadioExportCurrentView.isChecked()) {
            this.mData.getWorldSpaceTransformer().moveOrigin(
                    -wholeMapRect.left, -wholeMapRect.top);
        }

        new MapDrawer()
                .drawGridLines(this.mCheckGridLines.isChecked())
                .drawGmNotes(this.mCheckGmNotes.isChecked())
                .drawTokens(this.mCheckTokens.isChecked())
                .areTokensManipulable(true)
                .drawAnnotations(this.mCheckAnnotations.isChecked())
                .gmNotesFogOfWar(FogOfWarMode.NOTHING)
                .backgroundFogOfWar(
                        this.mCheckFogOfWar.isChecked() ? FogOfWarMode.CLIP
                                : FogOfWarMode.NOTHING)
                .draw(canvas, this.mData, canvas.getClipBounds());

        new DataManager(this.getContext()).exportImage(this.mEditExportName
                .getText().toString(), bitmap, Bitmap.CompressFormat.PNG);

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
}
