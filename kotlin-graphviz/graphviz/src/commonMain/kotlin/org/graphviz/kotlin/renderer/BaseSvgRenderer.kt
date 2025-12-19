package org.graphviz.kotlin.renderer

import org.graphviz.kotlin.model.*
import org.graphviz.kotlin.layout.NodeSize
import org.graphviz.kotlin.layout.LayoutOptions
import org.graphviz.kotlin.layout.LayoutResult
import org.graphviz.kotlin.layout.dot.DotLayoutEngine

/**
 * Base implementation of SVG renderer providing common functionality.
 */
abstract class BaseSvgRenderer : SvgRenderer {
    
    override fun render(graph: Graph, options: RenderOptions): String {
        return try {
            val result = renderInternal(graph, options)
            when (result) {
                is RenderResult.Success -> result.svg
                is RenderResult.Error -> throw RenderException(result.message, result.cause)
            }
        } catch (e: Exception) {
            throw RenderException("Failed to render graph: ${e.message}", e)
        }
    }
    
    override fun renderToFile(graph: Graph, path: String, options: RenderOptions) {
        val svg = render(graph, options)
        // Note: File writing would be platform-specific
        // For now, this is a placeholder that would need platform-specific implementation
        throw NotImplementedError("File writing is platform-specific and not implemented in common code")
    }
    
    /**
     * Internal rendering method that returns a RenderResult.
     */
    protected fun renderInternal(graph: Graph, options: RenderOptions): RenderResult {
        return try {
            // If graph is not positioned, apply a default layout (dot)
            val positionedGraph = if (requiresLayout(graph)) {
                val engine = DotLayoutEngine()
                when (val layoutResult = engine.layout(graph, LayoutOptions.default())) {
                    is LayoutResult.Success -> layoutResult.graph
                    is LayoutResult.Error -> return RenderResult.Error(
                        "Layout failed: ${layoutResult.message}", layoutResult.cause
                    )
                }
            } else {
                graph
            }

            // Validate positioned graph
            val validation = validateGraph(positionedGraph)
            if (!validation.isValid) {
                val errors = (validation as ValidationResult.Invalid).errors
                return RenderResult.Error("Graph validation failed: ${errors.joinToString("; ")}")
            }
            
            // Calculate viewport and coordinate system
            val viewport = calculateViewport(positionedGraph, options)
            val boundingBox = positionedGraph.getBoundingBox()
            
            // Create SVG document with coordinate system
            val document = if (boundingBox != null) {
                SvgDocument.create(boundingBox, options)
            } else {
                SvgDocument.createSimple(viewport.width, viewport.height, viewport.viewBox)
            }
            
            // Render graph elements
            renderGraph(positionedGraph, document, options, viewport)
            
            // Generate SVG markup
            val svg = document.toSvg(options)
            RenderResult.Success(svg)
            
        } catch (e: Exception) {
            RenderResult.Error("Rendering failed: ${e.message}", e)
        }
    }

    /**
     * Determine if the graph requires a layout pass (missing node positions or edge control points).
     */
    private fun requiresLayout(graph: Graph): Boolean {
        val hasUnpositionedNodes = graph.getAllNodes().any { it.position == null }
        val hasEdgesWithoutControl = graph.getAllEdges().any { it.controlPoints.isEmpty() }
        return hasUnpositionedNodes || hasEdgesWithoutControl
    }
    
    /**
     * Validate that the graph is suitable for rendering.
     */
    protected fun validateGraph(graph: Graph): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Check for positioned nodes
        val allNodes = graph.getAllNodes()
        val unpositionedNodes = allNodes.filter { it.position == null }
        if (unpositionedNodes.isNotEmpty()) {
            errors.add("Graph contains unpositioned nodes: ${unpositionedNodes.map { it.id }.joinToString(", ")}")
        }
        
