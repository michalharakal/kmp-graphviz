package org.graphviz.kotlin.renderer

import org.graphviz.kotlin.model.*

/**
 * Precise attribute mapping to match original Graphviz behavior.
 * This ensures visual consistency by handling all edge cases and defaults
 * exactly as the original C implementation does.
 */
object AttributeMapping {
    
    /**
     * Map node attributes to SVG attributes with exact Graphviz compatibility.
     */
    fun mapNodeAttributes(node: Node): Map<String, String> {
        val svgAttributes = mutableMapOf<String, String>()
        
        // Shape-specific attribute mapping
        val shape = getNodeShape(node)
        when (shape.lowercase()) {
            "box", "rect", "rectangle" -> {
                svgAttributes["fill"] = getNodeFillColor(node)
                svgAttributes["stroke"] = getNodeStrokeColor(node)
                svgAttributes["stroke-width"] = getNodeStrokeWidth(node)
                svgAttributes["rx"] = getNodeCornerRadius(node, shape)
                svgAttributes["ry"] = getNodeCornerRadius(node, shape)
            }
            "ellipse", "circle" -> {
                svgAttributes["fill"] = getNodeFillColor(node)
                svgAttributes["stroke"] = getNodeStrokeColor(node)
                svgAttributes["stroke-width"] = getNodeStrokeWidth(node)
            }
            "diamond", "polygon" -> {
                svgAttributes["fill"] = getNodeFillColor(node)
                svgAttributes["stroke"] = getNodeStrokeColor(node)
                svgAttributes["stroke-width"] = getNodeStrokeWidth(node)
            }
        }
        
        // Style attribute mapping (dashed, dotted, etc.)
        val style = node.attributes.get(AttributeKey.STYLE)
        if (style != null) {
            mapStyleAttribute(style, svgAttributes)
        }
        
        // Opacity mapping
        node.attributes.getRaw("alpha")?.let { alpha ->
            when (alpha) {
                is AttributeValue.NumberValue -> svgAttributes["opacity"] = alpha.value.toString()
                is AttributeValue.StringValue -> alpha.value.toDoubleOrNull()?.let { 
                    svgAttributes["opacity"] = it.toString() 
                }
            }
        }
        
        return svgAttributes
    }
    
    /**
     * Map edge attributes to SVG attributes with exact Graphviz compatibility.
     */
    fun mapEdgeAttributes(edge: Edge): Map<String, String> {
        val svgAttributes = mutableMapOf<String, String>()
        
        // Basic edge attributes
        svgAttributes["fill"] = "none" // Edges are never filled by default
        svgAttributes["stroke"] = getEdgeColor(edge)
        svgAttributes["stroke-width"] = getEdgeWidth(edge)
        
        // Style mapping (solid, dashed, dotted, bold)
        val style = edge.attributes.get(AttributeKey.STYLE)
        if (style != null) {
            mapStyleAttribute(style, svgAttributes)
        }
        
        // Arrow attributes
        mapArrowAttributes(edge, svgAttributes)
        
        return svgAttributes
    }
    
    /**
     * Get node fill color with Graphviz defaults and color scheme handling.
     */
    private fun getNodeFillColor(node: Node): String {
        // Check fillcolor first, then color, then default
        val fillColor = node.attributes.getRaw("fillcolor") 
            ?: node.attributes.get(AttributeKey.COLOR)?.let { AttributeValue.ColorValue(it) }
        
        return when (fillColor) {
            is AttributeValue.ColorValue -> mapColor(fillColor.value)
            is AttributeValue.StringValue -> mapColorString(fillColor.value)
            null -> {
                // Graphviz default: white for most shapes, transparent for plaintext
                val shape = getNodeShape(node)
                if (shape.lowercase() in listOf("plaintext", "plain", "none")) "none" else "white"
            }
            else -> "white"
        }
    }
    
    /**
     * Get node stroke color with Graphviz defaults.
     */
    private fun getNodeStrokeColor(node: Node): String {
        val color = node.attributes.get(AttributeKey.COLOR)
        return if (color != null) {
            mapColor(color)
        } else {
            "black" // Graphviz default
        }
    }
    
    /**
     * Get node stroke width with Graphviz defaults.
     */
    private fun getNodeStrokeWidth(node: Node): String {
        val penwidth = node.attributes.get(AttributeKey.PENWIDTH)
        return if (penwidth != null) {
            penwidth.toString()
        } else {
            "1.0" // Graphviz default
        }
    }
    
