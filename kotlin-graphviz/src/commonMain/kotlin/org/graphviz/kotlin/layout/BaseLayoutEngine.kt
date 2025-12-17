package org.graphviz.kotlin.layout

import org.graphviz.kotlin.model.*

/**
 * Base class for layout engines providing common functionality and utilities.
 */
abstract class BaseLayoutEngine : LayoutEngine {
    
    /**
     * Validate that the input graph is suitable for layout.
     */
    protected fun validateGraph(graph: Graph): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Check for empty graph
        if (graph.nodes.isEmpty()) {
            errors.add("Cannot layout empty graph")
        }
        
        // Validate that all edges reference existing nodes
        val allNodes = graph.getAllNodes()
        for (edge in graph.getAllEdges()) {
            if (!allNodes.contains(edge.source)) {
                errors.add("Edge references non-existent source node: ${edge.source.id}")
            }
            if (!allNodes.contains(edge.target)) {
                errors.add("Edge references non-existent target node: ${edge.target.id}")
            }
        }
        
        // Check for duplicate node IDs
        val nodeIds = allNodes.map { it.id }
        val duplicateIds = nodeIds.groupBy { it }.filter { it.value.size > 1 }.keys
        if (duplicateIds.isNotEmpty()) {
            errors.add("Duplicate node IDs found: ${duplicateIds.joinToString(", ")}")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Calculate node sizes for all nodes in the graph.
     */
    protected fun calculateNodeSizes(graph: Graph, options: LayoutOptions): Map<Node, NodeSize> {
        return graph.getAllNodes().associateWith { node ->
            LayoutUtils.calculateNodeSize(node, options)
        }
    }
    
    /**
     * Create a positioned version of a node with the given coordinates.
     */
    protected fun positionNode(node: Node, position: Point): Node {
        return node.withPosition(position)
    }
    
    /**
     * Create a positioned version of an edge with the given control points.
     */
    protected fun positionEdge(edge: Edge, controlPoints: List<Point>): Edge {
        return edge.withControlPoints(controlPoints)
    }
    
    /**
     * Apply positions to all nodes and edges in a graph, creating a new positioned graph.
     */
    protected fun applyPositions(
        graph: Graph,
        nodePositions: Map<Node, Point>,
        edgeControlPoints: Map<Edge, List<Point>>
    ): Graph {
        // Position nodes and create a mapping from old to new nodes
        val nodeMapping = mutableMapOf<Node, Node>()
        val positionedNodes = graph.nodes.map { node ->
            val position = nodePositions[node]
            val positionedNode = if (position != null) {
                positionNode(node, position)
            } else {
                node
            }
            nodeMapping[node] = positionedNode
            positionedNode
        }.toSet()
        
        // Position edges and update node references
        val positionedEdges = graph.edges.map { edge ->
            val controlPoints = edgeControlPoints[edge]
            val newSource = nodeMapping[edge.source] ?: edge.source
            val newTarget = nodeMapping[edge.target] ?: edge.target
            
            // Create edge with updated node references
            val updatedEdge = EdgeImpl(
                source = newSource,
                target = newTarget,
                attributes = edge.attributes,
                controlPoints = controlPoints ?: edge.controlPoints
            )
            updatedEdge
        }.toSet()
        
        // Recursively position subgraphs
        val positionedSubgraphs = graph.subgraphs.map { subgraph ->
            applyPositions(subgraph, nodePositions, edgeControlPoints)
        }.toSet()
        
        return GraphImpl(
            id = graph.id,
            isDirected = graph.isDirected,
            nodes = positionedNodes,
            edges = positionedEdges,
            subgraphs = positionedSubgraphs,
            attributes = graph.attributes
        )
    }
    
    /**
     * Handle layout errors by creating appropriate error results.
     */
    protected fun handleError(
        message: String,
        cause: Throwable? = null,
        partialGraph: Graph? = null
    ): LayoutResult.Error {
        return LayoutResult.Error(message, cause, partialGraph)
    }
    
    /**
     * Create a successful layout result.
     */
    protected fun success(graph: Graph): LayoutResult.Success {
        return LayoutResult.Success(graph)
    }
    
    /**
     * Perform common pre-layout validation and setup.
     */
    protected fun preLayout(graph: Graph, options: LayoutOptions): PreLayoutResult {
        // Validate the graph
        val validation = validateGraph(graph)
        if (!validation.isValid) {
            val errors = (validation as ValidationResult.Invalid).errors
            return PreLayoutResult.Error("Graph validation failed: ${errors.joinToString("; ")}")
        }
        
        // Calculate node sizes
        val nodeSizes = calculateNodeSizes(graph, options)
        
        return PreLayoutResult.Success(nodeSizes)
    }
    
    /**
     * Result of pre-layout validation and setup.
     */
    sealed class PreLayoutResult {
        data class Success(val nodeSizes: Map<Node, NodeSize>) : PreLayoutResult()
        data class Error(val message: String) : PreLayoutResult()
    }
}

/**
 * Registry for layout engines.
 */
object LayoutEngineRegistry {
    private val engines = mutableMapOf<String, LayoutEngine>()
    
    /**
     * Register a layout engine.
     */
    fun register(engine: LayoutEngine) {
        engines[engine.name] = engine
    }
    
    /**
     * Get a layout engine by name.
     */
    fun get(name: String): LayoutEngine? {
        return engines[name]
    }
    
    /**
     * Get all registered layout engines.
     */
    fun getAll(): Map<String, LayoutEngine> {
        return engines.toMap()
    }
    
    /**
     * Check if a layout engine is registered.
     */
    fun isRegistered(name: String): Boolean {
        return engines.containsKey(name)
    }
    
    /**
     * Unregister a layout engine.
     */
    fun unregister(name: String): LayoutEngine? {
        return engines.remove(name)
    }
    
    /**
     * Clear all registered engines.
     */
    fun clear() {
        engines.clear()
    }
}