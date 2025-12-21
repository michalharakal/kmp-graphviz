package org.graphviz.kotlin.collections

import org.graphviz.kotlin.model.Node
import org.graphviz.kotlin.model.Edge
import org.graphviz.kotlin.Platform
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashSet

/**
 * JVM-optimized collections for handling large graphs efficiently.
 * These collections are designed to minimize memory overhead and provide
 * better performance characteristics for graph operations.
 */
object JvmOptimizedCollections {
    
    /**
     * Create an optimized node set for JVM with memory-efficient storage
     */
    fun createOptimizedNodeSet(estimatedSize: Int = 1000): MutableSet<Node> {
        val optimalSize = Platform.getOptimalCollectionSize(estimatedSize * 200) // Estimate 200 bytes per node
        
        return when {
            optimalSize > 10000 -> {
                // For very large graphs, use concurrent collections
                ConcurrentSkipListSet<Node> { a, b -> a.id.compareTo(b.id) }
            }
            optimalSize > 1000 -> {
                // For medium graphs, use LinkedHashSet with optimal initial capacity
                LinkedHashSet<Node>(optimalSize)
            }
            else -> {
                // For small graphs, use standard HashSet
                HashSet<Node>(optimalSize)
            }
        }
    }
    
    /**
     * Create an optimized edge set for JVM with memory-efficient storage
     */
    fun createOptimizedEdgeSet(estimatedSize: Int = 1000): MutableSet<Edge> {
        val optimalSize = Platform.getOptimalCollectionSize(estimatedSize * 300) // Estimate 300 bytes per edge
        
        return when {
            optimalSize > 10000 -> {
                // For very large graphs, use concurrent collections
                ConcurrentSkipListSet<Edge> { a, b -> 
                    val sourceCompare = a.source.id.compareTo(b.source.id)
                    if (sourceCompare != 0) sourceCompare else a.target.id.compareTo(b.target.id)
                }
            }
            optimalSize > 1000 -> {
                // For medium graphs, use LinkedHashSet with optimal initial capacity
                LinkedHashSet<Edge>(optimalSize)
            }
            else -> {
                // For small graphs, use standard HashSet
                HashSet<Edge>(optimalSize)
            }
        }
    }
    
    /**
     * Create an optimized node lookup map for fast ID-based access
     */
    fun createOptimizedNodeMap(estimatedSize: Int = 1000): MutableMap<String, Node> {
        val optimalSize = Platform.getOptimalCollectionSize(estimatedSize * 250) // Estimate 250 bytes per entry
        
        return when {
            optimalSize > 10000 -> {
                // For very large graphs, use concurrent map
                ConcurrentHashMap<String, Node>(optimalSize)
            }
            else -> {
                // For smaller graphs, use standard HashMap with optimal capacity
                HashMap<String, Node>(optimalSize)
            }
        }
    }
    
    /**
     * Create an optimized adjacency list for edge lookups
     */
    fun createOptimizedAdjacencyMap(estimatedSize: Int = 1000): MutableMap<String, MutableSet<Edge>> {
        val optimalSize = Platform.getOptimalCollectionSize(estimatedSize * 400) // Estimate 400 bytes per entry
        
        return when {
            optimalSize > 10000 -> {
                // For very large graphs, use concurrent map
                ConcurrentHashMap<String, MutableSet<Edge>>(optimalSize)
            }
            else -> {
                // For smaller graphs, use standard HashMap
                HashMap<String, MutableSet<Edge>>(optimalSize)
            }
        }
    }
}

/**
 * JVM-optimized graph implementation that uses efficient collections
 * and memory management strategies for large graphs.
 */
