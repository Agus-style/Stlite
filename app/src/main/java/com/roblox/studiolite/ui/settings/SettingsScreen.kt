package com.roblox.studiolite.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.roblox.studiolite.ui.common.HorizontalStudioDivider
import com.roblox.studiolite.ui.common.PanelHeader
import com.roblox.studiolite.ui.theme.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// ─── DataStore keys ───────────────────────────────────────────────────────────

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "studio_settings")

object SettingsKeys {
    val GRID_SNAP    = booleanPreferencesKey("grid_snap")
    val SNAP_SIZE    = floatPreferencesKey("snap_size")
    val ROTATE_SNAP  = floatPreferencesKey("rotate_snap")
    val SHOW_GRID    = booleanPreferencesKey("show_grid")
    val SHOW_RULERS  = booleanPreferencesKey("show_rulers")
    val CAMERA_SPEED = floatPreferencesKey("camera_speed")
    val FONT_SIZE    = intPreferencesKey("script_font_size")
    val AUTO_SAVE    = booleanPreferencesKey("auto_save")
    val AUTO_SAVE_INTERVAL = intPreferencesKey("auto_save_interval")
    val SAVED_API_KEY = stringPreferencesKey("api_key")
    val SAVED_UNIVERSE_ID = stringPreferencesKey("universe_id")
    val SAVED_PLACE_ID = stringPreferencesKey("place_id")
}

// ─── Settings data class ──────────────────────────────────────────────────────

data class StudioSettings(
    val gridSnap: Boolean = true,
    val snapSize: Float = 1.0f,
    val rotateSnap: Float = 15.0f,
    val showGrid: Boolean = true,
    val showRulers: Boolean = false,
    val cameraSpeed: Float = 1.0f,
    val scriptFontSize: Int = 13,
    val autoSave: Boolean = true,
    val autoSaveIntervalSec: Int = 60,
    val savedApiKey: String = "",
    val savedUniverseId: String = "",
    val savedPlaceId: String = "",
)

