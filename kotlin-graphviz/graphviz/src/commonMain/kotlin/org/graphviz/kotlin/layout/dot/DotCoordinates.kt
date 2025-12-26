package org.graphviz.kotlin.layout.dot

import org.graphviz.kotlin.model.*
import org.graphviz.kotlin.layout.LayoutOptions
import org.graphviz.kotlin.layout.LayoutUtils
import org.graphviz.kotlin.layout.NodeSize

/**
 * Implements coordinate assignment for the dot layout algorithm.
 * Calculates final x,y positions for nodes and routes edges with control points.
 */
class DotCoordinates {
    
    /**
     * Assign final coordinates to all nodes and edges.
     */
    fun assignCoordinates(
        graph: Graph,
        ranks: Map<Node, Int>,
        ordering: Map<Int, List<NodePosition>>,
        options: LayoutOptions
    ): CoordinateResult {
        try {
            // Calculate Y coordinates for ranks
            val rankY = calculateRankYCoordinates(ranks, options)
            
            // Convert node ordering to final positions
            val nodePositions = calculateNodePositions(ordering, rankY)
            
            // Route edges with control points
            val edgeControlPoints = routeEdges(graph, nodePositions, ranks, options)
            
            // Apply final adjustments and normalization
            val finalNodePositions = normalizePositions(nodePositions)
            val finalEdgeControlPoints = normalizeEdgeControlPoints(edgeControlPoints, finalNodePositions)
            
            return CoordinateResult.Success(finalNodePositions, finalEdgeControlPoints)
            
        } catch (e: Exception) {
            return CoordinateResult.Error("Failed to assign coordinates: ${e.message}", e)
        }
    }
    
    /**
     * Calculate Y coordinates for each rank.
     */
    private fun calculateRankYCoordinates(
        ranks: Map<Node, Int>,
        options: LayoutOptions
    ): Map<Int, Double> {
        val rankY = mutableMapOf<Int, Double>()
        val sortedRanks = ranks.values.distinct().sorted()
        
        var currentY = 0.0
        for (rank in sortedRanks) {
            rankY[rank] = currentY
            currentY += options.rankSpacing
        }
        
        return rankY
    }
    
    /**
     * Calculate final node positions from ordering and rank Y coordinates.
     */
    private fun calculateNodePositions(
        ordering: Map<Int, List<NodePosition>>,
        rankY: Map<Int, Double>
    ): Map<Node, Point> {
        val positions = mutableMapOf<Node, Point>()
        
        for ((rank, nodePositions) in ordering) {
            val y = rankY[rank] ?: 0.0
            
            for (nodePos in nodePositions) {
                positions[nodePos.node] = Point(nodePos.x, y)
            }
        }
        
        return positions
    }
    
    /**
     * Route edges with appropriate control points.
     */
    private fun routeEdges(
        graph: Graph,
        nodePositions: Map<Node, Point>,
        ranks: Map<Node, Int>,
        options: LayoutOptions
    ): Map<Edge, List<Point>> {
        val edgeControlPoints = mutableMapOf<Edge, List<Point>>()
        
        for (edge in graph.getAllEdges()) {
            val sourcePos = nodePositions[edge.source] ?: Point.ORIGIN
            val targetPos = nodePositions[edge.target] ?: Point.ORIGIN
            val sourceRank = ranks[edge.source] ?: 0
            val targetRank = ranks[edge.target] ?: 0
            
            val controlPoints = when {
                targetRank == sourceRank + 1 -> {
                    // Direct edge between adjacent ranks
                    routeDirectEdge(edge, sourcePos, targetPos)
                }
                targetRank > sourceRank + 1 -> {
                    // Long edge spanning multiple ranks
                    routeLongEdge(edge, sourcePos, targetPos, sourceRank, targetRank, options)
                }
                else -> {
                    // Feedback edge or same-rank edge
                    routeFeedbackEdge(edge, sourcePos, targetPos, options)
                }
            }
            
            edgeControlPoints[edge] = controlPoints
        }
        
        return edgeControlPoints
    }
    
    /**
     * Route a direct edge between adjacent ranks.
     */
    private fun routeDirectEdge(
        edge: Edge,
        sourcePos: Point,
        targetPos: Point
    ): List<Point> {
        // Simple straight line for adjacent ranks
        return listOf(sourcePos, targetPos)
    }
    
