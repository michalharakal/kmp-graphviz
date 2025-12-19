package org.graphviz.kotlin.renderer

import org.graphviz.kotlin.model.*
import kotlin.math.*

/**
 * Manages coordinate system transformations for SVG rendering.
 * 
 * Graphviz uses a coordinate system where (0,0) is at the bottom-left,
 * while SVG uses (0,0) at the top-left. This class handles the necessary
 * transformations and scaling operations.
 */
class CoordinateSystem(
    private val graphBounds: BoundingBox,
    private val svgViewport: Viewport,
    private val margin: Double = 0.0
) {
    
    /**
     * The scale factor applied to convert from graph coordinates to SVG coordinates.
     */
    val scaleX: Double
    val scaleY: Double
    
    /**
     * The translation offset to center the graph in the viewport.
     */
    val offsetX: Double
    val offsetY: Double
    
    init {
        // Calculate scale factors to fit the graph within the viewport
        val availableWidth = svgViewport.width - 2 * margin
        val availableHeight = svgViewport.height - 2 * margin
        
        scaleX = if (graphBounds.width > 0) availableWidth / graphBounds.width else 1.0
        scaleY = if (graphBounds.height > 0) availableHeight / graphBounds.height else 1.0
        
        // Use uniform scaling to maintain aspect ratio
        val uniformScale = minOf(scaleX, scaleY)
        
        // Calculate offsets to center the scaled graph
        val scaledWidth = graphBounds.width * uniformScale
        val scaledHeight = graphBounds.height * uniformScale
        
        offsetX = margin + (availableWidth - scaledWidth) / 2 - graphBounds.minX * uniformScale
        offsetY = margin + (availableHeight - scaledHeight) / 2 - graphBounds.minY * uniformScale
    }
    
    /**
     * Transform a point from graph coordinates to SVG coordinates.
     * This includes scaling and flipping the Y-axis.
     */
    fun transformPoint(point: Point): Point {
        val uniformScale = minOf(scaleX, scaleY)
        return Point(
            x = point.x * uniformScale + offsetX,
            y = svgViewport.height - (point.y * uniformScale + offsetY) // Flip Y-axis
        )
    }
    
    /**
     * Transform a list of points from graph coordinates to SVG coordinates.
     */
    fun transformPoints(points: List<Point>): List<Point> {
        return points.map { transformPoint(it) }
    }
    
    /**
     * Transform a bounding box from graph coordinates to SVG coordinates.
     */
    fun transformBoundingBox(boundingBox: BoundingBox): BoundingBox {
        val topLeft = transformPoint(Point(boundingBox.minX, boundingBox.maxY))
        val bottomRight = transformPoint(Point(boundingBox.maxX, boundingBox.minY))
        
        return BoundingBox(
            minX = minOf(topLeft.x, bottomRight.x),
            minY = minOf(topLeft.y, bottomRight.y),
            maxX = maxOf(topLeft.x, bottomRight.x),
            maxY = maxOf(topLeft.y, bottomRight.y)
        )
    }
    
    /**
     * Transform a dimension (width or height) from graph units to SVG units.
     */
    fun transformDimension(dimension: Double): Double {
        val uniformScale = minOf(scaleX, scaleY)
        return dimension * uniformScale
    }
    
    /**
     * Get the uniform scale factor used for both X and Y dimensions.
     */
    fun getUniformScale(): Double = minOf(scaleX, scaleY)
    
    /**
     * Create a transformation matrix for SVG transform attribute.
     * Returns a string suitable for the SVG transform attribute.
     */
    fun getTransformMatrix(): String {
        val uniformScale = minOf(scaleX, scaleY)
        return "translate($offsetX, ${svgViewport.height - offsetY}) scale($uniformScale, ${-uniformScale})"
    }
    
    /**
     * Check if the coordinate system requires scaling.
     */
    fun requiresScaling(): Boolean {
        val uniformScale = minOf(scaleX, scaleY)
        return abs(uniformScale - 1.0) > 1e-6
    }
    
    /**
     * Check if the coordinate system requires translation.
     */
    fun requiresTranslation(): Boolean {
        return abs(offsetX) > 1e-6 || abs(offsetY) > 1e-6
    }
    
    companion object {
        /**
         * Create a coordinate system that fits the graph within the given viewport.
         */
        fun fitToViewport(
            graphBounds: BoundingBox,
            viewport: Viewport,
            margin: Double = 20.0
        ): CoordinateSystem {
            return CoordinateSystem(graphBounds, viewport, margin)
        }
        
        /**
         * Create a coordinate system with a specific scale factor.
         */
        fun withScale(
            graphBounds: BoundingBox,
            viewport: Viewport,
            scale: Double,
            margin: Double = 20.0
        ): CoordinateSystem {
            // Create a custom viewport that enforces the desired scale
            val scaledWidth = graphBounds.width * scale + 2 * margin
            val scaledHeight = graphBounds.height * scale + 2 * margin
            
            val scaledViewport = Viewport(
                width = scaledWidth,
                height = scaledHeight,
                viewBox = viewport.viewBox
            )
            
            return CoordinateSystem(graphBounds, scaledViewport, margin)
        }
        
        /**
         * Create an identity coordinate system (no transformations).
         */
        fun identity(viewport: Viewport): CoordinateSystem {
            val identityBounds = BoundingBox(0.0, 0.0, viewport.width, viewport.height)
            return CoordinateSystem(identityBounds, viewport, 0.0)
        }
    }
}

