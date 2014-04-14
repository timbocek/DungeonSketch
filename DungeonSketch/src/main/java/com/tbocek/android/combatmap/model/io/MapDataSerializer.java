package com.tbocek.android.combatmap.model.io;

import java.io.IOException;
import java.io.Writer;

/**
 * A fast serializer/deserializer that is also built to robustly allow object
 * changes. Format is object = 1 2 3 a b c [ a r r a y ] multi word string
 * subobject = 4 f [ 6.1 6.2 ] ; ; Line breaks are mandatory
 * 
 * @author Tim
 * 
 */
public class MapDataSerializer {
    /**
     * Writer to write data to.
     */
    private Writer mWriter;

    /**
     * Constructor.
     * 
     * @param writer
     *            The writer to write map data to.
     */
    public MapDataSerializer(Writer writer) {
        this.mWriter = writer;
    }

    /**
     * Writes the tokens needed to signal an array end.
     * 
     * @throws IOException
     *             On write error.
     */
    public void endArray() throws IOException {
        this.mWriter.write("]\n");
    }

    /**
     * Writes the tokens needed to signal an object end.
     * 
     * @throws IOException
     *             On write error.
     */
    public void endObject() throws IOException {
        this.mWriter.write("}\n");
    }

    /**
     * Writes a boolean value.
     * 
     * @param value
     *            Boolean to write.
     * @throws IOException
     *             On write error.
     */
    public void serializeBoolean(boolean value) throws IOException {
        this.mWriter.write(value ? "1\n" : "0\n");
    }

    /**
     * Writes a floating point value.
     * 
     * @param value
     *            Float to write.
     * @throws IOException
     *             On write error.
     */
    public void serializeFloat(float value) throws IOException {
        this.mWriter.write(Float.toString(value));
        this.mWriter.write('\n');
    }

    /**
     * Writes an integer value.
     * 
     * @param value
     *            Integer to write.
     * @throws IOException
     *             On write error.
     */
    public void serializeInt(int value) throws IOException {
        this.mWriter.write(Integer.toString(value));
        this.mWriter.write('\n');
    }

    /**
     * Writes a string value.
     * 
     * @param value
     *            String to write.
     * @throws IOException
     *             On write error.
     */
    public void serializeString(String value) throws IOException {
        this.mWriter.write(value);
        this.mWriter.write('\n');
    }

    /**
     * Writes the tokens needed to signal an array start.
     * 
     * @throws IOException
     *             On write error.
     */
    public void startArray() throws IOException {
        this.mWriter.write("[\n");
    }

    /**
     * Writes the tokens needed to signal an object start.
     * 
     * @throws IOException
     *             On write error.
     */
    public void startObject() throws IOException {
        this.mWriter.write("{\n");
    }
}
