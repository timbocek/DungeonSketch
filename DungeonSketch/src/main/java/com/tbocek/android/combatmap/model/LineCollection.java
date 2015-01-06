package com.tbocek.android.combatmap.model;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;
import com.tbocek.android.combatmap.model.primitives.BoundingRectangle;
import com.tbocek.android.combatmap.model.primitives.Circle;
import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.model.primitives.FreehandLine;
import com.tbocek.android.combatmap.model.primitives.Information;
import com.tbocek.android.combatmap.model.primitives.OnScreenText;
import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.model.primitives.Rectangle;
import com.tbocek.android.combatmap.model.primitives.Shape;
import com.tbocek.android.combatmap.model.primitives.StraightLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Provides operations over an aggregate collection of lines. Invariant: Lines
 * are sorted by descending stroke width. This is so that a thick line can be
 * used to paint an area bounded by a thin line.
 * 
 * @author Tim Bocek
 */
public final class LineCollection implements UndoRedoTarget {

    private static final String TAG = "LineCollection";
    /**
     * Undo/Redo History.
     */
    private final CommandHistory mCommandHistory;

    /**
     * The internal list of lines.
     */
    private List<Shape> mLines = Lists.newArrayList();
    
    /**
     * Cache of lines that should be drawn above the grid.
     */
    private final List<Shape> mAboveGridLines = Lists.newArrayList();
    
    /**
     * Cache of lines that should be drawn below the grid.
     */
    private final List<Shape> mBelowGridLines = Lists.newArrayList();

    /**
     * The selection managed by this line collection.
     */
    private final Selection mSelection = new Selection(this);

    /**
     * Constructor allowing multiple line collections to share one undo/redo
     * history.
     * 
     * @param history
     *            The undo/redo history.
     */
    public LineCollection(CommandHistory history) {
        this.mCommandHistory = history;
    }

