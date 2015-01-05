package com.tbocek.android.combatmap.model;

import android.graphics.Canvas;
import android.util.Log;

import com.google.common.collect.Lists;
import com.tbocek.android.combatmap.TokenDatabase;
import com.tbocek.android.combatmap.model.io.MapDataDeserializer;
import com.tbocek.android.combatmap.model.io.MapDataSerializer;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.model.primitives.BoundingRectangle;
import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.model.primitives.PointF;
import com.tbocek.android.combatmap.model.primitives.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Encapsulates a set of tokens that have been placed on the grid.
 * 
 * @author Tim Bocek
 * 
 */
public final class TokenCollection implements UndoRedoTarget {

    private static final String TAG = "TokenCollection";
    /**
     * Command that is checkpointed to while modifying a token, so that the
     * state can be saved for undo/redo.
     */
    private transient ModifyTokenCommand mBuildingCommand;

    /**
     * Command history that supports undo and redo of token operations.
     */
    private CommandHistory mCommandHistory;

    /**
     * The tokens that have been added to the grid.
     */
    private final List<BaseToken> mTokens = new ArrayList<BaseToken>();

    /**
     * Constructor.
     * 
     * @param history
     *            Command history to use for undo/redo operations.
     */
    public TokenCollection(CommandHistory history) {
        this.mCommandHistory = history;
    }

