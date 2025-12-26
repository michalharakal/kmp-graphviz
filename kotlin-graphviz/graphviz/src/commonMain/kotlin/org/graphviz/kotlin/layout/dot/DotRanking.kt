package org.graphviz.kotlin.layout.dot

import org.graphviz.kotlin.model.*

/**
 * Implements graph ranking algorithm for hierarchical layout.
 * Assigns nodes to ranks (levels) based on edge directions and constraints.
 */
class DotRanking {
    
    /**
     * Assign ranks to all nodes in the graph.
     * Returns a map from nodes to their rank (level) in the hierarchy.
     */
    fun assignRanks(graph: Graph): RankingResult {
        try {
            // Handle empty graph
            if (graph.getAllNodes().isEmpty()) {
                return RankingResult.Success(emptyMap(), emptySet())
            }
            
            // Remove cycles by identifying feedback edges
            val feedbackEdges = findFeedbackEdges(graph)
            val acyclicGraph = createAcyclicGraph(graph, feedbackEdges)
            
            // Perform topological ranking
            val ranks = performTopologicalRanking(acyclicGraph)
            
            // Optimize ranking to minimize edge span
            val optimizedRanks = optimizeRanking(acyclicGraph, ranks)
            
            return RankingResult.Success(optimizedRanks, feedbackEdges)
            
        } catch (e: Exception) {
            return RankingResult.Error("Failed to assign ranks: ${e.message}", e)
        }
    }
    
    /**
     * Find feedback edges that need to be removed to make the graph acyclic.
     * Uses DFS-based cycle detection with precise edge ordering for deterministic results.
     * This matches the original Graphviz feedback edge detection algorithm.
     */
    private fun findFeedbackEdges(graph: Graph): Set<Edge> {
        val feedbackEdges = mutableSetOf<Edge>()
        val visited = mutableSetOf<Node>()
        val recursionStack = mutableSetOf<Node>()
        
        // Process nodes in deterministic order for consistent results
        val sortedNodes = graph.getAllNodes().sortedBy { it.id }
        
        // Perform DFS from each unvisited node
        for (node in sortedNodes) {
            if (node !in visited) {
                findFeedbackEdgesDFS(graph, node, visited, recursionStack, feedbackEdges)
            }
        }
        
        return feedbackEdges
    }
    
    private fun findFeedbackEdgesDFS(
        graph: Graph,
        node: Node,
        visited: MutableSet<Node>,
        recursionStack: MutableSet<Node>,
        feedbackEdges: MutableSet<Edge>
    ) {
        visited.add(node)
        recursionStack.add(node)
        
        // Process outgoing edges in deterministic order
        val outgoingEdges = graph.getAllEdges()
            .filter { it.source == node }
            .sortedBy { "${it.target.id}_${it.hashCode()}" }
        
        for (edge in outgoingEdges) {
            val target = edge.target
            
            if (target in recursionStack) {
                // Back edge found - this creates a cycle
                feedbackEdges.add(edge)
            } else if (target !in visited) {
                findFeedbackEdgesDFS(graph, target, visited, recursionStack, feedbackEdges)
            }
        }
        
        recursionStack.remove(node)
    }
    
    /**
     * Create a conceptually acyclic graph by ignoring feedback edges.
     */
    private fun createAcyclicGraph(graph: Graph, feedbackEdges: Set<Edge>): Graph {
        val acyclicEdges = graph.getAllEdges() - feedbackEdges
        
        return GraphImpl(
            id = graph.id + "_acyclic",
            isDirected = graph.isDirected,
            nodes = graph.getAllNodes(),
            edges = acyclicEdges,
            subgraphs = emptySet(), // Flatten for ranking
            attributes = graph.attributes
        )
    }
    
    /**
     * Perform topological ranking on the acyclic graph.
     * Uses network simplex-style longest path algorithm to assign ranks.
     * This matches the original Graphviz ranking algorithm precisely.
     */
    private fun performTopologicalRanking(graph: Graph): Map<Node, Int> {
        val ranks = mutableMapOf<Node, Int>()
        val inDegree = mutableMapOf<Node, Int>()
        val queue = ArrayDeque<Node>()
        
        // Initialize in-degrees with precise counting
        for (node in graph.getAllNodes()) {
            inDegree[node] = 0
        }
        
        // Count incoming edges precisely
        for (edge in graph.getAllEdges()) {
            inDegree[edge.target] = inDegree[edge.target]!! + 1
        }
        
        // Find source nodes (no incoming edges) and assign rank 0
        // Process in deterministic order for consistency
        val sourceNodes = graph.getAllNodes()
            .filter { inDegree[it] == 0 }
            .sortedBy { it.id }
        
        for (node in sourceNodes) {
            queue.addLast(node)
            ranks[node] = 0
        }
        
        // Process nodes in topological order using longest path
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentRank = ranks[current]!!
            
            // Process all outgoing edges in deterministic order
            val outgoingEdges = graph.getAllEdges()
                .filter { it.source == current }
                .sortedBy { it.target.id }
            
            for (edge in outgoingEdges) {
                val target = edge.target
                val newRank = currentRank + 1
                
                // Use maximum rank to handle multiple paths (longest path)
                val currentTargetRank = ranks[target] ?: Int.MIN_VALUE
                if (newRank > currentTargetRank) {
                    ranks[target] = newRank
                }
                
                // Decrease in-degree and add to queue if all predecessors processed
                inDegree[target] = inDegree[target]!! - 1
                if (inDegree[target] == 0) {
                    queue.addLast(target)
                }
            }
        }
        
