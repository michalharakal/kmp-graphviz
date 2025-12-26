package org.graphviz.kotlin.layout.dot

import org.graphviz.kotlin.layout.*
import org.graphviz.kotlin.model.*

/**
 * Implementation of the dot layout algorithm for hierarchical directed graphs.
 * 
 * The dot layout algorithm works in four main phases:
 * 1. Ranking: Assign nodes to hierarchical levels
 * 2. Ordering: Arrange nodes within ranks to minimize crossings
 * 3. Coordinate Assignment: Calculate final positions and route edges
 * 4. Final Adjustments: Handle overlaps and normalize coordinates
 */
class DotLayoutEngine : BaseLayoutEngine() {
    
    override val name = "dot"
    
    private val ranking = DotRanking()
    private val ordering = DotOrdering()
    private val coordinates = DotCoordinates()
    
    override fun layout(graph: Graph, options: LayoutOptions): LayoutResult {
        // Handle edge cases first
        val edgeCaseResult = handleEdgeCases(graph, options)
        if (edgeCaseResult != null) {
            return edgeCaseResult
        }
        
        // Perform pre-layout validation and setup
        val preResult = preLayout(graph, options)
        if (preResult is PreLayoutResult.Error) {
            return handleError(preResult.message)
        }
        
        val nodeSizes = (preResult as PreLayoutResult.Success).nodeSizes
        
        try {
            // Phase 1: Assign ranks to nodes
            val rankingResult = ranking.assignRanks(graph)
            if (!rankingResult.isSuccess) {
                val error = rankingResult as RankingResult.Error
                return handleError("Ranking failed: ${error.message}", error.cause)
            }
            
            val ranks = (rankingResult as RankingResult.Success).ranks
            val feedbackEdges = rankingResult.feedbackEdges
            
            // Phase 2: Order nodes within ranks
            val orderingResult = ordering.orderNodes(graph, ranks, nodeSizes)
            if (!orderingResult.isSuccess) {
                val error = orderingResult as OrderingResult.Error
                return handleError("Ordering failed: ${error.message}", error.cause)
            }
            
            val nodeOrdering = (orderingResult as OrderingResult.Success).ordering
            
            // Phase 3: Assign coordinates
            val coordinateResult = coordinates.assignCoordinates(graph, ranks, nodeOrdering, options)
            if (!coordinateResult.isSuccess) {
                val error = coordinateResult as CoordinateResult.Error
                return handleError("Coordinate assignment failed: ${error.message}", error.cause)
            }
            
            val nodePositions = (coordinateResult as CoordinateResult.Success).nodePositions
            val edgeControlPoints = coordinateResult.edgeControlPoints
            
            // Phase 4: Final adjustments
            val adjustedPositions = coordinates.adjustForOverlaps(nodePositions, nodeSizes)
            
            // Apply positions to create the final positioned graph
            val positionedGraph = applyPositions(graph, adjustedPositions, edgeControlPoints)
            
            return success(positionedGraph)
            
        } catch (e: Exception) {
            return handleError("Dot layout failed: ${e.message}", e)
        }
    }
    
    /**
     * Handle edge cases that require special layout treatment.
     * Returns a LayoutResult if the case was handled, null if normal processing should continue.
     */
    private fun handleEdgeCases(graph: Graph, options: LayoutOptions): LayoutResult? {
        val nodes = graph.getAllNodes()
        val edges = graph.getAllEdges()
        
        // Case 1: Empty graph
        if (nodes.isEmpty()) {
            return success(graph) // Return the empty graph as-is
        }
        
        // Case 2: Single node
        if (nodes.size == 1) {
            return handleSingleNode(graph, nodes.first(), options)
        }
        
        // Case 3: Disconnected components
        val components = DotLayoutEdgeCases.findConnectedComponents(graph)
        if (components.size > 1) {
            return handleDisconnectedComponents(graph, components, options)
        }
        
        // Case 4: Graph with only self-loops
        if (edges.all { it.source == it.target }) {
            return handleSelfLoopsOnly(graph, options)
        }
        
        // Case 5: Multiple edges between same nodes
        if (DotLayoutEdgeCases.hasMultipleEdges(graph)) {
            // This is handled during normal processing but we can optimize
            return null // Continue with normal processing
        }
        
        return null // No edge case detected, continue with normal processing
    }
    
