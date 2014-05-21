package com.tbocek.android.combatmap.model;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.AsyncTask;

import com.google.common.collect.Lists;
import com.tbocek.android.combatmap.model.CommandHistory.Command;
import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;
import com.tbocek.android.combatmap.model.primitives.BackgroundImage;
import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.model.primitives.PointF;

import java.io.IOException;
import java.util.List;

/**
 * This class manages a collection of background images (e.g. predrawn maps that
 * users have imported into Dungeon Sketch to play on.
 * @author Tim
 *
 */
public class BackgroundImageCollection {

    /**
     * Undo/Redo History.
     */
    private CommandHistory mCommandHistory;

    /**
     * List of managed images.
     */
    private final List<BackgroundImage> mImages = Lists.newArrayList();

    private ModifyImageCommand mCheckpointedImageCommand;

    /**
     * Constructor.
     * @param commandHistory The command history that modifications to this
     *      collection will be added to.
     */
    public BackgroundImageCollection(CommandHistory commandHistory) {
        this.mCommandHistory = commandHistory;
    }

    public BackgroundImageCollection(BackgroundImageCollection copyFrom) {
       for (BackgroundImage i : copyFrom.mImages) {
           mImages.add(new BackgroundImage(i));
       }
    }

    /**
     * Adds a new background image.
     * @param path Path to the image to add.
     * @param initialLocation Location of the background image.
     */
    public BackgroundImage addImage(String path, PointF initialLocation) {
        // TODO: Enable undo/redo.
        BackgroundImage image = new BackgroundImage(path, initialLocation);
        Command c = new NewImageCommand(image);
        this.mCommandHistory.execute(c);
        return image;
    }


    /**
     * Because of the way image drawing works, we need to be able to make the
     * assumption that the canvas is *untransformed*. But, we still want to
     * respect the fog of war. So, we let the calling code assume that the
     * canvas is transformed, and undo the transformation here.
     * @param canvas The canvas to draw on.
     * @param transformer Transformation from screen space to world space.
     * @param worldSpaceBounds The area in world space that needs to be redrawn (used for clip
     *     detection).
     */
    public void draw(Canvas canvas, CoordinateTransformer transformer, RectF worldSpaceBounds) {
        canvas.save();
        transformer.setInverseMatrix(canvas);

        for (BackgroundImage i : this.mImages) {
        	if (i.getBoundingRectangle().testClip(worldSpaceBounds)) {
        		i.draw(canvas, transformer);
        	}
        }

        canvas.restore();
    }

    /**
     * Finds the object underneath the given point in world space.
     * 
     * @param point
     *            Location to check in world space.
     * @param borderWorldSpace Width of a border to place around the image.
     *      (for example, if the image's x bounds are (x1, x2), we will actually
     *      test for (x1 - borderWorldSpace, x2 + borderWorldSpace).  This
     *      allows some margin of error to detect "handles" which manipulate the
     *      image but are not actually contained within the image's bounding
     *      rectangle.
     * @return The background image under the given point, or null if no image
     *      found.
     */
    public BackgroundImage getImageOnPoint(
            PointF point, float borderWorldSpace) {
        for (BackgroundImage i : this.mImages) {
            if (i.getBoundingRectangle(borderWorldSpace).contains(point)) {
                return i;
            }
        }
        return null;
    }

    /**
     * Checkpoints the state of the given image so that whatever actions the
     * user performs with it are already set up to be undoable.
     * 
     * If called after another call to checkpointImageBefore and before the
     * corresponding call to checkpointImageAfter, this is a noop.
     * 
     * @param i The image to checkpoint.
     */
    public void checkpointImageBefore(BackgroundImage i) {
        if (mCheckpointedImageCommand == null) {
            mCheckpointedImageCommand = new ModifyImageCommand(i);
        }

    }

    /**
     * Checkpoints the state of the the actively checkpointed image and adds
     * the checkpoint data to the undo/redo stack.
     * 
     * The image checkpointed is the image passed to checkpointImageBefore.
     * 
     * Must be called in a pair with checkpointImageBefore.
     */
    public void checkpointImageAfter() {
        // We are not ready to "execute" this command yet - instead we are
        // checkpointing its state.
        if (this.mCheckpointedImageCommand != null) {
            this.mCheckpointedImageCommand.checkpointAfterState();
            this.mCommandHistory.addToCommandHistory(mCheckpointedImageCommand);
            mCheckpointedImageCommand = null;
        }
    }

