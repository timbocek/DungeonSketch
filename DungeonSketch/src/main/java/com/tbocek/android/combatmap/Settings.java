package com.tbocek.android.combatmap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.tbocek.android.combatmap.about.AboutDialog;
import com.tbocek.android.combatmap.about.ArtCredits;
import com.tbocek.dungeonsketch.R;

/**
 * Preferences activity for Dungeon Sketch.
 * 
 * @author Tim Bocek
 * 
 */
// Because there is no PreferenceFragment in the compatibility libraries.
@SuppressWarnings("deprecation")
public final class Settings extends PreferenceActivity {

    /**
     * ID for the about dialog.
     */
    public static final int DIALOG_ID_ABOUT = 0;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.settings);

        // Hook up the about preference
        Preference dialogPref = this.findPreference("about");
        dialogPref
        .setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Settings.this.showDialog(DIALOG_ID_ABOUT);
                return true;
            }

        });

        Preference artCreditPref = this.findPreference("artcredits");
        artCreditPref
        .setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Settings.this.startActivity(new Intent(Settings.this,
                        ArtCredits.class));
                return true;
            }

        });

        Preference migrateDataPref = this.findPreference("migrate_data");
        migrateDataPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
                Settings.this.startActivity(new Intent(Settings.this,
                        ImportDataDialog.class));
                return true;
            }

        });

        Preference restoreDefaultsPref = this.findPreference("restore_all_default_tokens");
        restoreDefaultsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(Settings.this)
                        .setTitle(getString(R.string.confirm_restore_tokens_title))
                        .setMessage(getString(R.string.confirm_restore_tokens_message))
                        .setCancelable(true)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                               TokenDatabase.getInstance(Settings.this.getApplicationContext())
                                       .restoreDefaults(Settings.this.getApplicationContext());
                            }
                        })
                        .show();

                return true;
            }
        });
    }

    @Override
    public Dialog onCreateDialog(final int id) {
        switch (id) {
        case DIALOG_ID_ABOUT:
            return new AboutDialog(this);
        default:
            return null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in action bar clicked; go home
            Intent intent = new Intent(this, CombatMap.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
