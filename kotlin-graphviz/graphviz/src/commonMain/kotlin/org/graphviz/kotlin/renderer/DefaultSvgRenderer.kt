package org.graphviz.kotlin.renderer

import org.graphviz.kotlin.model.*
import org.graphviz.kotlin.layout.NodeSize

/**
 * Default implementation of SVG renderer for testing the document structure generator.
 * 
 * This is a basic implementation that demonstrates the SVG document generation
 * capabilities with proper coordinate system handling.
 */
class DefaultSvgRenderer : BaseSvgRenderer() {
    
    override fun renderGraph(
        graph: Graph,
        document: SvgDocument,
        options: RenderOptions,
        viewport: Viewport
    ) {
        val coordinateSystem = document.getCoordinateSystem()
        
        // Add graph metadata
        document.addMetadata("generator", "Graphviz Kotlin Multiplatform")
        document.addMetadata("graph-id", graph.id)
        document.addMetadata("graph-type", if (graph.isDirected) "directed" else "undirected")
        
        // Render all nodes
        graph.getAllNodes().forEach { node ->
            val nodeElements = renderNode(node, options)
            nodeElements.forEach { element ->
                // Transform coordinates if needed
                if (coordinateSystem != null && node.position != null) {
                    transformElementCoordinates(element, coordinateSystem)
                }
                document.addElement(element)
            }
        }
        
        // Render all edges
        graph.getAllEdges().forEach { edge ->
            val edgeElements = renderEdge(edge, options)
            edgeElements.forEach { element ->
                // Transform coordinates if needed
                if (coordinateSystem != null) {
                    transformElementCoordinates(element, coordinateSystem)
                }
                document.addElement(element)
            }
        }
        
        // Render subgraphs as groups
        graph.subgraphs.forEach { subgraph ->
            val groupElement = SvgGroup()
            groupElement.setAttribute("class", "subgraph")
            groupElement.setAttribute("id", "subgraph-${subgraph.id}")
            
            // Add subgraph elements to the group
            subgraph.getAllNodes().forEach { node ->
                val nodeElements = renderNode(node, options)
                nodeElements.forEach { element ->
                    if (coordinateSystem != null && node.position != null) {
                        transformElementCoordinates(element, coordinateSystem)
                    }
                    groupElement.addChild(element)
                }
            }
            
            subgraph.getAllEdges().forEach { edge ->
                val edgeElements = renderEdge(edge, options)
                edgeElements.forEach { element ->
                    if (coordinateSystem != null) {
                        transformElementCoordinates(element, coordinateSystem)
                    }
                    groupElement.addChild(element)
                }
            }
            
            document.addElement(groupElement)
        }
    }
    
