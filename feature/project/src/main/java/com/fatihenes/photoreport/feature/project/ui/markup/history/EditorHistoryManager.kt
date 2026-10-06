package com.fatihenes.photoreport.feature.project.ui.markup.history

import com.fatihenes.photoreport.core.model.PhotoEditorSession

class EditorHistoryManager(
    private val maxHistorySize: Int = 50,
    initialSession: PhotoEditorSession = PhotoEditorSession.EMPTY
) {
    private val undoStack = ArrayDeque<EditorCommand>()
    private val redoStack = ArrayDeque<EditorCommand>()

    var currentSession: PhotoEditorSession = initialSession
        private set

    var lastSavedSession: PhotoEditorSession = initialSession
        private set

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    val isDirty: Boolean
        get() = currentSession.isDirtyComparedTo(lastSavedSession)

    /**
     * Executes a command, updates the current session, pushes to undo stack, and clears redo stack.
     */
    fun execute(command: EditorCommand): PhotoEditorSession {
        currentSession = command.apply(currentSession)
        undoStack.addLast(command)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        return currentSession
    }

    /**
     * Reverts the most recent command.
     */
    fun undo(): PhotoEditorSession? {
        if (undoStack.isEmpty()) return null
        val command = undoStack.removeLast()
        currentSession = command.revert(currentSession)
        redoStack.addLast(command)
        return currentSession
    }

    /**
     * Re-applies the most recently reverted command.
     */
    fun redo(): PhotoEditorSession? {
        if (redoStack.isEmpty()) return null
        val command = redoStack.removeLast()
        currentSession = command.apply(currentSession)
        undoStack.addLast(command)
        return currentSession
    }

    /**
     * Commits the current session as the saved baseline (resets dirty state).
     */
    fun markAsSaved() {
        lastSavedSession = currentSession
    }

    /**
     * Resets the entire session to empty or new initial state.
     */
    fun reset(session: PhotoEditorSession = PhotoEditorSession.EMPTY) {
        undoStack.clear()
        redoStack.clear()
        currentSession = session
        lastSavedSession = session
    }
}
