package com.roblox.studiolite.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roblox.studiolite.api.ApiResult
import com.roblox.studiolite.api.RobloxOpenCloudApi
import com.roblox.studiolite.api.Universe
import com.roblox.studiolite.data.model.RbxInstance
import com.roblox.studiolite.data.model.RbxProperty
import com.roblox.studiolite.data.model.buildDefaultDataModel
import com.roblox.studiolite.data.rbxl.RbxlParserV2
import com.roblox.studiolite.data.rbxl.RbxlWriter
import com.roblox.studiolite.data.undo.*
import com.roblox.studiolite.ui.toolbox.ToolboxItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ToolMode { SELECT, MOVE, SCALE, ROTATE }

sealed class UploadState {
    object Idle    : UploadState()
    object Loading : UploadState()
    data class Success(val message: String) : UploadState()
    data class Error(val message: String)   : UploadState()
}

class StudioViewModel : ViewModel() {

    // ── Scene ─────────────────────────────────────────────────────────────────
    var dataModel by mutableStateOf(buildDefaultDataModel())
        private set
    var selectedInstance by mutableStateOf<RbxInstance?>(null)
        private set
    val expandedReferents = mutableStateListOf<String>()

    // ── Tool ──────────────────────────────────────────────────────────────────
    var currentTool by mutableStateOf(ToolMode.SELECT)
        private set
    var isPlaying by mutableStateOf(false)
        private set

    // ── Undo/Redo ─────────────────────────────────────────────────────────────
    private val undoManager = UndoRedoManager()
    var canUndo by mutableStateOf(false); private set
    var canRedo by mutableStateOf(false); private set

    // ── API ───────────────────────────────────────────────────────────────────
    var apiKey by mutableStateOf("")
    var uploadState by mutableStateOf<UploadState>(UploadState.Idle)
        private set
    val universes = mutableStateListOf<Universe>()
    var selectedUniverseId by mutableStateOf("")
    var selectedPlaceId    by mutableStateOf("")

    // ── File ──────────────────────────────────────────────────────────────────
    var currentFilePath by mutableStateOf<String?>(null)
        private set

    // ── Explorer ──────────────────────────────────────────────────────────────
    fun selectInstance(inst: RbxInstance?) { selectedInstance = inst }

    fun toggleExpand(referent: String) {
        if (expandedReferents.contains(referent)) expandedReferents.remove(referent)
        else expandedReferents.add(referent)
    }
    fun isExpanded(ref: String) = expandedReferents.contains(ref)

    // ── Properties ────────────────────────────────────────────────────────────
    fun updateProperty(instance: RbxInstance, key: String, newValue: String) {
        val old = instance.properties[key] ?: return
        val new = when (old) {
            is RbxProperty.StringVal -> RbxProperty.StringVal(newValue)
            is RbxProperty.IntVal    -> RbxProperty.IntVal(newValue.toIntOrNull() ?: old.value)
            is RbxProperty.FloatVal  -> RbxProperty.FloatVal(newValue.toFloatOrNull() ?: old.value)
            is RbxProperty.BoolVal   -> RbxProperty.BoolVal(newValue.lowercase() == "true")
            else -> return
        }
        val cmd = SetPropertyCommand(instance, key, old, new)
        undoManager.execute(cmd)
        refreshUndo()
        selectedInstance = null; selectedInstance = instance
    }

    // ── Insert ────────────────────────────────────────────────────────────────
    fun insertPart(parent: RbxInstance? = null) {
        val workspace = dataModel.children.firstOrNull { it.className == "Workspace" } ?: return
        val target = parent ?: workspace
        val part = RbxInstance(className = "Part", name = "Part", parent = target).apply {
            properties["Name"]     = RbxProperty.StringVal("Part")
            properties["Size"]     = RbxProperty.Vector3Val(4f, 1.2f, 2f)
            properties["Position"] = RbxProperty.Vector3Val(0f, 0.6f, 0f)
            properties["Anchored"] = RbxProperty.BoolVal(false)
            properties["BrickColor"] = RbxProperty.StringVal("Bright blue")
        }
        val cmd = InsertInstanceCommand(target, part)
        undoManager.execute(cmd)
        refreshUndo()
        expandedReferents.add(target.referent)
        selectInstance(part)
    }