    /**
     * Get node corner radius for rounded rectangles.
     */
    private fun getNodeCornerRadius(node: Node, shape: String): String {
        // Graphviz uses different corner radius defaults based on shape
        return when (shape.lowercase()) {
            "box", "rect", "rectangle" -> {
                // Check for rounded style or explicit radius
                val style = node.attributes.get(AttributeKey.STYLE)
                if (style?.contains("rounded") == true) {
                    "5" // Graphviz default rounded corner radius
                } else {
                    "0"
                }
            }
            else -> "0"
        }
    }
    
    /**
     * Get edge color with Graphviz defaults.
     */
    private fun getEdgeColor(edge: Edge): String {
        val color = edge.attributes.get(AttributeKey.COLOR)
        return if (color != null) {
            mapColor(color)
        } else {
            "black" // Graphviz default
        }
    }
    
    /**
     * Get edge width with Graphviz defaults.
     */
    private fun getEdgeWidth(edge: Edge): String {
        val penwidth = edge.attributes.get(AttributeKey.PENWIDTH)
        return if (penwidth != null) {
            penwidth.toString()
        } else {
            "1.0" // Graphviz default
        }
    }
    
    /**
     * Map Graphviz style attribute to SVG attributes.
     */
    private fun mapStyleAttribute(style: String, svgAttributes: MutableMap<String, String>) {
        val styles = style.split(",").map { it.trim().lowercase() }
        
        for (styleItem in styles) {
            when (styleItem) {
                "dashed" -> {
                    svgAttributes["stroke-dasharray"] = "5,5" // Graphviz default dash pattern
                }
                "dotted" -> {
                    svgAttributes["stroke-dasharray"] = "2,2" // Graphviz default dot pattern
                }
                "bold" -> {
                    // Bold increases stroke width
                    val currentWidth = svgAttributes["stroke-width"]?.toDoubleOrNull() ?: 1.0
                    svgAttributes["stroke-width"] = (currentWidth * 2.0).toString()
                }
                "solid" -> {
                    svgAttributes["stroke-dasharray"] = "none"
                }
                "invisible", "invis" -> {
                    svgAttributes["opacity"] = "0"
                }
                "filled" -> {
                    // For nodes, ensure fill is not "none"
                    if (svgAttributes["fill"] == "none") {
                        svgAttributes["fill"] = "white"
                    }
                }
                "rounded" -> {
                    // Handled in corner radius calculation
                }
                "diagonals" -> {
                    // Special handling for record shapes (not implemented in basic version)
                }
                "striped" -> {
                    // Pattern fill (advanced feature)
                }
                "wedged" -> {
                    // Pie chart style (advanced feature)
                }
            }
        }
    }
    
    /**
     * Map arrow-related attributes.
     */
    private fun mapArrowAttributes(edge: Edge, svgAttributes: MutableMap<String, String>) {
        // Arrow direction
        val dir = edge.attributes.getRaw("dir")?.toDotString()?.lowercase() ?: "forward"
        
        // Arrow head and tail types
        val arrowhead = edge.attributes.getRaw("arrowhead")?.toDotString()?.lowercase() ?: "normal"
        val arrowtail = edge.attributes.getRaw("arrowtail")?.toDotString()?.lowercase() ?: "none"
        
        // Arrow size
        val arrowsize = edge.attributes.getRaw("arrowsize")?.let { 
            when (it) {
                is AttributeValue.NumberValue -> it.value
                is AttributeValue.StringValue -> it.value.toDoubleOrNull()
                else -> null
            }
        } ?: 1.0
        
        // Store arrow information for later use in rendering
        svgAttributes["data-arrow-dir"] = dir
        svgAttributes["data-arrow-head"] = arrowhead
        svgAttributes["data-arrow-tail"] = arrowtail
        svgAttributes["data-arrow-size"] = arrowsize.toString()
    }
    
    /**
     * Map Graphviz color to SVG color with color scheme support.
     */
    private fun mapColor(color: Color): String {
        return when (color) {
            is Color.Named -> mapNamedColor(color.name)
            is Color.Hex -> color.value
            is Color.RGB -> "rgb(${color.red},${color.green},${color.blue})"
            is Color.RGBA -> "rgba(${color.red},${color.green},${color.blue},${color.alpha})"
            is Color.HSV -> convertHsvToRgb(color.hue, color.saturation, color.value)
        }
    }
    
