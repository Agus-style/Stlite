package com.roblox.studiolite.ui.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roblox.studiolite.data.model.RbxInstance
import com.roblox.studiolite.data.model.RbxProperty
import com.roblox.studiolite.ui.StudioViewModel

// ─── Properties Panel ─────────────────────────────────────────────────────────

@Composable
fun PropertiesPanel(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val instance = viewModel.selectedInstance

    Column(modifier = modifier.background(Color(0xFF1E1E1E))) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Properties",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            if (instance != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    instance.className,
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                )
            }
        }

        if (instance == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Select an instance", color = Color(0xFF666666), fontSize = 12.sp)
            }
        } else {
            PropertiesContent(instance = instance, viewModel = viewModel)
        }
    }
}

@Composable
private fun PropertiesContent(
    instance: RbxInstance,
    viewModel: StudioViewModel
) {
    // Sort: Name first, then Archivable, then alphabetical
    val sortedProps = remember(instance.referent, instance.properties.size) {
        instance.properties.entries.sortedWith(
            compareBy {
                when (it.key) {
                    "Name" -> "0"
                    "Archivable" -> "1"
                    "ClassName" -> "2"
                    "Parent" -> "3"
                    else -> "9_${it.key}"
                }
            }
        )
    }

    // Show ClassName and Parent as read-only at top
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // ClassName (read-only)
        item {
            PropertyRow(
                name = "ClassName",
                value = instance.className,
                isReadOnly = true,
                onValueChange = {}
            )
        }
        // Name (editable)
        item {
            PropertyRow(
                name = "Name",
                value = instance.name,
                isReadOnly = false,
                onValueChange = { newVal ->
                    instance.name = newVal
                    instance.properties["Name"] = RbxProperty.StringVal(newVal)
                    viewModel.selectInstance(null)
                    viewModel.selectInstance(instance)
                }
            )
        }
        // Parent (read-only)
        item {
            PropertyRow(
                name = "Parent",
                value = instance.parent?.name ?: "nil",
                isReadOnly = true,
                onValueChange = {}
            )
        }

        // All other properties
        items(sortedProps.filter { it.key != "Name" }) { (key, prop) ->
            when (prop) {
                is RbxProperty.BoolVal -> BoolPropertyRow(
                    name = key,
                    value = prop.value,
                    onChange = { newVal ->
                        viewModel.updateProperty(instance, key, newVal.toString())
                    }
                )
                else -> PropertyRow(
                    name = key,
                    value = prop.displayString(),
                    isReadOnly = prop is RbxProperty.CFrameVal ||
                            prop is RbxProperty.RefVal ||
                            prop is RbxProperty.EnumVal,
                    onValueChange = { newVal ->
                        viewModel.updateProperty(instance, key, newVal)
                    }
                )
            }
        }
    }
}

// ─── Single property row ──────────────────────────────────────────────────────

@Composable
private fun PropertyRow(
    name: String,
    value: String,
    isReadOnly: Boolean,
    onValueChange: (String) -> Unit
) {
    var editValue by remember(value) { mutableStateOf(value) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(
                if (name.length % 2 == 0) Color(0xFF252525) else Color(0xFF1E1E1E)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Property name
        Text(
            text = name,
            color = Color(0xFFAAAAAA),
            fontSize = 11.sp,
            modifier = Modifier
                .width(140.dp)
                .padding(start = 8.dp)
        )

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF333333))
        )

        // Value
        if (isReadOnly) {
            Text(
                text = value,
                color = Color(0xFF888888),
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        } else {
            BasicTextField(
                value = editValue,
                onValueChange = { editValue = it },
                textStyle = TextStyle(
                    color = Color(0xFFE0E0E0),
                    fontSize = 11.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                singleLine = true,
                decorationBox = { inner ->
                    inner()
                }
            )
            // Commit on focus lost is handled by LaunchedEffect or key event — simplified here
            LaunchedEffect(editValue) {
                if (editValue != value) {
                    onValueChange(editValue)
                }
            }
        }
    }
}

@Composable
private fun BoolPropertyRow(
    name: String,
    value: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(
                if (name.length % 2 == 0) Color(0xFF252525) else Color(0xFF1E1E1E)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = Color(0xFFAAAAAA),
            fontSize = 11.sp,
            modifier = Modifier
                .width(140.dp)
                .padding(start = 8.dp)
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF333333))
        )

        Checkbox(
            checked = value,
            onCheckedChange = onChange,
            modifier = Modifier
                .size(20.dp)
                .padding(start = 4.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF0078D4),
                uncheckedColor = Color(0xFF666666),
                checkmarkColor = Color.White
            )
        )
    }
}
