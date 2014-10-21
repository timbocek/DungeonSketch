package com.tbocek.android.combatmap;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.tbocek.android.combatmap.model.MapData;
import com.tbocek.android.combatmap.model.primitives.Units;
import com.tbocek.android.combatmap.model.primitives.Util;

import org.apache.commons.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class manages saved map and token data and provides an interface to
 * query it.
 * 
 * @author Tim Bocek
 * 
 */
public final class DataManager {
    private static final String TAG = "DataManager";

    /**
     * Extension to use for image files.
     */
    private static final String IMAGE_EXTENSION = ".jpg";

    /**
     * JPEG compression to use when saving images.
     */
    private static final int JPEG_COMPRESSION = 75;

    /**
     * Extension to use for map files.
     */
    private static final String MAP_EXTENSION = ".map";

    /**
     * Tag to add to files that are map previews.
     */
    private static final String PREVIEW_TAG = ".preview";

    /**
     * Extension to use for preview files.
     */
    private static final String PREVIEW_EXTENSION = PREVIEW_TAG
            + IMAGE_EXTENSION;

    /**
     * Name of the temporary map.
     */
    public static final String TEMP_MAP_NAME = "tmp";

    /**
     * The context that this data manager goes through to read and write data.
     */
    private final Context mContext;

    /**
     * Constructor.
     * 
     * @param context
     *            The context to go through when reading and writing data.
     */
    public DataManager(final Context context) {
        this.mContext = context;
        this.ensureExternalDirectoriesCreated();
    }

    /**
     * Deletes the save file and the associated preview image.
     * 
     * @param fileName
     *            Name of the save file without the extension to delete.
     */
    public void deleteSaveFile(final String fileName) {
        if (!this.getSavedMapFile(fileName).delete()) {
            Log.w(TAG, "Could not delete map file " + fileName);
        }
        if (!this.getSavedMapPreviewImageFile(fileName).delete()) {
            Log.w(TAG, "Could not delete map preview image " + fileName);
        }
    }

    /**
     * Deletes the given token image.
     * 
     * @param fileName
     *            Name of the token to delete.
     */
    public void deleteTokenImage(final String fileName) {
        if (!this.getTokenImageFile(fileName).delete()) {
            Log.w(TAG, "Could not delete token image " + fileName);
        }
    }

    /**
     * Creates the directory that contains external token images, if it has not
     * been created.
     */
    private void ensureExternalDirectoriesCreated() {
        if (!this.getTokenImageDir().mkdirs()) {
            Log.e(TAG, "Could not create token image dir");
        }
        if (!this.getSavedMapDir().mkdirs()) {
            Log.e(TAG, "Could not create saved map dir");
        }
        if (!this.getExportedImageDir().mkdirs()) {
            Log.e(TAG, "Could not create exported image dir");
        }
    }

    /**
     * Saves a preview of a map.
     * 
     * @param name
     *            Filename to export, without extension.
     * @param preview
     *            Preview image for the map.
     * @param format
     *            Format to export in.
     * @throws IOException
     *             On write error.
     */
    public void exportImage(final String name, final Bitmap preview,
            final Bitmap.CompressFormat format) throws IOException {
        String filename =
                name + (format == Bitmap.CompressFormat.JPEG ? ".jpg" : ".png");
        FileOutputStream s =
                new FileOutputStream(this.getExportedImageFileName(filename));
        BufferedOutputStream buf = new BufferedOutputStream(s);
        preview.compress(format, JPEG_COMPRESSION, buf);
        buf.close();
        s.close();
    }

    /**
     * @return File object representing the directory containing exported
     *         images.
     */
    private File getExportedImageDir() {
        File imageDir =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
        return new File(imageDir, "DungeonSketch");
    }


    /**
     * Given the name of the map, gets a path to export an image.
     * @param mapName The name of the map to export.
     * @return Path to the export image.
     */
    private File getExportedImageFileName(String mapName) {
        File sdcard = this.getExportedImageDir();
        return new File(sdcard, mapName);
    }