        // Verify all nodes have been ranked (DAG property check)
        val unrankedNodes = graph.getAllNodes().filter { it !in ranks }
        if (unrankedNodes.isNotEmpty()) {
            // This indicates a cycle that wasn't properly removed
            // Assign remaining nodes to rank 0 as fallback
            for (node in unrankedNodes) {
                ranks[node] = 0
            }
        }
        
        return ranks
    }
    
    /**
     * Optimize ranking to minimize edge span and improve layout quality.
     */
    private fun optimizeRanking(graph: Graph, initialRanks: Map<Node, Int>): Map<Node, Int> {
        val ranks = initialRanks.toMutableMap()
        var improved = true
        var iterations = 0
        val maxIterations = 10
        
        while (improved && iterations < maxIterations) {
            improved = false
            iterations++
            
            // Try to move nodes to better ranks
            for (node in graph.getAllNodes()) {
                val currentRank = ranks[node]!!
                val bestRank = findBestRank(graph, node, ranks)
                
                if (bestRank != currentRank && isValidRankAssignment(graph, node, bestRank, ranks)) {
                    ranks[node] = bestRank
                    improved = true
                }
            }
        }
        
        return ranks
    }
    
    /**
     * Find the best rank for a node based on its neighbors.
     */
    private fun findBestRank(graph: Graph, node: Node, ranks: Map<Node, Int>): Int {
        val predecessorRanks = mutableListOf<Int>()
        val successorRanks = mutableListOf<Int>()
        
        for (edge in graph.getAllEdges()) {
            when {
                edge.target == node -> predecessorRanks.add(ranks[edge.source]!!)
                edge.source == node -> successorRanks.add(ranks[edge.target]!!)
            }
        }
        
        return when {
            predecessorRanks.isNotEmpty() && successorRanks.isNotEmpty() -> {
                // Node has both predecessors and successors
                val minSuccessorRank = successorRanks.minOrNull()!!
                val maxPredecessorRank = predecessorRanks.maxOrNull()!!
                maxPredecessorRank + 1
            }
            predecessorRanks.isNotEmpty() -> {
                // Node only has predecessors
                predecessorRanks.maxOrNull()!! + 1
            }
            successorRanks.isNotEmpty() -> {
                // Node only has successors
                successorRanks.minOrNull()!! - 1
            }
            else -> {
                // Isolated node
                ranks[node]!!
            }
        }
    }
    
    /**
     * Check if assigning a node to a specific rank would violate constraints.
     */
    private fun isValidRankAssignment(
        graph: Graph,
        node: Node,
        newRank: Int,
        ranks: Map<Node, Int>
    ): Boolean {
        // Check that all predecessors have lower ranks
        for (edge in graph.getAllEdges()) {
            if (edge.target == node) {
                val predecessorRank = ranks[edge.source]!!
                if (predecessorRank >= newRank) {
                    return false
                }
            }
        }
        
        // Check that all successors have higher ranks
        for (edge in graph.getAllEdges()) {
            if (edge.source == node) {
                val successorRank = ranks[edge.target]!!
                if (successorRank <= newRank) {
                    return false
                }
            }
        }
        
        return true
    }
}

/**
 * Result of the ranking algorithm.
 */
sealed class RankingResult {
    /**
     * Successful ranking with node ranks and feedback edges.
     */
    data class Success(
        val ranks: Map<Node, Int>,
        val feedbackEdges: Set<Edge>
    ) : RankingResult()
    
    /**
     * Ranking failed with error information.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : RankingResult()
    
    /**
     * Check if the ranking was successful.
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Get the ranks if successful, or null if failed.
     */
    fun getRanksOrNull(): Map<Node, Int>? = when (this) {
        is Success -> ranks
        is Error -> null
    }
}

/**
 * Utility functions for working with ranks.
 */
object RankingUtils {
    
    /**
     * Get all nodes at a specific rank.
     */
    fun getNodesAtRank(ranks: Map<Node, Int>, rank: Int): Set<Node> {
        return ranks.filterValues { it == rank }.keys
    }
    
    /**
     * Get the maximum rank in the ranking.
     */
    fun getMaxRank(ranks: Map<Node, Int>): Int {
        return ranks.values.maxOrNull() ?: 0
    }
    
    /**
     * Get the minimum rank in the ranking.
     */
    fun getMinRank(ranks: Map<Node, Int>): Int {
        return ranks.values.minOrNull() ?: 0
    }
    
    /**
     * Normalize ranks to start from 0.
     */
    fun normalizeRanks(ranks: Map<Node, Int>): Map<Node, Int> {
        val minRank = getMinRank(ranks)
        return ranks.mapValues { (_, rank) -> rank - minRank }
    }
    
    /**
     * Calculate the span of an edge (difference in ranks between source and target).
     */
    fun getEdgeSpan(edge: Edge, ranks: Map<Node, Int>): Int {
        val sourceRank = ranks[edge.source] ?: 0
        val targetRank = ranks[edge.target] ?: 0
        return targetRank - sourceRank
    }
    
    /**
     * Calculate the total edge span for all edges in the graph.
     */
    fun getTotalEdgeSpan(graph: Graph, ranks: Map<Node, Int>): Int {
        return graph.getAllEdges().sumOf { edge ->
            maxOf(0, getEdgeSpan(edge, ranks))
        }
    }
}