    /**
     * Copy constructor
     * @param copyFrom LineCollection to copy from.
     */
    public LineCollection(LineCollection copyFrom) {
        mCommandHistory = new CommandHistory(); // Create dummy command history

        Set<Shape> aboveGridLineSet = Sets.newHashSet(copyFrom.mAboveGridLines);
        Set<Shape> belowGridLineSet = Sets.newHashSet(copyFrom.mBelowGridLines);

        for (Shape s: copyFrom.mLines) {
            try {
                Shape copy = s.clone();
                mLines.add(copy);
                if (aboveGridLineSet.contains(s)) {
                    mAboveGridLines.add(copy);
                }
                if (belowGridLineSet.contains(s)) {
                    mBelowGridLines.add(copy);
                }
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "Cloning shape failed", e);
            }
        }
    }

    @Override
    public boolean canRedo() {
        return this.mCommandHistory.canRedo();
    }

    @Override
    public boolean canUndo() {
        return this.mCommandHistory.canUndo();
    }

    /**
     * Draws all lines on the given canvas.
     *  @param canvas The canvas to draw on.
     *
     */
    public void clipFogOfWar(final Canvas canvas) {
        Rect r = canvas.getClipBounds();

        // Remove the current clip.
        canvas.clipRect(r, Op.DIFFERENCE);

        // Union together the regions that are supposed to draw.
        for (Shape maskRegion: this.mLines) {
        	maskRegion.clipFogOfWar(canvas);
        }

        canvas.clipRect(r, Op.INTERSECT);
    }

    /**
     * Factory method that creates a circle, adds it to the list of lines, and
     * returns the newly created line.
     * 
     * @param newLineColor
     *            The new line's color.
     * @param newLineStrokeWidth
     *            The new line's stroke width.
     * @return The new line.
     */
    public Shape createCircle(int newLineColor, float newLineStrokeWidth) {
        Circle l = new Circle(newLineColor, newLineStrokeWidth);
        Command c = new Command(this);
        c.addCreatedShape(l);
        this.mCommandHistory.execute(c);
        return l;
    }

    /**
     * Factory method that creates a freehand line, adds it to the list of
     * lines, and returns the newly created line.
     * 
     * @param newLineColor
     *            The new line's color.
     * @param newLineStrokeWidth
     *            The new line's stroke width.
     * @return The new line.
     */
    public Shape createFreehandLine(final int newLineColor,
            final float newLineStrokeWidth) {
        FreehandLine l = new FreehandLine(newLineColor, newLineStrokeWidth);
        Command c = new Command(this);
        c.addCreatedShape(l);
        this.mCommandHistory.execute(c);
        return l;
    }

    /**
     * Factory method that creates a rectangle, adds it to the list of lines,
     * and returns the newly created line.
     * 
     * @param newLineColor
     *            The new line's color.
     * @param newLineStrokeWidth
     *            The new line's stroke width.
     * @return The new line.
     */
    public Shape createRectangle(int newLineColor, float newLineStrokeWidth) {
        Rectangle l = new Rectangle(newLineColor, newLineStrokeWidth);
        Command c = new Command(this);
        c.addCreatedShape(l);
        this.mCommandHistory.execute(c);
        return l;
    }

    /**
     * Factory method that creates a straight line, adds it to the list of
     * lines, and returns the newly created line.
     * 
     * @param newLineColor
     *            The new line's color.
     * @param newLineStrokeWidth
     *            The new line's stroke width.
     * @return The new line.
     */
    public Shape createStraightLine(int newLineColor, float newLineStrokeWidth) {
        StraightLine l = new StraightLine(newLineColor, newLineStrokeWidth);
        Command c = new Command(this);
        c.addCreatedShape(l);
        this.mCommandHistory.execute(c);
        return l;
    }

    /**
     * Creates a new text object with the given parameters in this line
     * collection.
     * 
     * @param text
     *            Text that the object contains.
     * @param size
     *            Font size.
     * @param color
     *            Font color.
     * @param strokeWidth
     *            Stroke width (currently unused).
     * @param location
     *            Location.
     * @param transform
     *            World to screen space transformer.
     * @return The created text object.
     */
    public Shape
    createText(String text, float size, int color, float strokeWidth,
            PointF location, CoordinateTransformer transform) {
        OnScreenText t = new OnScreenText(text, size, color, strokeWidth, location, transform);
        Command c = new Command(this);
        c.addCreatedShape(t);
        this.mCommandHistory.execute(c);
        return t;
    }



    public Shape createInfo(String text, PointF newObjectLocationWorldSpace, int iconId) {
        Information m = new Information(newObjectLocationWorldSpace, text);
        m.setIcon(iconId);
        Command c = new Command(this);
        c.addCreatedShape(m);
        this.mCommandHistory.execute(c);
        return m;
    }


    /**
     * Deletes the given shape.
     * 
     * @param l
     *            The shape to delete.
     */
    public void deleteShape(Shape l) {
        if (this.mLines.contains(l)) {
            Command c = new Command(this);
            c.addDeletedShape(l);
            this.mCommandHistory.execute(c);
        }
    }

    /**
     * Populates this line collection by reading from the given stream.
     * 
     * @param s
     *            Stream to load from.
     * @throws IOException
     *             On deserialization error.
     */
    public void deserialize(MapDataDeserializer s) throws IOException {
        int arrayLevel = s.expectArrayStart();
        while (s.hasMoreArrayItems(arrayLevel)) {
        	Shape shape = Shape.deserialize(s);
            this.mLines.add(shape);
            if (shape.shouldDrawBelowGrid()) {
            	mBelowGridLines.add(shape);
            } else {
            	mAboveGridLines.add(shape);
            }
        }
        s.expectArrayEnd();
    }

    /**
     * Draws all lines on the given canvas.
     *  @param canvas
     *            The canvas to draw on.
     *
     */
    public void drawAllLines(final Canvas canvas) {
        for (Shape shape: mLines) {
            shape.applyDrawOffsetToCanvas(canvas);
            shape.draw(canvas);
            shape.revertDrawOffsetFromCanvas(canvas);
        }
    }

    /**
     * Draws all lines on the given canvas that should be drawn above the grid.
     *  @param canvas
     *            The canvas to draw on.
     *
     */
    public void drawAllLinesAboveGrid(final Canvas canvas) {
        for (Shape shape: mAboveGridLines) {
            shape.applyDrawOffsetToCanvas(canvas);
            shape.draw(canvas);
            shape.revertDrawOffsetFromCanvas(canvas);
        }
    }

    /**
     * Draws all lines on the given canvas that should be drawn below the grid.
     *  @param canvas
     *            The canvas to draw on.
     *
     */
    public void drawAllLinesBelowGrid(final Canvas canvas) {
        for (Shape shape: mBelowGridLines) {
            shape.applyDrawOffsetToCanvas(canvas);
            shape.draw(canvas);
            shape.revertDrawOffsetFromCanvas(canvas);
        }
    }

    /**
     * Draws all lines on the given canvas.
     *  @param canvas
     *            The canvas to draw on.
     *
     */
    public void drawFogOfWar(final Canvas canvas) {
        for (Shape shape: mLines) {
        	shape.drawFogOfWar(canvas);
        }
    }

    /**
     * Modifies the given text object's contents and font.
     * @param editedTextObject Text object to modify.
     * @param text The new text.
     * @param size The new font size.
     * @param transformer The world space transformer to use (needed because this draw operation
     *     needs to work in screen space).
     */
    public void editText(OnScreenText editedTextObject, String text, float size,
                         CoordinateTransformer transformer) {
        OnScreenText newText =
                new OnScreenText(text, size, editedTextObject.getColor(),
                        editedTextObject.getWidth(),
                        editedTextObject.getLocation(), transformer);
        Command c = new Command(this);
        c.addCreatedShape(newText);
        c.addDeletedShape(editedTextObject);
        this.mCommandHistory.execute(c);
    }

    /**
     * Modifies the given text object's contents and font.
     * @param editedTextObject Text object to modify.
     * @param text The new text.
     * @param iconId The new icon to use in this info point.
     */
    public void editInfo(Information editedTextObject, String text, int iconId) {
        Information newInfo =
                new Information(editedTextObject.getLocation(), text);
        newInfo.setIcon(iconId);
        Command c = new Command(this);
        c.addCreatedShape(newInfo);
        c.addDeletedShape(editedTextObject);
        this.mCommandHistory.execute(c);
    }

    /**
     * Erases all points on lines centered at a given location.
     * 
     * @param location
     *            The point in world space to center the erase on.
     * @param radius
     *            Radius around the point to erase, in world space.
     */						
    public void erase(final PointF location, final float radius) {
        for (Shape mLine : this.mLines) {
            mLine.erase(location, radius);
        }
    }

    /**
     * Finds and returns the shape under the given point. If there are multiple
     * candidates, returns one arbitrarily.
     * 
     * @param under
     *            Point that should lie in the found shape.
     * @return A shape that meets the criteria.
     */
    public Shape findShape(PointF under) {
        return this.findShape(under, null);
    }

    /**
     * Finds and returns the shape under the given point, potentially with the
     * given type. If there are multiple candidates, returns one arbitrarily.
     * 
     * @param under
     *            Point that should lie in the found shape.
     * @param requestedClass
     *            Desired class (subclass of Shape) to find, or null to find
     *            anything.
     * @return A shape that meets the criteria.
     */
    public Shape findShape(final PointF under, final Class<?> requestedClass) {
        for (Shape l : this.mLines) {
            if ((requestedClass == null || l.getClass() == requestedClass)
                    && l.contains(under)) {
                return l;
            }
        }
        return null;
    }

    /**
     * @return The bounding rectangle that bounds all lines in the collection.
     */
    public BoundingRectangle getBoundingRectangle() {
        BoundingRectangle r = new BoundingRectangle();
        for (Shape l : this.mLines) {
            r.updateBounds(l.getBoundingRectangle());
        }
        return r;
    }

    /**
     * Inserts a new line into the list of lines, making sure that the lines are
     * sorted by line width.
     * 
     * @param line
     *            The line to add.
     */
    private void insertLine(final Shape line) {
        if (this.mLines.isEmpty()) {
            this.mLines.add(line);
            return;
        }

        ListIterator<Shape> it = this.mLines.listIterator();
        while (it.hasNext()
                && this.mLines.get(it.nextIndex()).getStrokeWidth() >= line
                .getStrokeWidth()) {
            it.next();
        }
        it.add(line);
        
        if (line.shouldDrawBelowGrid()) {
	        it = this.mBelowGridLines.listIterator();
	        while (it.hasNext()
	                && this.mBelowGridLines.get(it.nextIndex()).getStrokeWidth() >= line
	                .getStrokeWidth()) {
	            it.next();
	        }
	        it.add(line);
        } else {
	        it = this.mAboveGridLines.listIterator();
	        while (it.hasNext()
	                && this.mAboveGridLines.get(it.nextIndex()).getStrokeWidth() >= line
	                .getStrokeWidth()) {
	            it.next();
	        }
	        it.add(line);
        }
    }

    /**
     * @return True if this collection has no lines in it, False otherwise.
     */
    public boolean isEmpty() {
        return this.mLines.isEmpty();
    }

    /**
     * Performs an optimization pass on the lines. This removes all erased
     * points (rather than keeping them marked as not drawn), and splits each
     * line with erased points into individual lines representing the newly
     * disjoint sections.
     */
    public void optimize() {
        Command c = new Command(this);
        for (Shape shape : this.mLines) {
            if (!shape.isValid()) {
                c.addDeletedShape(shape);
            } else if (shape.needsOptimization()) {
                List<Shape> optimizedLines =
                        shape.removeErasedPoints();
                c.addDeletedShape(shape);
                c.addCreatedShapes(optimizedLines);
            } else if (shape.hasOffset()) {
                c.addDeletedShape(shape);
                c.addCreatedShape(shape.commitDrawOffset());
            }
        }
        this.mCommandHistory.execute(c);
    }

    /**
     * Redoes an operation, if there is one to redo.
     */
    @Override
    public void redo() {
        this.mCommandHistory.redo();
    }

    /**
     * Saves this line collection to the given stream.
     * 
     * @param s
     *            Stream to save to.
     * @throws IOException
     *             On serialization error.
     */
    public void serialize(MapDataSerializer s) throws IOException {
        s.startArray();
        for (Shape shape : this.mLines) {
            if (shape.isValid()) {
                shape.serialize(s);
            }
        }
        s.endArray();
    }

    /**
     * Undoes an operation, if there is one to undo.
     */
    @Override
    public void undo() {
        this.mCommandHistory.undo();
    }
    
    private void partitionLinesBelowAboveGrid() {
    	mBelowGridLines.clear();
    	mAboveGridLines.clear();
    	
    	for (Shape s: mLines) {
    		if (s.shouldDrawBelowGrid()) {
    			mBelowGridLines.add(s);
    		} else {
    			mAboveGridLines.add(s);
    		}
    	}
    }

    public void addAll(Collection<Shape> lines) {
        Command c = new Command(this);
        c.addCreatedShapes(lines);
        this.mCommandHistory.execute(c);
    }

    public Iterable<Shape> allShapes() {
        return mLines;
    }

    public Selection startSelection() {
        mSelection.clear();
        return mSelection;
    }

    /**
     * This class represents a command that adds and deletes lines.
     * 
     * @author Tim Bocek
     * 
     */
    private static class Command implements CommandHistory.Command {
        /**
         * Lines created in this operation.
         */
        private final Collection<Shape> mCreated = new ArrayList<Shape>();

        /**
         * Lines deleted in this operation.
         */
        private final Collection<Shape> mDeleted = new ArrayList<Shape>();

        /**
         * Collection of lines to modify.
         */
        private final LineCollection mLineCollection;

        /**
         * Constructor.
         * 
         * @param lineCollection
         *            The LineCollection that this command modifies.
         */
        public Command(final LineCollection lineCollection) {
            this.mLineCollection = lineCollection;
        }

        /**
         * Adds a line to the list of lines created by this command.
         * 
         * @param l
         *            The line to add.
         */
        public void addCreatedShape(final Shape l) {
            this.mCreated.add(l);
        }

        /**
         * Adds several lines to the list of lines created by this command.
         * 
         * @param lc
         *            The lines to add.
         */
        public void addCreatedShapes(final Collection<Shape> lc) {
            this.mCreated.addAll(lc);
        }

        /**
         * Adds a line to the list of lines removed by this command.
         * 
         * @param l
         *            The line to remove.
         */
        public void addDeletedShape(final Shape l) {
            this.mDeleted.add(l);
        }

        /**
         * Executes the command on the LineCollection that this command mutates.
         */
        @Override
        public void execute() {
            // If the size of the created and deleted lists are the same, it means that these are
            // the same lines that have been modified (like moved) and should be replaced in the
            // selection so that the selection is still seen as modifying the "same" lines.
            // If the sizes are different it means that a line has been added or deleted, and
            // shouldn't modify the currently selected lines.
            if (mDeleted.size() == mCreated.size()) {
                this.mLineCollection.mSelection.replace(mDeleted, mCreated);
            }
            List<Shape> newLines = new LinkedList<Shape>();
            for (Shape l : this.mLineCollection.mLines) {
                if (!this.mDeleted.contains(l)) {
                    newLines.add(l);
                }
            }
            this.mLineCollection.mLines = newLines;

            for (Shape l : this.mCreated) {
                this.mLineCollection.insertLine(l);
            }
            this.mLineCollection.partitionLinesBelowAboveGrid();
        }

        /**
         * @return True if the command is a no-op, false if it modifies lines.
         */
        @Override
        public boolean isNoop() {
            return this.mCreated.isEmpty() && this.mDeleted.isEmpty();
        }

        /**
         * Undoes the command on the LineCollection that this command mutates.
         */
        @Override
        public void undo() {
            if (mDeleted.size() == mCreated.size()) {
                this.mLineCollection.mSelection.replace(mCreated, mDeleted);
            }
            List<Shape> newLines = new LinkedList<Shape>();
            for (Shape l : this.mLineCollection.mLines) {
                if (!this.mCreated.contains(l)) {
                    newLines.add(l);
                }
            }
            this.mLineCollection.mLines = newLines;

            for (Shape l : this.mDeleted) {
                this.mLineCollection.insertLine(l);
            }
            this.mLineCollection.partitionLinesBelowAboveGrid();
        }
    }

}