    /**
     * Handle layout for a single node graph.
     */
    private fun handleSingleNode(graph: Graph, node: Node, options: LayoutOptions): LayoutResult {
        try {
            // Position the single node at origin
            val positionedNode = NodeImpl(
                id = node.id,
                attributes = node.attributes,
                position = Point(0.0, 0.0)
            )
            
            // Handle self-loops if present
            val positionedEdges = graph.getAllEdges().map { edge ->
                if (edge.source == node && edge.target == node) {
                    // Create self-loop control points
                    val selfLoopPoints = DotLayoutEdgeCases.createSelfLoopControlPoints(Point(0.0, 0.0), options)
                    EdgeImpl(
                        source = positionedNode,
                        target = positionedNode,
                        attributes = edge.attributes,
                        controlPoints = selfLoopPoints
                    )
                } else {
                    edge // This shouldn't happen in a single-node graph
                }
            }
            
            val positionedGraph = GraphImpl(
                id = graph.id,
                isDirected = graph.isDirected,
                nodes = setOf(positionedNode),
                edges = positionedEdges.toSet(),
                subgraphs = graph.subgraphs,
                attributes = graph.attributes
            )
            
            return success(positionedGraph)
            
        } catch (e: Exception) {
            return handleError("Failed to layout single node: ${e.message}", e)
        }
    }
    
    /**
     * Handle layout for disconnected components.
     */
    private fun handleDisconnectedComponents(
        graph: Graph, 
        components: List<Set<Node>>, 
        options: LayoutOptions
    ): LayoutResult {
        try {
            val componentGraphs = mutableListOf<Graph>()
            val componentResults = mutableListOf<LayoutResult>()
            
            // Layout each component separately
            for ((index, component) in components.withIndex()) {
                val componentEdges = graph.getAllEdges().filter { 
                    it.source in component && it.target in component 
                }
                
                val componentGraph = GraphImpl(
                    id = "${graph.id}_component_$index",
                    isDirected = graph.isDirected,
                    nodes = component,
                    edges = componentEdges.toSet(),
                    subgraphs = emptySet(),
                    attributes = graph.attributes
                )
                
                // Recursively layout the component
                val componentResult = layout(componentGraph, options)
                if (componentResult.isError) {
                    return componentResult // Propagate error
                }
                
                componentResults.add(componentResult)
                componentGraphs.add((componentResult as LayoutResult.Success).graph)
            }
            
            // Arrange components horizontally with spacing
            val arrangedGraph = DotLayoutEdgeCases.arrangeComponentsHorizontally(componentGraphs, options)
            
            return success(arrangedGraph)
            
        } catch (e: Exception) {
            return handleError("Failed to layout disconnected components: ${e.message}", e)
        }
    }
    
    /**
     * Handle layout for graphs with only self-loops.
     */
    private fun handleSelfLoopsOnly(graph: Graph, options: LayoutOptions): LayoutResult {
        try {
            val nodes = graph.getAllNodes()
            val positionedNodes = mutableSetOf<Node>()
            val positionedEdges = mutableSetOf<Edge>()
            
            // Arrange nodes in a simple grid layout
            val gridSize = kotlin.math.ceil(kotlin.math.sqrt(nodes.size.toDouble())).toInt()
            val spacing = options.nodeSpacing * 2 // Extra spacing for self-loops
            
            nodes.forEachIndexed { index, node ->
                val row = index / gridSize
                val col = index % gridSize
                val position = Point(col * spacing, row * spacing)
                
                val positionedNode = NodeImpl(
                    id = node.id,
                    attributes = node.attributes,
                    position = position
                )
                positionedNodes.add(positionedNode)
                
                // Handle self-loops for this node
                graph.getAllEdges().filter { it.source == node && it.target == node }.forEach { edge ->
                    val selfLoopPoints = DotLayoutEdgeCases.createSelfLoopControlPoints(position, options)
                    val positionedEdge = EdgeImpl(
                        source = positionedNode,
                        target = positionedNode,
                        attributes = edge.attributes,
                        controlPoints = selfLoopPoints
                    )
                    positionedEdges.add(positionedEdge)
                }
            }
            
            val positionedGraph = GraphImpl(
                id = graph.id,
                isDirected = graph.isDirected,
                nodes = positionedNodes,
                edges = positionedEdges,
                subgraphs = graph.subgraphs,
                attributes = graph.attributes
            )
            
            return success(positionedGraph)
            
        } catch (e: Exception) {
            return handleError("Failed to layout self-loops only graph: ${e.message}", e)
        }
    }
    