    /**
     * Route a long edge that spans multiple ranks with precise intermediate positioning.
     */
    private fun routeLongEdge(
        edge: Edge,
        sourcePos: Point,
        targetPos: Point,
        sourceRank: Int,
        targetRank: Int,
        options: LayoutOptions
    ): List<Point> {
        val controlPoints = mutableListOf<Point>()
        controlPoints.add(sourcePos)
        
        // Calculate precise intermediate points for each spanned rank
        val rankSpacing = options.rankSpacing
        val totalRankSpan = targetRank - sourceRank
        
        for (rank in sourceRank + 1 until targetRank) {
            val y = sourcePos.y + (rank - sourceRank) * rankSpacing
            
            // Use precise linear interpolation for X coordinate
            val t = (rank - sourceRank).toDouble() / totalRankSpan
            val x = sourcePos.x + t * (targetPos.x - sourcePos.x)
            
            // Add small perturbation to avoid exact overlaps with other edges
            val perturbation = (edge.hashCode() % 100) * 0.01
            controlPoints.add(Point(x + perturbation, y))
        }
        
        controlPoints.add(targetPos)
        return controlPoints
    }
    
    /**
     * Route a feedback edge with precise curved path calculation.
     */
    private fun routeFeedbackEdge(
        edge: Edge,
        sourcePos: Point,
        targetPos: Point,
        options: LayoutOptions
    ): List<Point> {
        // Create a curved path that goes around the normal flow
        val midY = (sourcePos.y + targetPos.y) / 2
        val offset = options.rankSpacing * 0.6
        
        // Determine curve direction based on X positions and edge hash for consistency
        val baseDirection = if (sourcePos.x < targetPos.x) -1.0 else 1.0
        val hashPerturbation = (edge.hashCode() % 3 - 1) * 0.1 // -0.1, 0, or 0.1
        val curveDirection = baseDirection + hashPerturbation
        
        val curveX = (sourcePos.x + targetPos.x) / 2 + curveDirection * offset
        
        return listOf(
            sourcePos,
            Point(sourcePos.x, sourcePos.y - offset * 0.7),
            Point(curveX, midY - offset),
            Point(targetPos.x, targetPos.y - offset * 0.7),
            targetPos
        )
    }
    
    /**
     * Normalize node positions to ensure they start from origin and fit well.
     */
    private fun normalizePositions(positions: Map<Node, Point>): Map<Node, Point> {
        if (positions.isEmpty()) return positions
        
        val minX = positions.values.minOf { it.x }
        val minY = positions.values.minOf { it.y }
        
        return positions.mapValues { (_, pos) ->
            Point(pos.x - minX, pos.y - minY)
        }
    }
    
    /**
     * Normalize edge control points to match normalized node positions.
     */
    private fun normalizeEdgeControlPoints(
        edgeControlPoints: Map<Edge, List<Point>>,
        normalizedNodePositions: Map<Node, Point>
    ): Map<Edge, List<Point>> {
        if (edgeControlPoints.isEmpty()) return edgeControlPoints
        
        // Calculate the offset used in node normalization
        val allPoints = edgeControlPoints.values.flatten()
        if (allPoints.isEmpty()) return edgeControlPoints
        
        val minX = allPoints.minOf { it.x }
        val minY = allPoints.minOf { it.y }
        
        return edgeControlPoints.mapValues { (edge, controlPoints) ->
            controlPoints.mapIndexed { index, point ->
                when (index) {
                    0 -> normalizedNodePositions[edge.source] ?: Point(point.x - minX, point.y - minY)
                    controlPoints.size - 1 -> normalizedNodePositions[edge.target] ?: Point(point.x - minX, point.y - minY)
                    else -> Point(point.x - minX, point.y - minY)
                }
            }
        }
    }
    
    /**
     * Calculate the bounding box of the entire layout.
     */
    fun calculateBoundingBox(
        nodePositions: Map<Node, Point>,
        nodeSizes: Map<Node, NodeSize>
    ): BoundingBox? {
        if (nodePositions.isEmpty()) return null
        
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        
        for ((node, position) in nodePositions) {
            val size = nodeSizes[node] ?: NodeSize(30.0, 20.0)
            val halfWidth = size.width / 2
            val halfHeight = size.height / 2
            
            minX = minOf(minX, position.x - halfWidth)
            minY = minOf(minY, position.y - halfHeight)
            maxX = maxOf(maxX, position.x + halfWidth)
            maxY = maxOf(maxY, position.y + halfHeight)
        }
        
        return BoundingBox(minX, minY, maxX, maxY)
    }
    
