package com.roblox.studiolite.ui.viewport

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.roblox.studiolite.data.model.RbxInstance
import com.roblox.studiolite.data.model.RbxProperty
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

// ─── Shaders ──────────────────────────────────────────────────────────────────

private const val VERTEX_SHADER = """
attribute vec4 aPosition;
attribute vec3 aNormal;
uniform mat4 uMVP;
uniform mat4 uModel;
varying vec3 vNormal;
varying vec3 vFragPos;
void main() {
    gl_Position = uMVP * aPosition;
    vFragPos    = vec3(uModel * aPosition);
    vNormal     = mat3(uModel) * aNormal;
}
"""

private const val FRAGMENT_SHADER = """
precision mediump float;
varying vec3 vNormal;
varying vec3 vFragPos;
uniform vec3 uColor;
uniform vec3 uLightDir;
void main() {
    vec3 norm     = normalize(vNormal);
    vec3 lightDir = normalize(-uLightDir);
    float diff    = max(dot(norm, lightDir), 0.0);
    vec3 ambient  = 0.35 * uColor;
    vec3 diffuse  = diff * uColor;
    // Simple rim light
    vec3 viewDir  = normalize(vec3(0.0, 0.0, 1.0) - vFragPos);
    float rim     = pow(1.0 - max(dot(norm, viewDir), 0.0), 3.0);
    vec3 rimColor = rim * 0.15 * vec3(1.0, 1.0, 1.0);
    gl_FragColor  = vec4(ambient + diffuse + rimColor, 1.0);
}
"""

private const val GRID_VERTEX_SHADER = """
attribute vec4 aPosition;
uniform mat4 uMVP;
void main() {
    gl_Position = uMVP * aPosition;
}
"""

private const val GRID_FRAGMENT_SHADER = """
precision mediump float;
uniform vec4 uColor;
void main() {
    gl_FragColor = uColor;
}
"""

// ─── GL helpers ───────────────────────────────────────────────────────────────

private fun compileShader(type: Int, src: String): Int {
    val sh = GLES20.glCreateShader(type)
    GLES20.glShaderSource(sh, src)
    GLES20.glCompileShader(sh)
    return sh
}

private fun linkProgram(vs: Int, fs: Int): Int {
    val prog = GLES20.glCreateProgram()
    GLES20.glAttachShader(prog, vs)
    GLES20.glAttachShader(prog, fs)
    GLES20.glLinkProgram(prog)
    return prog
}