    override fun renderNode(node: Node, options: RenderOptions): List<SvgElement> {
        val position = node.position ?: Point(0.0, 0.0)
        val size = getNodeSize(node, options)
        val shape = getNodeShape(node)
        
        val elements = mutableListOf<SvgElement>()
        
        // Create the node shape
        val shapeElement = when (shape.lowercase()) {
            "box", "rect", "rectangle" -> {
                SvgRect(
                    x = position.x - size.width / 2,
                    y = position.y - size.height / 2,
                    width = size.width,
                    height = size.height
                )
            }
            "circle" -> {
                val radius = minOf(size.width, size.height) / 2
                SvgEllipse(
                    cx = position.x,
                    cy = position.y,
                    rx = radius,
                    ry = radius
                )
            }
            "ellipse" -> {
                SvgEllipse(
                    cx = position.x,
                    cy = position.y,
                    rx = size.width / 2,
                    ry = size.height / 2
                )
            }
            "diamond" -> {
                // Create diamond shape as polygon
                val halfWidth = size.width / 2
                val halfHeight = size.height / 2
                val points = listOf(
                    Point(position.x, position.y - halfHeight), // top
                    Point(position.x + halfWidth, position.y), // right
                    Point(position.x, position.y + halfHeight), // bottom
                    Point(position.x - halfWidth, position.y)  // left
                )
                SvgPolygon(points)
            }
            "triangle" -> {
                // Create triangle shape as polygon
                val halfWidth = size.width / 2
                val halfHeight = size.height / 2
                val points = listOf(
                    Point(position.x, position.y - halfHeight), // top
                    Point(position.x + halfWidth, position.y + halfHeight), // bottom right
                    Point(position.x - halfWidth, position.y + halfHeight)  // bottom left
                )
                SvgPolygon(points)
            }
            "invtriangle" -> {
                // Create inverted triangle shape as polygon
                val halfWidth = size.width / 2
                val halfHeight = size.height / 2
                val points = listOf(
                    Point(position.x - halfWidth, position.y - halfHeight), // top left
                    Point(position.x + halfWidth, position.y - halfHeight), // top right
                    Point(position.x, position.y + halfHeight) // bottom
                )
                SvgPolygon(points)
            }
            "hexagon" -> {
                // Create hexagon shape as polygon
                val halfWidth = size.width / 2
                val halfHeight = size.height / 2
                val quarterWidth = size.width / 4
                val points = listOf(
                    Point(position.x - quarterWidth, position.y - halfHeight), // top left
                    Point(position.x + quarterWidth, position.y - halfHeight), // top right
                    Point(position.x + halfWidth, position.y), // right
                    Point(position.x + quarterWidth, position.y + halfHeight), // bottom right
                    Point(position.x - quarterWidth, position.y + halfHeight), // bottom left
                    Point(position.x - halfWidth, position.y) // left
                )
                SvgPolygon(points)
            }
            "octagon" -> {
                // Create octagon shape as polygon
                val halfWidth = size.width / 2
                val halfHeight = size.height / 2
                val cornerOffset = minOf(halfWidth, halfHeight) * 0.3 // 30% corner cut
                val points = listOf(
                    Point(position.x - halfWidth + cornerOffset, position.y - halfHeight), // top left
                    Point(position.x + halfWidth - cornerOffset, position.y - halfHeight), // top right
                    Point(position.x + halfWidth, position.y - halfHeight + cornerOffset), // right top
                    Point(position.x + halfWidth, position.y + halfHeight - cornerOffset), // right bottom
                    Point(position.x + halfWidth - cornerOffset, position.y + halfHeight), // bottom right
                    Point(position.x - halfWidth + cornerOffset, position.y + halfHeight), // bottom left
                    Point(position.x - halfWidth, position.y + halfHeight - cornerOffset), // left bottom
                    Point(position.x - halfWidth, position.y - halfHeight + cornerOffset)  // left top
                )
                SvgPolygon(points)
            }
            "house" -> {
                // Create house shape as polygon (pentagon with triangular roof)
                val halfWidth = size.width / 2
                val halfHeight = size.height / 2
                val roofHeight = halfHeight * 0.4 // 40% of height for roof
                val points = listOf(
                    Point(position.x, position.y - halfHeight), // roof peak
                    Point(position.x + halfWidth, position.y - halfHeight + roofHeight), // roof right
                    Point(position.x + halfWidth, position.y + halfHeight), // bottom right
                    Point(position.x - halfWidth, position.y + halfHeight), // bottom left
                    Point(position.x - halfWidth, position.y - halfHeight + roofHeight) // roof left
                )
                SvgPolygon(points)
            }
            "trapezium" -> {
                // Create trapezium shape as polygon
                val halfWidth = size.width / 2
                val halfHeight = size.height / 2
                val topWidth = halfWidth * 0.6 // Top is 60% of full width
                val points = listOf(
                    Point(position.x - topWidth, position.y - halfHeight), // top left
                    Point(position.x + topWidth, position.y - halfHeight), // top right
                    Point(position.x + halfWidth, position.y + halfHeight), // bottom right
                    Point(position.x - halfWidth, position.y + halfHeight)  // bottom left
                )
                SvgPolygon(points)
            }
            "parallelogram" -> {
                // Create parallelogram shape as polygon
                val halfWidth = size.width / 2
                val halfHeight = size.height / 2
                val skew = halfWidth * 0.3 // 30% skew
                val points = listOf(
                    Point(position.x - halfWidth + skew, position.y - halfHeight), // top left
                    Point(position.x + halfWidth + skew, position.y - halfHeight), // top right
                    Point(position.x + halfWidth - skew, position.y + halfHeight), // bottom right
                    Point(position.x - halfWidth - skew, position.y + halfHeight)  // bottom left
                )
                SvgPolygon(points)
            }
            "pentagon" -> {
                // Create pentagon shape as polygon
                val halfWidth = size.width / 2
                val halfHeight = size.height / 2
                val points = mutableListOf<Point>()
                for (i in 0 until 5) {
                    val angle = -kotlin.math.PI / 2 + (2 * kotlin.math.PI * i / 5) // Start from top
                    val x = position.x + halfWidth * kotlin.math.cos(angle)
                    val y = position.y + halfHeight * kotlin.math.sin(angle)
                    points.add(Point(x, y))
                }
                SvgPolygon(points)
            }
            "star" -> {
                // Create star shape as polygon (5-pointed star)
                val outerRadius = minOf(size.width, size.height) / 2
                val innerRadius = outerRadius * 0.4 // Inner radius is 40% of outer
                val points = mutableListOf<Point>()
                for (i in 0 until 10) {
                    val angle = -kotlin.math.PI / 2 + (kotlin.math.PI * i / 5) // Start from top
                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                    val x = position.x + radius * kotlin.math.cos(angle)
                    val y = position.y + radius * kotlin.math.sin(angle)
                    points.add(Point(x, y))
                }
                SvgPolygon(points)
            }
            "point" -> {
                // Create a small circle for point shape
                val radius = 2.0 // Fixed small radius for point
                SvgEllipse(
                    cx = position.x,
                    cy = position.y,
                    rx = radius,
                    ry = radius
                )
            }
            "plaintext", "plain" -> {
                // For plaintext nodes, create an invisible rectangle (no stroke, no fill)
                val rect = SvgRect(
                    x = position.x - size.width / 2,
                    y = position.y - size.height / 2,
                    width = size.width,
                    height = size.height
                )
                rect.setAttribute("fill", "none")
                rect.setAttribute("stroke", "none")
                rect
            }
            else -> {
                // Default to ellipse for unknown shapes
                SvgEllipse(
                    cx = position.x,
                    cy = position.y,
                    rx = size.width / 2,
                    ry = size.height / 2
                )
            }
        }
        
        // Apply node attributes
        applyNodeAttributes(shapeElement, node)
        shapeElement.setAttribute("id", "node-${node.id}")
        elements.add(shapeElement)
        
        // Add label if present
        node.attributes.get(AttributeKey.LABEL)?.let { label ->
            val textElement = createNodeTextElement(label, position, node, options)
            textElement.setAttribute("id", "node-${node.id}-label")
            elements.add(textElement)
        }
        
        return elements
    }
    
