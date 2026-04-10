package com.roblox.studiolite.data.rbxl

import android.util.Xml
import com.roblox.studiolite.data.model.RbxInstance
import com.roblox.studiolite.data.model.RbxProperty
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.InputStream
import java.io.OutputStream

// ─── RBXL Parser ─────────────────────────────────────────────────────────────

class RbxlParser {

    /**
     * Parse .rbxl / .rbxlx file (XML format) into RbxInstance tree.
     * Returns root DataModel instance.
     */
    fun parse(input: InputStream): RbxInstance {
        val root = RbxInstance(className = "DataModel", name = "Game")
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, null)
        }

        // Referent map for resolving Ref properties later
        val referentMap = mutableMapOf<String, RbxInstance>()

        fun parseItem(parentInstance: RbxInstance) {
            val className = parser.getAttributeValue(null, "class") ?: "Unknown"
            val referent = parser.getAttributeValue(null, "referent") ?: ""
            val instance = RbxInstance(
                referent = referent.ifEmpty { java.util.UUID.randomUUID().toString() },
                className = className,
                name = className,
                parent = parentInstance
            )
            if (referent.isNotEmpty()) referentMap[referent] = instance

            var eventType = parser.next()
            while (!(eventType == XmlPullParser.END_TAG && parser.name == "Item")) {
                when {
                    eventType == XmlPullParser.START_TAG && parser.name == "Properties" -> {
                        parseProperties(instance)
                    }
                    eventType == XmlPullParser.START_TAG && parser.name == "Item" -> {
                        parseItem(instance)
                    }
                }
                eventType = parser.next()
            }

            // Update name from parsed properties
            (instance.properties["Name"] as? RbxProperty.StringVal)?.let {
                instance.name = it.value
            }

            parentInstance.addChild(instance)
        }

        fun parseRoot() {
            var eventType = parser.next()
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "Item") {
                    parseItem(root)
                }
                eventType = parser.next()
            }
        }

        try {
            // Skip to <roblox> root element
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "roblox") {
                    parseRoot()
                    break
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        }

        return root
    }

    private fun parseProperties(instance: RbxInstance) {
        val parser = instance // can't capture XmlPullParser directly, use workaround below
        // NOTE: This is called from inside the parse() scope where `parser` is accessible
        // Restructure: pass parser as parameter
    }
}

// ─── Proper implementation with parser passed in ──────────────────────────────

class RbxlParserV2 {

