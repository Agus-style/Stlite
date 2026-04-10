package com.roblox.studiolite.ui.script

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roblox.studiolite.data.model.RbxInstance
import com.roblox.studiolite.data.model.RbxProperty
import com.roblox.studiolite.ui.common.HorizontalStudioDivider
import com.roblox.studiolite.ui.theme.*

// ─── Luau syntax sets ─────────────────────────────────────────────────────────

private val LUAU_KEYWORDS = setOf(
    "and","break","do","else","elseif","end","false","for","function",
    "if","in","local","nil","not","or","repeat","return","then","true",
    "until","while","continue","type","export"
)

private val LUAU_BUILTINS = setOf(
    "print","warn","error","assert","pcall","xpcall","pairs","ipairs",
    "next","type","typeof","tostring","tonumber","select","unpack",
    "rawget","rawset","rawequal","rawlen","setmetatable","getmetatable",
    "require","game","workspace","script","wait","tick","time","spawn","delay",
    "task","Instance","Vector3","Vector2","CFrame","Color3","BrickColor",
    "Enum","UDim","UDim2","TweenInfo","Players","RunService",
    "UserInputService","TweenService","DataStoreService","HttpService",
    "ReplicatedStorage","ServerStorage","ServerScriptService",
    "StarterGui","StarterPlayer","coroutine","table","string","math","os"
)

// ─── Syntax highlighter ───────────────────────────────────────────────────────

fun buildHighlightedLuau(source: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < source.length) {
        when {
            source.startsWith("--[[", i) -> {
                val end = source.indexOf("]]", i + 4).let { if (it == -1) source.length else it + 2 }
                withStyle(SpanStyle(color = SyntaxComment)) { append(source.substring(i, end)) }
                i = end
            }
            source.startsWith("--", i) -> {
                val end = source.indexOf('\n', i).let { if (it == -1) source.length else it }
                withStyle(SpanStyle(color = SyntaxComment)) { append(source.substring(i, end)) }
                i = end
            }
            source[i] == '"' || source[i] == '\'' -> {
                val q = source[i]; var j = i + 1
                while (j < source.length && source[j] != q && source[j] != '\n') {
                    if (source[j] == '\\') j++; j++
                }
                j = minOf(j + 1, source.length)
                withStyle(SpanStyle(color = SyntaxString)) { append(source.substring(i, j)) }
                i = j
            }
            source.startsWith("[[", i) -> {
                val end = source.indexOf("]]", i + 2).let { if (it == -1) source.length else it + 2 }
                withStyle(SpanStyle(color = SyntaxString)) { append(source.substring(i, end)) }
                i = end
            }
            source[i].isDigit() -> {
                var j = i
                while (j < source.length && (source[j].isDigit() || source[j] == '.' || source[j] == '_')) j++
                withStyle(SpanStyle(color = SyntaxNumber)) { append(source.substring(i, j)) }
                i = j
            }
            source[i].isLetter() || source[i] == '_' -> {
                var j = i
                while (j < source.length && (source[j].isLetterOrDigit() || source[j] == '_')) j++
                val word = source.substring(i, j)
                withStyle(when {
                    word in LUAU_KEYWORDS -> SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold)
                    word in LUAU_BUILTINS -> SpanStyle(color = SyntaxFunction)
                    else -> SpanStyle(color = StudioText)
                }) { append(word) }
                i = j
            }
            else -> { withStyle(SpanStyle(color = StudioTextDim)) { append(source[i]) }; i++ }
        }
    }
}

// ─── Script Editor ────────────────────────────────────────────────────────────

@Composable
fun ScriptEditor(
    instance: RbxInstance,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sourceCode by remember(instance.referent) {
        mutableStateOf(
            (instance.properties["Source"] as? RbxProperty.StringVal)?.value
                ?: defaultScriptTemplate(instance.className)
        )
    }
    var isDirty by remember { mutableStateOf(false) }
    val vScroll = rememberScrollState()

    Column(modifier = modifier.background(Color(0xFF1E1E1E))) {
        // Tab bar
        Row(
            modifier = Modifier.fillMaxWidth().height(32.dp).background(Color(0xFF252525)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${instance.name}${if (isDirty) " ●" else ""}", color = StudioText, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Close, "Close", tint = StudioTextOff,
                    modifier = Modifier.size(14.dp).clickable(onClick = onClose))
            }
            Spacer(Modifier.weight(1f))
            if (isDirty) {
                TextButton(onClick = {
                    instance.properties["Source"] = RbxProperty.StringVal(sourceCode)
                    isDirty = false
                }, modifier = Modifier.height(28.dp)) {
                    Text("Save", color = StudioAccentBlue, fontSize = 11.sp)
                }
            }
        }
        HorizontalStudioDivider()

        // Editor
        Row(modifier = Modifier.fillMaxSize().verticalScroll(vScroll)) {
            // Line numbers
            val lineCount = sourceCode.count { it == '\n' } + 1
            Column(modifier = Modifier.background(Color(0xFF252525)).padding(horizontal = 8.dp, vertical = 8.dp)) {
                repeat(lineCount) { line ->
                    Text("${line + 1}", color = StudioTextOff, fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
                }
            }
            // Code
            Box(modifier = Modifier.fillMaxSize().padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)) {
                BasicTextField(
                    value = sourceCode,
                    onValueChange = { sourceCode = it; isDirty = true },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                        lineHeight = 18.sp, color = Color.Transparent),
                    cursorBrush = SolidColor(StudioAccentBlue),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Text(text = buildHighlightedLuau(sourceCode),
                            fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 18.sp)
                        innerTextField()
                    }
                )
            }
        }
    }
}

fun defaultScriptTemplate(className: String): String = when (className) {
    "Script" -> "-- Script\nlocal Players = game:GetService(\"Players\")\n\nprint(\"Script loaded!\")\n"
    "LocalScript" -> "-- LocalScript\nlocal Players = game:GetService(\"Players\")\nlocal player = Players.LocalPlayer\n\nprint(\"LocalScript loaded for\", player.Name)\n"
    "ModuleScript" -> "-- ModuleScript\nlocal Module = {}\n\nfunction Module.hello()\n    print(\"Hello from module!\")\nend\n\nreturn Module\n"
    else -> "-- $className\n"
}
