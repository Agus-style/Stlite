package com.roblox.studiolite.ui.toolbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roblox.studiolite.ui.common.HorizontalStudioDivider
import com.roblox.studiolite.ui.common.PanelHeader
import com.roblox.studiolite.ui.theme.*

data class ToolboxItem(
    val name: String, val icon: String, val className: String,
    val description: String = "", val isBuiltin: Boolean = true,
)

val BUILTIN_PARTS = listOf(
    ToolboxItem("Part",           "🧱", "Part",               "Basic block part"),
    ToolboxItem("Wedge",          "📐", "WedgePart",          "Wedge-shaped part"),
    ToolboxItem("Corner Wedge",   "🔺", "CornerWedgePart",    "Corner wedge"),
    ToolboxItem("Truss",          "🔩", "TrussPart",          "Truss structure"),
    ToolboxItem("Model",          "📦", "Model",              "Group container"),
    ToolboxItem("Folder",         "📁", "Folder",             "Organization folder"),
    ToolboxItem("Script",         "📜", "Script",             "Server-side script"),
    ToolboxItem("LocalScript",    "📄", "LocalScript",        "Client-side script"),
    ToolboxItem("ModuleScript",   "🧩", "ModuleScript",       "Shared module"),
    ToolboxItem("ScreenGui",      "📱", "ScreenGui",          "2D GUI container"),
    ToolboxItem("Frame",          "▬",  "Frame",              "UI Frame"),
    ToolboxItem("TextLabel",      "T",  "TextLabel",          "Text display"),
    ToolboxItem("TextButton",     "🔘", "TextButton",         "Clickable button"),
    ToolboxItem("TextBox",        "📝", "TextBox",            "Text input"),
    ToolboxItem("ImageLabel",     "🖼", "ImageLabel",         "Image display"),
    ToolboxItem("ImageButton",    "🖼️", "ImageButton",        "Clickable image"),
    ToolboxItem("ScrollingFrame", "📜", "ScrollingFrame",     "Scrollable container"),
    ToolboxItem("RemoteEvent",    "📡", "RemoteEvent",        "Client-server event"),
    ToolboxItem("RemoteFunction", "🔧", "RemoteFunction",     "Client-server function"),
    ToolboxItem("BindableEvent",  "🔔", "BindableEvent",      "Script event"),
    ToolboxItem("Sound",          "🔊", "Sound",              "Audio player"),
    ToolboxItem("SpawnLocation",  "🚩", "SpawnLocation",      "Player spawn point"),
    ToolboxItem("PointLight",     "💡", "PointLight",         "Omnidirectional light"),
    ToolboxItem("SpotLight",      "🔦", "SpotLight",          "Spotlight"),
    ToolboxItem("BillboardGui",   "📋", "BillboardGui",       "3D world space UI"),
    ToolboxItem("SurfaceGui",     "🪟", "SurfaceGui",         "Part surface UI"),
    ToolboxItem("WeldConstraint", "🔗", "WeldConstraint",     "Weld two parts"),
    ToolboxItem("HingeConstraint","🔩", "HingeConstraint",    "Hinge joint"),
    ToolboxItem("Attachment",     "📌", "Attachment",         "Constraint anchor"),
    ToolboxItem("ParticleEmitter","✨", "ParticleEmitter",    "Particle effects"),
    ToolboxItem("Trail",          "〰️", "Trail",              "Motion trail"),
    ToolboxItem("Fire",           "🔥", "Fire",               "Fire effect"),
    ToolboxItem("Smoke",          "💨", "Smoke",              "Smoke effect"),
    ToolboxItem("Sparkles",       "⭐", "Sparkles",           "Sparkle effect"),
    ToolboxItem("Camera",         "📷", "Camera",             "Scene camera"),
    ToolboxItem("VehicleSeat",    "🚗", "VehicleSeat",        "Drive vehicles"),
    ToolboxItem("Seat",           "🪑", "Seat",               "Sit down"),
    ToolboxItem("Tool",           "🔨", "Tool",               "Player holdable tool"),
    ToolboxItem("IntValue",       "🔢", "IntValue",           "Integer value"),
    ToolboxItem("StringValue",    "🔤", "StringValue",        "String value"),
    ToolboxItem("BoolValue",      "✅", "BoolValue",          "Boolean value"),
    ToolboxItem("NumberValue",    "💯", "NumberValue",        "Number value"),
    ToolboxItem("ObjectValue",    "🎯", "ObjectValue",        "Reference value"),
    ToolboxItem("Animator",       "🎭", "Animator",           "Play animations"),
    ToolboxItem("Humanoid",       "🧍", "Humanoid",           "Character controller"),
    ToolboxItem("Sky",            "🌅", "Sky",                "Custom sky"),
    ToolboxItem("Atmosphere",     "🌫️", "Atmosphere",         "Atmosphere effects"),
)

@Composable
fun ToolboxPanel(onInsert: (ToolboxItem) -> Unit, modifier: Modifier = Modifier) {
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(true) } // true=grid

    val filtered = remember(searchQuery) {
        if (searchQuery.isEmpty()) BUILTIN_PARTS
        else BUILTIN_PARTS.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.className.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = modifier.background(StudioSurface)) {
        PanelHeader("Toolbox") {
            IconButton(onClick = { viewMode = !viewMode }, modifier = Modifier.size(24.dp)) {
                Icon(if (viewMode) Icons.Default.ViewList else Icons.Default.GridView,
                    null, tint = StudioTextDim, modifier = Modifier.size(14.dp))
            }
        }
        HorizontalStudioDivider()

        // Search
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF252525))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = StudioTextOff, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                textStyle = TextStyle(color = StudioText, fontSize = 12.sp),
                singleLine = true, modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) Text("Search...", color = StudioTextOff, fontSize = 12.sp)
                    inner()
                }
            )
            if (searchQuery.isNotEmpty()) {
                Icon(Icons.Default.Clear, null, tint = StudioTextOff,
                    modifier = Modifier.size(14.dp).clickable { searchQuery = "" })
            }
        }
        HorizontalStudioDivider()

        if (viewMode) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered) { item ->
                    Column(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF252525))
                            .clickable { onInsert(item) }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(item.icon, fontSize = 20.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(item.name, color = StudioTextDim, fontSize = 9.sp,
                            maxLines = 1, fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered.size) { idx ->
                    val item = filtered[idx]
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onInsert(item) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.icon, fontSize = 16.sp, modifier = Modifier.width(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(item.name, color = StudioText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            if (item.description.isNotEmpty())
                                Text(item.description, color = StudioTextOff, fontSize = 10.sp)
                        }
                    }
                    HorizontalStudioDivider()
                }
            }
        }
    }
}