    fun parse(input: InputStream): RbxInstance {
        val root = RbxInstance(className = "DataModel", name = "Game")
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, null)
        }

        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "roblox") {
                    parseChildren(parser, root)
                    break
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return root
    }

    private fun parseChildren(parser: XmlPullParser, parent: RbxInstance) {
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG &&
                    (parser.name == "roblox" || parser.name == "Item"))
        ) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "Item") {
                val className = parser.getAttributeValue(null, "class") ?: "Unknown"
                val referent = parser.getAttributeValue(null, "referent") ?: java.util.UUID.randomUUID().toString()
                val instance = RbxInstance(
                    referent = referent,
                    className = className,
                    name = className,
                    parent = parent
                )
                parseItemBody(parser, instance)
                (instance.properties["Name"] as? RbxProperty.StringVal)?.let {
                    instance.name = it.value
                }
                parent.addChild(instance)
            }
            eventType = parser.next()
        }
    }

    private fun parseItemBody(parser: XmlPullParser, instance: RbxInstance) {
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "Item")) {
            when {
                eventType == XmlPullParser.START_TAG && parser.name == "Properties" -> {
                    parseProperties(parser, instance)
                }
                eventType == XmlPullParser.START_TAG && parser.name == "Item" -> {
                    val className = parser.getAttributeValue(null, "class") ?: "Unknown"
                    val referent = parser.getAttributeValue(null, "referent") ?: java.util.UUID.randomUUID().toString()
                    val child = RbxInstance(
                        referent = referent,
                        className = className,
                        name = className,
                        parent = instance
                    )
                    parseItemBody(parser, child)
                    (child.properties["Name"] as? RbxProperty.StringVal)?.let {
                        child.name = it.value
                    }
                    instance.addChild(child)
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseProperties(parser: XmlPullParser, instance: RbxInstance) {
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "Properties")) {
            if (eventType == XmlPullParser.START_TAG) {
                val propType = parser.name
                val propName = parser.getAttributeValue(null, "name") ?: ""
                val prop = when (propType) {
                    "string" -> RbxProperty.StringVal(parser.nextText())
                    "int" -> RbxProperty.IntVal(parser.nextText().toIntOrNull() ?: 0)
                    "float" -> RbxProperty.FloatVal(parser.nextText().toFloatOrNull() ?: 0f)
                    "double" -> RbxProperty.FloatVal(parser.nextText().toFloatOrNull() ?: 0f)
                    "bool" -> RbxProperty.BoolVal(parser.nextText().trim() == "true")
                    "token" -> RbxProperty.EnumVal(propName, parser.nextText().toIntOrNull() ?: 0)
                    "Vector3" -> {
                        var x = 0f; var y = 0f; var z = 0f
                        var innerEvent = parser.next()
                        while (!(innerEvent == XmlPullParser.END_TAG && parser.name == "Vector3")) {
                            if (innerEvent == XmlPullParser.START_TAG) {
                                when (parser.name) {
                                    "X" -> x = parser.nextText().toFloatOrNull() ?: 0f
                                    "Y" -> y = parser.nextText().toFloatOrNull() ?: 0f
                                    "Z" -> z = parser.nextText().toFloatOrNull() ?: 0f
                                }
                            }
                            innerEvent = parser.next()
                        }
                        RbxProperty.Vector3Val(x, y, z)
                    }
                    "Color3" -> {
                        var r = 0f; var g = 0f; var b = 0f
                        var innerEvent = parser.next()
                        while (!(innerEvent == XmlPullParser.END_TAG && parser.name == "Color3")) {
                            if (innerEvent == XmlPullParser.START_TAG) {
                                when (parser.name) {
                                    "R" -> r = parser.nextText().toFloatOrNull() ?: 0f
                                    "G" -> g = parser.nextText().toFloatOrNull() ?: 0f
                                    "B" -> b = parser.nextText().toFloatOrNull() ?: 0f
                                }
                            }
                            innerEvent = parser.next()
                        }
                        RbxProperty.Color3Val(r, g, b)
                    }
                    "CoordinateFrame" -> {
                        var cx = 0f; var cy = 0f; var cz = 0f
                        var r00 = 1f; var r01 = 0f; var r02 = 0f
                        var r10 = 0f; var r11 = 1f; var r12 = 0f
                        var r20 = 0f; var r21 = 0f; var r22 = 1f
                        var innerEvent = parser.next()
                        while (!(innerEvent == XmlPullParser.END_TAG && parser.name == "CoordinateFrame")) {
                            if (innerEvent == XmlPullParser.START_TAG) {
                                when (parser.name) {
                                    "X" -> cx = parser.nextText().toFloatOrNull() ?: 0f
                                    "Y" -> cy = parser.nextText().toFloatOrNull() ?: 0f
                                    "Z" -> cz = parser.nextText().toFloatOrNull() ?: 0f
                                    "R00" -> r00 = parser.nextText().toFloatOrNull() ?: 1f
                                    "R01" -> r01 = parser.nextText().toFloatOrNull() ?: 0f
                                    "R02" -> r02 = parser.nextText().toFloatOrNull() ?: 0f
                                    "R10" -> r10 = parser.nextText().toFloatOrNull() ?: 0f
                                    "R11" -> r11 = parser.nextText().toFloatOrNull() ?: 1f
                                    "R12" -> r12 = parser.nextText().toFloatOrNull() ?: 0f
                                    "R20" -> r20 = parser.nextText().toFloatOrNull() ?: 0f
                                    "R21" -> r21 = parser.nextText().toFloatOrNull() ?: 0f
                                    "R22" -> r22 = parser.nextText().toFloatOrNull() ?: 1f
                                }
                            }
                            innerEvent = parser.next()
                        }
                        RbxProperty.CFrameVal(cx, cy, cz, r00, r01, r02, r10, r11, r12, r20, r21, r22)
                    }
                    "Ref" -> RbxProperty.RefVal(parser.nextText().takeIf { it != "null" })
                    else -> null
                }
                if (prop != null && propName.isNotEmpty()) {
                    instance.properties[propName] = prop
                }
            }
            eventType = parser.next()
        }
    }
}

