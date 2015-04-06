package com.tbocek.android.combatmap.model;

import android.graphics.PointF;
import android.graphics.RectF;

import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.model.primitives.BoundingRectangle;
import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * This is a data class that collects everything that makes up the current map
 * state, including drawing, tokens, and the current view transformation so that
 * this can all be stored independently of the view.
 * 
 * @author Tim Bocek
 * 
 */
public final class MapData {

    /**
     * Initial zoom level of newly created maps. Corresponds to 1 square = 64
     * pixels. Not density independent.
     */
    private static final int INITIAL_ZOOM = 64;

    /**
     * The map data currently being managed.
     */
    private static MapData instance;

    /**
     * Version level of this map data, used for saving/loading.
     * Version History:
     * 0: Initial Version
     * 1: Added background image collection.
     * 2: Added last tag accessed on the map.
     */
    private static final int MAP_DATA_VERSION = 2;

    /**
     * Command history to use for the annotations.
     */
    private CommandHistory mAnntationCommandHistory = new CommandHistory();

    /**
     * Annotation lines.
     */
    private LineCollection mAnnotationLines = new LineCollection(
            this.mAnntationCommandHistory);

    /**
     * Command history object to use for the background and associated fog of
     * war.
     */
    private CommandHistory mBackgroundCommandHistory = new CommandHistory();

    /**
     * Lines that represent the fog of war.
     */
    private LineCollection mBackgroundFogOfWar = new LineCollection(
            this.mBackgroundCommandHistory);

    /**
     * Collection of background images.
     */
    private BackgroundImageCollection mBackgroundImages =
            new BackgroundImageCollection(this.mBackgroundCommandHistory);

    /**
     * Background lines.
     */
    private LineCollection mBackgroundLines = new LineCollection(
            this.mBackgroundCommandHistory);

    /**
     * Command history to use for the GM notes and associated fog of war.
     */
    private CommandHistory mGmNotesCommandHistory = new CommandHistory();

    /**
     * Notes for the GM that are not visible when combat is occurring.
     */
    private LineCollection mGmNoteLines = new LineCollection(
            this.mGmNotesCommandHistory);

    /**
     * Lines that represent the fog of war.
     */
    private LineCollection mGmNotesFogOfWar = new LineCollection(
            this.mGmNotesCommandHistory);

    /**
     * The grid to draw.
     */
    private Grid mGrid = new Grid();
    
    private String mLastTag = TokenDatabase.ALL;

    /**
     * Command history to use for combat tokens.
     */
    private CommandHistory mTokenCollectionCommandHistory =
            new CommandHistory();

    /**
     * Tokens that have been placed on the map.
     */
    private TokenCollection mTokens = new TokenCollection(
            this.mTokenCollectionCommandHistory);

    /**
     * Transformation from world space to screen space.
     */
    private CoordinateTransformer mTransformer = new CoordinateTransformer(0,
            0, INITIAL_ZOOM);

    /**
     * Clears the map by loading a new instance.
     */
    public static void clear() {
        instance = new MapData();
    }

    /**
     * Creates, populates, and returns a new MapData object from the given
     * deserialization stream.
     * 
     * @param s
     *            The stream to read from.
     * @param tokens
     *            Token database to load tokens from.
     * @return The created map data.
     * @throws IOException
     *             On deserialization error.
     */
    public static MapData deserialize(MapDataDeserializer s,
            TokenDatabase tokens) throws IOException {
        @SuppressWarnings("unused")
        int mapDataVersion = s.readInt();
        MapData data = new MapData();
        data.mGrid = Grid.deserialize(s);
        data.mTransformer = CoordinateTransformer.deserialize(s);
        data.mTokens.deserialize(s, tokens);
        data.mBackgroundLines.deserialize(s);
        data.mBackgroundFogOfWar.deserialize(s);
        data.mGmNoteLines.deserialize(s);
        data.mGmNotesFogOfWar.deserialize(s);
        data.mAnnotationLines.deserialize(s);
        if (mapDataVersion >= 1) {
            data.mBackgroundImages.deserialize(s);
        }
        if (mapDataVersion >= 2) {
        	data.mLastTag = s.readString();
        }

        return data;
    }

    /**
     * Gets the current map data instance.
     * 
     * @return The map data.
     */
    public static MapData getInstance() {
        if (instance == null) {
            clear();
        }
        return instance;
    }