    /**
     * Get layout statistics for debugging and optimization.
     */
    fun getLayoutStatistics(
        graph: Graph,
        ranks: Map<Node, Int>,
        ordering: Map<Int, List<Node>>,
        nodePositions: Map<Node, Point>,
        edgeControlPoints: Map<Edge, List<Point>>,
        nodeSizes: Map<Node, NodeSize>
    ): DotLayoutStatistics {
        val crossings = this.ordering.countCrossings(graph, ordering)
        val totalEdgeSpan = RankingUtils.getTotalEdgeSpan(graph, ranks)
        val totalEdgeLength = CoordinateUtils.calculateTotalEdgeLength(edgeControlPoints)
        val boundingBox = coordinates.calculateBoundingBox(nodePositions, nodeSizes)
        val hasOverlaps = CoordinateUtils.hasOverlaps(nodePositions, nodeSizes)
        
        return DotLayoutStatistics(
            nodeCount = graph.getAllNodes().size,
            edgeCount = graph.getAllEdges().size,
            rankCount = ranks.values.distinct().size,
            crossings = crossings,
            totalEdgeSpan = totalEdgeSpan,
            totalEdgeLength = totalEdgeLength,
            boundingBox = boundingBox,
            hasOverlaps = hasOverlaps
        )
    }
}

/**
 * Statistics about a dot layout for analysis and debugging.
 */
data class DotLayoutStatistics(
    val nodeCount: Int,
    val edgeCount: Int,
    val rankCount: Int,
    val crossings: Int,
    val totalEdgeSpan: Int,
    val totalEdgeLength: Double,
    val boundingBox: BoundingBox?,
    val hasOverlaps: Boolean
) {
    /**
     * Calculate a quality score for the layout (lower is better).
     */
    fun calculateQualityScore(): Double {
        var score = 0.0
        
        // Penalize crossings heavily
        score += crossings * 100.0
        
        // Penalize long edge spans
        score += totalEdgeSpan * 10.0
        
        // Penalize overlaps
        if (hasOverlaps) {
            score += 1000.0
        }
        
        // Prefer compact layouts
        boundingBox?.let { bbox ->
            score += bbox.width * bbox.height * 0.01
        }
        
        return score
    }
    
    override fun toString(): String {
        return buildString {
            appendLine("Dot Layout Statistics:")
            appendLine("  Nodes: $nodeCount")
            appendLine("  Edges: $edgeCount")
            appendLine("  Ranks: $rankCount")
            appendLine("  Crossings: $crossings")
            appendLine("  Total Edge Span: $totalEdgeSpan")
            appendLine("  Total Edge Length: ${totalEdgeLength}")
            appendLine("  Has Overlaps: $hasOverlaps")
            boundingBox?.let { bbox ->
                appendLine("  Bounding Box: ${bbox.width} x ${bbox.height}")
            }
            appendLine("  Quality Score: ${calculateQualityScore()}")
        }
    }
}

/**
 * Utility functions for handling edge cases in dot layout.
 */
object DotLayoutEdgeCases {
    
    /**
     * Find connected components in the graph using DFS.
     */
    fun findConnectedComponents(graph: Graph): List<Set<Node>> {
        val visited = mutableSetOf<Node>()
        val components = mutableListOf<Set<Node>>()
        
        for (node in graph.getAllNodes()) {
            if (node !in visited) {
                val component = mutableSetOf<Node>()
                dfsComponent(graph, node, visited, component)
                components.add(component)
            }
        }
        
        return components
    }
    
    private fun dfsComponent(
        graph: Graph,
        node: Node,
        visited: MutableSet<Node>,
        component: MutableSet<Node>
    ) {
        visited.add(node)
        component.add(node)
        
        // Visit all connected nodes (ignore edge direction for component detection)
        for (edge in graph.getAllEdges()) {
            when {
                edge.source == node && edge.target !in visited -> {
                    dfsComponent(graph, edge.target, visited, component)
                }
                edge.target == node && edge.source !in visited -> {
                    dfsComponent(graph, edge.source, visited, component)
                }
            }
        }
    }
    
