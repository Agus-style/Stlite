package com.roblox.studiolite.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roblox.studiolite.ui.theme.*

// ─── Panel header ─────────────────────────────────────────────────────────────

@Composable
fun PanelHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().height(30.dp)
            .background(StudioToolbar).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = StudioText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

// ─── Dividers ─────────────────────────────────────────────────────────────────

@Composable
fun HorizontalStudioDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioDivider))
}

@Composable
fun VerticalStudioDivider(height: Dp = 32.dp) {
    Box(modifier = Modifier.padding(horizontal = 4.dp).width(1.dp).height(height).background(StudioDivider))
}

// ─── Labeled icon button ──────────────────────────────────────────────────────

@Composable
fun LabeledIconButton(
    icon: ImageVector, label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = StudioTextDim,
    isActive: Boolean = false,
    enabled: Boolean = true,
) {
    val bg = if (isActive) StudioSelected else Color.Transparent
    val fg = when { !enabled -> StudioTextOff; isActive -> Color.White; else -> tint }
    Column(
        modifier = modifier.clip(RoundedCornerShape(4.dp)).background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(18.dp))
        Text(label, color = fg, fontSize = 9.sp)
    }
}

// ─── Confirm dialog ───────────────────────────────────────────────────────────

@Composable
fun ConfirmDialog(
    title: String, message: String,
    confirmText: String = "OK", dismissText: String = "Cancel",
    onConfirm: () -> Unit, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioToolbar,
        titleContentColor = StudioText,
        textContentColor = StudioTextDim,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message, fontSize = 13.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = StudioAccentRed, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText, color = StudioTextDim) }
        }
    )
}

// ─── Log / Output ─────────────────────────────────────────────────────────────

data class LogEntry(
    val message: String,
    val level: LogLevel = LogLevel.INFO,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LogLevel { INFO, WARN, ERROR, SUCCESS }

@Composable
fun OutputPanel(logs: List<LogEntry>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(Color(0xFF141414))) {
        PanelHeader("Output")
        HorizontalStudioDivider()
        LazyColumn(modifier = Modifier.fillMaxSize().padding(4.dp), reverseLayout = true) {
            itemsIndexed(logs.reversed()) { _, log ->
                val color = when (log.level) {
                    LogLevel.INFO    -> StudioTextDim
                    LogLevel.WARN    -> StudioAccentOrange
                    LogLevel.ERROR   -> StudioAccentRed
                    LogLevel.SUCCESS -> Color(0xFF4CAF50)
                }
                Text(log.message, color = color, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}
