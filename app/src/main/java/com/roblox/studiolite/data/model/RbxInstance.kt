package com.roblox.studiolite.data.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import java.util.UUID

// ─── Roblox Instance tree ────────────────────────────────────────────────────

data class RbxInstance(
    val referent: String = UUID.randomUUID().toString(),
    val className: String,
    var name: String = className,
    val parent: RbxInstance? = null,
    val children: MutableList<RbxInstance> = mutableStateListOf(),
    val properties: MutableMap<String, RbxProperty> = mutableStateMapOf(),
) {
    /** Recursively find by referent */
    fun findByReferent(ref: String): RbxInstance? {
        if (referent == ref) return this
        for (child in children) {
            child.findByReferent(ref)?.let { return it }
        }
        return null
    }

    fun addChild(child: RbxInstance) {
        children.add(child)
    }
}

// ─── Property value types ────────────────────────────────────────────────────

sealed class RbxProperty {
    data class StringVal(val value: String) : RbxProperty()
    data class IntVal(val value: Int) : RbxProperty()
    data class FloatVal(val value: Float) : RbxProperty()
    data class BoolVal(val value: Boolean) : RbxProperty()
    data class Vector3Val(val x: Float, val y: Float, val z: Float) : RbxProperty()
    data class Color3Val(val r: Float, val g: Float, val b: Float) : RbxProperty()
    data class CFrameVal(
        val x: Float, val y: Float, val z: Float,
        val r00: Float, val r01: Float, val r02: Float,
        val r10: Float, val r11: Float, val r12: Float,
        val r20: Float, val r21: Float, val r22: Float,
    ) : RbxProperty()
    data class EnumVal(val enumType: String, val value: Int) : RbxProperty()
    data class RefVal(val referent: String?) : RbxProperty()

    fun displayString(): String = when (this) {
        is StringVal -> value
        is IntVal -> value.toString()
        is FloatVal -> "%.3f".format(value)
        is BoolVal -> if (value) "true" else "false"
        is Vector3Val -> "%.2f, %.2f, %.2f".format(x, y, z)
        is Color3Val -> "[%.2f %.2f %.2f]".format(r, g, b)
        is CFrameVal -> "%.2f, %.2f, %.2f".format(x, y, z)
        is EnumVal -> "$enumType($value)"
        is RefVal -> referent ?: "nil"
    }
}

// ─── Default Roblox DataModel ────────────────────────────────────────────────

fun buildDefaultDataModel(): RbxInstance {
    val dm = RbxInstance(className = "DataModel", name = "Game")

    val services = listOf(
        "Workspace", "Players", "Lighting", "ReplicatedFirst",
        "ReplicatedStorage", "StarterGui", "StarterPack",
        "StarterPlayer", "Teams", "ServerScriptService", "ServerStorage",
        "UIDragDetectorService"
    )

    services.forEach { svc ->
        val inst = RbxInstance(className = svc, name = svc, parent = dm)
        // Default properties
        inst.properties["Name"] = RbxProperty.StringVal(svc)
        inst.properties["Archivable"] = RbxProperty.BoolVal(true)
        if (svc == "StarterGui") {
            inst.properties["ResetPlayerGuiOnSpawn"] = RbxProperty.BoolVal(true)
            inst.properties["ScreenOrientation"] = RbxProperty.EnumVal("ScreenOrientation", 0)
            inst.properties["ShowDevelopmentGui"] = RbxProperty.BoolVal(true)
        }
        if (svc == "Workspace") {
            inst.properties["Gravity"] = RbxProperty.FloatVal(196.2f)
            inst.properties["FallenPartsDestroyHeight"] = RbxProperty.FloatVal(-500f)
            // Add Baseplate
            val baseplate = RbxInstance(className = "Part", name = "Baseplate", parent = inst)
            baseplate.properties["Name"] = RbxProperty.StringVal("Baseplate")
            baseplate.properties["Size"] = RbxProperty.Vector3Val(512f, 20f, 512f)
            baseplate.properties["Position"] = RbxProperty.Vector3Val(0f, -10f, 0f)
            baseplate.properties["Anchored"] = RbxProperty.BoolVal(true)
            baseplate.properties["Material"] = RbxProperty.EnumVal("Material", 256) // SmoothPlastic
            baseplate.properties["BrickColor"] = RbxProperty.StringVal("Medium stone grey")
            inst.addChild(baseplate)
        }
        dm.addChild(inst)
    }
    return dm
}
