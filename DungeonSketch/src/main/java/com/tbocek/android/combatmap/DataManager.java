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
    private Context mContext;

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
        this.getSavedMapFile(fileName).delete();
        this.getSavedMapPreviewImageFile(fileName).delete();
    }

    /**
     * Deletes the given token image.
     * 
     * @param fileName
     *            Name of the token to delete.
     */
    public void deleteTokenImage(final String fileName) {
        this.getTokenImageFile(fileName).delete();
    }

    /**
     * Creates the directory that contains external token images, if it has not
     * been created.
     */
    private void ensureExternalDirectoriesCreated() {
        this.getTokenImageDir().mkdirs();
        this.getSavedMapDir().mkdirs();
        this.getExportedImageDir().mkdirs();
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
        File dir = new File(imageDir, "DungeonSketch");
        return dir;
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
            dir.mkdirs();
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
            dir.mkdirs();
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
            dir.mkdirs();
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
    public boolean isImageFileName(final String file) {
        return file.endsWith(IMAGE_EXTENSION);
    }

    /**
     * Loads the map from the given name. This takes care of looking up the full
     * path.
     * 
     * @param name
     *            The name of the map to load, without the extension.
     * @throws ClassNotFoundException
     *             On deserialize error.
     * @throws IOException
     *             On read error.
     */
    public void loadMapName(final String name) throws IOException,
    ClassNotFoundException {
        File f = this.getSavedMapFile(name);
        if (f.exists()) {
            FileInputStream s = new FileInputStream(f);
            MapData.loadFromStream(s, TokenDatabase.getInstanceOrNull());
            s.close();
            MapData.getInstance().setMapAttributesLocked(true);
        } else if (name.equals(TEMP_MAP_NAME)) {
            MapData.clear();
            MapData.getInstance().setMapAttributesLocked(false);
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

    /**
     * Loads the given token image.
     * 
     * @param filename
     *            Filename to load, with extension.
     * @return Bitmap of the loaded image.
     * @throws IOException
     *             On read error.
     */
    public Bitmap loadTokenImage(final String filename) throws IOException {
        return loadTokenImage(filename, null);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public Bitmap loadTokenImage(final String filename, Bitmap existingBuffer) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(
                this.getTokenImageFile(filename).getAbsolutePath(), options);
        options.inJustDecodeBounds = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            options.inMutable = true; // Make bitmaps mutable so they are reusable.
        }
        if (existingBuffer != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && existingBuffer.isMutable() && canUseForInBitmap(existingBuffer, options)) {
                options.inBitmap = existingBuffer;
            } else {
                existingBuffer.recycle();
            }
        }
        Bitmap b = BitmapFactory.decodeFile(
                this.getTokenImageFile(filename).getAbsolutePath(), options);
        return b;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public Bitmap loadTokenImage(final int resource_id, Bitmap existingBuffer) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(this.mContext.getResources(), resource_id, options);
        options.inJustDecodeBounds = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            options.inMutable = true; // Make bitmaps mutable so they are reusable.
        }

        if (existingBuffer != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && existingBuffer.isMutable() && canUseForInBitmap(existingBuffer, options)) {
                options.inBitmap = existingBuffer;
            } else {
                existingBuffer.recycle();
            }
        }
        Bitmap b = BitmapFactory.decodeResource(this.mContext.getResources(), resource_id, options);
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
    public File getMapDataFile(String filename) {
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

    static boolean canUseForInBitmap(
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
    static int getBytesPerPixel(Bitmap.Config config) {
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
}