// ─── RBXL Writer (save scene back to .rbxl) ──────────────────────────────────

class RbxlWriter {

    fun write(root: RbxInstance, output: OutputStream) {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        sb.appendLine("""<roblox xmlns:xmime="http://www.w3.org/2005/05/xmlmime" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://www.roblox.com/roblox.xsd" version="4">""")

        for (child in root.children) {
            writeItem(sb, child, indent = 1)
        }

        sb.appendLine("</roblox>")
        output.write(sb.toString().toByteArray(Charsets.UTF_8))
    }

    private fun writeItem(sb: StringBuilder, instance: RbxInstance, indent: Int) {
        val tab = "\t".repeat(indent)
        sb.appendLine("""$tab<Item class="${instance.className}" referent="${instance.referent}">""")
        sb.appendLine("$tab\t<Properties>")

        for ((name, prop) in instance.properties) {
            writeProperty(sb, name, prop, indent + 2)
        }

        sb.appendLine("$tab\t</Properties>")

        for (child in instance.children) {
            writeItem(sb, child, indent + 1)
        }

        sb.appendLine("$tab</Item>")
    }

    private fun writeProperty(sb: StringBuilder, name: String, prop: RbxProperty, indent: Int) {
        val tab = "\t".repeat(indent)
        when (prop) {
            is RbxProperty.StringVal ->
                sb.appendLine("""$tab<string name="$name">${prop.value}</string>""")
            is RbxProperty.IntVal ->
                sb.appendLine("""$tab<int name="$name">${prop.value}</int>""")
            is RbxProperty.FloatVal ->
                sb.appendLine("""$tab<float name="$name">${prop.value}</float>""")
            is RbxProperty.BoolVal ->
                sb.appendLine("""$tab<bool name="$name">${prop.value}</bool>""")
            is RbxProperty.EnumVal ->
                sb.appendLine("""$tab<token name="$name">${prop.value}</token>""")
            is RbxProperty.Vector3Val -> {
                sb.appendLine("""$tab<Vector3 name="$name">""")
                sb.appendLine("$tab\t<X>${prop.x}</X>")
                sb.appendLine("$tab\t<Y>${prop.y}</Y>")
                sb.appendLine("$tab\t<Z>${prop.z}</Z>")
                sb.appendLine("$tab</Vector3>")
            }
            is RbxProperty.Color3Val -> {
                sb.appendLine("""$tab<Color3 name="$name">""")
                sb.appendLine("$tab\t<R>${prop.r}</R>")
                sb.appendLine("$tab\t<G>${prop.g}</G>")
                sb.appendLine("$tab\t<B>${prop.b}</B>")
                sb.appendLine("$tab</Color3>")
            }
            is RbxProperty.CFrameVal -> {
                sb.appendLine("""$tab<CoordinateFrame name="$name">""")
                sb.appendLine("$tab\t<X>${prop.x}</X>")
                sb.appendLine("$tab\t<Y>${prop.y}</Y>")
                sb.appendLine("$tab\t<Z>${prop.z}</Z>")
                sb.appendLine("$tab\t<R00>${prop.r00}</R00>")
                sb.appendLine("$tab\t<R01>${prop.r01}</R01>")
                sb.appendLine("$tab\t<R02>${prop.r02}</R02>")
                sb.appendLine("$tab\t<R10>${prop.r10}</R10>")
                sb.appendLine("$tab\t<R11>${prop.r11}</R11>")
                sb.appendLine("$tab\t<R12>${prop.r12}</R12>")
                sb.appendLine("$tab\t<R20>${prop.r20}</R20>")
                sb.appendLine("$tab\t<R21>${prop.r21}</R21>")
                sb.appendLine("$tab\t<R22>${prop.r22}</R22>")
                sb.appendLine("$tab</CoordinateFrame>")
            }
            is RbxProperty.RefVal ->
                sb.appendLine("""$tab<Ref name="$name">${prop.referent ?: "null"}</Ref>""")
        }
    }
}
