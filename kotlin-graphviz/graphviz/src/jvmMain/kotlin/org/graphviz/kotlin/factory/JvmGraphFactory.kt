package org.graphviz.kotlin.factory

import org.graphviz.kotlin.model.*
import org.graphviz.kotlin.collections.JvmOptimizedGraphImpl
import org.graphviz.kotlin.memory.JvmMemoryManager
import org.graphviz.kotlin.memory.MemoryRecommendation
import org.graphviz.kotlin.performance.JvmPerformanceMonitor
import org.graphviz.kotlin.Platform

/**
 * JVM-specific factory for creating optimized graph instances.
 * Automatically selects the best implementation based on graph size and memory conditions.
 */
object JvmGraphFactory {
    
    /**
     * Create an optimized graph instance based on expected size and current memory conditions
     */
    fun createGraph(
        id: String,
        isDirected: Boolean = true,
        estimatedNodes: Int = 1000,
        estimatedEdges: Int = 1000,
        attributes: AttributeMap = AttributeMap.empty()
    ): Graph {
        return JvmPerformanceMonitor.monitorOperation("graph_creation") {
            val recommendation = JvmMemoryManager.getMemoryRecommendation(estimatedNodes, estimatedEdges)
            
            when (recommendation) {
                MemoryRecommendation.INSUFFICIENT_MEMORY -> {
                    throw OutOfMemoryError("Insufficient memory to create graph with $estimatedNodes nodes and $estimatedEdges edges")
                }
                MemoryRecommendation.OPTIMIZE_BEFORE_PROCESSING -> {
                    // Optimize memory before creating the graph
                    JvmMemoryManager.optimizeMemory()
                    createOptimizedGraphImpl(id, isDirected, estimatedNodes, estimatedEdges, attributes)
                }
                MemoryRecommendation.USE_STREAMING_PROCESSING -> {
                    // For very large graphs, use the most optimized implementation
                    createOptimizedGraphImpl(id, isDirected, estimatedNodes, estimatedEdges, attributes)
                }
                else -> {
                    // Use appropriate implementation based on size
                    if (estimatedNodes > 5000 || estimatedEdges > 10000) {
                        createOptimizedGraphImpl(id, isDirected, estimatedNodes, estimatedEdges, attributes)
                    } else {
                        createStandardGraphImpl(id, isDirected, attributes)
                    }
                }
            }
        }.result
    }
    
    /**
     * Create a graph from existing nodes and edges with automatic optimization
     */
    fun createGraphFromElements(
        id: String,
        isDirected: Boolean = true,
        nodes: Set<Node>,
        edges: Set<Edge>,
        subgraphs: Set<Graph> = emptySet(),
        attributes: AttributeMap = AttributeMap.empty()
    ): Graph {
        return JvmPerformanceMonitor.monitorOperation("graph_creation_from_elements") {
            val nodeCount = nodes.size + subgraphs.sumOf { it.getAllNodes().size }
            val edgeCount = edges.size + subgraphs.sumOf { it.getAllEdges().size }
            
            val recommendation = JvmMemoryManager.getMemoryRecommendation(nodeCount, edgeCount)
            
            when (recommendation) {
                MemoryRecommendation.INSUFFICIENT_MEMORY -> {
                    throw OutOfMemoryError("Insufficient memory to create graph with $nodeCount nodes and $edgeCount edges")
                }
                MemoryRecommendation.OPTIMIZE_BEFORE_PROCESSING -> {
                    JvmMemoryManager.optimizeMemory()
                    JvmOptimizedGraphImpl(id, isDirected, nodes, edges, subgraphs, attributes)
                }
                else -> {
                    if (nodeCount > 5000 || edgeCount > 10000) {
                        JvmOptimizedGraphImpl(id, isDirected, nodes, edges, subgraphs, attributes)
                    } else {
                        GraphImpl(id, isDirected, nodes, edges, subgraphs, attributes)
                    }
                }
            }.also { graph ->
                JvmMemoryManager.registerGraph(graph)
            }
        }.result
    }
    