    fun insertFromToolbox(item: ToolboxItem) {
        val workspace = dataModel.children.firstOrNull { it.className == "Workspace" }
        val starterGui = dataModel.children.firstOrNull { it.className == "StarterGui" }
        val serverScript = dataModel.children.firstOrNull { it.className == "ServerScriptService" }

        val target = when (item.className) {
            "ScreenGui", "Frame", "TextLabel", "TextButton", "TextBox",
            "ImageLabel", "ImageButton", "ScrollingFrame", "ViewportFrame",
            "BillboardGui", "SurfaceGui" -> starterGui ?: workspace
            "Script", "ModuleScript" -> serverScript ?: workspace
            "LocalScript" -> starterGui ?: workspace
            else -> workspace
        } ?: return

        val inst = RbxInstance(className = item.className, name = item.name, parent = target).apply {
            properties["Name"] = RbxProperty.StringVal(item.name)
            properties["Archivable"] = RbxProperty.BoolVal(true)
            // Set default size for Parts
            if (item.className == "Part") {
                properties["Size"]     = RbxProperty.Vector3Val(4f, 1.2f, 2f)
                properties["Position"] = RbxProperty.Vector3Val(0f, 0.6f, 0f)
                properties["Anchored"] = RbxProperty.BoolVal(false)
                properties["BrickColor"] = RbxProperty.StringVal("Bright blue")
            }
        }
        val cmd = InsertInstanceCommand(target, inst)
        undoManager.execute(cmd)
        refreshUndo()
        expandedReferents.add(target.referent)
        selectInstance(inst)
    }

    fun deleteSelected() {
        val inst = selectedInstance ?: return
        val parent = inst.parent ?: return
        val idx = parent.children.indexOf(inst)
        val cmd = DeleteInstanceCommand(parent, inst, idx)
        undoManager.execute(cmd)
        refreshUndo()
        selectedInstance = null
    }

    fun duplicateSelected() {
        val inst = selectedInstance ?: return
        val parent = inst.parent ?: return
        val copy = RbxInstance(className = inst.className, name = inst.name + "_Copy", parent = parent).apply {
            properties.putAll(inst.properties)
            properties["Name"] = RbxProperty.StringVal(inst.name + "_Copy")
        }
        val cmd = InsertInstanceCommand(parent, copy)
        undoManager.execute(cmd)
        refreshUndo()
        selectInstance(copy)
    }

    // ── Undo/Redo ─────────────────────────────────────────────────────────────
    fun undo() { undoManager.undo(); refreshUndo(); selectedInstance = null }
    fun redo() { undoManager.redo(); refreshUndo(); selectedInstance = null }
    private fun refreshUndo() { canUndo = undoManager.canUndo; canRedo = undoManager.canRedo }

    // ── Tool ──────────────────────────────────────────────────────────────────
    fun setTool(tool: ToolMode) { currentTool = tool }
    fun togglePlay() { isPlaying = !isPlaying }

    // ── File I/O ──────────────────────────────────────────────────────────────
    fun openFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: return@launch
                val parsed = RbxlParserV2().parse(stream)
                stream.close()
                withContext(Dispatchers.Main) {
                    dataModel = parsed
                    selectedInstance = null
                    expandedReferents.clear()
                    undoManager.clear()
                    refreshUndo()
                    currentFilePath = uri.path
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun saveFile(context: Context): File? {
        return try {
            val file = File(context.filesDir, "scene.rbxl")
            RbxlWriter().write(dataModel, file.outputStream())
            currentFilePath = file.absolutePath
            file
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    fun newScene() {
        dataModel = buildDefaultDataModel()
        selectedInstance = null
        expandedReferents.clear()
        undoManager.clear()
        refreshUndo()
        currentFilePath = null
    }

    // ── Publish ───────────────────────────────────────────────────────────────
    fun loadUniverses() {
        if (apiKey.isBlank()) return
        viewModelScope.launch {
            when (val r = RobloxOpenCloudApi(apiKey).listUniverses()) {
                is ApiResult.Success -> { universes.clear(); universes.addAll(r.data) }
                is ApiResult.Error   -> uploadState = UploadState.Error("${r.code}: ${r.message}")
                is ApiResult.NetworkError -> uploadState = UploadState.Error(r.exception.message ?: "Network error")
            }
        }
    }

    fun publishToRoblox(context: Context) {
        if (apiKey.isBlank() || selectedUniverseId.isBlank() || selectedPlaceId.isBlank()) {
            uploadState = UploadState.Error("Isi API Key, Universe ID, dan Place ID dulu!")
            return
        }
        uploadState = UploadState.Loading
        viewModelScope.launch {
            val file = saveFile(context) ?: run {
                uploadState = UploadState.Error("Gagal save file")
                return@launch
            }
            when (val r = RobloxOpenCloudApi(apiKey).publishPlace(selectedUniverseId, selectedPlaceId, file)) {
                is ApiResult.Success -> uploadState = UploadState.Success("✅ Published! Version: ${r.data.versionNumber}")
                is ApiResult.Error   -> uploadState = UploadState.Error("Error ${r.code}: ${r.message}")
                is ApiResult.NetworkError -> uploadState = UploadState.Error(r.exception.message ?: "Network error")
            }
        }
    }
}
