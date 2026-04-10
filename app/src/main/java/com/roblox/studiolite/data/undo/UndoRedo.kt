package com.roblox.studiolite.data.undo

import com.roblox.studiolite.data.model.RbxInstance
import com.roblox.studiolite.data.model.RbxProperty

// ─── Command interface ────────────────────────────────────────────────────────

interface StudioCommand {
    val description: String
    fun execute()
    fun undo()
}

// ─── UndoRedoManager ─────────────────────────────────────────────────────────

class UndoRedoManager(private val maxHistory: Int = 50) {
    private val undoStack = ArrayDeque<StudioCommand>()
    private val redoStack = ArrayDeque<StudioCommand>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun execute(command: StudioCommand) {
        command.execute()
        undoStack.addLast(command)
        redoStack.clear()
        if (undoStack.size > maxHistory) undoStack.removeFirst()
    }

    fun undo() { val cmd = undoStack.removeLastOrNull() ?: return; cmd.undo(); redoStack.addLast(cmd) }
    fun redo() { val cmd = redoStack.removeLastOrNull() ?: return; cmd.execute(); undoStack.addLast(cmd) }
    fun clear() { undoStack.clear(); redoStack.clear() }
}

// ─── Commands ─────────────────────────────────────────────────────────────────

class InsertInstanceCommand(
    private val parent: RbxInstance,
    private val instance: RbxInstance,
) : StudioCommand {
    override val description = "Insert ${instance.className}"
    override fun execute() { parent.addChild(instance) }
    override fun undo()    { parent.children.remove(instance) }
}

class DeleteInstanceCommand(
    private val parent: RbxInstance,
    private val instance: RbxInstance,
    private val index: Int,
) : StudioCommand {
    override val description = "Delete ${instance.name}"
    override fun execute() { parent.children.remove(instance) }
    override fun undo()    { parent.children.add(index.coerceAtMost(parent.children.size), instance) }
}

class RenameInstanceCommand(
    private val instance: RbxInstance,
    private val oldName: String,
    private val newName: String,
) : StudioCommand {
    override val description = "Rename to $newName"
    override fun execute() { instance.name = newName; instance.properties["Name"] = RbxProperty.StringVal(newName) }
    override fun undo()    { instance.name = oldName; instance.properties["Name"] = RbxProperty.StringVal(oldName) }
}

class SetPropertyCommand(
    private val instance: RbxInstance,
    private val key: String,
    private val oldValue: RbxProperty,
    private val newValue: RbxProperty,
) : StudioCommand {
    override val description = "Set $key"
    override fun execute() { instance.properties[key] = newValue }
    override fun undo()    { instance.properties[key] = oldValue }
}

class MoveInstanceCommand(
    private val instance: RbxInstance,
    private val oldParent: RbxInstance,
    private val newParent: RbxInstance,
    private val oldIndex: Int,
) : StudioCommand {
    override val description = "Move ${instance.name}"
    override fun execute() { oldParent.children.remove(instance); newParent.addChild(instance) }
    override fun undo()    { newParent.children.remove(instance); oldParent.children.add(oldIndex.coerceAtMost(oldParent.children.size), instance) }
}
