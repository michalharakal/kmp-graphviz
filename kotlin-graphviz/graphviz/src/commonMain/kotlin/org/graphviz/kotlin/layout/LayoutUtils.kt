package org.graphviz.kotlin.layout

import org.graphviz.kotlin.model.*
import kotlin.math.*

/**
 * Utility functions for layout calculations and coordinate transformations.
 */
object LayoutUtils {
    
    /**
     * Calculate the size of a node based on its attributes and label.
     */
    fun calculateNodeSize(node: Node, options: LayoutOptions): NodeSize {
        val width = node.attributes.get(AttributeKey.WIDTH) ?: options.minNodeWidth
        val height = node.attributes.get(AttributeKey.HEIGHT) ?: options.minNodeHeight
        
        // If node has a label, estimate size based on text
        val label = node.attributes.get(AttributeKey.LABEL)
        if (label != null) {
            val estimatedWidth = estimateTextWidth(label)
            val estimatedHeight = estimateTextHeight(label)
            return NodeSize(
                maxOf(width, estimatedWidth + 20.0), // Add padding
                maxOf(height, estimatedHeight + 10.0)
            )
        }
        
        return NodeSize(width, height)
    }
    
    /**
     * Estimate the width of text for layout purposes.
     * This is a simple approximation - real implementations would use font metrics.
     */
    fun estimateTextWidth(text: String): Double {
        // Simple approximation: average character width * length
        return text.length * 8.0
    }
    
    /**
     * Estimate the height of text for layout purposes.
     */
    fun estimateTextHeight(text: String): Double {
        // Count newlines and estimate height
        val lines = text.split('\n').size
        return lines * 14.0 // Approximate line height
    }
    
    /**
     * Calculate the distance between two points.
     */
    fun distance(p1: Point, p2: Point): Double {
        return p1.distanceTo(p2)
    }
    
    /**
     * Calculate the angle between two points in radians.
     */
    fun angle(from: Point, to: Point): Double {
        return atan2(to.y - from.y, to.x - from.x)
    }
    
    /**
     * Rotate a point around another point by the given angle in radians.
     */
    fun rotatePoint(point: Point, center: Point, angle: Double): Point {
        val cos = cos(angle)
        val sin = sin(angle)
        val dx = point.x - center.x
        val dy = point.y - center.y
        return Point(
            center.x + dx * cos - dy * sin,
            center.y + dx * sin + dy * cos
        )
    }
    
    /**
     * Calculate the intersection point of a line from center to target with a rectangle.
     * Used for edge routing to node boundaries.
     */
    fun rectangleIntersection(center: Point, target: Point, rect: Rectangle): Point {
        val dx = target.x - center.x
        val dy = target.y - center.y
        
        if (abs(dx) < 1e-10 && abs(dy) < 1e-10) {
            return center
        }
        
        val rectCenter = rect.center
        val halfWidth = rect.width / 2
        val halfHeight = rect.height / 2
        
        // Calculate intersection with rectangle edges
        val t1 = if (abs(dx) > 1e-10) (rectCenter.x + halfWidth - center.x) / dx else Double.POSITIVE_INFINITY
        val t2 = if (abs(dx) > 1e-10) (rectCenter.x - halfWidth - center.x) / dx else Double.POSITIVE_INFINITY
        val t3 = if (abs(dy) > 1e-10) (rectCenter.y + halfHeight - center.y) / dy else Double.POSITIVE_INFINITY
        val t4 = if (abs(dy) > 1e-10) (rectCenter.y - halfHeight - center.y) / dy else Double.POSITIVE_INFINITY
        
        val validTs = listOf(t1, t2, t3, t4).filter { it > 0 && it.isFinite() }
        val t = validTs.minOrNull() ?: 0.0
        
        return Point(center.x + t * dx, center.y + t * dy)
    }
    
    /**
     * Create a simple straight-line path between two points.
     */
    fun createStraightPath(from: Point, to: Point): List<Point> {
        return listOf(from, to)
    }
    
    /**
     * Create a path with control points for curved edges.
     */
    fun createCurvedPath(from: Point, to: Point, curvature: Double = 0.3): List<Point> {
        val midX = (from.x + to.x) / 2
        val midY = (from.y + to.y) / 2
        val dx = to.x - from.x
        val dy = to.y - from.y
        val length = sqrt(dx * dx + dy * dy)
        
        if (length < 1e-10) {
            return listOf(from, to)
        }
        
        // Create control point perpendicular to the line
        val perpX = -dy / length * curvature * length
        val perpY = dx / length * curvature * length
        val controlPoint = Point(midX + perpX, midY + perpY)
        
        return listOf(from, controlPoint, to)
    }
    
    /**
     * Normalize coordinates to ensure they are within reasonable bounds.
     */
    fun normalizeCoordinates(points: List<Point>): List<Point> {
        if (points.isEmpty()) return points
        
        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val maxX = points.maxOf { it.x }
        val maxY = points.maxOf { it.y }
        
        val width = maxX - minX
        val height = maxY - minY
        
        // If the graph is too large, scale it down
        val maxDimension = maxOf(width, height)
        val scale = if (maxDimension > 10000.0) 10000.0 / maxDimension else 1.0
        
        return points.map { point ->
            Point(
                (point.x - minX) * scale,
                (point.y - minY) * scale
            )
        }
    }
    
    /**
     * Check if two rectangles overlap.
     */
    fun rectanglesOverlap(rect1: Rectangle, rect2: Rectangle): Boolean {
        return rect1.intersects(rect2)
    }
    
    /**
     * Calculate the minimum distance to separate two overlapping rectangles.
     */
    fun separationDistance(rect1: Rectangle, rect2: Rectangle): Point {
        val overlapX = minOf(rect1.right, rect2.right) - maxOf(rect1.left, rect2.left)
        val overlapY = minOf(rect1.bottom, rect2.bottom) - maxOf(rect1.top, rect2.top)
        
        return if (overlapX > 0 && overlapY > 0) {
            // Rectangles overlap, calculate minimum separation
            if (overlapX < overlapY) {
                // Separate horizontally
                val direction = if (rect1.center.x < rect2.center.x) -1.0 else 1.0
                Point(direction * (overlapX + 1.0), 0.0)
            } else {
                // Separate vertically
                val direction = if (rect1.center.y < rect2.center.y) -1.0 else 1.0
                Point(0.0, direction * (overlapY + 1.0))
            }
        } else {
            Point.ORIGIN
        }
    }
}

/**
 * Represents the size of a node for layout calculations.
 */
data class NodeSize(
    val width: Double,
    val height: Double
) {
    init {
        require(width >= 0) { "Width must be non-negative, got $width" }
        require(height >= 0) { "Height must be non-negative, got $height" }
    }
    
    /**
     * Convert to a rectangle centered at the given point.
     */
    fun toRectangle(center: Point): Rectangle {
        return Rectangle(
            center.x - width / 2,
            center.y - height / 2,
            width,
            height
        )
    }
}