    /**
     * Return a *copy* of the singleton map data instance.  This should be treated as immutable,
     * as changes will not be reflected in the singleton instance.
     * @return A copy of the map data.
     */
    public static MapData getCopy() {
        if (instance == null) return null;
        return new MapData(instance);
    }

    /**
     * @return True if an instance of MapData has already been created.
     */
    public static boolean hasValidInstance() {
        return instance != null;
    }

    /**
     * Clears the currently loaded map data, forcing a reload next time map data
     * is needed.
     */
    public static void invalidate() {
        instance = null;
    }

    /**
     * Loads the map data from an input stream.
     * 
     * @param input
     *            The stream to read from.
     * @param tokens
     *            Token database to use when creating tokens.
     * @return The final state of the map data deserializer, to check for error messages.
     */
    public static MapDataDeserializer loadFromStream(final InputStream input,
            TokenDatabase tokens) {
        InputStreamReader inReader = new InputStreamReader(input);
        BufferedReader reader = new BufferedReader(inReader);
        MapDataDeserializer s = new MapDataDeserializer(reader);
        try {
            instance = MapData.deserialize(s, tokens);
        } catch (Exception e) {
            s.addError(e.toString());
        } finally {
            try {
                reader.close();
                inReader.close();
            } catch (IOException e) {
                // Intentionally swallowed.
            }
        }
        return s;
    }

    /**
     * Saves the map data to a stream.
     * 
     * @param output
     *            The stream to write to.
     * @throws IOException
     *             On write error.
     */
    public void saveToStream(final OutputStream output)
            throws IOException {
        OutputStreamWriter outWriter = new OutputStreamWriter(output);
        BufferedWriter writer = new BufferedWriter(outWriter);
        MapDataSerializer s = new MapDataSerializer(writer);
        // TODO: Buffer this into memory and then write on a different thread.
        try {
            serialize(s);
        } finally {
            writer.close();
            outWriter.close();
        }
    }

    /**
     * Private constructor - singleton pattern.
     */
    private MapData() {

    }

    /**
     * Copy constructor
     */
    private MapData(MapData copyFrom) {
        this.mAnnotationLines = new LineCollection(copyFrom.mAnnotationLines);
        this.mBackgroundLines = new LineCollection(copyFrom.mBackgroundLines);
        this.mGmNoteLines = new LineCollection(copyFrom.mGmNoteLines);
        this.mBackgroundFogOfWar = new LineCollection(copyFrom.mBackgroundFogOfWar);
        this.mGmNotesFogOfWar = new LineCollection(copyFrom.mGmNotesFogOfWar);
        this.mAnntationCommandHistory = null;
        this.mBackgroundCommandHistory = null;
        this.mGmNotesCommandHistory = null;
        this.mTokenCollectionCommandHistory = null;

        this.mTokens = new TokenCollection(copyFrom.mTokens);
        this.mTransformer = new CoordinateTransformer(copyFrom.mTransformer);
        this.mBackgroundImages = new BackgroundImageCollection(copyFrom.mBackgroundImages);
        this.mGrid = new Grid(copyFrom.mGrid);
    }

    /**
     * @return the annotationLines
     */
    public LineCollection getAnnotationLines() {
        return this.mAnnotationLines;
    }

    /**
     * @return the fog of war lines.
     */
    public LineCollection getBackgroundFogOfWar() {
        return this.mBackgroundFogOfWar;
    }

    /**
     * @return The collection of background images.
     */
    public BackgroundImageCollection getBackgroundImages() {
        return this.mBackgroundImages;
    }

    /**
     * @return the backgroundLines
     */
    public LineCollection getBackgroundLines() {
        return this.mBackgroundLines;
    }

    /**
     * Gets a rectangle that encompasses the entire map.
     * 
     * @return The bounding rectangle.
     */
    public BoundingRectangle getBoundingRectangle() {
        BoundingRectangle r = new BoundingRectangle();

        r.updateBounds(this.mBackgroundLines.getBoundingRectangle());
        r.updateBounds(this.mAnnotationLines.getBoundingRectangle());
        r.updateBounds(this.mGmNoteLines.getBoundingRectangle());
        r.updateBounds(this.getTokens().getBoundingRectangle());

        return r;
    }