    /**
     * Copy Constructor
     * @param copyFrom TokenCollection to copy from.
     */
    public TokenCollection(TokenCollection copyFrom) {
        for (BaseToken t: copyFrom.mTokens) {
            try {
                mTokens.add(t.clone());
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "Clone failed in copy operation", e);
            }
        }
    }

    /**
     * Adds a token to the collection.
     * 
     * @param t
     *            The token to add.
     */
    public void addToken(final BaseToken t) {
        AddTokenCommand c = new AddTokenCommand(this, t);
        this.mCommandHistory.execute(c);
    }

    public List<BaseToken> asList() {
        return this.mTokens;
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
     * Sets up this TokenCollection to create a command that modifies the given
     * token. The current state of this token will be duplicated and saved for
     * undo purposes.
     * 
     * @param t
     *            The token to checkpoint.
     */
    public void checkpointToken(BaseToken t) {
        ArrayList<BaseToken> l = Lists.newArrayList();
        l.add(t);
        this.checkpointTokens(l);
    }

    /**
     * Sets up this TokenCollection to create a command that modifies the given
     * list of tokens. The current state of these tokens will be duplicated and
     * saved for undo purposes.
     * 
     *            List of tokens to checkpoint.
     */
    public void checkpointTokens(Collection<BaseToken> l) {
        this.mBuildingCommand = new ModifyTokenCommand(l);
        this.mBuildingCommand.checkpointBeforeState();
    }

    /**
     * Adds an entry to the command history based on the previously checkpointed
     * token. The created command will swap the token's state between a copy of
     * the state when this method was called, and the checkpointed state. The
     * checkpoint is cleared after this method is called.
     */
    public void createCommandHistory() {
        if (this.mBuildingCommand != null) {
            this.mBuildingCommand.checkpointAfterState();
            this.mCommandHistory.addToCommandHistory(this.mBuildingCommand);
            this.mBuildingCommand = null;
        }
    }

    /**
     * Replace all placeholder tokens with actual tokens.
     * 
     * @param tokenDatabase
     *            The database to load new tokens from.
     */
    public void deplaceholderize(TokenDatabase tokenDatabase) {
        for (int i = 0; i < this.mTokens.size(); ++i) {
            BaseToken realToken =
                    this.mTokens.get(i).deplaceholderize(tokenDatabase);
            if (realToken != this.mTokens.get(i)) {
                this.mTokens.get(i).copyAttributesTo(realToken);
                this.mTokens.set(i, realToken);
            }
        }
    }

    /**
     * Populates this token collection from the given deserialization stream.
     * 
     * @param s
     *            The stream to load from.
     * @param tokenDatabase
     *            Token database to use when creating tokens.
     * @throws IOException
     *             On read error.
     */
    public void deserialize(MapDataDeserializer s, TokenDatabase tokenDatabase)
            throws IOException {
        int arrayLevel = s.expectArrayStart();
        while (s.hasMoreArrayItems(arrayLevel)) {
            this.mTokens.add(BaseToken.deserialize(s, tokenDatabase));
        }
        s.expectArrayEnd();
    }

    /**
     * Draws all tokens.
     * 
     * @param canvas
     *            The canvas to draw on.
     * @param transformer
     *            Transformer from grid space to screen space.
     * @param isDark
     *            Whether to draw as if on a dark background.
     * @param isManipulable
     *            Whether tokens can currently be manipulated.
     */
    public void drawAllTokens(final Canvas canvas,
            final CoordinateTransformer transformer, boolean isDark,
            boolean isManipulable) {
        for (BaseToken token : this.mTokens) {
            token.drawInPosition(canvas, transformer, isDark,
                    isManipulable);
        }
    }

    /**
     * Computes and returns a bounding rectangle that can contain all the
     * tokens.
     * 
     * @return The bounding rectangle.
     */
    public BoundingRectangle getBoundingRectangle() {
        BoundingRectangle r = new BoundingRectangle();
        for (BaseToken t : this.mTokens) {
            r.updateBounds(t.getBoundingRectangle());
        }
        return r;
    }

    /**
     * Given a point in screen space, returns the token under that point.
     * 
     * @param p
     *            The point in screen space.
     * @param transformer
     *            Grid space to screen space transformation.
     * @return The token under the point, or null if no tokens.
     */
    public BaseToken getTokenUnderPoint(final PointF p,
            final CoordinateTransformer transformer) {
        for (BaseToken token : this.mTokens) {
            float distance =
                    Util.distance(p, transformer
                            .worldSpaceToScreenSpace(token
                                    .getLocation()));
            if (distance < transformer.worldSpaceToScreenSpace(token.getSize() / 2)) {
                return token;
            }
        }
        return null;
    }

    /**
     * Checks whether placing a token with the given radius at the give location
     * would intersect any other tokens.
     * 
     * @param point
     *            Center of the token to try placing here.
     * @param radius
     *            Radius of the token to try placing here.
     * @return True if the location is unoccupied, False if there would be an
     *         intersection.
     */
    private boolean
    isLocationUnoccupied(final PointF point, final double radius) {
        for (BaseToken t : this.mTokens) {
            if (Util.distance(point, t.getLocation()) < radius + t.getSize()
                    / 2) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds a location on the given grid to place this token, and places the
     * token there. This allows the token to snap to the grid.
     * 
     * @param t
     *            The token to place.
     * @param attemptedPoint
     *            The location where this token should try to be placed.
     * @param grid
     *            The grid to snap to.
     * @param tokensSnapToIntersections
     *            Whether to place new tokens on line intersections as opposed
     *            to grid spaces.
     */
    public void placeTokenNearby(final BaseToken t,
            final PointF attemptedPoint, final Grid grid,
            boolean tokensSnapToIntersections) {
        int attemptedDistance = 0;
        PointF point = attemptedPoint;
        // Continually increment the attempted distance until an open grid space
        // is found. This is guaranteed to succeed. Note that there are some
        // inefficiencies here (the center point is tried four times, each
        // corner of a square is tried twice, etc). I don't care. This runs
        // fast enough for reasonable token collections on screen.
        point =
                grid.getNearestSnapPoint(point, tokensSnapToIntersections
                        ? 0
                                : t.getSize());
        while (true) {
            // Go clockwise around the size of a square centered on the
            // originally attempted point and with sized of
            // length attemptedDistance*2

            // Across the top
            // The -attemptedDistance + 1 ensures a nice spiral pattern
            for (int i = -attemptedDistance + 1; i <= attemptedDistance; ++i) {
                if (this.tryToPlaceHere(t, new PointF(point.x + i, point.y
                        - attemptedDistance))) {
                    return;
                }
            }

            // Down the right
            for (int i = -attemptedDistance; i <= attemptedDistance; ++i) {
                if (this.tryToPlaceHere(t, new PointF(point.x
                        + attemptedDistance, point.y + i))) {
                    return;
                }
            }

            // Across the bottom
            for (int i = attemptedDistance; i >= -attemptedDistance; --i) {
                if (this.tryToPlaceHere(t, new PointF(point.x + i, point.y
                        + attemptedDistance))) {
                    return;
                }
            }

            // Up the left
            for (int i = attemptedDistance; i >= -attemptedDistance; --i) {
                if (this.tryToPlaceHere(t, new PointF(point.x
                        - attemptedDistance, point.y + i))) {
                    return;
                }
            }
            attemptedDistance++;
        }
    }

    /**
     * Redoes the current operation in the token collection's command history.
     */
    @Override
    public void redo() {
        this.mCommandHistory.redo();
    }

    /**
     * Removes a collection of tokens from the collection.
     * 
     * @param tokens
     *            The tokens to remove.
     */
    public void removeAll(final Collection<BaseToken> tokens) {
        RemoveTokensCommand c = new RemoveTokensCommand(this, tokens);
        this.mCommandHistory.execute(c);
    }

    /**
     * Interrupts the current action that has been checkpointed, and restores
     * the checkpointed token to its initial state.
     */
    public void restoreCheckpointedTokens() {
        if (this.mBuildingCommand != null) {
            this.mBuildingCommand.undo();
            this.mBuildingCommand = null;
        }
    }

    /**
     * Saves this token collection to the given serialization stream.
     * 
     * @param s
     *            The stream to save to.
     * @throws IOException
     *             On write error.
     */
    public void serialize(MapDataSerializer s) throws IOException {
        s.startArray();
        for (BaseToken t : this.mTokens) {
            t.serialize(s);
        }
        s.endArray();
    }

    /**
     * Tries to place the token at the specified location. If a token is already
     * here, returns False. If not, sets the tokens location and returns True.
     * 
     * @param t
     *            The token to try to place.
     * @param point
     *            The location at which to try to place the token.
     * @return True if successfully placed.
     */
    private boolean tryToPlaceHere(final BaseToken t, final PointF point) {
        if (this.isLocationUnoccupied(point, t.getSize() / 2)) {
            t.setLocation(point);
            return true;
        }
        return false;
    }

    /**
     * Undoes the current operation in the token collection's command history.
     */
    @Override
    public void undo() {
        this.mCommandHistory.undo();
    }

    /**
     * Command that adds a token to the token collection.
     * 
     * @author Tim
     * 
     */
    private static class AddTokenCommand implements CommandHistory.Command {

        /**
         * Token collection to modify.
         */
        private final TokenCollection mCollection;

        /**
         * Token to add to the collection.
         */
        private final BaseToken mToAdd;

        /**
         * 
         * @param collection
         *            The token collection to modify.
         * @param toAdd
         *            The token to add.
         */
        public AddTokenCommand(TokenCollection collection, BaseToken toAdd) {
            this.mCollection = collection;
            this.mToAdd = toAdd;
        }

        @Override
        public void execute() {
            this.mCollection.mTokens.add(this.mToAdd);
        }

        @Override
        public boolean isNoop() {
            return false;
        }

        @Override
        public void undo() {
            this.mCollection.mTokens.remove(this.mToAdd);

        }
    }

    /**
     * Represents a command that modifies a token.
     * 
     * @author Tim
     * 
     */
    private static class ModifyTokenCommand implements CommandHistory.Command {

        private static final String TAG = "ModifyTokenCommand";
        /**
         * State of the token after modification.
         */
        private final List<BaseToken> mAfterState = Lists.newArrayList();

        /**
         * State of the token before modification.
         */
        private final List<BaseToken> mBeforeState = Lists.newArrayList();

        /**
         * The token that this command modifies. Should always be the "live"
         * version of the token.
         */
        private final List<BaseToken> mTokensToModify;

        /**
         * Constructor.
         * 
         * @param tokens
         *            List of tokens that this command modifies.
         */
        public ModifyTokenCommand(Collection<BaseToken> tokens) {
            this.mTokensToModify = new ArrayList<BaseToken>(tokens);
        }

        /**
         * Saves the final state of the token being modified.
         */
        public void checkpointAfterState() {
            for (BaseToken t : this.mTokensToModify) {
                try {
                    this.mAfterState.add(t.clone());
                } catch (CloneNotSupportedException e) {
                    Log.e(TAG, "Clone failed when checkpointing state", e);
                }
            }
        }

        /**
         * Saves the initial state of the token being modified.
         */
        public void checkpointBeforeState() {
            for (BaseToken t : this.mTokensToModify) {
                try {
                    this.mBeforeState.add(t.clone());
                } catch (CloneNotSupportedException e) {
                    Log.e(TAG, "Clone failed when checkpointing state", e);
                }
            }
        }

        @Override
        public void execute() {
            for (int i = 0; i < this.mTokensToModify.size(); ++i) {
                this.mAfterState.get(i).copyAttributesTo(
                        this.mTokensToModify.get(i));
            }
        }

        @Override
        public boolean isNoop() {
            return false;
        }

        @Override
        public void undo() {
            for (int i = 0; i < this.mTokensToModify.size(); ++i) {
                this.mBeforeState.get(i).copyAttributesTo(
                        this.mTokensToModify.get(i));
            }
        }

    }

    /**
     * Command that removes a token from the token collection.
     * 
     * @author Tim
     * 
     */
    private static class RemoveTokensCommand implements CommandHistory.Command {

        /**
         * Token collection to modify.
         */
        private final TokenCollection mCollection;

        /**
         * Token to remove from the collection.
         */
        private final Collection<BaseToken> mToRemove;

        /**
         * Constructor.
         * 
         * @param collection
         *            The token collection to modify.
         * @param toRemove
         *            Single token to remove.
         */
        public RemoveTokensCommand(TokenCollection collection,
                BaseToken toRemove) {
            Collection<BaseToken> arr = Lists.newArrayList();
            arr.add(toRemove);
            this.mCollection = collection;
            this.mToRemove = arr;
        }

        /**
         * Constructor.
         * 
         * @param collection
         *            The token collection to modify.
         * @param toRemove
         *            Collection of tokens to remove.
         */
        public RemoveTokensCommand(TokenCollection collection,
                Collection<BaseToken> toRemove) {
            this.mCollection = collection;
            this.mToRemove = toRemove;
        }

        @Override
        public void execute() {
            for (BaseToken t : this.mToRemove) {
                this.mCollection.mTokens.remove(t);
            }
        }

        @Override
        public boolean isNoop() {
            return false;
        }

        @Override
        public void undo() {
            this.mCollection.mTokens.addAll(this.mToRemove);
        }
    }
}
