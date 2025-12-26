package org.graphviz.kotlin.renderer

import org.graphviz.kotlin.model.*

/**
 * Represents an SVG document structure for building SVG markup.
 * 
 * This class handles the generation of valid SVG XML with proper DOCTYPE,
 * namespaces, viewport configuration, and coordinate system management.
 */
class SvgDocument(
    private val width: Double,
    private val height: Double,
    private val viewBox: ViewBox? = null,
    private val coordinateSystem: CoordinateSystem? = null
) {
    private val elements = mutableListOf<SvgElement>()
    private val styles = mutableListOf<String>()
    private val defs = mutableListOf<SvgElement>()
    private val metadata = mutableMapOf<String, String>()
    
    /**
     * Add an SVG element to the document.
     */
    fun addElement(element: SvgElement) {
        elements.add(element)
    }
    
    /**
     * Add a style definition to the document.
     */
    fun addStyle(style: String) {
        styles.add(style)
    }
    
    /**
     * Add a definition (for reusable elements like gradients, patterns).
     */
    fun addDefinition(element: SvgElement) {
        defs.add(element)
    }
    
    /**
     * Add metadata to the SVG document.
     */
    fun addMetadata(key: String, value: String) {
        metadata[key] = value
    }
    
    /**
     * Get the coordinate system used by this document.
     */
    fun getCoordinateSystem(): CoordinateSystem? = coordinateSystem
    
    /**
     * Generate the complete SVG markup with proper DOCTYPE and namespaces.
     */
    fun toSvg(options: RenderOptions): String {
        val builder = StringBuilder()
        
        // XML declaration
        if (options.includeXmlDeclaration) {
            builder.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            
            // DOCTYPE declaration for SVG 1.1
            if (options.rendererOptions["includeDoctypeDeclaration"] as? Boolean != false) {
                builder.appendLine("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">")
            }
        }
        
        // SVG root element with proper namespaces
        builder.append("<svg")
        
        // Dimensions
        builder.append(" width=\"${SvgUtils.formatDimension(width)}\"")
        builder.append(" height=\"${SvgUtils.formatDimension(height)}\"")
        
        // ViewBox for coordinate system
        if (viewBox != null) {
            builder.append(" viewBox=\"${SvgUtils.formatNumber(viewBox.x)} ${SvgUtils.formatNumber(viewBox.y)} ${SvgUtils.formatNumber(viewBox.width)} ${SvgUtils.formatNumber(viewBox.height)}\"")
        }
        
        // Namespaces
        builder.append(" xmlns=\"http://www.w3.org/2000/svg\"")
        builder.append(" xmlns:xlink=\"http://www.w3.org/1999/xlink\"")
        
        // Version and other attributes
        builder.append(" version=\"1.1\"")
        
        // Coordinate system preservation
        if (coordinateSystem?.requiresScaling() == true || coordinateSystem?.requiresTranslation() == true) {
            builder.append(" preserveAspectRatio=\"xMidYMid meet\"")
        }
        
        builder.appendLine(">")
        
        // Metadata
        if (metadata.isNotEmpty()) {
            builder.appendLine("  <metadata>")
            metadata.forEach { (key, value) ->
                builder.appendLine("    <$key>${SvgUtils.escapeXml(value)}</$key>")
            }
            builder.appendLine("  </metadata>")
        }
        
        // Title and description if provided
        options.rendererOptions["title"]?.let { title ->
            builder.appendLine("  <title>${SvgUtils.escapeXml(title.toString())}</title>")
        }
        
        options.rendererOptions["description"]?.let { description ->
            builder.appendLine("  <desc>${SvgUtils.escapeXml(description.toString())}</desc>")
        }
        
        // Background
        if (options.backgroundColor != null) {
            builder.appendLine("  <rect width=\"100%\" height=\"100%\" fill=\"${options.backgroundColor}\" class=\"background\"/>")
        }
        
        // Styles
        if (styles.isNotEmpty() || options.additionalStyles != null || shouldIncludeDefaultStyles(options)) {
            builder.appendLine("  <style type=\"text/css\">")
            builder.appendLine("    <![CDATA[")
            
            // Default styles
            if (shouldIncludeDefaultStyles(options)) {
                builder.appendLine("      /* Default Graphviz styles */")
                builder.appendLine("      .node { fill: white; stroke: black; stroke-width: 1; }")
                builder.appendLine("      .edge { fill: none; stroke: black; stroke-width: 1; }")
                builder.appendLine("      .text { font-family: ${options.fontFamily}; font-size: ${options.fontSize}px; text-anchor: middle; dominant-baseline: central; }")
                builder.appendLine("      .background { fill: none; }")
                builder.appendLine("      .graph { }")
                builder.appendLine("      .cluster { fill: none; stroke: black; stroke-width: 1; }")
            }
            
            // Custom styles
            styles.forEach { style ->
                builder.appendLine("      $style")
            }
            
            // Additional styles from options
            options.additionalStyles?.let { additionalStyles ->
                builder.appendLine("      $additionalStyles")
            }
            
            builder.appendLine("    ]]>")
            builder.appendLine("  </style>")
        }
        
        // Definitions
        if (defs.isNotEmpty()) {
            builder.appendLine("  <defs>")
            defs.forEach { def ->
                builder.appendLine("    ${def.toSvg()}")
            }
            builder.appendLine("  </defs>")
        }
        
        // Main content group with coordinate transformation if needed
        if (coordinateSystem != null && (coordinateSystem.requiresScaling() || coordinateSystem.requiresTranslation())) {
            builder.appendLine("  <g transform=\"${coordinateSystem.getTransformMatrix()}\" class=\"graph-content\">")
            elements.forEach { element ->
                builder.appendLine("    ${element.toSvg()}")
            }
            builder.appendLine("  </g>")
        } else {
            // No transformation needed
            elements.forEach { element ->
                builder.appendLine("  ${element.toSvg()}")
            }
        }
        
        builder.appendLine("</svg>")
        
        return if (options.optimize) {
            optimizeSvg(builder.toString())
        } else {
            builder.toString()
        }
    }
    
    /**
     * Check if default styles should be included.
     */
    private fun shouldIncludeDefaultStyles(options: RenderOptions): Boolean {
        return options.rendererOptions["includeDefaultStyles"] as? Boolean != false
    }
    
    /**
     * Optimize SVG output for smaller file size.
     */
    private fun optimizeSvg(svg: String): String {
        // Basic optimizations - remove extra whitespace and empty lines
        return svg.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }
    
    companion object {
        /**
         * Create an SVG document with automatic viewport calculation.
         */
        fun create(
            graphBounds: BoundingBox,
            options: RenderOptions = RenderOptions.default()
        ): SvgDocument {
            val margin = options.margin
            val width = options.width ?: (graphBounds.width + 2 * margin)
            val height = options.height ?: (graphBounds.height + 2 * margin)
            
            val viewport = Viewport(width, height, ViewBox.fromBoundingBox(graphBounds, margin))
            val coordinateSystem = CoordinateSystem.fitToViewport(graphBounds, viewport, margin)
            
            return SvgDocument(width, height, viewport.viewBox, coordinateSystem)
        }
        
        /**
         * Create an SVG document with explicit dimensions.
         */
        fun create(
            width: Double,
            height: Double,
            graphBounds: BoundingBox? = null,
            margin: Double = 20.0
        ): SvgDocument {
            val viewport = Viewport(width, height, null)
            val coordinateSystem = graphBounds?.let { 
                CoordinateSystem.fitToViewport(it, viewport, margin) 
            }
            
            return SvgDocument(width, height, viewport.viewBox, coordinateSystem)
        }
        
        /**
         * Create an SVG document with a specific coordinate system.
         */
        fun createWithCoordinateSystem(
            coordinateSystem: CoordinateSystem,
            viewport: Viewport
        ): SvgDocument {
            return SvgDocument(
                viewport.width, 
                viewport.height, 
                viewport.viewBox, 
                coordinateSystem
            )
        }
        
        /**
         * Create a minimal SVG document without coordinate transformations.
         */
        fun createSimple(
            width: Double,
            height: Double,
            viewBox: ViewBox? = null
        ): SvgDocument {
            return SvgDocument(width, height, viewBox, null)
        }
    }
}

/**
 * Represents the viewBox attribute for SVG coordinate system.
 */
data class ViewBox(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
) {
    companion object {
        /**
         * Create a viewBox from a bounding box with optional margin.
         */
        fun fromBoundingBox(boundingBox: BoundingBox, margin: Double = 0.0): ViewBox {
            return ViewBox(
                x = boundingBox.minX - margin,
                y = boundingBox.minY - margin,
                width = boundingBox.width + 2 * margin,
                height = boundingBox.height + 2 * margin
            )
        }
    }
}

/**
 * Base class for SVG elements.
 */
abstract class SvgElement {
    protected val attributes = mutableMapOf<String, String>()
    
    /**
     * Set an attribute on this element.
     */
    fun setAttribute(name: String, value: String): SvgElement {
        attributes[name] = value
        return this
    }
    
    /**
     * Set an attribute on this element with a numeric value.
     */
    fun setAttribute(name: String, value: Double): SvgElement {
        attributes[name] = value.toString()
        return this
    }
    
    /**
     * Get an attribute value from this element.
     */
    fun getAttribute(name: String): String? {
        return attributes[name]
    }
    
    /**
     * Generate SVG markup for this element.
     */
    abstract fun toSvg(): String
    
    /**
     * Helper to format attributes as a string.
     */
    protected fun formatAttributes(): String {
        return if (attributes.isEmpty()) {
            ""
        } else {
            " " + attributes.entries.joinToString(" ") { (key, value) ->
                "$key=\"$value\""
            }
        }
    }
}

/**
 * SVG group element for organizing related elements.
 */
class SvgGroup : SvgElement() {
    private val children = mutableListOf<SvgElement>()
    
    /**
     * Add a child element to this group.
     */
    fun addChild(element: SvgElement): SvgGroup {
        children.add(element)
        return this
    }
    
    override fun toSvg(): String {
        val builder = StringBuilder()
        builder.append("<g${formatAttributes()}>")
        
        if (children.isNotEmpty()) {
            builder.appendLine()
            children.forEach { child ->
                builder.appendLine("  ${child.toSvg()}")
            }
        }
        
        builder.append("</g>")
        return builder.toString()
    }
}

/**
 * SVG rectangle element.
 */
class SvgRect(
    x: Double,
    y: Double,
    width: Double,
    height: Double
) : SvgElement() {
    
    init {
        setAttribute("x", x)
        setAttribute("y", y)
        setAttribute("width", width)
        setAttribute("height", height)
    }
    
    override fun toSvg(): String {
        return "<rect${formatAttributes()}/>"
    }
}

/**
 * SVG ellipse element.
 */
class SvgEllipse(
    cx: Double,
    cy: Double,
    rx: Double,
    ry: Double
) : SvgElement() {
    
    init {
        setAttribute("cx", cx)
        setAttribute("cy", cy)
        setAttribute("rx", rx)
        setAttribute("ry", ry)
    }
    
    override fun toSvg(): String {
        return "<ellipse${formatAttributes()}/>"
    }
}

/**
 * SVG path element.
 */
class SvgPath(
    private val pathData: String
) : SvgElement() {
    
    init {
        setAttribute("d", pathData)
    }
    
    override fun toSvg(): String {
        return "<path${formatAttributes()}/>"
    }
    
    companion object {
        /**
         * Create a path from a list of points.
         */
        fun fromPoints(points: List<Point>): SvgPath {
            if (points.isEmpty()) {
                return SvgPath("")
            }
            
            val builder = StringBuilder()
            builder.append("M ${points[0].x} ${points[0].y}")
            
            for (i in 1 until points.size) {
                builder.append(" L ${points[i].x} ${points[i].y}")
            }
            
            return SvgPath(builder.toString())
        }
        
        /**
         * Create a curved path using quadratic Bezier curves.
         */
        fun fromCurvedPoints(points: List<Point>): SvgPath {
            if (points.isEmpty()) {
                return SvgPath("")
            }
            
            if (points.size < 3) {
                return fromPoints(points)
            }
            
            val builder = StringBuilder()
            builder.append("M ${points[0].x} ${points[0].y}")
            
            // Use quadratic curves for smooth paths
            for (i in 1 until points.size - 1) {
                val control = points[i]
                val end = points[i + 1]
                builder.append(" Q ${control.x} ${control.y} ${end.x} ${end.y}")
            }
            
            return SvgPath(builder.toString())
        }
    }
}

/**
 * SVG text element.
 */
class SvgText(
    val x: Double,
    val y: Double,
    private val text: String
) : SvgElement() {
    
    private val children = mutableListOf<SvgElement>()
    
    init {
        setAttribute("x", x)
        setAttribute("y", y)
    }
    
    /**
     * Add a child element (e.g., tspan).
     */
    fun addChild(child: SvgElement) {
        children.add(child)
    }
    
    override fun toSvg(): String {
        val childrenSvg = children.joinToString("") { it.toSvg() }
        return "<text${formatAttributes()}>${SvgUtils.escapeXml(text)}$childrenSvg</text>"
    }
}

/**
 * SVG tspan element for multi-line text.
 */
class SvgTspan(
    private val text: String
) : SvgElement() {
    
    override fun toSvg(): String {
        return "<tspan${formatAttributes()}>${SvgUtils.escapeXml(text)}</tspan>"
    }
}

/**
 * SVG line element.
 */
class SvgLine(
    x1: Double,
    y1: Double,
    x2: Double,
    y2: Double
) : SvgElement() {
    
    init {
        setAttribute("x1", x1)
        setAttribute("y1", y1)
        setAttribute("x2", x2)
        setAttribute("y2", y2)
    }
    
    override fun toSvg(): String {
        return "<line${formatAttributes()}/>"
    }
}

/**
 * SVG polygon element.
 */
class SvgPolygon(
    points: List<Point>
) : SvgElement() {
    
    init {
        val pointsString = points.joinToString(" ") { "${it.x},${it.y}" }
        setAttribute("points", pointsString)
    }
    
    override fun toSvg(): String {
        return "<polygon${formatAttributes()}/>"
    }
}

/**
 * Utility functions for SVG generation.
 */
object SvgUtils {
    /**
     * Escape XML special characters.
     */
    fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
    
    /**
     * Format a number for SVG output with appropriate precision.
     */
    fun formatNumber(value: Double): String {
        return if (value == value.toInt().toDouble()) {
            value.toInt().toString()
        } else {
            // Round to 3 decimal places
            val rounded = (value * 1000).toLong() / 1000.0
            rounded.toString()
        }
    }
    
    /**
     * Format a dimension value for SVG output.
     */
    fun formatDimension(value: Double): String {
        return if (value == value.toInt().toDouble()) {
            value.toInt().toString()
        } else {
            // Round to 2 decimal places
            val rounded = (value * 100).toLong() / 100.0
            rounded.toString()
        }
    }
}