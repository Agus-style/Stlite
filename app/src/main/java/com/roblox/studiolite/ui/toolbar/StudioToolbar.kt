package com.roblox.studiolite.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roblox.studiolite.ui.StudioViewModel
import com.roblox.studiolite.ui.ToolMode
import com.roblox.studiolite.ui.theme.*

@Composable
fun StudioToolbar(
    viewModel: StudioViewModel,
    onOpenFile:   () -> Unit,
    onSaveFile:   () -> Unit,
    onNewFile:    () -> Unit,
    onShowUpload: () -> Unit,
    onSettings:   () -> Unit,
    onUndo:       () -> Unit,
    onRedo:       () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(StudioToolbar)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarIconButton(Icons.Default.Add,        "New",  onClick = onNewFile)
        ToolbarIconButton(Icons.Default.FolderOpen,  "Open", onClick = onOpenFile)
        ToolbarIconButton(Icons.Default.Save,        "Save", onClick = onSaveFile)
        VerticalDivider()
        ToolbarIconButton(Icons.Default.Undo, "Undo", enabled = viewModel.canUndo, onClick = onUndo)
        ToolbarIconButton(Icons.Default.Redo, "Redo", enabled = viewModel.canRedo, onClick = onRedo)
        VerticalDivider()
        ToolModeButton("Select", Icons.Default.NearMe,            ToolMode.SELECT, viewModel)
        ToolModeButton("Move",   Icons.Default.OpenWith,          ToolMode.MOVE,   viewModel)
        ToolModeButton("Scale",  Icons.Default.Fullscreen,        ToolMode.SCALE,  viewModel)
        ToolModeButton("Rotate", Icons.Default.Rotate90DegreesCw, ToolMode.ROTATE, viewModel)
        VerticalDivider()
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (viewModel.isPlaying) StudioAccentGreen else StudioAccentBlue)
                .clickable { viewModel.togglePlay() }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (viewModel.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(3.dp))
                Text(if (viewModel.isPlaying) "Stop" else "Play",
                    color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
        VerticalDivider()
        ToolbarIconButton(Icons.Default.ViewInAr, "Part", onClick = { viewModel.insertPart() })
        if (viewModel.selectedInstance != null) {
            ToolbarIconButton(Icons.Default.Delete, "Delete", tint = StudioAccentRed, onClick = { viewModel.deleteSelected() })
            ToolbarIconButton(Icons.Default.ContentCopy, "Dupli", onClick = { viewModel.duplicateSelected() })
        }
        Spacer(Modifier.weight(1f))
        ToolbarIconButton(Icons.Default.Settings, "Settings", onClick = onSettings)
        VerticalDivider()
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(StudioAccentRed)
                .clickable(onClick = onShowUpload)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudUpload, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("Publish", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ToolModeButton(label: String, icon: ImageVector, mode: ToolMode, vm: StudioViewModel) {
    val active = vm.currentTool == mode
    Column(
        modifier = Modifier.clip(RoundedCornerShape(4.dp))
            .background(if (active) StudioAccentBlue else Color.Transparent)
            .clickable { vm.setTool(mode) }
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = if (active) Color.White else StudioTextDim, modifier = Modifier.size(17.dp))
        Text(label, color = if (active) Color.White else StudioTextDim, fontSize = 8.sp)
    }
}

@Composable
private fun ToolbarIconButton(icon: ImageVector, label: String,
    tint: Color = StudioTextDim, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = if (enabled) tint else StudioTextOff, modifier = Modifier.size(17.dp))
        Text(label, color = if (enabled) tint else StudioTextOff, fontSize = 8.sp)
    }
}

@Composable
private fun VerticalDivider() {
    Box(modifier = Modifier.padding(horizontal = 4.dp).width(1.dp).height(30.dp).background(StudioDivider))
}
