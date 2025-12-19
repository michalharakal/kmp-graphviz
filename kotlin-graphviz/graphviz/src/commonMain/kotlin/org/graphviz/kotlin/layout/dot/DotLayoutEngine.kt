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