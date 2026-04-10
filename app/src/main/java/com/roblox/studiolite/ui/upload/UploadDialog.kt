package com.roblox.studiolite.ui.upload

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.roblox.studiolite.ui.StudioViewModel
import com.roblox.studiolite.ui.UploadState

@Composable
fun UploadDialog(
    viewModel: StudioViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showApiKey by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2D2D2D))
                .padding(20.dp)
                .width(320.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Publish to Roblox",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFFAAAAAA))
                }
            }

            Spacer(Modifier.height(16.dp))

            // API Key field
            Text("Open Cloud API Key", color = Color(0xFFAAAAAA), fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = viewModel.apiKey,
                onValueChange = { viewModel.apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showApiKey) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null,
                            tint = Color(0xFFAAAAAA)
                        )
                    }
                },
                placeholder = { Text("roblox_xxxx...", color = Color(0xFF666666), fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF0078D4),
                    unfocusedBorderColor = Color(0xFF444444)
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )

            Spacer(Modifier.height(4.dp))
            Text(
                "Dapatkan API Key di: create.roblox.com → Credentials",
                color = Color(0xFF666666),
                fontSize = 10.sp
            )

            Spacer(Modifier.height(12.dp))

            // Universe ID
            Text("Universe ID", color = Color(0xFFAAAAAA), fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = viewModel.selectedUniverseId,
                onValueChange = { viewModel.selectedUniverseId = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("123456789", color = Color(0xFF666666), fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF0078D4),
                    unfocusedBorderColor = Color(0xFF444444)
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )

            Spacer(Modifier.height(12.dp))

            // Place ID
            Text("Place ID", color = Color(0xFFAAAAAA), fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = viewModel.selectedPlaceId,
                onValueChange = { viewModel.selectedPlaceId = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("987654321", color = Color(0xFF666666), fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF0078D4),
                    unfocusedBorderColor = Color(0xFF444444)
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )

            Spacer(Modifier.height(4.dp))
            Text(
                "Universe & Place ID ada di URL game kamu di Roblox",
                color = Color(0xFF666666),
                fontSize = 10.sp
            )

            Spacer(Modifier.height(16.dp))

            // Status
            when (val state = viewModel.uploadState) {
                is UploadState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF0078D4),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Publishing...", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                is UploadState.Success -> {
                    Text(state.message, color = Color(0xFF4CAF50), fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                }
                is UploadState.Error -> {
                    Text(state.message, color = Color(0xFFFF5252), fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                }
                else -> {}
            }

            // Publish button
            Button(
                onClick = { viewModel.publishToRoblox(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.uploadState !is UploadState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE74C3C),
                    disabledContainerColor = Color(0xFF444444)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "🚀 Publish ke Roblox",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
