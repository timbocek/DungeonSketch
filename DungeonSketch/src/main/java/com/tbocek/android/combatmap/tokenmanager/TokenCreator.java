package com.tbocek.android.combatmap.tokenmanager;

import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.tbocek.android.combatmap.CombatMap;
import com.tbocek.android.combatmap.DataManager;
import com.tbocek.android.combatmap.DeveloperMode;
import com.tbocek.android.combatmap.R;
import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.model.primitives.CustomBitmapToken;
import com.tbocek.android.combatmap.model.primitives.Util;

/**
 * This activity allows the user to create a new token by importing an image and
 * specifying a circular region of the image to use.
 * 
 * @author Tim Bocek
 * 
 */
public final class TokenCreator extends SherlockActivity {

    /**
     * Maximum dimension allowed in any image before the image starts being
     * downsampled on load.
     */
    private static final int MAX_IMAGE_DIMENSION = 1280;

    /**
     * Request ID that is passed to the image gallery activity; this is returned
     * by the image gallery to let us know that a new image was picked.
     */
    static final int PICK_IMAGE_REQUEST = 0;

	public static final String TAG_TO_ADD = "tag_to_add";

    /**
     * Whether the image selector activity was started automatically. If true,
     * and the activity was cancelled, this activity should end as well.
     */
    private boolean mImageSelectorStartedAutomatically = false;

    /**
     * The view that implements drawing the selected image and allowing the user
     * to sepcify a circle on it.
     */
    private TokenCreatorView mTokenCreatorView;

	private String mTagPathToAdd;

    @Override
    protected void onActivityResult(final int requestCode,
            final int resultCode, final Intent data) {
        // If an image was successfully picked, use it.
        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    Uri selectedImage = data.getData();
                    Bitmap bitmap =
                            Util.loadImageWithMaxBounds(selectedImage,
                                    MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION,
                                    this.getContentResolver());
                    this.mTokenCreatorView.setImage(new BitmapDrawable(bitmap));

                    Toast t =
                            Toast.makeText(this.getApplicationContext(),
                                    "Pinch to change the cut out region",
                                    Toast.LENGTH_LONG);
                    t.show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast toast =
                            Toast.makeText(this.getApplicationContext(),
                                    "Couldn't load image: " + e.toString(),
                                    Toast.LENGTH_LONG);
                    toast.show();
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (this.mImageSelectorStartedAutomatically) {
                    this.finish();
                }
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        DeveloperMode.strictMode();
        super.onCreate(savedInstanceState);
        this.mTokenCreatorView = new TokenCreatorView(this);
        this.setContentView(this.mTokenCreatorView);
        
        this.mTagPathToAdd = this.getIntent().getStringExtra(TAG_TO_ADD);

        // Automatically select a new image when the view starts.
        this.mImageSelectorStartedAutomatically = true;
        this.startImageSelectorActivity();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = this.getSupportMenuInflater();
        inflater.inflate(R.menu.token_image_creator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.token_image_creator_pick) {
            this.mImageSelectorStartedAutomatically = false;
            this.startImageSelectorActivity();
            return true;
        } else if (itemId == R.id.token_image_creator_accept) {
            try {
                // Pick a filename based on the current date and time. This
                // ensures that the tokens load in the order added.
                Date now = new Date();
                String filename = Long.toString(now.getTime());
                filename = this.saveToInternalImage(filename);

                // Add this token to the token database
                TokenDatabase tokenDatabase =
                        TokenDatabase.getInstance(this.getApplicationContext());
                BaseToken t = new CustomBitmapToken(filename);
                tokenDatabase.addTokenPrototype(t);
                tokenDatabase.tagToken(t.getTokenId(), t.getDefaultTags());
                tokenDatabase.tagToken(t.getTokenId(), TokenDatabase.RECENTLY_ADDED);
                if (this.mTagPathToAdd != null) {
                	tokenDatabase.tagToken(t.getTokenId(), this.mTagPathToAdd);
                }

                this.setResult(Activity.RESULT_OK);
                this.finish();
            } catch (IOException e) {
                e.printStackTrace();
                Toast toast =
                        Toast.makeText(this.getApplicationContext(),
                                "Couldn't save image: " + e.toString(),
                                Toast.LENGTH_LONG);
                toast.show();
            }
            return true;
        } else if (itemId == android.R.id.home) {
            // app icon in action bar clicked; go home
            Intent intent = new Intent(this, CombatMap.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the cropped region of the selected image and saves it to the
     * standard token storage location.
     * 
     * @param name
     *            The name of the image to save, without file extension.
     * @return The filename that was saved to.
     * @throws IOException
     *             On write error.
     */
    private String saveToInternalImage(final String name) throws IOException {
        Bitmap bitmap = this.mTokenCreatorView.getClippedBitmap();
        if (bitmap == null) {
            return null;
        }
        return new DataManager(this.getApplicationContext()).saveTokenImage(
                name, bitmap);
    }

    /**
     * Starts the activity to pick an image. This will probably be the image
     * gallery, but the user might have a different app installed that does the
     * same thing.
     */
    private void startImageSelectorActivity() {
        this.startActivityForResult(new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                PICK_IMAGE_REQUEST);
    }
}