    /**
     * Create a node with JVM-specific optimizations
     */
    fun createNode(
        id: String,
        attributes: AttributeMap = AttributeMap.empty(),
        position: Point? = null
    ): Node {
        require(id.isNotBlank()) { "Node ID cannot be blank" }
        return NodeImpl(id, attributes, position)
    }
    
    /**
     * Create an edge with JVM-specific optimizations
     */
    fun createEdge(
        source: Node,
        target: Node,
        attributes: AttributeMap = AttributeMap.empty(),
        controlPoints: List<Point> = emptyList()
    ): Edge {
        return EdgeImpl(source, target, attributes, controlPoints)
    }
    
    /**
     * Create a builder for constructing graphs incrementally with memory monitoring
     */
    fun createBuilder(
        id: String,
        isDirected: Boolean = true,
        estimatedNodes: Int = 1000,
        estimatedEdges: Int = 1000
    ): JvmOptimizedGraphBuilder {
        return JvmOptimizedGraphBuilder(id, isDirected, estimatedNodes, estimatedEdges)
    }
    
    /**
     * Clone a graph with potential optimization based on current memory conditions
     */
    fun cloneGraph(original: Graph, newId: String = original.id): Graph {
        return JvmPerformanceMonitor.monitorOperation("graph_cloning") {
            val nodeCount = original.getAllNodes().size
            val edgeCount = original.getAllEdges().size
            
            createGraphFromElements(
                id = newId,
                isDirected = original.isDirected,
                nodes = original.nodes,
                edges = original.edges,
                subgraphs = original.subgraphs,
                attributes = original.attributes
            )
        }.result
    }
    
    private fun createOptimizedGraphImpl(
        id: String,
        isDirected: Boolean,
        estimatedNodes: Int,
        estimatedEdges: Int,
        attributes: AttributeMap
    ): Graph {
        return JvmOptimizedGraphImpl(
            id = id,
            isDirected = isDirected,
            attributes = attributes
        ).also { graph ->
            JvmMemoryManager.registerGraph(graph)
        }
    }
    
    private fun createStandardGraphImpl(
        id: String,
        isDirected: Boolean,
        attributes: AttributeMap
    ): Graph {
        return GraphImpl(
            id = id,
            isDirected = isDirected,
            nodes = emptySet(),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = attributes
        ).also { graph ->
            JvmMemoryManager.registerGraph(graph)
        }
    }
}

/**
 * JVM-optimized graph builder with memory monitoring and performance tracking
 */