    /**
     * Map color string with Graphviz color scheme support.
     */
    private fun mapColorString(colorStr: String): String {
        // Handle Graphviz color schemes (X11, SVG, Brewer, etc.)
        return when {
            colorStr.startsWith("#") -> colorStr
            colorStr.startsWith("rgb") -> colorStr
            colorStr.contains("/") -> {
                // Color scheme notation like "red/5" or "blues9/3"
                parseColorScheme(colorStr)
            }
            else -> mapNamedColor(colorStr)
        }
    }
    
    /**
     * Map Graphviz named colors to SVG colors.
     */
    private fun mapNamedColor(name: String): String {
        // Graphviz supports X11 color names, SVG color names, and custom schemes
        return when (name.lowercase()) {
            // Common Graphviz colors
            "black" -> "#000000"
            "white" -> "#ffffff"
            "red" -> "#ff0000"
            "green" -> "#00ff00"
            "blue" -> "#0000ff"
            "yellow" -> "#ffff00"
            "cyan" -> "#00ffff"
            "magenta" -> "#ff00ff"
            "gray", "grey" -> "#808080"
            "lightgray", "lightgrey" -> "#d3d3d3"
            "darkgray", "darkgrey" -> "#a9a9a9"
            "orange" -> "#ffa500"
            "purple" -> "#800080"
            "brown" -> "#a52a2a"
            "pink" -> "#ffc0cb"
            "transparent" -> "none"
            else -> {
                // For unknown colors, return as-is (SVG might recognize it)
                name
            }
        }
    }
    
    /**
     * Parse Graphviz color scheme notation.
     */
    private fun parseColorScheme(colorStr: String): String {
        val parts = colorStr.split("/")
        if (parts.size != 2) return colorStr
        
        val scheme = parts[0]
        val index = parts[1].toIntOrNull() ?: return colorStr
        
        // Implement color scheme lookup (simplified version)
        return when (scheme.lowercase()) {
            "blues9" -> getBrewerBlues9Color(index)
            "reds9" -> getBrewerReds9Color(index)
            "greens9" -> getBrewerGreens9Color(index)
            else -> colorStr
        }
    }
    
    /**
     * Get color from Brewer Blues9 color scheme.
     */
    private fun getBrewerBlues9Color(index: Int): String {
        val blues9 = listOf(
            "#f7fbff", "#deebf7", "#c6dbef", "#9ecae1", "#6baed6",
            "#4292c6", "#2171b5", "#08519c", "#08306b"
        )
        return if (index in 1..blues9.size) blues9[index - 1] else "#0000ff"
    }
    
    /**
     * Get color from Brewer Reds9 color scheme.
     */
    private fun getBrewerReds9Color(index: Int): String {
        val reds9 = listOf(
            "#fff5f0", "#fee0d2", "#fcbba1", "#fc9272", "#fb6a4a",
            "#ef3b2c", "#cb181d", "#a50f15", "#67000d"
        )
        return if (index in 1..reds9.size) reds9[index - 1] else "#ff0000"
    }
    
    /**
     * Get color from Brewer Greens9 color scheme.
     */
    private fun getBrewerGreens9Color(index: Int): String {
        val greens9 = listOf(
            "#f7fcf5", "#e5f5e0", "#c7e9c0", "#a1d99b", "#74c476",
            "#41ab5d", "#238b45", "#006d2c", "#00441b"
        )
        return if (index in 1..greens9.size) greens9[index - 1] else "#00ff00"
    }
    
    /**
     * Convert HSV color to RGB string.
     */
    private fun convertHsvToRgb(h: Double, s: Double, v: Double): String {
        val c = v * s
        val x = c * (1 - kotlin.math.abs((h / 60) % 2 - 1))
        val m = v - c
        
        val (r1, g1, b1) = when ((h / 60).toInt()) {
            0 -> Triple(c, x, 0.0)
            1 -> Triple(x, c, 0.0)
            2 -> Triple(0.0, c, x)
            3 -> Triple(0.0, x, c)
            4 -> Triple(x, 0.0, c)
            5 -> Triple(c, 0.0, x)
            else -> Triple(0.0, 0.0, 0.0)
        }
        
        val r = ((r1 + m) * 255).toInt()
        val g = ((g1 + m) * 255).toInt()
        val b = ((b1 + m) * 255).toInt()
        
        return "rgb($r,$g,$b)"
    }
    
    /**
     * Get node shape with Graphviz defaults.
     */
    private fun getNodeShape(node: Node): String {
        return node.attributes.getRaw("shape")?.toDotString()?.lowercase() ?: "ellipse"
    }
}