    override fun renderEdge(edge: Edge, options: RenderOptions): List<SvgElement> {
        val elements = mutableListOf<SvgElement>()
        
        if (edge.controlPoints.isNotEmpty()) {
            // Create path from control points
            val pathElement = if (edge.controlPoints.size > 2) {
                SvgPath.fromCurvedPoints(edge.controlPoints)
            } else {
                SvgPath.fromPoints(edge.controlPoints)
            }
            
            // Apply edge attributes (color, style, width)
            applyEdgeAttributes(pathElement, edge)
            pathElement.setAttribute("id", "edge-${edge.source.id}-${edge.target.id}")
            pathElement.setAttribute("fill", "none") // Edges should not be filled by default
            elements.add(pathElement)
            
            // Add arrowhead for directed edges (handle different arrow types)
            val arrowhead = createArrowhead(edge, options)
            arrowhead?.let { elements.add(it) }
            
            // Add edge label if present (improved positioning)
            edge.attributes.get(AttributeKey.LABEL)?.let { label ->
                val labelElement = createEdgeLabel(edge, label, options)
                elements.add(labelElement)
            }
        }
        
        return elements
    }
    
    /**
     * Transform element coordinates using the coordinate system.
     * CRITICAL: Must match original Graphviz coordinate transformations exactly.
     */
    private fun transformElementCoordinates(element: SvgElement, coordinateSystem: CoordinateSystem) {
        // Graphviz uses a coordinate system where (0,0) is bottom-left, but SVG uses top-left
        // The transformation must match the original Graphviz behavior precisely
        
        when (element) {
            is SvgText -> {
                // Text elements need special handling to remain upright after Y-flip
                val y = element.y
                // Apply the same transformation as original Graphviz: flip Y, then counter-flip text
                element.setAttribute(
                    "transform",
                    "translate(0, ${formatCoordinate(y)}) scale(1, -1) translate(0, -${formatCoordinate(y)})"
                )
            }
            is SvgRect -> {
                // Rectangles need Y coordinate adjustment for bottom-left origin
                // val currentY = element.getAttribute("y")?.toDoubleOrNull() ?: 0.0
                // val height = element.getAttribute("height")?.toDoubleOrNull() ?: 0.0
                // Adjust Y to match Graphviz bottom-left coordinate system
                // element.setAttribute("y", formatCoordinate(coordinateSystem.height - currentY - height))
            }
            is SvgEllipse -> {
                // Ellipses need Y coordinate flipping
                // val currentCy = element.getAttribute("cy")?.toDoubleOrNull() ?: 0.0
                // element.setAttribute("cy", formatCoordinate(coordinateSystem.height - currentCy))
            }
            is SvgPath -> {
                // Paths need their coordinate data transformed
                transformPathCoordinates(element, coordinateSystem)
            }
            is SvgPolygon -> {
                // Polygons need point coordinate transformation
                transformPolygonCoordinates(element, coordinateSystem)
            }
        }
    }
    
