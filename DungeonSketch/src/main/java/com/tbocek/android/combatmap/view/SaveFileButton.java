package com.tbocek.android.combatmap.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tbocek.dungeonsketch.R;

/**
 * A button that displays the name and preview of a save file.
 * 
 * @author Tim Bocek
 * 
 */
public final class SaveFileButton extends LinearLayout {

    /**
     * The displayed preview.
     */
    private final ImageView mPreview;

    /**
     * The displayed file name.
     */
    private final TextView mText;

    /**
     * Constructor.
     * 
     * @param context
     *            The context to create this view in.
     */
    public SaveFileButton(final Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.saved_map_file, this);
        this.mPreview = (ImageView) this.findViewById(R.id.saved_map_preview);
        this.mText = (TextView) this.findViewById(R.id.saved_map_file_name);

        // Clicking on the preview should count as clicking on the button its
        // self.
        this.mPreview.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View arg0) {
                SaveFileButton.this.performClick();
            }
        });
    }

    /**
     * Returns the filename associated with this button.
     * 
     * @return The filename.
     */
    public String getFileName() {
        return this.mText.getText().toString();
    }

    /**
     * Sets the name of the save file.
     * 
     * @param name
     *            The name to display.
     */
    public void setFileName(final String name) {
        this.mText.setText(name);
    }

    @Override
    public void setOnCreateContextMenuListener(
            final View.OnCreateContextMenuListener l) {
        super.setOnCreateContextMenuListener(l);
        this.mPreview.setOnCreateContextMenuListener(l);
        this.mText.setOnCreateContextMenuListener(l);
    }

    /**
     * Sets the image to display as the preview.
     * 
     * @param image
     *            The image to display.
     */
    public void setPreviewImage(final Bitmap image) {
        this.mPreview.setImageBitmap(image);
    }
}
