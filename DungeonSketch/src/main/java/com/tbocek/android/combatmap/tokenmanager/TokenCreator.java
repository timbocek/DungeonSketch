package com.tbocek.android.combatmap.tokenmanager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.tbocek.android.combatmap.CombatMap;
import com.tbocek.android.combatmap.DataManager;
import com.tbocek.android.combatmap.DeveloperMode;
import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.model.primitives.CustomBitmapToken;
import com.tbocek.android.combatmap.model.primitives.Util;
import com.tbocek.dungeonsketch.R;

import java.io.IOException;
import java.util.Date;

/**
 * This activity allows the user to create a new token by importing an image and
 * specifying a circular region of the image to use.
 * 
 * @author Tim Bocek
 * 
 */
public final class TokenCreator extends ActionBarActivity {
    private static final String TAG = "TokenCreator";

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

    private ProgressDialog mProgressDialog;

    @Override
    protected void onActivityResult(final int requestCode,
            final int resultCode, final Intent data) {
        // If an image was successfully picked, use it.
        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Uri selectedImage = data.getData();

                showProgressDialog(getString(R.string.loading_selected_image));
                AsyncTask<Uri, Void, Bitmap> loadImageTask =
                        new AsyncTask<Uri, Void, Bitmap>() {
                            @Override
                            protected Bitmap doInBackground(Uri... uris) {
                                try {
                                    return Util.loadImageWithMaxBounds(uris[0],
                                            MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION,
                                            getContentResolver());
                                } catch (IOException e) {
                                    Log.e(TAG, "Error loading bitmap", e);
                                    return null;
                                }
                            }

                            @Override
                            protected void onPostExecute(Bitmap bitmap) {
                                if (bitmap != null) {
                                    mTokenCreatorView.setImage(
                                            new BitmapDrawable(getResources(), bitmap));
                                    Toast t =  Toast.makeText(
                                            getApplicationContext(),
                                            getString(R.string.token_creator_help),
                                            Toast.LENGTH_LONG);
                                    t.show();
                                } else {
                                    Toast toast = Toast.makeText(getApplicationContext(),
                                            getString(R.string.token_creator_error),
                                            Toast.LENGTH_LONG);
                                    toast.show();
                                }
                                mProgressDialog.dismiss();
                            }
                        };

                loadImageTask.execute(selectedImage);

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
        MenuInflater inflater = this.getMenuInflater();
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

            showProgressDialog(getString(R.string.saving_token));
            AsyncTask<Void, Void, Boolean> saveTokenTask = new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voids) {
                    try {
                        Date now = new Date();
                        String filename = Long.toString(now.getTime());
                        filename = saveToInternalImage(filename);

                        // Add this token to the token database
                        TokenDatabase tokenDatabase =
                                TokenDatabase.getInstance(getApplicationContext());
                        BaseToken t = new CustomBitmapToken(filename);
                        tokenDatabase.addTokenPrototype(t);
                        tokenDatabase.tagToken(t.getTokenId(), t.getDefaultTags());
                        tokenDatabase.tagToken(t.getTokenId(), TokenDatabase.RECENTLY_ADDED);
                        if (mTagPathToAdd != null) {
                            tokenDatabase.tagToken(t.getTokenId(), mTagPathToAdd);
                        }
                        return true;
                    } catch (IOException e) {
                        Log.e(TAG, "Error saving token image", e);
                        return false;
                    }
                }

                protected void onPostExecute(Boolean success) {
                    if (success) {
                        setResult(Activity.RESULT_OK);
                    } else {
                        Toast toast =
                                Toast.makeText(getApplicationContext(),
                                        getString(R.string.error_saving_token), Toast.LENGTH_LONG);
                        toast.show();
                        setResult(Activity.RESULT_CANCELED);
                    }
                    finish();
                }
            };

            saveTokenTask.execute();
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

    private void showProgressDialog(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setTitle(getString(R.string.please_wait));
        }
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }
}