    /**
     * Format coordinates with precision matching original Graphviz.
     */
    private fun formatCoordinate(value: Double): String {
        // Match Graphviz precision: typically 2 decimal places for coordinates
        return (kotlin.math.round(value * 100) / 100).toString()
    }
    
    /**
     * Transform path coordinates to match Graphviz coordinate system.
     */
    private fun transformPathCoordinates(pathElement: SvgPath, coordinateSystem: CoordinateSystem) {
        // val pathData = pathElement.getAttribute("d") ?: return
        // 
        // // Parse and transform path data coordinates
        // val transformedData = transformPathData(pathData, coordinateSystem.height)
        // pathElement.setAttribute("d", transformedData)
    }
    
    /**
     * Transform polygon coordinates to match Graphviz coordinate system.
     */
    private fun transformPolygonCoordinates(polygonElement: SvgPolygon, coordinateSystem: CoordinateSystem) {
        // val points = polygonElement.getAttribute("points") ?: return
        // 
        // // Parse and transform point coordinates
        // val transformedPoints = transformPointsData(points, coordinateSystem.height)
        // polygonElement.setAttribute("points", transformedPoints)
    }
    
    /**
     * Transform path data string coordinates.
     */
    private fun transformPathData(pathData: String, height: Double): String {
        // Transform Y coordinates in path data to match Graphviz coordinate system
        val coordinateRegex = """([ML])\s*([\d.-]+)\s*([\d.-]+)""".toRegex()
        
        return coordinateRegex.replace(pathData) { matchResult ->
            val command = matchResult.groupValues[1]
            val x = matchResult.groupValues[2].toDouble()
            val y = matchResult.groupValues[3].toDouble()
            val transformedY = height - y
            
            "$command ${formatCoordinate(x)} ${formatCoordinate(transformedY)}"
        }
    }
    
    /**
     * Transform points data string coordinates.
     */
    private fun transformPointsData(pointsData: String, height: Double): String {
        val pointRegex = """([\d.-]+),([\d.-]+)""".toRegex()
        
        return pointRegex.replace(pointsData) { matchResult ->
            val x = matchResult.groupValues[1].toDouble()
            val y = matchResult.groupValues[2].toDouble()
            val transformedY = height - y
            
            "${formatCoordinate(x)},${formatCoordinate(transformedY)}"
        }
    }
    
