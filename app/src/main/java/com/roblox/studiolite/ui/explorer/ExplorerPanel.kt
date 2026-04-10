package com.roblox.studiolite.ui.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roblox.studiolite.data.model.RbxInstance
import com.roblox.studiolite.ui.StudioViewModel
import com.roblox.studiolite.ui.theme.*

fun classIcon(className: String): String = when (className) {
    "Workspace"           -> "🌍"
    "Players"             -> "👥"
    "Lighting"            -> "💡"
    "ReplicatedFirst"     -> "🔁"
    "ReplicatedStorage"   -> "📦"
    "StarterGui"          -> "🖼️"
    "StarterPack"         -> "🎒"
    "StarterPlayer"       -> "🎮"
    "Teams"               -> "🏳️"
    "ServerScriptService" -> "⚙️"
    "ServerStorage"       -> "🗄️"
    "Script"              -> "📜"
    "LocalScript"         -> "📄"
    "ModuleScript"        -> "🧩"
    "Part","WedgePart","CornerWedgePart","TrussPart","SpawnLocation" -> "🧱"
    "Model"               -> "📐"
    "Folder"              -> "📁"
    "Frame"               -> "▬"
    "TextLabel"           -> "T"
    "TextButton"          -> "🔘"
    "ImageLabel"          -> "🖼"
    "ScreenGui"           -> "📱"
    "RemoteEvent"         -> "📡"
    "RemoteFunction"      -> "🔧"
    "Sound"               -> "🔊"
    "PointLight","SpotLight","SurfaceLight" -> "💡"
    "Camera"              -> "📷"
    "ParticleEmitter"     -> "✨"
    "Fire"                -> "🔥"
    "Smoke"               -> "💨"
    "Humanoid"            -> "🧍"
    "Tool"                -> "🔨"
    else                  -> "📋"
}

@Composable
fun ExplorerPanel(
    viewModel: StudioViewModel,
    onScriptDoubleClick: (RbxInstance) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(StudioSurface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(30.dp)
                .background(StudioToolbar).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Explorer", color = StudioText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.insertPart() }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Add, "Insert", tint = StudioTextDim, modifier = Modifier.size(14.dp))
            }
            if (viewModel.selectedInstance != null) {
                IconButton(onClick = { viewModel.deleteSelected() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = StudioAccentRed, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = { viewModel.duplicateSelected() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, "Dup", tint = StudioTextDim, modifier = Modifier.size(14.dp))
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(viewModel.dataModel.children.size) { idx ->
                InstanceTreeItem(
                    instance = viewModel.dataModel.children[idx],
                    viewModel = viewModel,
                    depth = 0,
                    onScriptDoubleClick = onScriptDoubleClick
                )
            }
        }
    }
}

@Composable
fun InstanceTreeItem(
    instance: RbxInstance,
    viewModel: StudioViewModel,
    depth: Int,
    onScriptDoubleClick: (RbxInstance) -> Unit
) {
    val isSelected = viewModel.selectedInstance?.referent == instance.referent
    val isExpanded = viewModel.isExpanded(instance.referent)
    val hasChildren = instance.children.isNotEmpty()
    val isScript = instance.className in listOf("Script", "LocalScript", "ModuleScript")
    var tapCount by remember { mutableStateOf(0) }
    var lastTap by remember { mutableStateOf(0L) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) StudioSelected else Color.Transparent)
                .clickable {
                    val now = System.currentTimeMillis()
                    if (now - lastTap < 400) {
                        // Double tap
                        if (hasChildren) viewModel.toggleExpand(instance.referent)
                        if (isScript) onScriptDoubleClick(instance)
                        tapCount = 0
                    } else {
                        viewModel.selectInstance(instance)
                        tapCount = 1
                    }
                    lastTap = now
                }
                .padding(start = (depth * 14 + 4).dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasChildren) {
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    null, tint = StudioTextDim,
                    modifier = Modifier.size(14.dp).clickable { viewModel.toggleExpand(instance.referent) }
                )
            } else {
                Spacer(Modifier.size(14.dp))
            }
            Spacer(Modifier.width(2.dp))
            Text(classIcon(instance.className), fontSize = 11.sp)
            Spacer(Modifier.width(3.dp))
            Text(
                instance.name,
                color = if (isSelected) Color.White else StudioTextDim,
                fontSize = 11.sp, maxLines = 1
            )
        }
        if (isExpanded && hasChildren) {
            instance.children.forEach { child ->
                InstanceTreeItem(child, viewModel, depth + 1, onScriptDoubleClick)
            }
        }
    }
}