private fun floatBuffer(data: FloatArray): FloatBuffer =
    ByteBuffer.allocateDirect(data.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply { put(data); position(0) }

// ─── Box geometry ─────────────────────────────────────────────────────────────

private fun buildBox(sx: Float, sy: Float, sz: Float): FloatArray {
    val hx = sx / 2f; val hy = sy / 2f; val hz = sz / 2f
    // Each vertex: x,y,z, nx,ny,nz  (6 floats)
    return floatArrayOf(
        // Front +Z
        -hx,-hy, hz, 0f,0f,1f,   hx,-hy, hz, 0f,0f,1f,   hx, hy, hz, 0f,0f,1f,
        -hx,-hy, hz, 0f,0f,1f,   hx, hy, hz, 0f,0f,1f,  -hx, hy, hz, 0f,0f,1f,
        // Back -Z
         hx,-hy,-hz, 0f,0f,-1f, -hx,-hy,-hz, 0f,0f,-1f, -hx, hy,-hz, 0f,0f,-1f,
         hx,-hy,-hz, 0f,0f,-1f, -hx, hy,-hz, 0f,0f,-1f,  hx, hy,-hz, 0f,0f,-1f,
        // Left -X
        -hx,-hy,-hz,-1f,0f,0f,  -hx,-hy, hz,-1f,0f,0f,  -hx, hy, hz,-1f,0f,0f,
        -hx,-hy,-hz,-1f,0f,0f,  -hx, hy, hz,-1f,0f,0f,  -hx, hy,-hz,-1f,0f,0f,
        // Right +X
         hx,-hy, hz, 1f,0f,0f,   hx,-hy,-hz, 1f,0f,0f,   hx, hy,-hz, 1f,0f,0f,
         hx,-hy, hz, 1f,0f,0f,   hx, hy,-hz, 1f,0f,0f,   hx, hy, hz, 1f,0f,0f,
        // Top +Y
        -hx, hy, hz, 0f,1f,0f,   hx, hy, hz, 0f,1f,0f,   hx, hy,-hz, 0f,1f,0f,
        -hx, hy, hz, 0f,1f,0f,   hx, hy,-hz, 0f,1f,0f,  -hx, hy,-hz, 0f,1f,0f,
        // Bottom -Y
        -hx,-hy,-hz, 0f,-1f,0f,  hx,-hy,-hz, 0f,-1f,0f,  hx,-hy, hz, 0f,-1f,0f,
        -hx,-hy,-hz, 0f,-1f,0f,  hx,-hy, hz, 0f,-1f,0f, -hx,-hy, hz, 0f,-1f,0f,
    )
}

// ─── Grid geometry ────────────────────────────────────────────────────────────

private fun buildGrid(size: Int, step: Float): FloatArray {
    val lines = mutableListOf<Float>()
    val half = size * step / 2f
    for (i in -size..size) {
        val t = i * step
        // X lines
        lines += listOf(-half, 0f, t,  half, 0f, t)
        // Z lines
        lines += listOf(t, 0f, -half,  t, 0f,  half)
    }
    return lines.toFloatArray()
}

// ─── Renderer ─────────────────────────────────────────────────────────────────

class StudioRenderer(private val context: Context) : GLSurfaceView.Renderer {

    // Programs
    private var boxProgram  = 0
    private var gridProgram = 0

    // Scene data (set from outside)
    @Volatile var instances: List<PartDrawCall> = emptyList()

    // Camera
    var orbitYaw   = 45f
    var orbitPitch = 30f
    var orbitDist  = 40f
    var targetX    = 0f
    var targetY    = 0f
    var targetZ    = 0f

    // Matrices
    private val projMatrix  = FloatArray(16)
    private val viewMatrix  = FloatArray(16)
    private val mvpMatrix   = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val tempMatrix  = FloatArray(16)

    // Grid buffer
    private var gridBuffer: FloatBuffer? = null
    private var gridVertexCount = 0

    // Light direction (sun)
    private val lightDir = floatArrayOf(0.3f, -0.8f, 0.5f)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.53f, 0.81f, 0.92f, 1f) // Roblox sky blue
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        // Compile box program
        boxProgram = linkProgram(
            compileShader(GLES20.GL_VERTEX_SHADER,   VERTEX_SHADER),
            compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        )
        // Compile grid program
        gridProgram = linkProgram(
            compileShader(GLES20.GL_VERTEX_SHADER,   GRID_VERTEX_SHADER),
            compileShader(GLES20.GL_FRAGMENT_SHADER, GRID_FRAGMENT_SHADER)
        )

        // Build grid
        val gridData = buildGrid(24, 4f)
        gridBuffer = floatBuffer(gridData)
        gridVertexCount = gridData.size / 3
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 50f, aspect, 0.5f, 2000f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // ── Camera ────────────────────────────────────────────────────────────
        val yawRad   = Math.toRadians(orbitYaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(orbitPitch.toDouble()).toFloat()
        val eyeX = targetX + orbitDist * cos(pitchRad) * sin(yawRad)
        val eyeY = targetY + orbitDist * sin(pitchRad)
        val eyeZ = targetZ + orbitDist * cos(pitchRad) * cos(yawRad)

        Matrix.setLookAtM(viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            targetX, targetY, targetZ,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // ── Grid ──────────────────────────────────────────────────────────────
        drawGrid()

        // ── Parts ─────────────────────────────────────────────────────────────
        for (call in instances) {
            drawBox(call)
        }
    }

    private fun drawGrid() {
        val buf = gridBuffer ?: return
        GLES20.glUseProgram(gridProgram)

        val mvpLoc   = GLES20.glGetUniformLocation(gridProgram, "uMVP")
        val colorLoc = GLES20.glGetUniformLocation(gridProgram, "uColor")
        val posLoc   = GLES20.glGetAttribLocation(gridProgram, "aPosition")

        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniform4f(colorLoc, 0.4f, 0.4f, 0.4f, 0.6f)

        buf.position(0)
        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 12, buf)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, gridVertexCount)
        GLES20.glDisableVertexAttribArray(posLoc)
    }

    private fun drawBox(call: PartDrawCall) {
        GLES20.glUseProgram(boxProgram)

        // Build model matrix
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, call.px, call.py, call.pz)
        if (call.ry != 0f) Matrix.rotateM(modelMatrix, 0, call.ry, 0f, 1f, 0f)
        if (call.rx != 0f) Matrix.rotateM(modelMatrix, 0, call.rx, 1f, 0f, 0f)
        if (call.rz != 0f) Matrix.rotateM(modelMatrix, 0, call.rz, 0f, 0f, 1f)

        // MVP = proj * view * model
        Matrix.multiplyMM(tempMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

        val mvpLoc   = GLES20.glGetUniformLocation(boxProgram, "uMVP")
        val modLoc   = GLES20.glGetUniformLocation(boxProgram, "uModel")
        val colorLoc = GLES20.glGetUniformLocation(boxProgram, "uColor")
        val lightLoc = GLES20.glGetUniformLocation(boxProgram, "uLightDir")
        val posLoc   = GLES20.glGetAttribLocation(boxProgram,  "aPosition")
        val normLoc  = GLES20.glGetAttribLocation(boxProgram,  "aNormal")

        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, tempMatrix, 0)
        GLES20.glUniformMatrix4fv(modLoc, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(colorLoc, call.r, call.g, call.b)
        GLES20.glUniform3fv(lightLoc, 1, lightDir, 0)

        val buf = floatBuffer(buildBox(call.sx, call.sy, call.sz))
        val stride = 6 * 4 // 6 floats * 4 bytes

        buf.position(0)
        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, stride, buf)

        buf.position(3)
        GLES20.glEnableVertexAttribArray(normLoc)
        GLES20.glVertexAttribPointer(normLoc, 3, GLES20.GL_FLOAT, false, stride, buf)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)

        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glDisableVertexAttribArray(normLoc)
    }
}

