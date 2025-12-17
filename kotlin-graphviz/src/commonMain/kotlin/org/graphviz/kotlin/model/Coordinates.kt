package org.graphviz.kotlin.model

/**
 * Represents a 2D point with double precision coordinates.
 */
data class Point(
    val x: Double,
    val y: Double
) {
    companion object {
        val ORIGIN = Point(0.0, 0.0)
    }
    
    operator fun plus(other: Point): Point = Point(x + other.x, y + other.y)
    operator fun minus(other: Point): Point = Point(x - other.x, y - other.y)
    operator fun times(scalar: Double): Point = Point(x * scalar, y * scalar)
    
    fun distanceTo(other: Point): Double {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

/**
 * Represents a rectangle with position and dimensions.
 */
data class Rectangle(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
) {
    init {
        require(width >= 0) { "Width must be non-negative, got $width" }
        require(height >= 0) { "Height must be non-negative, got $height" }
    }
    
    val left: Double get() = x
    val right: Double get() = x + width
    val top: Double get() = y
    val bottom: Double get() = y + height
    val center: Point get() = Point(x + width / 2, y + height / 2)
    
    fun contains(point: Point): Boolean {
        return point.x >= left && point.x <= right && point.y >= top && point.y <= bottom
    }
    
    fun intersects(other: Rectangle): Boolean {
        return left <= other.right && right >= other.left && top <= other.bottom && bottom >= other.top
    }
    
    fun toBoundingBox(): BoundingBox = BoundingBox(left, top, right, bottom)
}

/**
 * Represents a bounding box defined by minimum and maximum coordinates.
 */
data class BoundingBox(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double
) {
    init {
        require(minX <= maxX) { "minX ($minX) must be <= maxX ($maxX)" }
        require(minY <= maxY) { "minY ($minY) must be <= maxY ($maxY)" }
    }
    
    val width: Double get() = maxX - minX
    val height: Double get() = maxY - minY
    val center: Point get() = Point((minX + maxX) / 2, (minY + maxY) / 2)
    
    fun contains(point: Point): Boolean {
        return point.x >= minX && point.x <= maxX && point.y >= minY && point.y <= maxY
    }
    
    fun contains(other: BoundingBox): Boolean {
        return minX <= other.minX && maxX >= other.maxX && minY <= other.minY && maxY >= other.maxY
    }
    
    fun intersects(other: BoundingBox): Boolean {
        return minX <= other.maxX && maxX >= other.minX && minY <= other.maxY && maxY >= other.minY
    }
    
    fun union(other: BoundingBox): BoundingBox {
        return BoundingBox(
            minOf(minX, other.minX),
            minOf(minY, other.minY),
            maxOf(maxX, other.maxX),
            maxOf(maxY, other.maxY)
        )
    }
    
    fun toRectangle(): Rectangle = Rectangle(minX, minY, width, height)
    
    companion object {
        fun fromPoints(points: Collection<Point>): BoundingBox {
            require(points.isNotEmpty()) { "Cannot create bounding box from empty point collection" }
            
            var minX = Double.POSITIVE_INFINITY
            var minY = Double.POSITIVE_INFINITY
            var maxX = Double.NEGATIVE_INFINITY
            var maxY = Double.NEGATIVE_INFINITY
            
            for (point in points) {
                minX = minOf(minX, point.x)
                minY = minOf(minY, point.y)
                maxX = maxOf(maxX, point.x)
                maxY = maxOf(maxY, point.y)
            }
            
            return BoundingBox(minX, minY, maxX, maxY)
        }
    }
}