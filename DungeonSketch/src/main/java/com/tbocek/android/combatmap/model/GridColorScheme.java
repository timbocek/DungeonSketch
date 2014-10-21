package com.tbocek.android.combatmap.model;

import android.graphics.Color;

import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;

import java.io.IOException;

/**
 * Stores a color scheme to use when drawing the grid.
 * 
 * @author Tim Bocek
 * 
 */
public final class GridColorScheme {

    // BUILT-IN COLOR SCHEMES

    /**
     * Green on black.
     */
    public static final GridColorScheme CONSOLE = new GridColorScheme(
            Color.rgb(0, 0, 0), Color.GREEN, true);

    /**
     * Dark red on grey.
     */
    public static final GridColorScheme DUNGEON = new GridColorScheme(
            Color.rgb(64, 64, 64), Color.rgb(64, 0, 0), true);

    /**
     * Dark blue on dark green.
     */
    public static final GridColorScheme FOREST = new GridColorScheme(Color.rgb(
            0, 128, 0), Color.rgb(0, 0, 100), true);

    /**
     * Green on light yellow, a classic graph paper look.
     */
    public static final GridColorScheme GRAPH_PAPER = new GridColorScheme(
            Color.rgb(248, 255, 180), Color.rgb(195, 255, 114), false);

    /**
     * Dark green on light green.
     */
    public static final GridColorScheme GRASS = new GridColorScheme(Color.rgb(
            63, 172, 41), Color.rgb(11, 121, 34), false);

    /**
     * Light blue on black.
     */
    public static final GridColorScheme HOLOGRAM = new GridColorScheme(
            Color.rgb(0, 0, 0), Color.rgb(41, 162, 255), true);

    /**
     * Light blue on white.
     */
    public static final GridColorScheme ICE = new GridColorScheme(Color.WHITE,
            Color.rgb(160, 160, 255), false);

    /**
     * Black on dark blue.
     */
    public static final GridColorScheme NIGHT = new GridColorScheme(Color.rgb(
            0, 0, 102), Color.rgb(0, 0, 0), true);

    /**
     * Grey on white.
     */
    public static final GridColorScheme STANDARD = new GridColorScheme(
            Color.WHITE, Color.rgb(200, 200, 200), false);

    /**
     * The color to draw in the background.
     */
    private final int mBackgroundColor;

    /**
     * Whether the color scheme has a dark background.
     */
    private final boolean mIsDark;

    /**
     * The color to draw grid lines with.
     */
    private final int mLineColor;

    /**
     * Reads and returns a color scheme from the given stream.
     * 
     * @param s
     *            The stream to read from.
     * @return The read color stream.
     * @throws IOException
     *             On read error.
     */
    public static GridColorScheme deserialize(MapDataDeserializer s)
            throws IOException {
        s.expectObjectStart();
        int bkg = s.readInt();
        int line = s.readInt();

        // For backwards compat, BUT this is dynamically computed now.
        boolean dark = s.readBoolean();

        s.expectObjectEnd();
        return new GridColorScheme(bkg, line, dark);
    }

    /**
     * Given the name of a color scheme, returns the scheme represented by that
     * name. If the scheme is not found, returns the standard grey-on-white
     * color scheme.
     * 
     * @param name
     *            The name of the scheme to use.
     * @return The color scheme.
     */
    public static GridColorScheme fromNamedScheme(final String name) {
        if (name.equals("Graph Paper")) {
            return GRAPH_PAPER;
        }
        if (name.equals("Grass")) {
            return GRASS;
        }
        if (name.equals("Ice")) {
            return ICE;
        }
        if (name.equals("Forest")) {
            return FOREST;
        }
        if (name.equals("Night")) {
            return NIGHT;
        }
        if (name.equals("Dungeon")) {
            return DUNGEON;
        }
        if (name.equals("Hologram")) {
            return HOLOGRAM;
        }
        if (name.equals("Console")) {
            return CONSOLE;
        }
        return STANDARD;
    }

    /**
     * Constructor.
     * 
     * @param backgroundColor
     *            The color to draw in the background.
     * @param lineColor
     *            The color to draw grid lines with.
     * @param isDark
     *            Whether the grid should request that dark background versions
     *            of tokens be drawn.
     */
    public GridColorScheme(final int backgroundColor, final int lineColor,
            final boolean isDark) {
        this.mBackgroundColor = backgroundColor;
        this.mLineColor = lineColor;
        this.mIsDark = isDark;
    }

    /**
     * @return The color to draw in the background.
     */
    public int getBackgroundColor() {
        return this.mBackgroundColor;
    }

    /**
     * @return The color to draw grid lines with.
     */
    public int getLineColor() {
        return this.mLineColor;
    }

    /**
     * @return Whether the grid has a dark background.
     */
    public boolean isDark() {
        return getColorValue(mBackgroundColor) < .5;
    }

    /**
     * Writes this color scheme to the given stream.
     * 
     * @param s
     *            Serialization stream.
     * @throws IOException
     *             On load error.
     */
    public void serialize(MapDataSerializer s) throws IOException {
        s.startObject();
        s.serializeInt(this.mBackgroundColor);
        s.serializeInt(this.mLineColor);
        s.serializeBoolean(this.mIsDark);
        s.endObject();
    }

    public boolean isSecondaryDark() {
        return getColorValue(mLineColor) < .5;
    }

    private float getColorValue(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv[2];
    }
}