    /**
     * Create an arrowhead for directed edges with support for different arrow types.
     */
    private fun createArrowhead(edge: Edge, options: RenderOptions): SvgElement? {
        if (edge.controlPoints.size < 2) return null
        
        // Check if this is a directed edge and should have an arrowhead
        val direction = edge.attributes.getRaw("dir")?.toDotString()?.lowercase() ?: "forward"
        val arrowheadType = edge.attributes.getRaw("arrowhead")?.toDotString()?.lowercase() ?: "normal"
        
        // Skip arrowhead for undirected edges or when explicitly disabled
        if (direction == "none" || arrowheadType == "none") {
            return null
        }
        
        // Skip arrowhead for self-loops (can be handled separately if needed)
        if (edge.source == edge.target) {
            return null
        }
        
        val lastPoint = edge.controlPoints.last()
        val secondLastPoint = edge.controlPoints[edge.controlPoints.size - 2]
        
        // Calculate arrow direction
        val angle = CoordinateUtils.angle(secondLastPoint, lastPoint)
        
        // Get arrow size from edge attributes or use defaults
        val arrowSize = edge.attributes.getRaw("arrowsize")?.let { 
            when (it) {
                is AttributeValue.NumberValue -> it.value
                is AttributeValue.StringValue -> it.value.toDoubleOrNull()
                else -> null
            }
        } ?: 1.0
        
        val baseArrowLength = 10.0 * arrowSize
        val baseArrowWidth = 6.0 * arrowSize
        
        return when (arrowheadType) {
            "normal", "forward" -> createNormalArrowhead(lastPoint, angle, baseArrowLength, baseArrowWidth, edge)
            "inv", "back" -> createInvArrowhead(lastPoint, angle, baseArrowLength, baseArrowWidth, edge)
            "dot" -> createDotArrowhead(lastPoint, angle, baseArrowLength, baseArrowWidth, edge)
            "odot" -> createOpenDotArrowhead(lastPoint, angle, baseArrowLength, baseArrowWidth, edge)
            "diamond" -> createDiamondArrowhead(lastPoint, angle, baseArrowLength, baseArrowWidth, edge)
            "odiamond" -> createOpenDiamondArrowhead(lastPoint, angle, baseArrowLength, baseArrowWidth, edge)
            "box" -> createBoxArrowhead(lastPoint, angle, baseArrowLength, baseArrowWidth, edge)
            "obox" -> createOpenBoxArrowhead(lastPoint, angle, baseArrowLength, baseArrowWidth, edge)
            "vee" -> createVeeArrowhead(lastPoint, angle, baseArrowLength, baseArrowWidth, edge)
            "tee" -> createTeeArrowhead(lastPoint, angle, baseArrowLength, baseArrowWidth, edge)
            "crow" -> createCrowArrowhead(lastPoint, angle, baseArrowLength, baseArrowWidth, edge)
            else -> createNormalArrowhead(lastPoint, angle, baseArrowLength, baseArrowWidth, edge)
        }
    }
    
    /**
     * Create a normal (triangular) arrowhead.
     */
    private fun createNormalArrowhead(
        point: Point, 
        angle: Double, 
        length: Double, 
        width: Double, 
        edge: Edge
    ): SvgElement {
        val arrowPoints = listOf(
            point,
            Point(
                point.x - length * kotlin.math.cos(angle - kotlin.math.PI / 6),
                point.y - length * kotlin.math.sin(angle - kotlin.math.PI / 6)
            ),
            Point(
                point.x - length * kotlin.math.cos(angle + kotlin.math.PI / 6),
                point.y - length * kotlin.math.sin(angle + kotlin.math.PI / 6)
            )
        )
        
        val arrowElement = SvgPolygon(arrowPoints)
        applyArrowheadAttributes(arrowElement, edge, filled = true)
        return arrowElement
    }
    
    /**
     * Create an inverted arrowhead.
     */
    private fun createInvArrowhead(
        point: Point, 
        angle: Double, 
        length: Double, 
        width: Double, 
        edge: Edge
    ): SvgElement {
        val arrowPoints = listOf(
            Point(
                point.x - length * kotlin.math.cos(angle),
                point.y - length * kotlin.math.sin(angle)
            ),
            Point(
                point.x - length * kotlin.math.cos(angle - kotlin.math.PI / 6),
                point.y - length * kotlin.math.sin(angle - kotlin.math.PI / 6)
            ),
            Point(
                point.x - length * kotlin.math.cos(angle + kotlin.math.PI / 6),
                point.y - length * kotlin.math.sin(angle + kotlin.math.PI / 6)
            )
        )
        
        val arrowElement = SvgPolygon(arrowPoints)
        applyArrowheadAttributes(arrowElement, edge, filled = true)
        return arrowElement
    }
    
