package com.roblox.studiolite.ui.gizmo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.roblox.studiolite.data.model.RbxInstance
import com.roblox.studiolite.data.model.RbxProperty
import com.roblox.studiolite.ui.ToolMode
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow
import kotlin.math.sqrt

private val AxisX = Color(0xFFFF3333)
private val AxisY = Color(0xFF33FF33)
private val AxisZ = Color(0xFF3399FF)
private val AxisSel = Color(0xFFFFFF33)

enum class GizmoAxis { X, Y, Z }

@Composable
fun GizmoOverlay(
    instance: RbxInstance?,
    toolMode: ToolMode,
    screenX: Float,
    screenY: Float,
    onDelta: (axis: GizmoAxis, delta: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (instance == null || toolMode == ToolMode.SELECT) return

    var activeAxis by remember { mutableStateOf<GizmoAxis?>(null) }

    Canvas(
        modifier = modifier.fillMaxSize().pointerInput(instance.referent, toolMode) {
            detectDragGestures(
                onDragStart = { offset ->
                    activeAxis = hitTest(offset, Offset(screenX, screenY), 60f)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val ax = activeAxis ?: return@detectDragGestures
                    val delta = when (ax) {
                        GizmoAxis.X -> dragAmount.x
                        GizmoAxis.Y -> -dragAmount.y
                        GizmoAxis.Z -> dragAmount.x
                    }
                    onDelta(ax, delta * 0.05f)
                },
                onDragEnd = { activeAxis = null }
            )
        }
    ) {
        val center = Offset(screenX, screenY)
        val arm = 60f
        when (toolMode) {
            ToolMode.MOVE   -> drawMoveGizmo(center, arm, activeAxis)
            ToolMode.SCALE  -> drawScaleGizmo(center, arm, activeAxis)
            ToolMode.ROTATE -> drawRotateGizmo(center, arm, activeAxis)
            else -> {}
        }
    }
}

private fun hitTest(touch: Offset, center: Offset, arm: Float): GizmoAxis? {
    val threshold = 14f
    fun distToLine(p: Offset, a: Offset, b: Offset): Float {
        val ab = b - a; val ap = p - a
        val t = ((ap.x * ab.x + ap.y * ab.y) / (ab.x * ab.x + ab.y * ab.y + 1e-6f)).coerceIn(0f, 1f)
        val c = a + Offset(ab.x * t, ab.y * t)
        return sqrt((p.x - c.x).pow(2) + (p.y - c.y).pow(2))
    }
    val xEnd = center + Offset(arm, 0f)
    val yEnd = center + Offset(0f, -arm)
    val zEnd = center + Offset(arm * 0.6f, arm * 0.6f)
    return when {
        distToLine(touch, center, xEnd) < threshold -> GizmoAxis.X
        distToLine(touch, center, yEnd) < threshold -> GizmoAxis.Y
        distToLine(touch, center, zEnd) < threshold -> GizmoAxis.Z
        else -> null
    }
}

private fun DrawScope.drawMoveGizmo(center: Offset, arm: Float, active: GizmoAxis?) {
    val arr = 10f
    fun drawArrow(end: Offset, color: Color) {
        drawLine(color, center, end, strokeWidth = 3f, cap = StrokeCap.Round)
        val dir = Offset(end.x - center.x, end.y - center.y)
        val len = sqrt(dir.x * dir.x + dir.y * dir.y)
        val n = Offset(dir.x / len, dir.y / len)
        val perp = Offset(-n.y, n.x)
        drawLine(color, end, end - Offset(n.x * arr, n.y * arr) + Offset(perp.x * arr / 2, perp.y * arr / 2), strokeWidth = 2f)
        drawLine(color, end, end - Offset(n.x * arr, n.y * arr) - Offset(perp.x * arr / 2, perp.y * arr / 2), strokeWidth = 2f)
    }
    drawArrow(center + Offset(arm, 0f),           if (active == GizmoAxis.X) AxisSel else AxisX)
    drawArrow(center + Offset(0f, -arm),           if (active == GizmoAxis.Y) AxisSel else AxisY)
    drawArrow(center + Offset(arm * .6f, arm * .6f), if (active == GizmoAxis.Z) AxisSel else AxisZ)
    drawCircle(Color.White, 5f, center)
}

private fun DrawScope.drawScaleGizmo(center: Offset, arm: Float, active: GizmoAxis?) {
    val box = 8f
    fun drawScaleArm(end: Offset, color: Color) {
        drawLine(color, center, end, strokeWidth = 3f)
        drawRect(color, topLeft = end - Offset(box / 2, box / 2), size = Size(box, box))
    }
    drawScaleArm(center + Offset(arm, 0f),             if (active == GizmoAxis.X) AxisSel else AxisX)
    drawScaleArm(center + Offset(0f, -arm),             if (active == GizmoAxis.Y) AxisSel else AxisY)
    drawScaleArm(center + Offset(arm * .6f, arm * .6f), if (active == GizmoAxis.Z) AxisSel else AxisZ)
    drawCircle(Color.White, 5f, center)
}

private fun DrawScope.drawRotateGizmo(center: Offset, arm: Float, active: GizmoAxis?) {
    drawArc(if (active == GizmoAxis.X) AxisSel else AxisX,
        0f, 180f, false,
        topLeft = center + Offset(-arm, -arm * 0.4f),
        size = Size(arm * 2, arm * 0.8f), style = Stroke(3f))
    drawArc(if (active == GizmoAxis.Y) AxisSel else AxisY,
        270f, 180f, false,
        topLeft = center + Offset(-arm, -arm),
        size = Size(arm * 2, arm * 2), style = Stroke(3f))
    drawArc(if (active == GizmoAxis.Z) AxisSel else AxisZ,
        45f, 180f, false,
        topLeft = center + Offset(-arm * .7f, -arm * .7f),
        size = Size(arm * 1.4f, arm * 1.4f), style = Stroke(3f))
    drawCircle(Color.White, 5f, center)
}

fun applyGizmoDelta(instance: RbxInstance, axis: GizmoAxis, delta: Float, toolMode: ToolMode) {
    when (toolMode) {
        ToolMode.MOVE -> {
            val pos = instance.properties["Position"] as? RbxProperty.Vector3Val
                ?: RbxProperty.Vector3Val(0f, 0f, 0f)
            instance.properties["Position"] = when (axis) {
                GizmoAxis.X -> pos.copy(x = pos.x + delta)
                GizmoAxis.Y -> pos.copy(y = pos.y + delta)
                GizmoAxis.Z -> pos.copy(z = pos.z + delta)
            }
        }
        ToolMode.SCALE -> {
            val size = instance.properties["Size"] as? RbxProperty.Vector3Val
                ?: RbxProperty.Vector3Val(4f, 1.2f, 2f)
            instance.properties["Size"] = when (axis) {
                GizmoAxis.X -> size.copy(x = maxOf(0.05f, size.x + delta))
                GizmoAxis.Y -> size.copy(y = maxOf(0.05f, size.y + delta))
                GizmoAxis.Z -> size.copy(z = maxOf(0.05f, size.z + delta))
            }
        }
        else -> {}
    }
}