// ─── Settings Screen ──────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Collect settings from DataStore
    val settingsFlow = remember {
        context.dataStore.data.map { prefs ->
            StudioSettings(
                gridSnap = prefs[SettingsKeys.GRID_SNAP] ?: true,
                snapSize = prefs[SettingsKeys.SNAP_SIZE] ?: 1.0f,
                rotateSnap = prefs[SettingsKeys.ROTATE_SNAP] ?: 15.0f,
                showGrid = prefs[SettingsKeys.SHOW_GRID] ?: true,
                showRulers = prefs[SettingsKeys.SHOW_RULERS] ?: false,
                cameraSpeed = prefs[SettingsKeys.CAMERA_SPEED] ?: 1.0f,
                scriptFontSize = prefs[SettingsKeys.FONT_SIZE] ?: 13,
                autoSave = prefs[SettingsKeys.AUTO_SAVE] ?: true,
                autoSaveIntervalSec = prefs[SettingsKeys.AUTO_SAVE_INTERVAL] ?: 60,
                savedApiKey = prefs[SettingsKeys.SAVED_API_KEY] ?: "",
                savedUniverseId = prefs[SettingsKeys.SAVED_UNIVERSE_ID] ?: "",
                savedPlaceId = prefs[SettingsKeys.SAVED_PLACE_ID] ?: "",
            )
        }
    }
    val settings by settingsFlow.collectAsState(initial = StudioSettings())

    fun save(block: suspend (MutablePreferences) -> Unit) {
        scope.launch { context.dataStore.edit { block(it) } }
    }

    Column(modifier = modifier.background(StudioSurface)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(StudioToolbar)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, null, tint = StudioText)
            }
            Spacer(Modifier.width(8.dp))
            Text("Settings", color = StudioText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalStudioDivider()

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // ── Section: Viewport ─────────────────────────────────────────────
            item { SectionHeader("Viewport") }
            item {
                ToggleSetting(
                    icon = Icons.Default.GridOn,
                    title = "Show Grid",
                    subtitle = "Display grid in 3D viewport",
                    value = settings.showGrid,
                    onToggle = { save { it[SettingsKeys.SHOW_GRID] = !settings.showGrid } }
                )
            }
            item {
                SliderSetting(
                    icon = Icons.Default.Speed,
                    title = "Camera Speed",
                    subtitle = "%.1fx".format(settings.cameraSpeed),
                    value = settings.cameraSpeed,
                    range = 0.1f..5.0f,
                    onValueChange = { save { p -> p[SettingsKeys.CAMERA_SPEED] = it } }
                )
            }

            // ── Section: Snapping ─────────────────────────────────────────────
            item { SectionHeader("Snapping") }
            item {
                ToggleSetting(
                    icon = Icons.Default.GridView,
                    title = "Grid Snap",
                    subtitle = "Snap parts to grid",
                    value = settings.gridSnap,
                    onToggle = { save { it[SettingsKeys.GRID_SNAP] = !settings.gridSnap } }
                )
            }
            item {
                SliderSetting(
                    icon = Icons.Default.Straighten,
                    title = "Snap Size",
                    subtitle = "${settings.snapSize} studs",
                    value = settings.snapSize,
                    range = 0.0625f..16.0f,
                    onValueChange = { save { p -> p[SettingsKeys.SNAP_SIZE] = it } }
                )
            }
            item {
                SliderSetting(
                    icon = Icons.Default.RotateRight,
                    title = "Rotate Snap",
                    subtitle = "${settings.rotateSnap}°",
                    value = settings.rotateSnap,
                    range = 1.0f..90.0f,
                    onValueChange = { save { p -> p[SettingsKeys.ROTATE_SNAP] = it } }
                )
            }

            // ── Section: Script Editor ────────────────────────────────────────
            item { SectionHeader("Script Editor") }
            item {
                SliderSetting(
                    icon = Icons.Default.TextFields,
                    title = "Font Size",
                    subtitle = "${settings.scriptFontSize}sp",
                    value = settings.scriptFontSize.toFloat(),
                    range = 9.0f..24.0f,
                    onValueChange = { save { p -> p[SettingsKeys.FONT_SIZE] = it.toInt() } }
                )
            }

            // ── Section: Auto Save ────────────────────────────────────────────
            item { SectionHeader("Auto Save") }
            item {
                ToggleSetting(
                    icon = Icons.Default.Save,
                    title = "Auto Save",
                    subtitle = "Automatically save scene",
                    value = settings.autoSave,
                    onToggle = { save { it[SettingsKeys.AUTO_SAVE] = !settings.autoSave } }
                )
            }

            // ── Section: Roblox API ───────────────────────────────────────────
            item { SectionHeader("Roblox Open Cloud") }
            item {
                TextInputSetting(
                    icon = Icons.Default.Key,
                    title = "API Key",
                    value = settings.savedApiKey,
                    placeholder = "roblox_xxxx...",
                    isPassword = true,
                    onSave = { save { p -> p[SettingsKeys.SAVED_API_KEY] = it } }
                )
            }
            item {
                TextInputSetting(
                    icon = Icons.Default.Games,
                    title = "Universe ID",
                    value = settings.savedUniverseId,
                    placeholder = "123456789",
                    onSave = { save { p -> p[SettingsKeys.SAVED_UNIVERSE_ID] = it } }
                )
            }
            item {
                TextInputSetting(
                    icon = Icons.Default.Place,
                    title = "Place ID",
                    value = settings.savedPlaceId,
                    placeholder = "987654321",
                    onSave = { save { p -> p[SettingsKeys.SAVED_PLACE_ID] = it } }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ─── Setting row components ───────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        color = StudioAccentBlue,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161616))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ToggleSetting(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = StudioTextDim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = StudioText, fontSize = 13.sp)
            Text(subtitle, color = StudioTextOff, fontSize = 11.sp)
        }
        Switch(
            checked = value,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = StudioAccentBlue,
                uncheckedThumbColor = StudioTextDim,
                uncheckedTrackColor = StudioDivider,
            )
        )
    }
    HorizontalStudioDivider()
}

@Composable
private fun SliderSetting(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = StudioTextDim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(title, color = StudioText, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(subtitle, color = StudioTextOff, fontSize = 11.sp)
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = StudioAccentBlue,
                    activeTrackColor = StudioAccentBlue,
                    inactiveTrackColor = StudioDivider,
                ),
                modifier = Modifier.height(28.dp)
            )
        }
    }
    HorizontalStudioDivider()
}

@Composable
private fun TextInputSetting(
    icon: ImageVector,
    title: String,
    value: String,
    placeholder: String,
    isPassword: Boolean = false,
    onSave: (String) -> Unit,
) {
    var editValue by remember(value) { mutableStateOf(value) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = StudioTextDim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = StudioText, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = editValue,
                onValueChange = { editValue = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(placeholder, color = StudioTextOff, fontSize = 12.sp) },
                visualTransformation = if (isPassword && editValue.isNotEmpty())
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                else androidx.compose.ui.text.input.VisualTransformation.None,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = StudioText,
                    unfocusedTextColor = StudioText,
                    focusedBorderColor = StudioAccentBlue,
                    unfocusedBorderColor = StudioDivider
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                trailingIcon = {
                    if (editValue != value) {
                        IconButton(onClick = { onSave(editValue) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Check, "Save", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )
        }
    }
    HorizontalStudioDivider()
}