class JvmOptimizedGraphImpl(
    override val id: String,
    override val isDirected: Boolean,
    initialNodes: Set<Node> = emptySet(),
    initialEdges: Set<Edge> = emptySet(),
    initialSubgraphs: Set<org.graphviz.kotlin.model.Graph> = emptySet(),
    override val attributes: org.graphviz.kotlin.model.AttributeMap = org.graphviz.kotlin.model.AttributeMap.empty()
) : org.graphviz.kotlin.model.Graph {
    
    // Use optimized collections for better performance
    private val _nodes = JvmOptimizedCollections.createOptimizedNodeSet(initialNodes.size)
    private val _edges = JvmOptimizedCollections.createOptimizedEdgeSet(initialEdges.size)
    private val _subgraphs = JvmOptimizedCollections.createOptimizedNodeSet(initialSubgraphs.size) as MutableSet<org.graphviz.kotlin.model.Graph>
    
    // Optimized lookup structures
    private val nodeMap = JvmOptimizedCollections.createOptimizedNodeMap(initialNodes.size)
    private val adjacencyMap = JvmOptimizedCollections.createOptimizedAdjacencyMap(initialNodes.size)
    
    // Statistics for monitoring
    private val nodeCount = AtomicInteger(0)
    private val edgeCount = AtomicInteger(0)
    
    init {
        // Initialize with provided data
        initialNodes.forEach { addNodeInternal(it) }
        initialEdges.forEach { addEdgeInternal(it) }
        _subgraphs.addAll(initialSubgraphs)
    }
    
    override val nodes: Set<Node> get() = _nodes.toSet()
    override val edges: Set<Edge> get() = _edges.toSet()
    override val subgraphs: Set<org.graphviz.kotlin.model.Graph> get() = _subgraphs.toSet()
    
    private fun addNodeInternal(node: Node) {
        if (_nodes.add(node)) {
            nodeMap[node.id] = node
            nodeCount.incrementAndGet()
        }
    }
    
    private fun addEdgeInternal(edge: Edge) {
        if (_edges.add(edge)) {
            // Update adjacency map for fast edge lookups
            adjacencyMap.computeIfAbsent(edge.source.id) { 
                JvmOptimizedCollections.createOptimizedEdgeSet(10) 
            }.add(edge)
            
            if (!isDirected) {
                adjacencyMap.computeIfAbsent(edge.target.id) { 
                    JvmOptimizedCollections.createOptimizedEdgeSet(10) 
                }.add(edge)
            }
            
            edgeCount.incrementAndGet()
        }
    }
    
    override fun getNode(id: String): Node? = nodeMap[id]
    
    override fun getEdges(node: Node): Set<Edge> {
        return adjacencyMap[node.id]?.toSet() ?: emptySet()
    }
    
    override fun getEdges(source: Node, target: Node): Set<Edge> {
        val sourceEdges = adjacencyMap[source.id] ?: return emptySet()
        return sourceEdges.filter { edge ->
            (edge.source == source && edge.target == target) ||
            (!isDirected && edge.source == target && edge.target == source)
        }.toSet()
    }
    
    override fun containsNode(node: Node): Boolean = _nodes.contains(node)
    
    override fun containsEdge(edge: Edge): Boolean = _edges.contains(edge)
    
    override fun getAllNodes(): Set<Node> {
        return _nodes + _subgraphs.flatMap { it.getAllNodes() }
    }
    
    override fun getAllEdges(): Set<Edge> {
        return _edges + _subgraphs.flatMap { it.getAllEdges() }
    }
    
    override fun getBoundingBox(): org.graphviz.kotlin.model.BoundingBox? {
        val positionedNodes = getAllNodes().mapNotNull { it.position }
        return if (positionedNodes.isNotEmpty()) {
            org.graphviz.kotlin.model.BoundingBox.fromPoints(positionedNodes)
        } else {
            null
        }
    }
    
    /**
     * Get memory usage statistics for this graph
     */
    fun getMemoryStats(): GraphMemoryStats {
        val memoryInfo = Platform.getMemoryInfo()
        return GraphMemoryStats(
            nodeCount = nodeCount.get(),
            edgeCount = edgeCount.get(),
            subgraphCount = _subgraphs.size,
            estimatedMemoryUsageKB = estimateMemoryUsage() / 1024,
            heapUtilization = memoryInfo.heapUtilization,
            isMemoryPressure = memoryInfo.isMemoryPressure
        )
    }
    
    /**
     * Estimate memory usage of this graph in bytes
     */
    private fun estimateMemoryUsage(): Long {
        val nodeMemory = nodeCount.get() * 200L // Estimate 200 bytes per node
        val edgeMemory = edgeCount.get() * 300L // Estimate 300 bytes per edge
        val mapOverhead = (nodeMap.size + adjacencyMap.size) * 50L // Estimate 50 bytes overhead per map entry
        return nodeMemory + edgeMemory + mapOverhead
    }
    
    /**
     * Optimize memory usage by compacting internal structures
     */
    fun optimizeMemory() {
        Platform.withPerformanceMonitoring("graph_memory_optimization") {
            // Remove empty adjacency lists
            adjacencyMap.entries.removeIf { it.value.isEmpty() }
            
            // Suggest GC if memory pressure is high
            val memoryInfo = Platform.getMemoryInfo()
            if (memoryInfo.isMemoryPressure) {
                Platform.forceGC()
            }
        }
    }
}

/**
 * Memory statistics for a graph
 */
data class GraphMemoryStats(
    val nodeCount: Int,
    val edgeCount: Int,
    val subgraphCount: Int,
    val estimatedMemoryUsageKB: Long,
    val heapUtilization: Double,
    val isMemoryPressure: Boolean
) {
    override fun toString(): String {
        return "GraphMemoryStats(nodes=$nodeCount, edges=$edgeCount, subgraphs=$subgraphCount, " +
               "memory=${estimatedMemoryUsageKB}KB, heapUtil=${String.format("%.1f", heapUtilization * 100)}%, " +
               "pressure=$isMemoryPressure)"
    }
}