    /**
     * @return File object representing the directory containing saved maps.
     */
    private File getSavedMapDir() {
        File sdcard = this.mContext.getExternalFilesDir(null);
        File dir = new File(sdcard, "maps");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "Could not create saved map dir at " + dir.getAbsolutePath());
            }
        }
        return dir;
    }

    /**
     * Gets a file object representing a saved map with the given name.
     * 
     * @param mapName
     *            Name of the map, without extension.
     * @return The saved map's file object.
     */
    private File getSavedMapFile(String mapName) {
        File sdcard = this.getSavedMapDir();
        return new File(sdcard, mapName + MAP_EXTENSION);
    }

    /**
     * Gets a file object representing the preview for a saved map with the
     * given name.
     * 
     * @param mapName
     *            Name of the map, without extension.
     * @return The saved map's preview's file object.
     */
    private File getSavedMapPreviewImageFile(String mapName) {
        File sdcard = this.getSavedMapDir();
        return new File(sdcard, mapName + PREVIEW_EXTENSION);
    }

    /**
     * @return File object representing the directory containing token images.
     */
    private File getTokenImageDir() {
        File sdcard = this.mContext.getExternalFilesDir(null);
        File dir = new File(sdcard, "tokens");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "Could not create token image dir at " + dir.getAbsolutePath());
            }
        }
        return dir;
    }

    /**
     * @return File object representing the directory containing map data files.
     */
    private File getMapDataDir() {
        File sdcard = this.mContext.getExternalFilesDir(null);
        File dir = new File(sdcard, "mapdata");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "Could not create map data dir at " + dir.getAbsolutePath());
            }
        }
        return dir;
    }

    /**
     * Opens a file for the given token image.
     * 
     * @param filename
     *            Name of the file to open, with extension but without
     *            directory.
     * @return Opened file object.
     */
    private File getTokenImageFile(final String filename) {
        File sdcard = this.getTokenImageDir();
        return new File(sdcard, filename);
    }

    /**
     * Returns true if the filename is an image, false otherwise.
     * 
     * @param file
     *            Filename to check.
     * @return True if an image.
     */
    boolean isImageFileName(final String file) {
        return file.endsWith(IMAGE_EXTENSION);
    }

    /**
     * Loads the map from the given name. This takes care of looking up the full
     * path.
     * 
     * @param name
     *            The name of the map to load, without the extension.
     * @throws IOException
     *             On read error.
     */
    public void loadMapName(final String name) throws IOException {
        File f = this.getSavedMapFile(name);
        if (f.exists()) {
            FileInputStream s = new FileInputStream(f);
            MapData.loadFromStream(s, TokenDatabase.getInstanceOrNull());
            s.close();
        } else if (name.equals(TEMP_MAP_NAME)) {
            MapData.clear();
        }
    }

    /**
     * Loads a preview image for the given save file.
     * 
     * @param saveFile
     *            Save file to load a preview for. Do not provide a file
     *            extension.
     * @return Loaded image.
     * @throws IOException
     *             On read error.
     */
    public Bitmap loadPreviewImage(final String saveFile) throws IOException {
        FileInputStream s =
                new FileInputStream(this.getSavedMapPreviewImageFile(saveFile));
        Bitmap b = BitmapFactory.decodeStream(s);
        s.close();
        return b;
    }

    private interface BitmapLoader {
        Bitmap load(BitmapFactory.Options options);
    }

    public Bitmap loadTokenImage(final String filename, Bitmap existingBuffer, int maxWidth,
                                 int maxHeight) {
        final String tokenImageFilePath = this.getTokenImageFile(filename).getAbsolutePath();
        return loadTokenImage(existingBuffer, maxWidth, maxHeight, new BitmapLoader() {
            @Override
            public Bitmap load(BitmapFactory.Options options) {
                return BitmapFactory.decodeFile(tokenImageFilePath, options);
            }
        });
    }

    public Bitmap loadTokenImage(final int resource_id, Bitmap existingBuffer, int maxWidth,
                                 int maxHeight) {
        return loadTokenImage(existingBuffer, maxWidth, maxHeight, new BitmapLoader() {
            @Override
            public Bitmap load(BitmapFactory.Options options) {
                return BitmapFactory.decodeResource(mContext.getResources(), resource_id, options);
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private Bitmap loadTokenImage(Bitmap existingBuffer, int maxWidthDp, int maxHeightDp,
                                  BitmapLoader loader) {

        int maxWidthPx = (int) Units.dpToPx(maxWidthDp);
        int maxHeightPx = (int) Units.dpToPx(maxHeightDp);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        loader.load(options);
        options.inJustDecodeBounds = false;

        // Make bitmaps mutable so they are reusable.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            options.inMutable = true;
        }

        // Set the sample size so that we load into maxWidth and maxHeight.
        options.inSampleSize = 1;
        while (options.outWidth / options.inSampleSize > maxWidthPx &&
                options.outHeight / options.inSampleSize > maxHeightPx) {
            options.inSampleSize *= 2;  // Sample size must be a power of 2.
        }

        // Detect whether we can reuse the existing buffer.
        if (existingBuffer != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && existingBuffer.isMutable()
                    && canUseForInBitmap(existingBuffer, options)) {
                options.inBitmap = existingBuffer;
            } else {
                try {
                    existingBuffer.recycle();
                } catch (NullPointerException e) {
                    Log.w(TAG, "NPE when attempting to recycle a bitmap", e);
                }
            }
        }

        // Attempt to load this bitmap into the existing buffer.  If this fails, recycle the bitmap
        // and load a bitmap into a new buffer.
        Bitmap b;
        try {
            b = loader.load(options);
        } catch (IllegalArgumentException e) {
            options.inBitmap = null;
            if (existingBuffer != null) {
                try {
                    existingBuffer.recycle();
                } catch (NullPointerException ex) {
                    Log.w(TAG, "NPE when attempting to recycle a bitmap", ex);
                }
            }
            b = loader.load(options);
        }
        return b;
    }

    /**
     * Gets a list of saved map names, without the extensions.
     * 
     * @return A list of available saved maps.
     */
    public List<String> savedFiles() {
        String[] files = this.getSavedMapDir().list();
        ArrayList<String> mapFiles = new ArrayList<String>();
        for (String file : files) {
            if (!file.equals("tmp" + MAP_EXTENSION)
                    && file.endsWith(MAP_EXTENSION)) {
                mapFiles.add(file.replace(MAP_EXTENSION, ""));
            }
        }
        return mapFiles;
    }

    /**
     * Checks whether a saved map exists.
     * 
     * @param file
     *            The map name to check.
     * @return True if the map exists, False otherwise.
     */
    public boolean saveFileExists(final String file) {
        return this.getSavedMapFile(file).exists();
    }

    /**
     * Saves the map to the given name. This takes care of looking up the full
     * path.
     * 
     * @param name
     *            Name of the map to save, without the extension.
     * @throws IOException
     *             On write error.
     */
    public void saveMapName(final String name) throws IOException {
        // Save to temporary map.
        FileOutputStream s =
                new FileOutputStream(this.getSavedMapFile(TEMP_MAP_NAME));
        MapData.saveToStream(s);
        s.close();

        // Copy temp to desired location
        if (!name.equals(TEMP_MAP_NAME)) {
            FileUtils.copyFile(this.getSavedMapFile(TEMP_MAP_NAME),
                    this.getSavedMapFile(name));
        }
    }

    /**
     * Saves a preview of a map.
     * 
     * @param name
     *            The name of the map, without the extension.
     * @param preview
     *            Preview image for the map.
     * @throws IOException
     *             On write error.
     */
    public void savePreviewImage(final String name, final Bitmap preview)
            throws IOException {
        FileOutputStream s =
                new FileOutputStream(this.getSavedMapPreviewImageFile(name));
        BufferedOutputStream buf = new BufferedOutputStream(s);
        preview.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION, buf);
        buf.close();
        s.close();
    }

    /**
     * Saves the given image as a token image file.
     * 
     * @param name
     *            Name of the file to save, without extension.
     * @param image
     *            Bitmap to save to this file.
     * @return The saved file name, with extension.
     * @throws IOException
     *             On write error.
     */
    public String saveTokenImage(final String name, final Bitmap image)
            throws IOException {
        String filename = name + IMAGE_EXTENSION;
        FileOutputStream s =
                new FileOutputStream(this.getTokenImageFile(filename));
        BufferedOutputStream buf = new BufferedOutputStream(s);
        image.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION, buf);
        buf.close();
        s.close();
        return filename;
    }

    /**
     * Gets a list of token files available to load.
     * 
     * @return A list of token files, with the extensions.
     */
    public List<String> tokenFiles() {
        String[] files = this.getTokenImageDir().list();
        ArrayList<String> imageFiles = new ArrayList<String>();
        if (files != null) {
            for (String file : files) {
                Log.d("tokenFiles", file);
                if ((this.isImageFileName(file))
                        && !file.endsWith(PREVIEW_EXTENSION)) {
                    imageFiles.add(file);
                }
            }
        }
        return imageFiles;
    }

    /**
     * Copies the given resource to a data directory for map resources (not
     * tokens).
     * @param path Path of the resource to copy.
     * @return Path to the saved instance of this resource.
     * @throws IOException if the file copy failed.
     */
    public String copyToMapDataFiles(Uri path) throws IOException {
        // Create a unique filename based on the date.
        Date now = new Date();
        String filename = Long.toString(now.getTime());

        InputStream input = this.mContext.getContentResolver()
                .openInputStream(path);

        FileUtils.copyInputStreamToFile(input, this.getMapDataFile(filename));
        input.close();

        return filename;
    }

    /**
     * Gets the full path to a map data file.
     * @param filename The filename to load.
     * @return The full path to the file.
     */
    File getMapDataFile(String filename) {
        return new File(this.getMapDataDir(), filename);
    }

    /**
     * Loads a preview image for the given save file.
     * 
     * @param filename
     *            Image file name to load.
     * @return Loaded image.
     * @throws IOException
     *             On read error.
     */
    public Bitmap loadMapDataImage(final String filename) throws IOException {
        FileInputStream s =
                new FileInputStream(this.getMapDataFile(filename));
        Bitmap b = BitmapFactory.decodeStream(s);
        s.close();
        return b;
    }

    private static boolean canUseForInBitmap(
            Bitmap candidate, BitmapFactory.Options targetOptions) {
        if (targetOptions.inSampleSize == 0) targetOptions.inSampleSize = 1;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // From Android 4.4 (KitKat) onward we can re-use if the byte size of
            // the new bitmap is smaller than the reusable bitmap candidate
            // allocation byte count.
            int width = targetOptions.outWidth / targetOptions.inSampleSize;
            int height = targetOptions.outHeight / targetOptions.inSampleSize;
            int byteCount = width * height * getBytesPerPixel(candidate.getConfig());
            return byteCount <= candidate.getAllocationByteCount();
        }

        // On earlier versions, the dimensions must match exactly and the inSampleSize must be 1
        return candidate.getWidth() == targetOptions.outWidth
                && candidate.getHeight() == targetOptions.outHeight
                && targetOptions.inSampleSize == 1;
    }

    /**
     * A helper function to return the byte usage per pixel of a bitmap based on its configuration.
     */
    private static int getBytesPerPixel(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        } else if (config == Bitmap.Config.RGB_565) {
            return 2;
        } else if (config == Bitmap.Config.ARGB_4444) {
            return 2;
        } else if (config == Bitmap.Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }

    public Context getContext() {
        return mContext;
    }
}