    /**
     * Check if the graph has multiple edges between the same pair of nodes.
     */
    fun hasMultipleEdges(graph: Graph): Boolean {
        val edgePairs = mutableSetOf<Pair<String, String>>()
        
        for (edge in graph.getAllEdges()) {
            val pair = if (graph.isDirected) {
                Pair(edge.source.id, edge.target.id)
            } else {
                // For undirected graphs, normalize the pair
                val (first, second) = if (edge.source.id <= edge.target.id) {
                    Pair(edge.source.id, edge.target.id)
                } else {
                    Pair(edge.target.id, edge.source.id)
                }
                Pair(first, second)
            }
            
            if (pair in edgePairs) {
                return true
            }
            edgePairs.add(pair)
        }
        
        return false
    }
    
    /**
     * Create control points for self-loop edges.
     */
    fun createSelfLoopControlPoints(nodePosition: Point, options: LayoutOptions): List<Point> {
        val radius = options.nodeSpacing * 0.8
        val centerX = nodePosition.x + radius
        val centerY = nodePosition.y - radius
        
        // Create a circular self-loop
        val points = mutableListOf<Point>()
        val numPoints = 8 // Number of points to approximate the circle
        
        for (i in 0 until numPoints) {
            val angle = 2 * kotlin.math.PI * i / numPoints
            val x = centerX + radius * kotlin.math.cos(angle)
            val y = centerY + radius * kotlin.math.sin(angle)
            points.add(Point(x, y))
        }
        
        // Ensure the loop starts and ends at the node
        points.add(0, nodePosition)
        points.add(nodePosition)
        
        return points
    }
    
    /**
     * Arrange multiple components horizontally with proper spacing.
     */
    fun arrangeComponentsHorizontally(
        componentGraphs: List<Graph>,
        options: LayoutOptions
    ): Graph {
        if (componentGraphs.isEmpty()) {
            throw IllegalArgumentException("No components to arrange")
        }
        
        if (componentGraphs.size == 1) {
            return componentGraphs.first()
        }
        
        val allNodes = mutableSetOf<Node>()
        val allEdges = mutableSetOf<Edge>()
        var currentX = 0.0
        
        for (componentGraph in componentGraphs) {
            // Calculate component bounding box
            val componentNodes = componentGraph.getAllNodes()
            if (componentNodes.isEmpty()) continue
            
            val minX = componentNodes.minOfOrNull { it.position?.x ?: 0.0 } ?: 0.0
            val maxX = componentNodes.maxOfOrNull { it.position?.x ?: 0.0 } ?: 0.0
            val componentWidth = maxX - minX
            
            // Offset all nodes in this component
            val offsetX = currentX - minX
            
            val offsetNodes = componentNodes.map { node ->
                val newPosition = node.position?.let { pos ->
                    Point(pos.x + offsetX, pos.y)
                } ?: Point(offsetX, 0.0)
                
                NodeImpl(
                    id = node.id,
                    attributes = node.attributes,
                    position = newPosition
                )
            }
            
            // Offset edge control points
            val offsetEdges = componentGraph.getAllEdges().map { edge ->
                val offsetControlPoints = edge.controlPoints.map { point ->
                    Point(point.x + offsetX, point.y)
                }
                
                val sourceNode = offsetNodes.find { it.id == edge.source.id }!!
                val targetNode = offsetNodes.find { it.id == edge.target.id }!!
                
                EdgeImpl(
                    source = sourceNode,
                    target = targetNode,
                    attributes = edge.attributes,
                    controlPoints = offsetControlPoints
                )
            }
            
            allNodes.addAll(offsetNodes)
            allEdges.addAll(offsetEdges)
            
            // Update position for next component
            currentX += componentWidth + options.nodeSpacing * 3 // Extra spacing between components
        }
        
        // Create the combined graph
        val firstGraph = componentGraphs.first()
        return GraphImpl(
            id = firstGraph.id,
            isDirected = firstGraph.isDirected,
            nodes = allNodes,
            edges = allEdges,
            subgraphs = firstGraph.subgraphs,
            attributes = firstGraph.attributes
        )
    }
}