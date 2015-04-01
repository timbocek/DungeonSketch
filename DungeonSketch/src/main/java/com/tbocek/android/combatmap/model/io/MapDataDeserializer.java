package com.tbocek.android.combatmap.model.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Wraps a BufferedReader to provide a layer of functions specific to the map
 * data format.
 * 
 * @author Tim
 * 
 */
public class MapDataDeserializer {
    /**
     * The current number of nested arrays.
     */
    private int mArrayLevel;

    /**
     * A buffer for tokens that we have peeked at. This allows us to look ahead
     * without consuming.
     */
    private final LinkedList<String> mPeekBuffer = new LinkedList<String>();

    /**
     * The reader to read from.
     */
    private final BufferedReader mReader;

    private final LinkedList<String> mErrorLog = new LinkedList<String>();

    /**
     * Constructor.
     * 
     * @param reader
     *            The reader to read from.
     */
    public MapDataDeserializer(BufferedReader reader) {
        this.mReader = reader;
    }

    /**
     * Consumes the end of an array if we are at the end of an object. If not,
     * throws an exception.
     * 
     * @throws IOException
     *             If we are not at the end of the array as expected.
     */
    public void expectArrayEnd() throws IOException {
        String t = this.nextToken();
        if (t.equals("]")) {
            this.mArrayLevel--;
        } else {
            throw new SyncException("Expected array end, got " + t);
        }
    }

    /**
     * Consumes the start of an array if we are at the end of an object. If not,
     * throws an exception.
     * 
     * @return The array level that we *left* by starting the array.
     * @throws IOException
     *             If we are not at the start of the array as expected.
     */
    public int expectArrayStart() throws IOException {
        String t = this.nextToken();
        if (t.equals("[")) {
            this.mArrayLevel++;
        } else {
            throw new SyncException("Expected array start, got " + t);
        }
        // Return the array level at which this array will end.
        return this.mArrayLevel - 1;
    }

    public boolean isArrayEnd() throws IOException {
        String t = this.peek();
        return t.equals("]");
    }

    public boolean isObjectEnd() throws IOException {
        String t = this.peek();
        return t.equals("}");
    }

    /**
     * Consumes the end of an object if we are at the end of an object. If not,
     * throws an exception.
     * 
     * @throws IOException
     *             If we are not at the end of the object as expected.
     */
    public void expectObjectEnd() throws IOException {
        String t = this.nextToken();
        if (!t.equals("}")) {
            throw new SyncException("Expected object end, got " + t);
        }
    }

    /**
     * Scans for the object end marker, discarding the rest of the object.
     * @throws IOException On read error.
     */
    public void recoverToObjectEnd() throws IOException {
        String t = null;
        do {
            t = this.nextToken();
        } while (!t.equals("}"));
    }

    /**
     * Consumes the start of an object if we are at the end of an object. If
     * not, throws an exception.
     * 
     * @throws IOException
     *             If we are not at the start of the object as expected.
     */
    public void expectObjectStart() throws IOException {
        String t = this.nextToken();
        if (!t.equals("{")) {
            throw new SyncException("Expected object start, got " + t);
        }
    }

    /**
     * @return The array level at which the next token will be read.
     * @throws IOException
     */
    private int getNextArrayLevel() throws IOException {
        int l = this.mArrayLevel;
        String s;
        do {
            s = this.peek();
            if (s == null) {
                break;
            }

            if (s.equals("]")) {
                l--;
            }
        } while (s.equals("]"));
        return l;
    }

    /**
     * Checks whether this array has more items.
     * 
     * @param terminateAtArrayLevel
     *            One less than this array's level (should be the return value
     *            from expectArrayStart().
     * @return True if there is another item, False otherwise.
     * @throws IOException
     *             On read error (since we need to prefetch some tokens)
     */
    public boolean hasMoreArrayItems(int terminateAtArrayLevel)
            throws IOException {
        return terminateAtArrayLevel < this.getNextArrayLevel();
    }

    /**
     * Reads and returns a token.
     * 
     * @return The read token, or null if at EOF.
     * @throws IOException
     *             On read error.
     */
    private String nextToken() throws IOException {
        String s;
        if (this.mPeekBuffer.size() == 0) {
            s = this.mReader.readLine();
        } else {
            s = this.mPeekBuffer.remove();
        }

        if (s == null) {
            return null;
        }
        return s;
    }

    /**
     * Reads and returns a token from the stream, placing it in a queue of
     * tokens that have already been peeked at.
     * 
     * @return The read token.
     * @throws IOException
     *             On read error.
     */
    private String peek() throws IOException {
        String s;
        s = this.mReader.readLine();
        if (s != null) {
            this.mPeekBuffer.add(s);
        }
        return s;
    }

    /**
     * Consumes and returns a boolean value.
     * 
     * @return The read value.
     * @throws IOException
     *             On read error.
     */
    public boolean readBoolean() throws IOException {
        return !this.nextDataToken().equals("0");
    }

    /**
     * Consumes and returns a floating point value.
     * 
     * @return The read value.
     * @throws IOException
     *             On read error.
     */
    public float readFloat() throws IOException {
        return Float.parseFloat(this.nextDataToken());
    }

    /**
     * Consumes and returns an integer value.
     * 
     * @return The read value.
     * @throws IOException
     *             On read error.
     */
    public int readInt() throws IOException {
        return Integer.parseInt(this.nextDataToken());
    }

    public String nextDataToken() throws IOException {
        String t = peek();
        if (t.equals("}") || t.equals("]") || t.equals("{") || t.equals("[")) {
            throw new IOException("Expected data token, got " + t);
        }
        return nextToken();
    }

    /**
     * Consumes and returns a string value.
     * 
     * @return The read value.
     * @throws IOException
     *             On read error.
     */
    public String readString() throws IOException {
        return this.nextToken();
    }

    public void addError(String errorMessage) {
        mErrorLog.add(errorMessage);
    }

    public boolean hasErrors() {
        return !mErrorLog.isEmpty();
    }

    public Collection<String> errorMessages() {
        return mErrorLog;
    }

    /**
     * Thrown when the start/end of an object/array is not at the expected
     * place.
     * 
     * @author Tim
     * 
     */
    public static class SyncException extends IOException {
        /**
		 * 
		 */
        private static final long serialVersionUID = 9205252802710503280L;

        /**
         * Constructor.
         * 
         * @param string
         *            Exception text.
         */
        public SyncException(String string) {
            super(string);
        }
    }
}