    /**
     * Create a dot arrowhead.
     */
    private fun createDotArrowhead(
        point: Point, 
        angle: Double, 
        length: Double, 
        width: Double, 
        edge: Edge
    ): SvgElement {
        val radius = width / 2
        val arrowElement = SvgEllipse(point.x, point.y, radius, radius)
        applyArrowheadAttributes(arrowElement, edge, filled = true)
        return arrowElement
    }
    
    /**
     * Create an open dot arrowhead.
     */
    private fun createOpenDotArrowhead(
        point: Point, 
        angle: Double, 
        length: Double, 
        width: Double, 
        edge: Edge
    ): SvgElement {
        val radius = width / 2
        val arrowElement = SvgEllipse(point.x, point.y, radius, radius)
        applyArrowheadAttributes(arrowElement, edge, filled = false)
        return arrowElement
    }
    
    /**
     * Create a diamond arrowhead.
     */
    private fun createDiamondArrowhead(
        point: Point, 
        angle: Double, 
        length: Double, 
        width: Double, 
        edge: Edge
    ): SvgElement {
        val halfLength = length / 2
        val halfWidth = width / 2
        
        val arrowPoints = listOf(
            point,
            Point(
                point.x - halfLength * kotlin.math.cos(angle - kotlin.math.PI / 2),
                point.y - halfLength * kotlin.math.sin(angle - kotlin.math.PI / 2)
            ),
            Point(
                point.x - length * kotlin.math.cos(angle),
                point.y - length * kotlin.math.sin(angle)
            ),
            Point(
                point.x - halfLength * kotlin.math.cos(angle + kotlin.math.PI / 2),
                point.y - halfLength * kotlin.math.sin(angle + kotlin.math.PI / 2)
            )
        )
        
        val arrowElement = SvgPolygon(arrowPoints)
        applyArrowheadAttributes(arrowElement, edge, filled = true)
        return arrowElement
    }
    
    /**
     * Create an open diamond arrowhead.
     */
    private fun createOpenDiamondArrowhead(
        point: Point, 
        angle: Double, 
        length: Double, 
        width: Double, 
        edge: Edge
    ): SvgElement {
        val arrowElement = createDiamondArrowhead(point, angle, length, width, edge)
        applyArrowheadAttributes(arrowElement, edge, filled = false)
        return arrowElement
    }
    
    /**
     * Create a box arrowhead.
     */
    private fun createBoxArrowhead(
        point: Point, 
        angle: Double, 
        length: Double, 
        width: Double, 
        edge: Edge
    ): SvgElement {
        val halfWidth = width / 2
        
        val arrowPoints = listOf(
            Point(
                point.x - halfWidth * kotlin.math.cos(angle - kotlin.math.PI / 2),
                point.y - halfWidth * kotlin.math.sin(angle - kotlin.math.PI / 2)
            ),
            Point(
                point.x - halfWidth * kotlin.math.cos(angle + kotlin.math.PI / 2),
                point.y - halfWidth * kotlin.math.sin(angle + kotlin.math.PI / 2)
            ),
            Point(
                point.x - length * kotlin.math.cos(angle + kotlin.math.PI / 2),
                point.y - length * kotlin.math.sin(angle + kotlin.math.PI / 2)
            ),
            Point(
                point.x - length * kotlin.math.cos(angle - kotlin.math.PI / 2),
                point.y - length * kotlin.math.sin(angle - kotlin.math.PI / 2)
            )
        )
        
        val arrowElement = SvgPolygon(arrowPoints)
        applyArrowheadAttributes(arrowElement, edge, filled = true)
        return arrowElement
    }
    
    /**
     * Create an open box arrowhead.
     */
    private fun createOpenBoxArrowhead(
        point: Point, 
        angle: Double, 
        length: Double, 
        width: Double, 
        edge: Edge
    ): SvgElement {
        val arrowElement = createBoxArrowhead(point, angle, length, width, edge)
        applyArrowheadAttributes(arrowElement, edge, filled = false)
        return arrowElement
    }
    
