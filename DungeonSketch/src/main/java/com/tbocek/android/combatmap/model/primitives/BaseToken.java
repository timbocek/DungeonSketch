package com.tbocek.android.combatmap.model.primitives;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Base class for token representing entities in combat.
 * 
 * @author Tim Bocek
 * 
 */
public abstract class BaseToken implements Cloneable {

    /**
     * OPTIMIZATION: Shared, preallocated StringBuffer used to concatenate
     * strings for token IDs.
     */
    private static final StringBuffer CONCAT_BUFFER = new StringBuffer(1024);

    /**
     * Stroke width to use for the custom token border if one is specified.
     */
    private static final float CUSTOM_BORDER_STROKE_WIDTH = 3;

    /**
     * Stroke width to use for the selection border.
     */
    public static final float SELECTION_STROKE_WIDTH = 4;

    /**
     * Tweak to the token's size so that it doesn't totally inscribe a grid
     * square.
     */
    private static final float TOKEN_SIZE_TWEAK = 0.9f;

    /**
     * Whether the token is bloodied.
     */
    private boolean mBloodied;

    /**
     * Cached paint object for the custom token border.
     */
    private transient Paint mCachedCustomBorderPaint;

    /**
     * OPTIMIZATION: This token's sort order. See the optimization note on
     * mCachedTokenId;
     */
    private String mCachedSortOrder;

    /**
     * OPTIMIZATION: This token's ID. Because the token ID is computed using
     * relatively expensive operations, is needed frequently, and never changes
     * throughout the object's lifetime, it can be cached here.
     */
    private String mCachedTokenId;

    /**
     * The color of the custom border if one is being used.
     */
    private int mCustomBorderColor;

    /**
     * Whether this token instance has been given a custom border.
     */
    private boolean mHasCustomBorder;

    /**
     * The token's location in grid space.
     */
    private PointF mLocation = new PointF(0, 0);

    /**
     * Whether this token is part of a selection.
     */
    private boolean mSelected;

    /**
     * The token's diameter in grid space.
     */
    private float mSize = 1.0f;

    /**
     * Whether the token should draw as a square.
     */
    private boolean mSquare;

