package com.tbocek.android.combatmap;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.IBinder;
import android.util.Log;

import com.tbocek.android.combatmap.model.MapData;
import com.tbocek.android.combatmap.model.MapDrawer;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This service accepts intents to write out the currently loaded map or the token database.
 * Its intent is to keep worker threads to save these resources alive even if there is no Dungeon
 * Sketch activity in the foreground.
 * Created by tbocek on 6/3/14.
 */
public class SaverService extends Service {
    private static final String TAG = "SaverService";

    private static final String EXTRA_DATA_TO_SAVE = "data_to_save";
    private static final int SAVE_MAP = 0;
    private static final int SAVE_TOKEN_DB = 1;

    private static final String EXTRA_MAP_NAME = "map_name";

    private BlockingQueue<Runnable> mOperationQueue = new LinkedBlockingQueue<Runnable>();


    public static void startSavingMap(Context context, String saveName) {
        Intent i = new Intent(context, SaverService.class);
        i.putExtra(EXTRA_DATA_TO_SAVE, SAVE_MAP);
        i.putExtra(EXTRA_MAP_NAME, saveName);
        context.startService(i);
    }

    public static void startSavingTokenDatabase(Context context) {
        Intent i = new Intent(context, SaverService.class);
        i.putExtra(EXTRA_DATA_TO_SAVE, SAVE_TOKEN_DB);
        context.startService(i);
    }

    @Override
    public void onCreate() {
        mBackgroundThread.start();
    }

    @Override
    public void onDestroy() {
        try {
            mBackgroundThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int operation = intent.getIntExtra(EXTRA_DATA_TO_SAVE, -1);

        switch(operation) {
            case SAVE_MAP:
                final MapData mapData = MapData.getCopy();
                if (mapData == null) break;

                final String filename = intent.getStringExtra(EXTRA_MAP_NAME);

                mOperationQueue.add(new Runnable() {
                    @Override
                    public void run() {
                        DataManager dm = new DataManager(getApplicationContext());
                        try {
                            dm.saveMapName(filename);
                            // Only save preview if not saving to temp file.
                            if (!filename.equals(DataManager.TEMP_MAP_NAME)) {
                                // TODO: pick better dimensions.
                                Bitmap bitmap = Bitmap.createBitmap(
                                        256, 256, Bitmap.Config.ARGB_8888);
                                Canvas canvas = new Canvas(bitmap);

                                new MapDrawer().drawGridLines(false).drawGmNotes(false)
                                        .drawTokens(true).areTokensManipulable(true)
                                        .drawAnnotations(false)
                                        .gmNotesFogOfWar(MapDrawer.FogOfWarMode.NOTHING)
                                        .backgroundFogOfWar(MapDrawer.FogOfWarMode.CLIP)
                                        .draw(canvas, mapData, canvas.getClipBounds());

                                dm.savePreviewImage(filename, bitmap);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error while saving map", e);
                        }
                    }
                });
                break;
            case SAVE_TOKEN_DB:
                try {
                    final TokenDatabase db = TokenDatabase.getCopy();

                    mOperationQueue.add(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                db.save(getApplicationContext());
                            } catch (Exception e) {
                                Log.e(TAG, "Error while saving token DB", e);
                            }
                        }
                    });
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }

                break;
            default:
                Log.e(TAG, "Invalid save operation passed to service: " + operation);
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Thread mBackgroundThread = new Thread() {
        @Override
        public void run() {
            while(true) {
                try {
                    Runnable r = mOperationQueue.take();
                    r.run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (mOperationQueue.isEmpty()) {
                    // TODO: stop the service on the UI thread.
                }

            }
        }
    };
}