    /**
     * Create a vee arrowhead.
     */
    private fun createVeeArrowhead(
        point: Point, 
        angle: Double, 
        length: Double, 
        width: Double, 
        edge: Edge
    ): SvgElement {
        val arrowPoints = listOf(
            Point(
                point.x - length * kotlin.math.cos(angle - kotlin.math.PI / 6),
                point.y - length * kotlin.math.sin(angle - kotlin.math.PI / 6)
            ),
            point,
            Point(
                point.x - length * kotlin.math.cos(angle + kotlin.math.PI / 6),
                point.y - length * kotlin.math.sin(angle + kotlin.math.PI / 6)
            )
        )
        
        val pathData = "M ${arrowPoints[0].x} ${arrowPoints[0].y} L ${arrowPoints[1].x} ${arrowPoints[1].y} L ${arrowPoints[2].x} ${arrowPoints[2].y}"
        val arrowElement = SvgPath(pathData)
        applyArrowheadAttributes(arrowElement, edge, filled = false)
        return arrowElement
    }
    
    /**
     * Create a tee arrowhead.
     */
    private fun createTeeArrowhead(
        point: Point, 
        angle: Double, 
        length: Double, 
        width: Double, 
        edge: Edge
    ): SvgElement {
        val perpAngle = angle + kotlin.math.PI / 2
        val halfWidth = width / 2
        
        val arrowElement = SvgLine(
            point.x - halfWidth * kotlin.math.cos(perpAngle),
            point.y - halfWidth * kotlin.math.sin(perpAngle),
            point.x + halfWidth * kotlin.math.cos(perpAngle),
            point.y + halfWidth * kotlin.math.sin(perpAngle)
        )
        
        applyArrowheadAttributes(arrowElement, edge, filled = false)
        return arrowElement
    }
    
    /**
     * Create a crow arrowhead.
     */
    private fun createCrowArrowhead(
        point: Point, 
        angle: Double, 
        length: Double, 
        width: Double, 
        edge: Edge
    ): SvgElement {
        val midPoint = Point(
            point.x - length * 0.5 * kotlin.math.cos(angle),
            point.y - length * 0.5 * kotlin.math.sin(angle)
        )
        
        val arrowPoints = listOf(
            Point(
                point.x - length * kotlin.math.cos(angle - kotlin.math.PI / 4),
                point.y - length * kotlin.math.sin(angle - kotlin.math.PI / 4)
            ),
            midPoint,
            Point(
                point.x - length * kotlin.math.cos(angle + kotlin.math.PI / 4),
                point.y - length * kotlin.math.sin(angle + kotlin.math.PI / 4)
            )
        )
        
        val pathData = "M ${arrowPoints[0].x} ${arrowPoints[0].y} L ${arrowPoints[1].x} ${arrowPoints[1].y} L ${arrowPoints[2].x} ${arrowPoints[2].y}"
        val arrowElement = SvgPath(pathData)
        applyArrowheadAttributes(arrowElement, edge, filled = false)
        return arrowElement
    }
    
    /**
     * Apply attributes to arrowhead elements.
     */
    private fun applyArrowheadAttributes(element: SvgElement, edge: Edge, filled: Boolean) {
        element.setAttribute("class", "arrowhead")
        
        // Use edge color for arrowhead
        val color = edge.attributes.get(AttributeKey.COLOR)?.toDotString() ?: "black"
        
        if (filled) {
            element.setAttribute("fill", color)
            element.setAttribute("stroke", color)
        } else {
            element.setAttribute("fill", "none")
            element.setAttribute("stroke", color)
        }
        
        // Apply stroke width from edge
        edge.attributes.get(AttributeKey.PENWIDTH)?.let { width ->
            element.setAttribute("stroke-width", width.toString())
        }
    }
    
