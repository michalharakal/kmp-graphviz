package org.graphviz.kotlin.layout.dot

import org.graphviz.kotlin.model.*
import org.graphviz.kotlin.layout.NodeSize

/**
 * Implements node ordering within ranks to minimize edge crossings.
 * Uses heuristic algorithms to arrange nodes optimally within each rank.
 */
class DotOrdering {
    
    /**
     * Order nodes within each rank to minimize edge crossings.
     */
    fun orderNodes(
        graph: Graph,
        ranks: Map<Node, Int>,
        nodeSizes: Map<Node, NodeSize>
    ): OrderingResult {
        try {
            // Group nodes by rank
            val nodesByRank = groupNodesByRank(ranks)
            
            // Initialize ordering with a simple heuristic
            val initialOrdering = initializeOrdering(nodesByRank, graph)
            
            // Iteratively improve ordering to reduce crossings
            val optimizedOrdering = optimizeOrdering(graph, nodesByRank, initialOrdering)
            
            // Apply spacing constraints based on node sizes
            val spacedOrdering = applySpacing(optimizedOrdering, nodeSizes)
            
            return OrderingResult.Success(spacedOrdering)
            
        } catch (e: Exception) {
            return OrderingResult.Error("Failed to order nodes: ${e.message}", e)
        }
    }
    
    /**
     * Group nodes by their rank.
     */
    private fun groupNodesByRank(ranks: Map<Node, Int>): Map<Int, List<Node>> {
        return ranks.entries.groupBy({ it.value }, { it.key })
    }
    
    /**
     * Initialize node ordering using a simple heuristic.
     */
    private fun initializeOrdering(
        nodesByRank: Map<Int, List<Node>>,
        graph: Graph
    ): Map<Int, List<Node>> {
        val ordering = mutableMapOf<Int, List<Node>>()
        
        // Sort ranks to process them in order
        val sortedRanks = nodesByRank.keys.sorted()
        
        for (rank in sortedRanks) {
            val nodes = nodesByRank[rank] ?: emptyList()
            
            if (rank == sortedRanks.first()) {
                // First rank: order by node ID for consistency
                ordering[rank] = nodes.sortedBy { it.id }
            } else {
                // Subsequent ranks: order based on connections to previous rank
                ordering[rank] = orderByConnections(nodes, ordering[rank - 1] ?: emptyList(), graph)
            }
        }
        
        return ordering
    }
    
    /**
     * Order nodes based on their connections to the previous rank.
     */
    private fun orderByConnections(
        nodes: List<Node>,
        previousRankNodes: List<Node>,
        graph: Graph
    ): List<Node> {
        // Calculate the average position of predecessors for each node
        val nodePositions = nodes.map { node ->
            val predecessorPositions = mutableListOf<Int>()
            
            for (edge in graph.getAllEdges()) {
                if (edge.target == node && edge.source in previousRankNodes) {
                    val position = previousRankNodes.indexOf(edge.source)
                    if (position >= 0) {
                        predecessorPositions.add(position)
                    }
                }
            }
            
            val averagePosition = if (predecessorPositions.isNotEmpty()) {
                predecessorPositions.average()
            } else {
                Double.MAX_VALUE // Nodes with no predecessors go to the end
            }
            
            node to averagePosition
        }
        
        // Sort by average predecessor position
        return nodePositions.sortedBy { it.second }.map { it.first }
    }
    