// ─── Draw call data class ─────────────────────────────────────────────────────

data class PartDrawCall(
    val sx: Float, val sy: Float, val sz: Float,
    val px: Float, val py: Float, val pz: Float,
    val rx: Float = 0f, val ry: Float = 0f, val rz: Float = 0f,
    val r: Float, val g: Float, val b: Float,
)

// ─── Scene builder ────────────────────────────────────────────────────────────

fun buildDrawCalls(dataModel: RbxInstance): List<PartDrawCall> {
    val calls = mutableListOf<PartDrawCall>()

    fun visit(inst: RbxInstance) {
        if (inst.className == "Part" || inst.className == "WedgePart" ||
            inst.className == "CornerWedgePart" || inst.className == "TrussPart" ||
            inst.className == "SpawnLocation") {

            val size = inst.properties["Size"] as? RbxProperty.Vector3Val
                ?: RbxProperty.Vector3Val(4f, 1.2f, 2f)
            val pos = inst.properties["Position"] as? RbxProperty.Vector3Val
                ?: RbxProperty.Vector3Val(0f, size.y / 2f, 0f)
            val color = getBrickColor(inst)

            calls.add(PartDrawCall(
                sx = size.x, sy = size.y, sz = size.z,
                px = pos.x,  py = pos.y,  pz = pos.z,
                r = color[0], g = color[1], b = color[2]
            ))
        }
        inst.children.forEach { visit(it) }
    }

    // Baseplate
    calls.add(PartDrawCall(
        sx = 512f, sy = 20f, sz = 512f,
        px = 0f,   py = -10f, pz = 0f,
        r = 0.64f, g = 0.64f, b = 0.64f
    ))

    dataModel.children.forEach { visit(it) }
    return calls
}

private fun getBrickColor(inst: RbxInstance): FloatArray {
    val c3 = inst.properties["Color"] as? RbxProperty.Color3Val
    if (c3 != null) return floatArrayOf(c3.r, c3.g, c3.b)
    return when ((inst.properties["BrickColor"] as? RbxProperty.StringVal)?.value) {
        "Bright blue"        -> floatArrayOf(0.05f, 0.41f, 0.68f)
        "Bright red"         -> floatArrayOf(0.77f, 0.16f, 0.11f)
        "Bright green"       -> floatArrayOf(0.29f, 0.59f, 0.29f)
        "Bright yellow"      -> floatArrayOf(0.96f, 0.80f, 0.19f)
        "White"              -> floatArrayOf(0.95f, 0.95f, 0.95f)
        "Black"              -> floatArrayOf(0.11f, 0.17f, 0.21f)
        "Dark orange"        -> floatArrayOf(0.65f, 0.49f, 0.24f)
        "Bright orange"      -> floatArrayOf(0.85f, 0.52f, 0.25f)
        "Hot pink"           -> floatArrayOf(1.0f,  0.26f, 0.64f)
        "Cyan"               -> floatArrayOf(0.11f, 0.68f, 0.84f)
        "Lime green"         -> floatArrayOf(0.35f, 0.67f, 0.18f)
        "Medium stone grey"  -> floatArrayOf(0.64f, 0.64f, 0.64f)
        else                 -> floatArrayOf(0.64f, 0.64f, 0.64f)
    }
}

// ─── GLSurfaceView wrapper ────────────────────────────────────────────────────

class StudioGLView(context: Context) : GLSurfaceView(context) {

    val renderer = StudioRenderer(context)
    private var lastX = 0f
    private var lastY = 0f
    private var pointerCount = 0

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.orbitDist = (renderer.orbitDist / detector.scaleFactor)
                    .coerceIn(2f, 500f)
                return true
            }
        })

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                lastX = event.x; lastY = event.y
                pointerCount = event.pointerCount
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    if (event.pointerCount == 1) {
                        // Orbit
                        renderer.orbitYaw   -= dx * 0.4f
                        renderer.orbitPitch  = (renderer.orbitPitch + dy * 0.3f).coerceIn(-85f, 85f)
                    } else if (event.pointerCount == 2) {
                        // Pan
                        val yawRad = Math.toRadians(renderer.orbitYaw.toDouble()).toFloat()
                        renderer.targetX -= (dx * cos(yawRad) + dy * sin(yawRad) * 0.3f) * 0.05f
                        renderer.targetZ += (dx * sin(yawRad) - dy * cos(yawRad) * 0.3f) * 0.05f
                    }
                }
                lastX = event.x; lastY = event.y
            }
        }
        return true
    }
}

// ─── Composable ───────────────────────────────────────────────────────────────

@Composable
fun StudioViewport(
    dataModel: RbxInstance,
    modifier: Modifier = Modifier
) {
    val drawCalls = remember(dataModel) { buildDrawCalls(dataModel) }

    AndroidView(
        factory = { ctx -> StudioGLView(ctx) },
        update  = { view ->
            view.renderer.instances = drawCalls
        },
        modifier = modifier
    )
}