        // Check for edges with control points
        val allEdges = graph.getAllEdges()
        val edgesWithoutControlPoints = allEdges.filter { it.controlPoints.isEmpty() }
        if (edgesWithoutControlPoints.isNotEmpty()) {
            errors.add("Graph contains edges without control points")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Calculate the viewport and coordinate system for the SVG.
     */
    protected fun calculateViewport(graph: Graph, options: RenderOptions): Viewport {
        val boundingBox = graph.getBoundingBox()
            ?: return Viewport(
                width = options.width ?: 400.0,
                height = options.height ?: 300.0,
                viewBox = null
            )
        
        // Calculate dimensions with margin
        val margin = options.margin
        val width = options.width ?: (boundingBox.width + 2 * margin)
        val height = options.height ?: (boundingBox.height + 2 * margin)
        
        // Create viewBox that encompasses the graph with margin
        val viewBox = ViewBox.fromBoundingBox(boundingBox, margin)
        
        return Viewport(
            width = width,
            height = height,
            viewBox = viewBox
        )
    }
    
    /**
     * Render the complete graph to the SVG document.
     */
    protected abstract fun renderGraph(
        graph: Graph,
        document: SvgDocument,
        options: RenderOptions,
        viewport: Viewport
    )
    
    /**
     * Render a single node to SVG elements.
     */
    protected abstract fun renderNode(
        node: Node,
        options: RenderOptions
    ): List<SvgElement>
    
    /**
     * Render a single edge to SVG elements.
     */
    protected abstract fun renderEdge(
        edge: Edge,
        options: RenderOptions
    ): List<SvgElement>
    
    /**
     * Apply node attributes to SVG elements.
     */
    protected fun applyNodeAttributes(element: SvgElement, node: Node) {
        // Apply color
        node.attributes.get(AttributeKey.COLOR)?.let { color ->
            element.setAttribute("stroke", color.toDotString())
        }
        
        // Apply fill color
        node.attributes.get(AttributeKey.FILLCOLOR)?.let { color ->
            element.setAttribute("fill", color.toDotString())
        }
        
        // Apply style
        node.attributes.get(AttributeKey.STYLE)?.let { style ->
            when (style.lowercase()) {
                "filled" -> element.setAttribute("fill", node.attributes.get(AttributeKey.FILLCOLOR)?.toDotString() ?: "lightgray")
                "dashed" -> element.setAttribute("stroke-dasharray", "5,5")
                "dotted" -> element.setAttribute("stroke-dasharray", "2,2")
                "bold" -> element.setAttribute("stroke-width", "2")
            }
        }
        
        // Apply pen width
        node.attributes.get(AttributeKey.PENWIDTH)?.let { width ->
            element.setAttribute("stroke-width", width)
        }
        
        // Add CSS class
        element.setAttribute("class", "node")
    }
    
    /**
     * Apply edge attributes to SVG elements.
     */
    protected fun applyEdgeAttributes(element: SvgElement, edge: Edge) {
        // Apply color (stroke color)
        edge.attributes.get(AttributeKey.COLOR)?.let { color ->
            element.setAttribute("stroke", color.toDotString())
        } ?: run {
            // Default edge color
            element.setAttribute("stroke", "black")
        }
        
        // Apply pen width (stroke width)
        edge.attributes.get(AttributeKey.PENWIDTH)?.let { width ->
            element.setAttribute("stroke-width", width.toString())
        } ?: run {
            // Default stroke width
            element.setAttribute("stroke-width", "1")
        }
        
        // Apply style (dashed, dotted, bold, etc.)
        edge.attributes.get(AttributeKey.STYLE)?.let { style ->
            when (style.lowercase()) {
                "dashed" -> element.setAttribute("stroke-dasharray", "5,5")
                "dotted" -> element.setAttribute("stroke-dasharray", "2,2")
                "bold" -> element.setAttribute("stroke-width", "2")
                "invis", "invisible" -> {
                    element.setAttribute("stroke", "none")
                    element.setAttribute("opacity", "0")
                }
                "solid" -> {
                    // Remove any dash array for solid lines
                    element.setAttribute("stroke-dasharray", "none")
                }
            }
        }
        
        // Apply arrowhead style (affects how arrowheads are rendered)
        edge.attributes.getRaw("arrowhead")?.let { arrowheadAttr ->
            // Store arrowhead type for use in createArrowhead
            element.setAttribute("data-arrowhead", arrowheadAttr.toDotString())
        }
        
        // Apply arrowtail style (for bidirectional edges)
        edge.attributes.getRaw("arrowtail")?.let { arrowtailAttr ->
            element.setAttribute("data-arrowtail", arrowtailAttr.toDotString())
        }
        
        // Apply direction (for controlling arrow placement)
        edge.attributes.getRaw("dir")?.let { dirAttr ->
            element.setAttribute("data-dir", dirAttr.toDotString())
        }
        
        // Add CSS class for styling
        element.setAttribute("class", "edge")
    }
    
    /**
     * Create a text element for a label.
     */
    protected fun createTextElement(
        text: String,
        position: Point,
        options: RenderOptions
    ): SvgText {
        val textElement = SvgText(position.x, position.y, text)
        textElement.setAttribute("class", "text")
        return textElement
    }
    
    /**
     * Get the shape of a node from its attributes.
     */
    protected fun getNodeShape(node: Node): String {
        return node.attributes.get(AttributeKey.SHAPE) ?: "ellipse"
    }
    
    /**
     * Calculate the size of a node for rendering.
     */
    protected fun getNodeSize(node: Node, options: RenderOptions): NodeSize {
        val width = node.attributes.get(AttributeKey.WIDTH) ?: options.rendererOptions["defaultNodeWidth"] as? Double ?: 60.0
        val height = node.attributes.get(AttributeKey.HEIGHT) ?: options.rendererOptions["defaultNodeHeight"] as? Double ?: 30.0
        return NodeSize(width, height)
    }
}

/**
 * Represents the viewport and coordinate system for SVG rendering.
 */
data class Viewport(
    val width: Double,
    val height: Double,
    val viewBox: ViewBox?
)