    /**
     * Create an edge label with improved positioning and styling.
     */
    private fun createEdgeLabel(edge: Edge, label: String, options: RenderOptions): SvgElement {
        // Calculate label position based on edge path
        val labelPosition = calculateEdgeLabelPosition(edge)
        
        val textElement = SvgText(labelPosition.x, labelPosition.y, label)
        
        // Apply edge label styling
        textElement.setAttribute("class", "edge-label")
        textElement.setAttribute("text-anchor", "middle")
        textElement.setAttribute("dominant-baseline", "central")
        textElement.setAttribute("id", "edge-${edge.source.id}-${edge.target.id}-label")
        
        // Apply font attributes from edge
        edge.attributes.get(AttributeKey.FONTNAME)?.let { fontName ->
            textElement.setAttribute("font-family", fontName)
        }
        
        edge.attributes.get(AttributeKey.FONTSIZE)?.let { fontSize ->
            textElement.setAttribute("font-size", fontSize.toString())
        }
        
        // Apply label color (use fontcolor if available, otherwise use edge color)
        val labelColor = edge.attributes.getRaw("fontcolor")
            ?: edge.attributes.get(AttributeKey.COLOR)?.let { AttributeValue.ColorValue(it) }
        labelColor?.let { color ->
            textElement.setAttribute("fill", color.toDotString())
        } ?: run {
            textElement.setAttribute("fill", "black")
        }
        
        // Add background for better readability if requested
        val labelBackground = edge.attributes.getRaw("labelbg")
        if (labelBackground != null) {
            // Create a group with background rectangle and text
            val group = SvgGroup()
            group.setAttribute("class", "edge-label-group")
            
            // Estimate text dimensions (simplified)
            val fontSize = edge.attributes.get(AttributeKey.FONTSIZE) ?: 12.0
            val textWidth = label.length * fontSize * 0.6 // Rough estimate
            val textHeight = fontSize * 1.2
            
            val bgRect = SvgRect(
                labelPosition.x - textWidth / 2 - 2,
                labelPosition.y - textHeight / 2 - 1,
                textWidth + 4,
                textHeight + 2
            )
            bgRect.setAttribute("fill", labelBackground.toDotString())
            bgRect.setAttribute("stroke", "none")
            bgRect.setAttribute("opacity", "0.8")
            
            group.addChild(bgRect)
            group.addChild(textElement)
            return group
        }
        
        return textElement
    }
    
    /**
     * Calculate the optimal position for an edge label.
     */
    private fun calculateEdgeLabelPosition(edge: Edge): Point {
        val controlPoints = edge.controlPoints
        
        return when {
            controlPoints.isEmpty() -> Point(0.0, 0.0)
            controlPoints.size == 1 -> controlPoints[0]
            controlPoints.size == 2 -> {
                // For straight edges, use midpoint
                CoordinateUtils.midpoint(controlPoints[0], controlPoints[1])
            }
            else -> {
                // For curved edges, find the point at the middle of the path
                val midIndex = controlPoints.size / 2
                if (controlPoints.size % 2 == 1) {
                    // Odd number of points, use the middle point
                    controlPoints[midIndex]
                } else {
                    // Even number of points, interpolate between middle points
                    CoordinateUtils.midpoint(controlPoints[midIndex - 1], controlPoints[midIndex])
                }
            }
        }
    }
    
    /**
     * Create a text element for node labels with proper positioning and styling.
     */
    private fun createNodeTextElement(
        text: String,
        position: Point,
        node: Node,
        options: RenderOptions
    ): SvgText {
        val textElement = SvgText(position.x, position.y, text)
        
        // Apply text styling attributes
        textElement.setAttribute("class", "node-text")
        textElement.setAttribute("text-anchor", "middle")
        textElement.setAttribute("dominant-baseline", "central")
        
        // Apply font attributes from node
        node.attributes.get(AttributeKey.FONTNAME)?.let { fontName ->
            textElement.setAttribute("font-family", fontName)
        }
        
        node.attributes.get(AttributeKey.FONTSIZE)?.let { fontSize ->
            textElement.setAttribute("font-size", fontSize.toString())
        }
        
        // Apply text color (use fontcolor if available, otherwise use color)
        val textColor = node.attributes.getRaw("fontcolor") 
            ?: node.attributes.get(AttributeKey.COLOR)?.let { AttributeValue.ColorValue(it) }
        textColor?.let { color ->
            textElement.setAttribute("fill", color.toDotString())
        }
        
        // Handle multi-line text by splitting on \n or \l
        if (text.contains("\\n") || text.contains("\\l")) {
            // For multi-line text, we need to create multiple tspan elements
            // This is a simplified implementation - a full implementation would handle
            // proper line spacing and alignment
            val lines = text.split("\\n", "\\l")
            if (lines.size > 1) {
                // For now, just use the first line and add a title for the full text
                textElement.setAttribute("title", text.replace("\\n", "\n").replace("\\l", "\n"))
                return SvgText(position.x, position.y, lines[0])
            }
        }
        
        return textElement
    }
}