/**
 * Utility class for SVG coordinate transformations and calculations.
 */
object CoordinateUtils {
    
    /**
     * Calculate the optimal viewport size for a given graph.
     */
    fun calculateOptimalViewport(
        graphBounds: BoundingBox,
        targetAspectRatio: Double? = null,
        minWidth: Double = 100.0,
        minHeight: Double = 100.0
    ): Viewport {
        val graphAspectRatio = if (graphBounds.height > 0) {
            graphBounds.width / graphBounds.height
        } else {
            1.0
        }
        
        val aspectRatio = targetAspectRatio ?: graphAspectRatio
        
        val width = maxOf(minWidth, graphBounds.width)
        val height = maxOf(minHeight, width / aspectRatio)
        
        return Viewport(
            width = width,
            height = height,
            viewBox = ViewBox.fromBoundingBox(graphBounds)
        )
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
    fun rotatePoint(point: Point, center: Point, angleRadians: Double): Point {
        val cos = cos(angleRadians)
        val sin = sin(angleRadians)
        
        val translatedX = point.x - center.x
        val translatedY = point.y - center.y
        
        return Point(
            x = translatedX * cos - translatedY * sin + center.x,
            y = translatedX * sin + translatedY * cos + center.y
        )
    }
    
    /**
     * Calculate the midpoint between two points.
     */
    fun midpoint(p1: Point, p2: Point): Point {
        return Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)
    }
    
    /**
     * Interpolate between two points by the given factor (0.0 to 1.0).
     */
    fun interpolate(p1: Point, p2: Point, factor: Double): Point {
        return Point(
            x = p1.x + (p2.x - p1.x) * factor,
            y = p1.y + (p2.y - p1.y) * factor
        )
    }
    
    /**
     * Check if a point is within a given distance of a line segment.
     */
    fun isPointNearLineSegment(
        point: Point,
        lineStart: Point,
        lineEnd: Point,
        tolerance: Double
    ): Boolean {
        val distance = distanceFromPointToLineSegment(point, lineStart, lineEnd)
        return distance <= tolerance
    }
    
    /**
     * Calculate the shortest distance from a point to a line segment.
     */
    fun distanceFromPointToLineSegment(
        point: Point,
        lineStart: Point,
        lineEnd: Point
    ): Double {
        val lineLength = distance(lineStart, lineEnd)
        if (lineLength == 0.0) {
            return distance(point, lineStart)
        }
        
        val t = maxOf(0.0, minOf(1.0, 
            ((point.x - lineStart.x) * (lineEnd.x - lineStart.x) + 
             (point.y - lineStart.y) * (lineEnd.y - lineStart.y)) / (lineLength * lineLength)
        ))
        
        val projection = Point(
            x = lineStart.x + t * (lineEnd.x - lineStart.x),
            y = lineStart.y + t * (lineEnd.y - lineStart.y)
        )
        
        return distance(point, projection)
    }
}