class JvmOptimizedGraphBuilder(
    private val id: String,
    private val isDirected: Boolean,
    private val estimatedNodes: Int,
    private val estimatedEdges: Int
) {
    private val nodes = mutableSetOf<Node>()
    private val edges = mutableSetOf<Edge>()
    private val subgraphs = mutableSetOf<Graph>()
    private var attributes = AttributeMap.empty()
    
    /**
     * Add a node to the graph
     */
    fun addNode(node: Node): JvmOptimizedGraphBuilder {
        nodes.add(node)
        checkMemoryUsage()
        return this
    }
    
    /**
     * Add multiple nodes to the graph
     */
    fun addNodes(nodesToAdd: Collection<Node>): JvmOptimizedGraphBuilder {
        nodes.addAll(nodesToAdd)
        checkMemoryUsage()
        return this
    }
    
    /**
     * Add an edge to the graph
     */
    fun addEdge(edge: Edge): JvmOptimizedGraphBuilder {
        edges.add(edge)
        checkMemoryUsage()
        return this
    }
    
    /**
     * Add multiple edges to the graph
     */
    fun addEdges(edgesToAdd: Collection<Edge>): JvmOptimizedGraphBuilder {
        edges.addAll(edgesToAdd)
        checkMemoryUsage()
        return this
    }
    
    /**
     * Add a subgraph to the graph
     */
    fun addSubgraph(subgraph: Graph): JvmOptimizedGraphBuilder {
        subgraphs.add(subgraph)
        checkMemoryUsage()
        return this
    }
    
    /**
     * Set graph attributes
     */
    fun setAttributes(newAttributes: AttributeMap): JvmOptimizedGraphBuilder {
        attributes = newAttributes
        return this
    }
    
    /**
     * Add a single attribute
     */
    fun <T> setAttribute(key: AttributeKey<T>, value: T): JvmOptimizedGraphBuilder {
        attributes = attributes.set(key, value)
        return this
    }
    
    /**
     * Build the final graph with optimal implementation selection
     */
    fun build(): Graph {
        return JvmPerformanceMonitor.monitorOperation("graph_building") {
            val totalNodes = nodes.size + subgraphs.sumOf { it.getAllNodes().size }
            val totalEdges = edges.size + subgraphs.sumOf { it.getAllEdges().size }
            
            JvmGraphFactory.createGraphFromElements(
                id = id,
                isDirected = isDirected,
                nodes = nodes.toSet(),
                edges = edges.toSet(),
                subgraphs = subgraphs.toSet(),
                attributes = attributes
            )
        }.result
    }
    
    /**
     * Get current build statistics
     */
    fun getBuildStatistics(): BuildStatistics {
        val totalNodes = nodes.size + subgraphs.sumOf { it.getAllNodes().size }
        val totalEdges = edges.size + subgraphs.sumOf { it.getAllEdges().size }
        val memoryInfo = Platform.getMemoryInfo()
        
        return BuildStatistics(
            currentNodes = nodes.size,
            currentEdges = edges.size,
            currentSubgraphs = subgraphs.size,
            totalNodes = totalNodes,
            totalEdges = totalEdges,
            estimatedMemoryUsageKB = (totalNodes * 200L + totalEdges * 300L) / 1024,
            currentHeapUtilization = memoryInfo.heapUtilization,
            memoryRecommendation = JvmMemoryManager.getMemoryRecommendation(totalNodes, totalEdges)
        )
    }
    
    private fun checkMemoryUsage() {
        val totalNodes = nodes.size + subgraphs.sumOf { it.getAllNodes().size }
        val totalEdges = edges.size + subgraphs.sumOf { it.getAllEdges().size }
        
        if (!JvmMemoryManager.isGraphSizeAcceptable(totalNodes, totalEdges)) {
            val recommendation = JvmMemoryManager.getMemoryRecommendation(totalNodes, totalEdges)
            when (recommendation) {
                MemoryRecommendation.INSUFFICIENT_MEMORY -> {
                    throw OutOfMemoryError("Graph builder has exceeded memory limits: $totalNodes nodes, $totalEdges edges")
                }
                MemoryRecommendation.OPTIMIZE_BEFORE_PROCESSING -> {
                    // Trigger memory optimization
                    JvmMemoryManager.optimizeMemory()
                }
                else -> {
                    // Continue building but monitor closely
                }
            }
        }
    }
}

/**
 * Statistics for graph building process
 */
data class BuildStatistics(
    val currentNodes: Int,
    val currentEdges: Int,
    val currentSubgraphs: Int,
    val totalNodes: Int,
    val totalEdges: Int,
    val estimatedMemoryUsageKB: Long,
    val currentHeapUtilization: Double,
    val memoryRecommendation: MemoryRecommendation
) {
    override fun toString(): String {
        return "BuildStatistics(nodes=$currentNodes+${totalNodes-currentNodes}, " +
               "edges=$currentEdges+${totalEdges-currentEdges}, " +
               "subgraphs=$currentSubgraphs, " +
               "memory=${estimatedMemoryUsageKB}KB, " +
               "heap=${String.format("%.1f", currentHeapUtilization * 100)}%, " +
               "recommendation=$memoryRecommendation)"
    }
}