    /**
     * Optimize node ordering using iterative crossing reduction.
     * Uses the median heuristic with precise tie-breaking for deterministic results.
     * This matches the original Graphviz mincross algorithm behavior.
     */
    private fun optimizeOrdering(
        graph: Graph,
        nodesByRank: Map<Int, List<Node>>,
        initialOrdering: Map<Int, List<Node>>
    ): Map<Int, List<Node>> {
        var currentOrdering = initialOrdering.toMutableMap()
        var improved = true
        var iterations = 0
        val maxIterations = 24 // Match original Graphviz default
        
        while (improved && iterations < maxIterations) {
            improved = false
            iterations++
            
            // Forward pass: optimize based on previous rank (down sweep)
            val sortedRanks = nodesByRank.keys.sorted()
            for (i in 1 until sortedRanks.size) {
                val rank = sortedRanks[i]
                val previousRank = sortedRanks[i - 1]
                
                val newOrdering = optimizeRankOrderingMedian(
                    currentOrdering[rank] ?: emptyList(),
                    currentOrdering[previousRank] ?: emptyList(),
                    graph,
                    forward = true
                )
                
                if (newOrdering != currentOrdering[rank]) {
                    currentOrdering[rank] = newOrdering
                    improved = true
                }
            }
            
            // Backward pass: optimize based on next rank (up sweep)
            for (i in sortedRanks.size - 2 downTo 0) {
                val rank = sortedRanks[i]
                val nextRank = sortedRanks[i + 1]
                
                val newOrdering = optimizeRankOrderingMedian(
                    currentOrdering[rank] ?: emptyList(),
                    currentOrdering[nextRank] ?: emptyList(),
                    graph,
                    forward = false
                )
                
                if (newOrdering != currentOrdering[rank]) {
                    currentOrdering[rank] = newOrdering
                    improved = true
                }
            }
        }
        
        return currentOrdering
    }
    
    /**
     * Optimize the ordering of nodes in a single rank using median heuristic.
     * This implements the precise median calculation used in original Graphviz.
     */
    private fun optimizeRankOrderingMedian(
        rankNodes: List<Node>,
        adjacentRankNodes: List<Node>,
        graph: Graph,
        forward: Boolean
    ): List<Node> {
        if (rankNodes.isEmpty() || adjacentRankNodes.isEmpty()) {
            return rankNodes
        }
        
        // Calculate median positions for each node
        val nodeMedians = rankNodes.map { node ->
            val connections = mutableListOf<Int>()
            
            // Find all connections to adjacent rank
            for (edge in graph.getAllEdges()) {
                val isConnected = if (forward) {
                    edge.target == node && edge.source in adjacentRankNodes
                } else {
                    edge.source == node && edge.target in adjacentRankNodes
                }
                
                if (isConnected) {
                    val adjacentNode = if (forward) edge.source else edge.target
                    val position = adjacentRankNodes.indexOf(adjacentNode)
                    if (position >= 0) {
                        connections.add(position)
                    }
                }
            }
            
            // Calculate median position with precise tie-breaking
            val median = when {
                connections.isEmpty() -> Double.MAX_VALUE // Nodes with no connections go to end
                connections.size == 1 -> connections[0].toDouble()
                connections.size % 2 == 1 -> {
                    // Odd number of connections: use middle value
                    connections.sorted()[connections.size / 2].toDouble()
                }
                else -> {
                    // Even number of connections: use average of two middle values
                    val sorted = connections.sorted()
                    val mid = connections.size / 2
                    (sorted[mid - 1] + sorted[mid]) / 2.0
                }
            }
            
            node to median
        }
        
        // Sort by median value, with stable sort for deterministic results
        // Use node ID as secondary sort key for consistent tie-breaking
        return nodeMedians
            .sortedWith(compareBy<Pair<Node, Double>> { it.second }.thenBy { it.first.id })
            .map { it.first }
    }
    
    /**
     * Apply spacing constraints based on node sizes.
     */
    private fun applySpacing(
        ordering: Map<Int, List<Node>>,
        nodeSizes: Map<Node, NodeSize>
    ): Map<Int, List<NodePosition>> {
        val result = mutableMapOf<Int, List<NodePosition>>()
        
        for ((rank, nodes) in ordering) {
            var currentX = 0.0
            val positions = mutableListOf<NodePosition>()
            
            for (node in nodes) {
                val size = nodeSizes[node] ?: NodeSize(30.0, 20.0)
                
                // Position node at current X + half its width
                val nodeX = currentX + size.width / 2
                positions.add(NodePosition(node, nodeX, size))
                
                // Move to next position with spacing
                currentX += size.width + 20.0 // 20.0 is minimum spacing
            }
            
            result[rank] = positions
        }
        
        return result
    }
    