    /**
     * @return Private notes for the GM
     */
    public LineCollection getGmNoteLines() {
        return this.mGmNoteLines;
    }

    /**
     * @return the fog of war lines.
     */
    public LineCollection getGmNotesFogOfWar() {
        return this.mGmNotesFogOfWar;
    }

    /**
     * @return the grid
     */
    public Grid getGrid() {
        return this.mGrid;
    }

    /**
     * Gets the screen space bounding rectangle of the entire map based on the
     * current screen space transformation.
     * 
     * @param marginsPx
     *            Margin to apply to each edge.
     * @return The bounding rectangle.
     */
    public RectF getScreenSpaceBoundingRect(int marginsPx) {
        BoundingRectangle worldSpaceRect = this.getBoundingRectangle();
        PointF ul =
                this.mTransformer.worldSpaceToScreenSpace(
                        worldSpaceRect.getXMin(), worldSpaceRect.getYMin());
        PointF lr =
                this.mTransformer.worldSpaceToScreenSpace(
                        worldSpaceRect.getXMax(), worldSpaceRect.getYMax());
        return new RectF(ul.x - marginsPx, ul.y - marginsPx, lr.x + marginsPx,
                lr.y + marginsPx);
    }

    /**
     * @return the tokens
     */
    public TokenCollection getTokens() {
        return this.mTokens;
    }

    /**
     * @return the transformer
     */
    public CoordinateTransformer getWorldSpaceTransformer() {
        return this.mTransformer;
    }

    /**
     * Saves the entire MapData to the given serialization stream.
     * 
     * @param s
     *            The stream to save to.
     * @throws IOException
     *             On serialization error.
     */
    public void serialize(MapDataSerializer s) throws IOException {
        s.serializeInt(MAP_DATA_VERSION);
        this.mGrid.serialize(s);
        this.mTransformer.serialize(s);
        this.mTokens.serialize(s);
        this.mBackgroundLines.serialize(s);
        this.mBackgroundFogOfWar.serialize(s);
        this.mGmNoteLines.serialize(s);
        this.mGmNotesFogOfWar.serialize(s);
        this.mAnnotationLines.serialize(s);
        this.mBackgroundImages.serialize(s);
        s.serializeString(mLastTag != null ? mLastTag : TokenDatabase.ALL);
    }

    /**
     * @param grid
     *            the grid to set
     */
    public void setGrid(final Grid grid) {
        this.mGrid = grid;
    }
    
    public void setLastTag(String lastTag) {
    	mLastTag = lastTag;
    }
    
    public String getLastTag() {
    	return mLastTag;
    }

    /**
     * @return The set of tokens currently visible on the screen.
     */
    public Set<String> getVisibleTokenIds(int screenWidth, int screenHeight) {
        CoordinateTransformer trans = getGrid().gridSpaceToScreenSpaceTransformer(getWorldSpaceTransformer());
        PointF wsOrigin = trans.screenSpaceToWorldSpace(0, 0);
        float wsWidth = trans.screenSpaceToWorldSpace(screenWidth);
        float wsHeight = trans.screenSpaceToWorldSpace(screenHeight);
        RectF worldSpaceBounds = new RectF(wsOrigin.x, wsOrigin.y, wsOrigin.x + wsWidth, wsOrigin.y + wsHeight);

        Set<String> tokens = new HashSet<String>();

        for (BaseToken t: getTokens().asList()) {
            if (t.getBoundingRectangle().testClip(worldSpaceBounds)) {
                tokens.add(t.getTokenId());
            }
        }
        return tokens;
    }

    CoordinateTransformer mSavedTransformer = null;
    CoordinateTransformer mCastTransformer = null;

    public void saveView() {
        mSavedTransformer = new CoordinateTransformer(mTransformer);
    }

    public void restoreView() {
        mTransformer = new CoordinateTransformer(mSavedTransformer);
    }

    public void castView() {
        mCastTransformer = new CoordinateTransformer(mTransformer);
    }

    public void stopCastingView() {
        mCastTransformer = null;
    }

    /**
     * @return The coordinate transformer that defines the currently saved Chromecast view, or null
     *     if the cast image is supposed to follow the on-device view.
     */
    public CoordinateTransformer getChromecastWorldSpaceTransformer() { return mCastTransformer; }
}