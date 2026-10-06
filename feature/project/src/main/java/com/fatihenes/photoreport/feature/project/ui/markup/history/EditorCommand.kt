package com.fatihenes.photoreport.feature.project.ui.markup.history

import com.fatihenes.photoreport.core.model.MarkupItem
import com.fatihenes.photoreport.core.model.PhotoAdjustments
import com.fatihenes.photoreport.core.model.PhotoCropState
import com.fatihenes.photoreport.core.model.PhotoEditorSession

sealed interface EditorCommand {

    fun apply(session: PhotoEditorSession): PhotoEditorSession
    fun revert(session: PhotoEditorSession): PhotoEditorSession

    data class AddItem(val item: MarkupItem) : EditorCommand {
        override fun apply(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(items = session.items + item)

        override fun revert(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(items = session.items.filter { it.id != item.id })
    }

    data class AddItems(val items: List<MarkupItem>) : EditorCommand {
        override fun apply(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(items = session.items + items)

        override fun revert(session: PhotoEditorSession): PhotoEditorSession {
            val addedIds = items.map { it.id }.toSet()
            return session.copy(items = session.items.filter { it.id !in addedIds })
        }
    }

    data class DeleteItem(val item: MarkupItem) : EditorCommand {
        override fun apply(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(items = session.items.filter { it.id != item.id })

        override fun revert(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(items = session.items + item)
    }

    data class DeleteItems(val items: List<MarkupItem>) : EditorCommand {
        override fun apply(session: PhotoEditorSession): PhotoEditorSession {
            val removedIds = items.map { it.id }.toSet()
            return session.copy(items = session.items.filter { it.id !in removedIds })
        }

        override fun revert(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(items = session.items + items)
    }

    data class ModifyItem(val before: MarkupItem, val after: MarkupItem) : EditorCommand {
        override fun apply(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(items = session.items.map { if (it.id == after.id) after else it })

        override fun revert(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(items = session.items.map { if (it.id == before.id) before else it })
    }

    data class ModifyItems(val before: List<MarkupItem>, val after: List<MarkupItem>) : EditorCommand {
        override fun apply(session: PhotoEditorSession): PhotoEditorSession {
            val afterMap = after.associateBy { it.id }
            return session.copy(items = session.items.map { afterMap[it.id] ?: it })
        }

        override fun revert(session: PhotoEditorSession): PhotoEditorSession {
            val beforeMap = before.associateBy { it.id }
            return session.copy(items = session.items.map { beforeMap[it.id] ?: it })
        }
    }

    data class ReorderItems(val before: List<MarkupItem>, val after: List<MarkupItem>) : EditorCommand {
        override fun apply(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(items = after)

        override fun revert(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(items = before)
    }

    data class UpdateAdjustments(val before: PhotoAdjustments, val after: PhotoAdjustments) : EditorCommand {
        override fun apply(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(adjustments = after)

        override fun revert(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(adjustments = before)
    }

    data class UpdateCrop(val before: PhotoCropState, val after: PhotoCropState) : EditorCommand {
        override fun apply(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(cropState = after)

        override fun revert(session: PhotoEditorSession): PhotoEditorSession =
            session.copy(cropState = before)
    }

    data class BatchCommand(val commands: List<EditorCommand>) : EditorCommand {
        override fun apply(session: PhotoEditorSession): PhotoEditorSession {
            var curr = session
            for (cmd in commands) {
                curr = cmd.apply(curr)
            }
            return curr
        }

        override fun revert(session: PhotoEditorSession): PhotoEditorSession {
            var curr = session
            for (cmd in commands.asReversed()) {
                curr = cmd.revert(curr)
            }
            return curr
        }
    }
}