    /**
     * Adjust positions to avoid overlaps (simple collision detection).
     */
    fun adjustForOverlaps(
        nodePositions: Map<Node, Point>,
        nodeSizes: Map<Node, NodeSize>,
        minSpacing: Double = 5.0
    ): Map<Node, Point> {
        val adjustedPositions = nodePositions.toMutableMap()
        val nodes = nodePositions.keys.toList()
        
        // Simple pairwise overlap resolution
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val node1 = nodes[i]
                val node2 = nodes[j]
                
                val pos1 = adjustedPositions[node1] ?: continue
                val pos2 = adjustedPositions[node2] ?: continue
                
                val size1 = nodeSizes[node1] ?: NodeSize(30.0, 20.0)
                val size2 = nodeSizes[node2] ?: NodeSize(30.0, 20.0)
                
                val rect1 = size1.toRectangle(pos1)
                val rect2 = size2.toRectangle(pos2)
                
                if (rect1.intersects(rect2)) {
                    val separation = LayoutUtils.separationDistance(rect1, rect2)
                    if (separation != Point.ORIGIN) {
                        // Move node2 to resolve overlap
                        adjustedPositions[node2] = Point(
                            pos2.x + separation.x,
                            pos2.y + separation.y
                        )
                    }
                }
            }
        }
        
        return adjustedPositions
    }
}

/**
 * Result of coordinate assignment.
 */
sealed class CoordinateResult {
    /**
     * Successful coordinate assignment.
     */
    data class Success(
        val nodePositions: Map<Node, Point>,
        val edgeControlPoints: Map<Edge, List<Point>>
    ) : CoordinateResult()
    
    /**
     * Coordinate assignment failed.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : CoordinateResult()
    
    /**
     * Check if coordinate assignment was successful.
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Get node positions if successful, or null if failed.
     */
    fun getNodePositionsOrNull(): Map<Node, Point>? = when (this) {
        is Success -> nodePositions
        is Error -> null
    }
    
    /**
     * Get edge control points if successful, or null if failed.
     */
    fun getEdgeControlPointsOrNull(): Map<Edge, List<Point>>? = when (this) {
        is Success -> edgeControlPoints
        is Error -> null
    }
}

/**
 * Utility functions for coordinate calculations.
 */
object CoordinateUtils {
    
    /**
     * Calculate the center point of a collection of positions.
     */
    fun calculateCenter(positions: Collection<Point>): Point {
        if (positions.isEmpty()) return Point.ORIGIN
        
        val sumX = positions.sumOf { it.x }
        val sumY = positions.sumOf { it.y }
        
        return Point(sumX / positions.size, sumY / positions.size)
    }
    
    /**
     * Scale all positions by a factor.
     */
    fun scalePositions(positions: Map<Node, Point>, scale: Double): Map<Node, Point> {
        return positions.mapValues { (_, pos) ->
            Point(pos.x * scale, pos.y * scale)
        }
    }
    
    /**
     * Translate all positions by an offset.
     */
    fun translatePositions(positions: Map<Node, Point>, offset: Point): Map<Node, Point> {
        return positions.mapValues { (_, pos) ->
            Point(pos.x + offset.x, pos.y + offset.y)
        }
    }
    
    /**
     * Calculate the total length of all edges.
     */
    fun calculateTotalEdgeLength(edgeControlPoints: Map<Edge, List<Point>>): Double {
        return edgeControlPoints.values.sumOf { controlPoints ->
            var length = 0.0
            for (i in 0 until controlPoints.size - 1) {
                length += controlPoints[i].distanceTo(controlPoints[i + 1])
            }
            length
        }
    }
    
    /**
     * Check if any nodes overlap.
     */
    fun hasOverlaps(nodePositions: Map<Node, Point>, nodeSizes: Map<Node, NodeSize>): Boolean {
        val nodes = nodePositions.keys.toList()
        
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val node1 = nodes[i]
                val node2 = nodes[j]
                
                val pos1 = nodePositions[node1] ?: continue
                val pos2 = nodePositions[node2] ?: continue
                
                val size1 = nodeSizes[node1] ?: NodeSize(30.0, 20.0)
                val size2 = nodeSizes[node2] ?: NodeSize(30.0, 20.0)
                
                val rect1 = size1.toRectangle(pos1)
                val rect2 = size2.toRectangle(pos2)
                
                if (rect1.intersects(rect2)) {
                    return true
                }
            }
        }
        
        return false
    }
}