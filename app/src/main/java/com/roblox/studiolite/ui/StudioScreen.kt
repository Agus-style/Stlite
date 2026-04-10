package com.roblox.studiolite.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roblox.studiolite.ui.common.LogEntry
import com.roblox.studiolite.ui.common.LogLevel
import com.roblox.studiolite.ui.common.OutputPanel
import com.roblox.studiolite.ui.explorer.ExplorerPanel
import com.roblox.studiolite.ui.gizmo.GizmoOverlay
import com.roblox.studiolite.ui.gizmo.applyGizmoDelta
import com.roblox.studiolite.ui.properties.PropertiesPanel
import com.roblox.studiolite.ui.script.ScriptEditor
import com.roblox.studiolite.ui.settings.SettingsScreen
import com.roblox.studiolite.ui.toolbar.StudioToolbar
import com.roblox.studiolite.ui.toolbox.ToolboxPanel
import com.roblox.studiolite.ui.upload.UploadDialog
import com.roblox.studiolite.ui.viewport.StudioViewport
import com.roblox.studiolite.ui.theme.*

enum class RightTab { EXPLORER, TOOLBOX }
enum class BottomTab { PROPERTIES, OUTPUT }

@Composable
fun StudioScreen(
    viewModel: StudioViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showUploadDialog by remember { mutableStateOf(false) }
    var showSettings     by remember { mutableStateOf(false) }
    var showScript       by remember { mutableStateOf(false) }
    var rightTab         by remember { mutableStateOf(RightTab.EXPLORER) }
    var bottomTab        by remember { mutableStateOf(BottomTab.PROPERTIES) }
    val logs = remember { mutableStateListOf<LogEntry>() }

    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.openFile(context, it)
            logs.add(LogEntry("Opened: ${it.lastPathSegment}", LogLevel.SUCCESS))
        }
    }

    if (showSettings) {
        SettingsScreen(onClose = { showSettings = false }, modifier = modifier)
        return
    }

    Column(modifier = modifier.background(StudioBg)) {
        StudioToolbar(
            viewModel    = viewModel,
            onOpenFile   = { openFileLauncher.launch(arrayOf("*/*")) },
            onSaveFile   = {
                val f = viewModel.saveFile(context)
                logs.add(if (f != null) LogEntry("Saved: ${f.name}", LogLevel.SUCCESS)
                          else LogEntry("Save failed!", LogLevel.ERROR))
            },
            onNewFile    = { viewModel.newScene(); logs.add(LogEntry("New scene", LogLevel.INFO)) },
            onShowUpload = { showUploadDialog = true },
            onSettings   = { showSettings = true },
            onUndo       = { viewModel.undo() },
            onRedo       = { viewModel.redo() },
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Left: viewport + bottom panel
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val inst = viewModel.selectedInstance
                if (showScript && inst != null &&
                    inst.className in listOf("Script","LocalScript","ModuleScript")) {
                    ScriptEditor(
                        instance = inst,
                        onClose  = { showScript = false },
                        modifier = Modifier.fillMaxWidth().weight(0.45f)
                    )
                    Divider(color = StudioDivider, thickness = 2.dp)
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    StudioViewport(
                        dataModel = viewModel.dataModel,
                        modifier  = Modifier.fillMaxSize()
                    )
                    GizmoOverlay(
                        instance = viewModel.selectedInstance,
                        toolMode = viewModel.currentTool,
                        screenX  = 300f, screenY = 300f,
                        onDelta  = { axis, delta ->
                            viewModel.selectedInstance?.let { applyGizmoDelta(it, axis, delta, viewModel.currentTool) }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Divider(color = StudioDivider)
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF141414))) {
                    listOf(BottomTab.PROPERTIES to "Properties", BottomTab.OUTPUT to "Output").forEach { (tab, label) ->
                        TextButton(onClick = { bottomTab = tab }, modifier = Modifier.height(28.dp)) {
                            Text(label,
                                color = if (bottomTab == tab) StudioAccentBlue else StudioTextOff,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                when (bottomTab) {
                    BottomTab.PROPERTIES -> PropertiesPanel(viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth().height(180.dp))
                    BottomTab.OUTPUT     -> OutputPanel(logs = logs,
                        modifier = Modifier.fillMaxWidth().height(180.dp))
                }
            }

            // Right: explorer / toolbox
            Column(modifier = Modifier.width(240.dp).fillMaxHeight().background(StudioSurface)) {
                Row(modifier = Modifier.fillMaxWidth().background(StudioToolbar)) {
                    listOf(RightTab.EXPLORER to "Explorer", RightTab.TOOLBOX to "Toolbox").forEach { (tab, label) ->
                        TextButton(onClick = { rightTab = tab }, modifier = Modifier.weight(1f).height(30.dp)) {
                            Text(label,
                                color = if (rightTab == tab) StudioAccentBlue else StudioTextDim,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Divider(color = StudioDivider)
                when (rightTab) {
                    RightTab.EXPLORER -> ExplorerPanel(
                        viewModel = viewModel,
                        onScriptDoubleClick = { i -> viewModel.selectInstance(i); showScript = true },
                        modifier = Modifier.fillMaxSize()
                    )
                    RightTab.TOOLBOX  -> ToolboxPanel(
                        onInsert = { item ->
                            viewModel.insertFromToolbox(item)
                            logs.add(LogEntry("Inserted ${item.name}", LogLevel.INFO))
                            rightTab = RightTab.EXPLORER
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showUploadDialog) {
        UploadDialog(viewModel = viewModel, onDismiss = { showUploadDialog = false })
    }
}
