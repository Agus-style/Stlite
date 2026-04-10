package com.roblox.studiolite.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var showUploadDialog  by remember { mutableStateOf(false) }
    var showSettings      by remember { mutableStateOf(false) }
    var showScript        by remember { mutableStateOf(false) }
    var rightTab          by remember { mutableStateOf(RightTab.EXPLORER) }
    var bottomTab         by remember { mutableStateOf(BottomTab.PROPERTIES) }

    // Panel visibility toggles
    var showRightPanel    by remember { mutableStateOf(true) }
    var showBottomPanel   by remember { mutableStateOf(false) } // default hidden!

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

        // ── Toolbar ───────────────────────────────────────────────────────────
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

        // ── Main layout ───────────────────────────────────────────────────────
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {

            // ── Left: Viewport ────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {

                // Script editor overlay
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

                // 3D Viewport — mengisi semua sisa ruang
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
                            viewModel.selectedInstance?.let {
                                applyGizmoDelta(it, axis, delta, viewModel.currentTool)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Tombol toggle panel kanan (pojok kanan atas viewport)
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                    ) {
                        // Toggle bottom panel (Properties/Output)
                        SmallToggleButton(
                            icon  = if (showBottomPanel) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            label = "Props",
                            active = showBottomPanel,
                            onClick = { showBottomPanel = !showBottomPanel }
                        )
                        Spacer(Modifier.width(4.dp))
                        // Toggle right panel (Explorer/Toolbox)
                        SmallToggleButton(
                            icon  = if (showRightPanel) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                            label = "Panel",
                            active = showRightPanel,
                            onClick = { showRightPanel = !showRightPanel }
                        )
                    }
                }

                // ── Bottom panel (collapsible) ────────────────────────────────
                AnimatedVisibility(
                    visible = showBottomPanel,
                    enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Column {
                        // Tab bar
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF141414))
                        ) {
                            listOf(BottomTab.PROPERTIES to "Properties",
                                   BottomTab.OUTPUT     to "Output").forEach { (tab, label) ->
                                TextButton(
                                    onClick  = { bottomTab = tab },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(label,
                                        color = if (bottomTab == tab) StudioAccentBlue else StudioTextOff,
                                        fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            // Close button
                            IconButton(
                                onClick  = { showBottomPanel = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, "Hide",
                                    tint = StudioTextOff, modifier = Modifier.size(16.dp))
                            }
                        }
                        Divider(color = StudioDivider)
                        when (bottomTab) {
                            BottomTab.PROPERTIES -> PropertiesPanel(
                                viewModel = viewModel,
                                modifier  = Modifier.fillMaxWidth().height(200.dp)
                            )
                            BottomTab.OUTPUT -> OutputPanel(
                                logs     = logs,
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                        }
                    }
                }
            }

            // ── Right panel (collapsible) ─────────────────────────────────────
            AnimatedVisibility(
                visible = showRightPanel,
                enter   = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit    = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .background(StudioSurface)
                ) {
                    // Tab bar
                    Row(
                        modifier = Modifier.fillMaxWidth().background(StudioToolbar),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(RightTab.EXPLORER to "Explorer",
                               RightTab.TOOLBOX  to "Toolbox").forEach { (tab, label) ->
                            TextButton(
                                onClick  = { rightTab = tab },
                                modifier = Modifier.weight(1f).height(30.dp)
                            ) {
                                Text(label,
                                    color = if (rightTab == tab) StudioAccentBlue else StudioTextDim,
                                    fontSize = 11.sp)
                            }
                        }
                        // Close button
                        IconButton(
                            onClick  = { showRightPanel = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, "Hide",
                                tint = StudioTextOff, modifier = Modifier.size(14.dp))
                        }
                    }
                    Divider(color = StudioDivider)
                    when (rightTab) {
                        RightTab.EXPLORER -> ExplorerPanel(
                            viewModel = viewModel,
                            onScriptDoubleClick = { i ->
                                viewModel.selectInstance(i)
                                showScript = true
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        RightTab.TOOLBOX -> ToolboxPanel(
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
    }

    if (showUploadDialog) {
        UploadDialog(viewModel = viewModel, onDismiss = { showUploadDialog = false })
    }
}

// ─── Small toggle button di atas viewport ─────────────────────────────────────

@Composable
private fun SmallToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                if (active) StudioAccentBlue.copy(alpha = 0.85f)
                else Color.Black.copy(alpha = 0.55f),
                androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}