    /**
     * Checks whether all tokens in the given collection are bloodied.
     * 
     * @param tokens
     *            Collection of tokens to check.
     * @return True if every token is bloodied, False if at least one isn't.
     */
    public static boolean allBloodied(Collection<BaseToken> tokens) {
        for (BaseToken t : tokens) {
            if (!t.isBloodied()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether all tokens in the collection have the same custom border.
     * 
     * @param tokens
     *            The collection to check.
     * @return True if all tokens have no custom border or the same custom
     *         border. False if custom border options differ in the collection.
     */
    public static boolean areTokenBordersSame(Collection<BaseToken> tokens) {
        boolean seenOne = false;
        boolean hasCustomBorder = false;
        int customBorderColor = 0;
        for (BaseToken t : tokens) {
            if (!seenOne) {
                seenOne = true;
                hasCustomBorder = t.mHasCustomBorder;
                customBorderColor = t.getCustomBorderColor();
            } else if (t.mHasCustomBorder != hasCustomBorder
                    || t.getCustomBorderColor() == customBorderColor) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether all tokens in the given collection are the same size.
     * 
     * @param tokens
     *            The list of tokens to check.
     * @return True if the tokens are the same size, False otherwise.
     */
    public static boolean areTokenSizesSame(Collection<BaseToken> tokens) {
        float commonSize = Float.NaN;
        for (BaseToken t : tokens) {
            if (Float.isNaN(commonSize)) {
                commonSize = t.getSize();
            } else if (Math.abs(commonSize - t.getSize()) > Util.FP_COMPARE_ERROR) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates and loads a token from the given deserialization stream.
     * 
     * @param s
     *            Deserialization object.
     * @param tokenDatabase
     *            Token database for token creation.
     * @return The newly created token.
     * @throws IOException
     *             On deserialization error.
     */
    public static BaseToken deserialize(MapDataDeserializer s,
            TokenDatabase tokenDatabase) throws IOException {
        s.expectObjectStart();
        String tokenId = s.readString();
        BaseToken t;

        try {
            // TODO: make tokenDatabase not create tokens for placeholders.
            t = tokenDatabase.createToken(tokenId);
        } catch (Exception e) {
            t = new PlaceholderToken(tokenId);
        }

        t.mSize = s.readFloat();
        t.mLocation.x = s.readFloat();
        t.mLocation.y = s.readFloat();
        t.mHasCustomBorder = s.readBoolean();
        t.mCustomBorderColor = s.readInt();
        t.mBloodied = s.readBoolean();
        s.expectObjectEnd();
        return t;
    }

    /**
     * Clears the custom border from the token.
     */
    public void clearCustomBorderColor() {
        this.mHasCustomBorder = false;
        this.mCachedCustomBorderPaint = null;
    }

    /**
     * @return A copy of this token.
     */
    @Override
    public abstract BaseToken clone() throws CloneNotSupportedException;

    /**
     * Copy attributes from this token to the given token.
     * 
     * @param clone
     *            Token to copy attributes into.
     * @return The token that was copied into.
     */
    public BaseToken copyAttributesTo(BaseToken clone) {
        clone.mBloodied = this.mBloodied;
        clone.mCustomBorderColor = this.mCustomBorderColor;
        clone.mHasCustomBorder = this.mHasCustomBorder;
        clone.mLocation = new PointF(this.mLocation.x, this.mLocation.y);
        clone.mSize = this.mSize;
        clone.mSquare = this.mSquare;
        return clone;
    }

    /**
     * Loads and returns a token that this placeholder represents.
     * 
     * @param database
     *            Token database to load from.
     * @return A fully loaded token if this token is a placeholder.
     */
    public BaseToken deplaceholderize(TokenDatabase database) {
        this.setSquare(database.getTokenPrototype(this.getTokenId()).isSquare());
        return this;
    }

    /**
     * Draw the token at the given coordinates and size. Everything in screen
     * space.
     * 
     * @param c
     *            Canvas to draw on.
     * @param x
     *            The x coordinate in screen space to draw the token at.
     * @param y
     *            The y coordinate in screen space to draw the token at.
     * @param radius
     *            The radius of the token in screen space.
     * @param darkBackground
     *            Whether the token is drawn against a dark background. The
     *            token can try to make its self more visible in this case.
     * @param isManipulable
     *            Whether the token can currently be manipulated.
     */
    public void draw(Canvas c, float x, float y, float radius,
            final boolean darkBackground, boolean isManipulable) {
        if (this.isBloodied()) {
            this.drawBloodiedImpl(c, x, y, radius, isManipulable);
        } else {
            this.drawImpl(c, x, y, radius, darkBackground, isManipulable);
        }

        if (this.mHasCustomBorder) {
            drawBorder(c, x, y, radius, this.getCustomBorderPaint());
        }

        if (this.mSelected) {
            // TODO: Cache this.
            Paint selectPaint = new Paint();
            selectPaint.setStrokeWidth(SELECTION_STROKE_WIDTH);
            selectPaint.setColor(Util.ICS_BLUE);
            selectPaint.setStyle(Style.STROKE);
            drawBorder(c, x, y, radius + SELECTION_STROKE_WIDTH, selectPaint);
        }
    }

    /**
     * Draws a border around the token with the given paint.
     * @param c
     * @param x
     * @param y
     * @param radius
     * @param paint
     */
    protected void drawBorder(Canvas c, float x, float y, float radius, Paint paint) {
        if (isSquare()) {
            c.drawRect(x - radius, y - radius, x + radius, y + radius, paint);
        } else {
            c.drawCircle(x, y, radius, paint);
        }
    }

    /**
     * Draw a bloodied version of the token at the given coordinates and size.
     * Everything in screen space.
     * 
     * @param c
     *            Canvas to draw on.
     * @param x
     *            The x coordinate in screen space to draw the token at.
     * @param y
     *            The y coordinate in screen space to draw the token at.
     * @param radius
     *            The radius of the token in screen space.
     * @param isManipulable
     *            Whether the token can currently be manipulated.
     */
    protected abstract void drawBloodiedImpl(Canvas c, float x, float y,
            float radius, boolean isManipulable);

    /**
     * Draw a ghost version of this token on the given canvas at the given point
     * in grid space.
     * 
     * @param c
     *            The canvas to draw the ghost on.
     * @param transformer
     *            Transformer between grid space and screen space.
     * @param ghostPoint
     *            The point to draw the ghost at.
     */
    public final void drawGhost(final Canvas c,
            final CoordinateTransformer transformer, final PointF ghostPoint) {
        PointF center = transformer.worldSpaceToScreenSpace(ghostPoint);
        float radius =
                transformer.worldSpaceToScreenSpace(this.getSize()
                        * TOKEN_SIZE_TWEAK / 2);
        this.drawGhost(c, center.x, center.y, radius);
    }

    /**
     * Draw a ghost version of the token at the given coordinates and size.
     * Everything in screen space.
     * 
     * @param c
     *            Canvas to draw on.
     * @param x
     *            The x coordinate in screen space to draw the token at.
     * @param y
     *            The y coordinate in screen space to draw the token at.
     * @param radius
     *            The radius of the token in screen space.
     */
    protected abstract void drawGhost(Canvas c, float x, float y, float radius);

    /**
     * Draw the token at the given coordinates and size. Everything in screen
     * space.
     * 
     * @param c
     *            Canvas to draw on.
     * @param x
     *            The x coordinate in screen space to draw the token at.
     * @param y
     *            The y coordinate in screen space to draw the token at.
     * @param radius
     *            The radius of the token in screen space.
     * @param darkBackground
     *            Whether the token is drawn against a dark background. The
     *            token can try to make its self more visible in this case.
     * @param isManipulable
     *            Whether the token can currently be manipulated.
     */
    protected abstract void drawImpl(Canvas c, float x, float y, float radius,
            final boolean darkBackground, boolean isManipulable);

    /**
     * Draws this token in the correct position on the given canvas.
     * 
     * @param c
     *            The canvas to draw on.
     * @param transformer
     *            Grid space to screen space transformer.
     * @param darkBackground
     *            Whether the token is drawn against a dark background. The
     *            token can try to make its self more visible in this case.
     * @param isManipulable
     *            Whether the token can currently be manipulated.
     */
    public final void drawInPosition(final Canvas c,
            final CoordinateTransformer transformer,
            final boolean darkBackground, boolean isManipulable) {
        PointF center = transformer.worldSpaceToScreenSpace(this.getLocation());
        float radius =
                transformer.worldSpaceToScreenSpace(this.getSize()
                        * TOKEN_SIZE_TWEAK / 2);

        if (this.isBloodied()) {
            this.drawBloodiedImpl(c, center.x, center.y, radius,
                    isManipulable);
        } else {
            this.drawImpl(c, center.x, center.y, radius, darkBackground,
                    isManipulable);
        }

        if (this.mHasCustomBorder) {
            drawBorder(c, center.x, center.y, radius, this.getCustomBorderPaint());
        }

        if (this.mSelected) {
            // TODO: Cache this.
            Paint selectPaint = new Paint();
            selectPaint.setStrokeWidth(SELECTION_STROKE_WIDTH);
            selectPaint.setColor(Util.ICS_BLUE);
            selectPaint.setStyle(Style.STROKE);
            drawBorder(c, center.x, center.y, radius + SELECTION_STROKE_WIDTH, selectPaint);
        }
    }

    /**
     * @return A rectangle that bounds the circle that this token draws as.
     */
    public final BoundingRectangle getBoundingRectangle() {
        BoundingRectangle r = new BoundingRectangle();
        r.updateBounds(new PointF(this.mLocation.x - this.mSize / 2,
                this.mLocation.y - this.mSize / 2));
        r.updateBounds(new PointF(this.mLocation.x + this.mSize / 2,
                this.mLocation.y + this.mSize / 2));
        return r;
    }

    /**
     * @return The color of this token's custom border.
     */
    public int getCustomBorderColor() {
        return this.mCustomBorderColor;
    }

    /**
     * Returns a paint object for the custom border, or null if no custom border
     * is requested for this token. Will create the paint object if not valid,
     * or returns a cached paint object.
     * 
     * @return The paint object.
     */
    protected Paint getCustomBorderPaint() {
        if (this.mCachedCustomBorderPaint == null && this.mHasCustomBorder) {
            this.mCachedCustomBorderPaint = new Paint();
            this.mCachedCustomBorderPaint
                    .setStrokeWidth(CUSTOM_BORDER_STROKE_WIDTH);
            this.mCachedCustomBorderPaint.setColor(this.getCustomBorderColor());

            this.mCachedCustomBorderPaint.setStyle(Style.STROKE);

        }
        return this.mCachedCustomBorderPaint;
    }

    /**
     * @return A set of tags to apply to this token by default.
     */
    public abstract Set<String> getDefaultTags();

    /**
     * @return The location of the token in grid space.
     */
    public final PointF getLocation() {
        return this.mLocation;
    }

    /**
     * @return The radius of this token in grid space
     */
    public final float getSize() {
        return this.mSize;
    }

    /**
     * Gets a global sort order incorporating this token's class and a
     * class-specific sort order. Tokens are sorted first by class, then by an
     * order specified by each class.
     * 
     * @return The sort order.
     */
    public final String getSortOrder() {
        if (this.mCachedSortOrder == null) {
            CONCAT_BUFFER.setLength(0);
            CONCAT_BUFFER.append(this.getClass().getSimpleName());
            CONCAT_BUFFER.append(this.getTokenClassSpecificSortOrder());
            this.mCachedSortOrder = CONCAT_BUFFER.toString();
        }
        return this.mCachedSortOrder;
    }

    /**
     * Gets an ID that differentiates this token from others in its class.
     * Subclasses should override this such that tokens that display the same
     * thing return the same ID. The class its self need not be represented.
     * 
     * @return The class-specific part of the ID.
     */
    protected abstract String getTokenClassSpecificId();

    /**
     * @return A sort order within this token class. By default, it is the same
     *         as the class specific ID, but subclasses can override this.
     */
    protected String getTokenClassSpecificSortOrder() {
        return this.getTokenClassSpecificId();
    }

    /**
     * Gets a unique identifier incorporating the token's type and a further
     * differentiator depending on the type.
     * 
     * @return The token ID.
     */
    public final String getTokenId() {
        if (this.mCachedTokenId == null) {
            CONCAT_BUFFER.setLength(0);
            CONCAT_BUFFER.append(this.getClass().getSimpleName());
            CONCAT_BUFFER.append(this.getTokenClassSpecificId());
            this.mCachedTokenId = CONCAT_BUFFER.toString();
        }
        return this.mCachedTokenId;
    }

    /**
     * @return Whether this token uses a custom border.
     */
    public boolean hasCustomBorder() {
        return this.mHasCustomBorder;
    }

    /**
     * @return True if the token is currently bloodied.
     */
    public final boolean isBloodied() {
        return this.mBloodied;
    }

    /**
     * @return True if this is a built-in token, False otherwise.
     */
    public boolean isBuiltIn() {
        return true;
    }

    /**
     * @return True if this token is part of a selection.
     */
    public final boolean isSelected() {
        return this.mSelected;
    }

    /**
     * If possible, permanently deletes this token from internal storage.
     * 
     * @return True if the token was deleted.
     * @throws IOException
     *             If the token was attempted to be deleted but there was an
     *             error.
     */
    public boolean maybeDeletePermanently() {
        return false;
    }

    /**
     * Moves this token the given distance relative to its current location. In
     * grid space.
     * 
     * @param distanceX
     *            Distance to move in X coordinate in grid space.
     * @param distanceY
     *            Distance to move in Y coordinate in grid space.
     */
    public final void move(final float distanceX, final float distanceY) {
        this.setLocation(new PointF(this.getLocation().x - distanceX, this
                .getLocation().y - distanceY));
    }

    /**
     * @return True if some expensive action (e.g. disk IO) is needed to load
     *         the token. Should return false if no action is needed, or if the
     *         action is already taken.
     */
    public boolean needsLoad() {
        return false;
    }

    /**
     * Saves this token to the given serialization stream.
     * 
     * @param s
     *            The serialization object to save to.
     * @throws IOException
     *             On serialization error.
     */
    public void serialize(MapDataSerializer s) throws IOException {
        s.startObject();
        s.serializeString(this.getTokenId());
        s.serializeFloat(this.mSize);
        s.serializeFloat(this.mLocation.x);
        s.serializeFloat(this.mLocation.y);
        s.serializeBoolean(this.mHasCustomBorder);
        s.serializeInt(this.getCustomBorderColor());
        s.serializeBoolean(this.mBloodied);
        s.endObject();
    }

    /**
     * Marks this token as bloodied.
     * 
     * @param bloodied
     *            Whether the token should be set to bloodied.
     */
    public final void setBloodied(final boolean bloodied) {
        this.mBloodied = bloodied;
    }

    /**
     * Sets a custom border color for the token.
     * 
     * @param color
     *            Color of the custom border.
     */
    public void setCustomBorder(int color) {
        this.mHasCustomBorder = true;
        this.mCustomBorderColor = color;
        this.mCachedCustomBorderPaint = null;
    }

    /**
     * Sets the location of the token in grid space.
     * 
     * @param location
     *            The new location in grid space.
     */
    public final void setLocation(final PointF location) {
        this.mLocation = location;
    }

    /**
     * Sets whether this token is currently selected.
     * 
     * @param selected
     *            Whether token is selected.
     */
    public final void setSelected(final boolean selected) {
        this.mSelected = selected;
    }

    /**
     * Sets the diameter of the token in grid space.
     * 
     * @param size
     *            The new diameter in grid space.
     */
    public final void setSize(final float size) {
        this.mSize = size;
    }

    public Bitmap loadBitmap(Bitmap existingBuffer) {
        return null;
    }

    public void setSquare(boolean square) {
        this.mSquare = square;
    }

    public boolean isSquare() {
        return mSquare;
    }
}