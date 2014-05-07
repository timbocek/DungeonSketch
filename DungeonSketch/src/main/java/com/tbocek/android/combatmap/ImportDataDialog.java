package com.tbocek.android.combatmap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;

import com.tbocek.dungeonsketch.R;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

/**
 * Dialog that allows importing data from other installed instances of Dungeon
 * Sketch.
 * @author Tim
 *
 */
public class ImportDataDialog extends RoboActivity {
	
	private static final String TAG = "com.tbocek.android.combatmap.ImportDataDialog";

    @InjectView(tag="import_alpha") RadioButton importAlpha;
    @InjectView(tag="import_beta") RadioButton importBeta;
    @InjectView(tag="import_legacy") RadioButton importLegacy;
    @InjectView(tag="import_current") RadioButton importCurrent;
    @InjectView(tag="import_debug") RadioButton importDebug;
    @InjectView(tag="check_overwrite_tokens") CheckBox overwriteTokens;
    @InjectView(tag="check_overwrite_token_lib") CheckBox overwriteTokenLibrary;
    @InjectView(tag="check_overwrite_maps") CheckBox overwriteMaps;
    @InjectView(tag="button_import") Button buttonImport;
    @InjectView(tag="spinner_import_data") ProgressBar spinner;

    Map<File, RadioButton> mImportOptions =
            new HashMap<File, RadioButton>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("com.tbocek.android.combatmap.ImportDataDialog", "this.getPackageName() = " + this.getPackageName());
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.import_data);

        boolean hasOption = addImportOption(importAlpha, "com.tbocek.dungeonsketchalpha");
        hasOption = addImportOption(importBeta, "com.tbocek.dungeonsketchbeta")
                || hasOption;
        hasOption = addImportOption(importCurrent, "com.tbocek.dungeonsketch")
        		|| hasOption;
        hasOption = addImportOption(importLegacy, "com.tbocek.android.combatmap")
        		|| hasOption;
        hasOption = addImportOption(importDebug, "com.tbocek.dungeonsketchdebug")
        		|| hasOption;

        if (!hasOption) { finish(); }

        buttonImport.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                File srcDir = getSelectedSrcDir();
                spinner.setVisibility(View.VISIBLE);
                buttonImport.setEnabled(false);
                mImportFilesTask.overwriteTokens = overwriteTokens.isChecked();
                mImportFilesTask.overwriteMaps = overwriteMaps.isChecked();
                mImportFilesTask.overwriteTokenLibrary = overwriteTokenLibrary.isChecked();
                mImportFilesTask.execute(srcDir);
            }
        });
    }

    boolean addImportOption(RadioButton importOption, String packageName) {
        if (packageName.equals(this.getPackageName())) {
            importOption.setVisibility(View.GONE);
            importOption.setChecked(false);
            return false;
        } else if (!getExternalFilesDirForPackage(packageName).exists()) {
            importOption.setVisibility(View.GONE);
            importOption.setChecked(false);
            return false;
        } else {
            importOption.setVisibility(View.VISIBLE);
            mImportOptions.put(getExternalFilesDirForPackage(packageName), importOption);
            importOption.setChecked(true);
            return true;
        }
    }

    File pathJoin(String ... path) {
        File result = null;
        for (String s: path) {
            if (result == null) {
                result = new File(s);
            } else {
                result = new File(result, s);
            }
        }
        return result;
    }

    File getExternalFilesDirForPackage(String packageName) {
        return pathJoin(this.getExternalFilesDir(null).toString(),
                "..", "..", packageName, "files");
    }

    File getSelectedSrcDir() {
        for (Entry<File, RadioButton> entry: this.mImportOptions.entrySet()) {
            if (entry.getValue().isChecked()) {
                return entry.getKey();
            }
        }
        return null;
    }

    class ImportFilesTask extends AsyncTask<File, Integer, Void>  {
        int filesRead = 0;
        int totalFiles = 0;
        
        public boolean overwriteTokens;
        public boolean overwriteMaps;
        public boolean overwriteTokenLibrary;

        @Override
        protected Void doInBackground(File... srcDirs) {
            try {
                if (srcDirs == null) { return null; }
                File srcDir = srcDirs[0].getCanonicalFile();
                
                CountFilesWalker walker = new CountFilesWalker();
                walker.Count(new File(srcDir, "tokens"));
                walker.Count(new File(srcDir, "maps"));
                totalFiles = walker.getCount();

                File destDir = ImportDataDialog.this.getExternalFilesDir(null);

                RecursiveCopyWalker copyWalker = new RecursiveCopyWalker();
                copyWalker.Copy(new File(srcDir, "tokens"),
                        new File(destDir, "tokens"),
                        overwriteTokens);
                copyWalker.Copy(new File(srcDir, "maps"),
                        new File(destDir, "maps"),
                        overwriteMaps);
                
                if (overwriteTokenLibrary) {
                	FileUtils.copyFile(
                			new File(srcDir, "token_database.xml"), 
                			new File(destDir, "token_database.xml"));
                }
                // Force a reload of the token database.
                TokenDatabase.getInstance(getApplicationContext(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            filesRead += progress[0];
            spinner.setMax(totalFiles);
            spinner.setProgress(filesRead);

        }

        @Override
        protected void onPostExecute(Void unused) {
            ImportDataDialog.this.finish();
        }

        /**
         * DirectoryWalker that recursively copies file from the provided source
         * to the provided destination, respecting settings with regard to
         * overwriting.
         * @author Tim
         *
         */
        class RecursiveCopyWalker extends DirectoryWalker<File> {
            File mSrc;
            File mDest;
            boolean mOverwrite;
            public List<File> Copy(File src, File dest, boolean overwrite) throws IOException {
                mSrc = src;
                mDest = dest;
                mOverwrite = overwrite;
                List<File> results = new ArrayList<File>();
                this.walk(src, results);
                return results;
            }

            @Override
            protected boolean handleDirectory(
                    File directory, int depth, Collection<File> results) {
                replacePrefix(directory, mSrc, mDest).mkdirs();
                return true;
            }

            @Override
            protected void handleFile(
                    File file, int depth, Collection<File> results) {
            	// Never overwrite tmp.map, as it is the current "autosave".
            	if (file.getName().equals("tmp.map")) {
            		publishProgress(1);
            		return;
            	}
            	
                File destFile = replacePrefix(file, mSrc, mDest);
                if (mOverwrite || !destFile.exists()) {
                    try {
                    	Log.d(TAG, "Copy " + file.toString() + " to " + destFile.toString());
                        FileUtils.copyFile(file, destFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                        results.add(file);
                    }
                } else {
                	Log.d(TAG, "Did not copy " + file.toString() + " to " + destFile.toString());
                }
                publishProgress(1);
            }

            private File replacePrefix(File path, File oldPrefix, File newPrefix) {
                return new File(path.toString().replace(oldPrefix.toString(), newPrefix.toString()));
            }
        }

        class CountFilesWalker extends DirectoryWalker<File> {
            int count = 0;
            public void Count(File src) throws IOException {
                this.walk(src, null);
            }

            @Override
            protected void handleFile(
                    File file, int depth, Collection<File> results) {
                count += 1;
            }

            public int getCount() {
                return count;
            }
        }
    }
    
    ImportFilesTask mImportFilesTask = new ImportFilesTask();



}