    /**
     * Deletes the given image from the background image collection.
     * @param selectedImage The image to delete.  Must belong to this
     *      collection.
     */
    public void deleteImage(BackgroundImage selectedImage) {
        Command c = new DeleteImageCommand(selectedImage);
        this.mCommandHistory.execute(c);
    }

    /**
     * A Command that adds the given image to the list of images.
     * @author Tim
     *
     */
    private class NewImageCommand implements CommandHistory.Command {

        /**
         * The image added by this command.
         */
        private final BackgroundImage mImage;

        /**
         * Constructor.
         * @param image The image added by this command.
         */
        public NewImageCommand(BackgroundImage image) {
            mImage = image;
        }

        @Override
        public void execute() {
            BackgroundImageCollection.this.mImages.add(mImage);
        }

        @Override
        public boolean isNoop() {
            return false;
        }

        @Override
        public void undo() {
            BackgroundImageCollection.this.mImages.remove(mImage);

        }
    }

    /**
     * A Command that removes the given image to the list of images.
     * @author Tim
     *
     */
    private class DeleteImageCommand implements CommandHistory.Command {

        /**
         * The image added by this command.
         */
        private final BackgroundImage mImage;

        /**
         * Constructor.
         * @param image The image added by this command.
         */
        public DeleteImageCommand(BackgroundImage image) {
            mImage = image;
        }

        @Override
        public void execute() {
            BackgroundImageCollection.this.mImages.remove(mImage);
        }

        @Override
        public boolean isNoop() {
            return false;
        }

        @Override
        public void undo() {
            BackgroundImageCollection.this.mImages.add(mImage);
        }
    }

    /**
     * A command that stores the state of the given image, so that undoing and
     * redoing the action can swap the state in and out.
     * @author Tim
     *
     */
    private class ModifyImageCommand implements CommandHistory.Command {
        /**
         * Copy of the image's state before it was modified.
         */
        private BackgroundImage mBefore;

        /**
         * Copy of the image's state after it was modified.
         */
        private BackgroundImage mAfter;

        /**
         * Reference to the "live" copy of the image.
         */
        private BackgroundImage mLiveImage;

        /**
         * Constructor.  Will make a copy of the BackgroundImage being modified.
         * @param toModify The image to modify.
         */
        public ModifyImageCommand(BackgroundImage toModify) {
            try {
                mBefore = toModify.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(
                        "Cloning BackgroundImage failed.", e);
            }
            mLiveImage = toModify;
        }

        /**
         * Saves the state of this image after all manipulation is finished.
         */
        public void checkpointAfterState() {
            try {
                mAfter = mLiveImage.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(
                        "Cloning BackgroundImage failed.", e);
            }
        }

        @Override
        public void execute() {
            this.mLiveImage.copyLocationDataFrom(this.mAfter);
        }

        @Override
        public boolean isNoop() {
            return false;
        }

        @Override
        public void undo() {
            this.mLiveImage.copyLocationDataFrom(this.mBefore);
        }
    }

    public void serialize(MapDataSerializer s) throws IOException {
        s.startArray();
        for (BackgroundImage image : this.mImages){
            image.serialize(s);
        }
        s.endArray();
    }

    public void deserialize(MapDataDeserializer s) throws IOException {
        int arrayLevel = s.expectArrayStart();
        while (s.hasMoreArrayItems(arrayLevel)) {
            this.mImages.add(BackgroundImage.deserialize(s));
        }
        s.expectArrayEnd();
    }

    public boolean contains(BackgroundImage selectedImage) {
        return this.mImages.contains(selectedImage);
    }

    /**
     * Loads all background images in a new thread.  Executes onLoadSuccess when all background
     * images have been loaded.
     * @param context Context to load images in.
     * @param onLoadSuccess Callback that will be executed on the UI thread once all images load.
     */
    private void loadImages(final Context context, final List<BackgroundImage> images,
                            final Runnable onLoadSuccess) {

        final List<BackgroundImage> image = mImages;
        AsyncTask<Void, Void, Void> loadImagesTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                for (BackgroundImage i: mImages) {
                    i.loadDrawable();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                onLoadSuccess.run();
            }
        };
        loadImagesTask.execute();
    }

    public void loadImages(final Context context, final Runnable onLoadSuccess) {
        loadImages(context, this.mImages, onLoadSuccess);
    }

    public void loadImage(final Context context, final BackgroundImage image,
                          final Runnable onLoadSuccess) {
        List<BackgroundImage> stupidList = Lists.newArrayList(image);
        loadImages(context, stupidList, onLoadSuccess);
    }
}