    /**
     * Count the number of edge crossings in the current ordering.
     */
    fun countCrossings(
        graph: Graph,
        ordering: Map<Int, List<Node>>
    ): Int {
        var crossings = 0
        val sortedRanks = ordering.keys.sorted()
        
        for (i in 0 until sortedRanks.size - 1) {
            val rank1 = sortedRanks[i]
            val rank2 = sortedRanks[i + 1]
            
            val nodes1 = ordering[rank1] ?: emptyList()
            val nodes2 = ordering[rank2] ?: emptyList()
            
            crossings += countCrossingsBetweenRanks(graph, nodes1, nodes2)
        }
        
        return crossings
    }
    
    /**
     * Count crossings between two adjacent ranks.
     */
    private fun countCrossingsBetweenRanks(
        graph: Graph,
        rank1Nodes: List<Node>,
        rank2Nodes: List<Node>
    ): Int {
        val edges = graph.getAllEdges().filter { edge ->
            edge.source in rank1Nodes && edge.target in rank2Nodes
        }
        
        var crossings = 0
        
        for (i in edges.indices) {
            for (j in i + 1 until edges.size) {
                val edge1 = edges[i]
                val edge2 = edges[j]
                
                val pos1Source = rank1Nodes.indexOf(edge1.source)
                val pos1Target = rank2Nodes.indexOf(edge1.target)
                val pos2Source = rank1Nodes.indexOf(edge2.source)
                val pos2Target = rank2Nodes.indexOf(edge2.target)
                
                // Check if edges cross
                if ((pos1Source < pos2Source && pos1Target > pos2Target) ||
                    (pos1Source > pos2Source && pos1Target < pos2Target)) {
                    crossings++
                }
            }
        }
        
        return crossings
    }
}

/**
 * Represents the position of a node within its rank.
 */
data class NodePosition(
    val node: Node,
    val x: Double,
    val size: NodeSize
) {
    val left: Double get() = x - size.width / 2
    val right: Double get() = x + size.width / 2
}

/**
 * Result of the node ordering algorithm.
 */
sealed class OrderingResult {
    /**
     * Successful ordering with positioned nodes by rank.
     */
    data class Success(
        val ordering: Map<Int, List<NodePosition>>
    ) : OrderingResult()
    
    /**
     * Ordering failed with error information.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : OrderingResult()
    
    /**
     * Check if the ordering was successful.
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Get the ordering if successful, or null if failed.
     */
    fun getOrderingOrNull(): Map<Int, List<NodePosition>>? = when (this) {
        is Success -> ordering
        is Error -> null
    }
}

/**
 * Utility functions for working with node orderings.
 */
object OrderingUtils {
    
    /**
     * Get the total width of a rank including spacing.
     */
    fun getRankWidth(positions: List<NodePosition>): Double {
        if (positions.isEmpty()) return 0.0
        
        val leftmost = positions.minOfOrNull { it.left } ?: 0.0
        val rightmost = positions.maxOfOrNull { it.right } ?: 0.0
        
        return rightmost - leftmost
    }
    
    /**
     * Center a rank horizontally around the given X coordinate.
     */
    fun centerRank(positions: List<NodePosition>, centerX: Double): List<NodePosition> {
        if (positions.isEmpty()) return positions
        
        val currentCenter = (positions.minOf { it.left } + positions.maxOf { it.right }) / 2
        val offset = centerX - currentCenter
        
        return positions.map { pos ->
            pos.copy(x = pos.x + offset)
        }
    }
    
    /**
     * Get all nodes in the ordering in rank order.
     */
    fun getAllNodes(ordering: Map<Int, List<NodePosition>>): List<Node> {
        return ordering.keys.sorted().flatMap { rank -> ordering[rank]?.map { it.node } ?: emptyList() }
    }
    
    /**
     * Convert ordering to a simple position map.
     */
    fun toPositionMap(ordering: Map<Int, List<NodePosition>>, rankY: Map<Int, Double>): Map<Node, Point> {
        val positions = mutableMapOf<Node, Point>()
        
        for ((rank, nodePositions) in ordering) {
            val y = rankY[rank] ?: (rank * 100.0)
            
            for (nodePos in nodePositions) {
                positions[nodePos.node] = Point(nodePos.x, y)
            }
        }
        
        return positions
    }
}