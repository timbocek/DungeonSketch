package com.tbocek.android.combatmap.model;

import java.util.Stack;

/**
 * Implements a queue of commands, and an interface to move through the queue
 * using undo and redo operations.
 * 
 * @author Tim
 * 
 */
public class CommandHistory {
    /**
     * Operations on this line collection that are available to redo.
     */
    private transient Stack<Command> mRedo = new Stack<Command>();

    /**
     * Operations on this line collection that are available to undo.
     */
    private transient Stack<Command> mUndo = new Stack<Command>();

    /**
     * Adds the given command to the command history without executing it.
     * 
     * @param command
     *            The command to add.
     */
    public void addToCommandHistory(final Command command) {
        this.mUndo.add(command);
        this.mRedo.clear();
    }

    /**
     * @return True if the redo operation can be performed, false otherwise.
     */
    public boolean canRedo() {
        return !this.mRedo.isEmpty();
    }

    /**
     * @return True if the undo operation can be performed, false otherwise.
     */
    public boolean canUndo() {
        return !this.mUndo.isEmpty();
    }

    /**
     * Executes the given command. This should not be called on commands to
     * redo.
     * 
     * @param command
     *            The command to execute.
     */
    public void execute(final Command command) {
        if (!command.isNoop()) {
            command.execute();
            this.mUndo.add(command);
            this.mRedo.clear();
        }
    }

    /**
     * Redo the last line operation.
     */
    public void redo() {
        if (this.canRedo()) {
            Command c = this.mRedo.pop();
            c.execute();
            this.mUndo.push(c);
        }
    }

    /**
     * Undo the last line operation.
     */
    public void undo() {
        if (this.canUndo()) {
            Command c = this.mUndo.pop();
            c.undo();
            this.mRedo.push(c);
        }
    }

    /**
     * Interface defining the operations that commands should support.
     * 
     * @author Tim
     * 
     */
    public interface Command {
        /**
         * Executes the command on the LineCollection that this command mutates.
         */
        void execute();

        /**
         * @return True if the command is a no-op, false if it modifies lines.
         *         noop.
         */
        boolean isNoop();

        /**
         * Undoes the command on the LineCollection that this command mutates.
         */
        void undo();
    }
}
