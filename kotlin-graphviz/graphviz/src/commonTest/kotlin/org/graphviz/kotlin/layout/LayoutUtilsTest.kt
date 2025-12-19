package org.graphviz.kotlin.layout

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.graphviz.kotlin.model.*
import kotlin.math.PI
import kotlin.math.abs

class LayoutUtilsTest : FunSpec({
    
    test("calculateNodeSize should use minimum dimensions") {
        val node = NodeImpl("test", AttributeMap.empty(), null)
        val options = LayoutOptions.default()
        
        val size = LayoutUtils.calculateNodeSize(node, options)
        
        size.width shouldBe options.minNodeWidth
        size.height shouldBe options.minNodeHeight
    }
    
    test("calculateNodeSize should respect explicit width and height") {
        val node = NodeImpl(
            "test",
            AttributeMap.builder()
                .set(AttributeKey.WIDTH, 100.0)
                .set(AttributeKey.HEIGHT, 50.0)
                .build(),
            null
        )
        val options = LayoutOptions.default()
        
        val size = LayoutUtils.calculateNodeSize(node, options)
        
        size.width shouldBe 100.0
        size.height shouldBe 50.0
    }
    
    test("calculateNodeSize should expand for labels") {
        val node = NodeImpl(
            "test",
            AttributeMap.builder()
                .set(AttributeKey.LABEL, "Very Long Label Text")
                .build(),
            null
        )
        val options = LayoutOptions.default()
        
        val size = LayoutUtils.calculateNodeSize(node, options)
        
        size.width shouldBeGreaterThan options.minNodeWidth
        size.height shouldBeGreaterThan options.minNodeHeight
    }
    
    test("estimateTextWidth should scale with text length") {
        val shortText = "Hi"
        val longText = "This is a much longer text"
        
        val shortWidth = LayoutUtils.estimateTextWidth(shortText)
        val longWidth = LayoutUtils.estimateTextWidth(longText)
        
        longWidth shouldBeGreaterThan shortWidth
    }
    
    test("estimateTextHeight should account for newlines") {
        val singleLine = "Single line"
        val multiLine = "Line 1\nLine 2\nLine 3"
        
        val singleHeight = LayoutUtils.estimateTextHeight(singleLine)
        val multiHeight = LayoutUtils.estimateTextHeight(multiLine)
        
        multiHeight shouldBeGreaterThan singleHeight
        multiHeight shouldBe 3 * 14.0 // 3 lines * 14 pixels per line
    }
    
    test("distance should calculate correct Euclidean distance") {
        val p1 = Point(0.0, 0.0)
        val p2 = Point(3.0, 4.0)
        
        val distance = LayoutUtils.distance(p1, p2)
        
        distance shouldBe 5.0 // 3-4-5 triangle
    }
    
    test("angle should calculate correct angle") {
        val from = Point(0.0, 0.0)
        val to = Point(1.0, 0.0)
        
        val angle = LayoutUtils.angle(from, to)
        
        angle shouldBe 0.0 // Horizontal right
    }
    
    test("rotatePoint should rotate correctly") {
        val point = Point(1.0, 0.0)
        val center = Point(0.0, 0.0)
        val angle = PI / 2 // 90 degrees
        
        val rotated = LayoutUtils.rotatePoint(point, center, angle)
        
        // Use tolerance for floating point comparison
        (abs(rotated.x) < 1e-10) shouldBe true // Should be approximately 0
        (abs(rotated.y - 1.0) < 1e-10) shouldBe true // Should be approximately 1.0
    }
    
    test("rectangleIntersection should find correct intersection") {
        val center = Point(0.0, 0.0)
        val target = Point(100.0, 0.0)
        val rect = Rectangle(40.0, -10.0, 20.0, 20.0) // Rectangle from (40,-10) to (60,10)
        
        val intersection = LayoutUtils.rectangleIntersection(center, target, rect)
        
        intersection.x shouldBe 40.0 // Left edge of rectangle
        intersection.y shouldBe 0.0 // On the horizontal line
    }
    
    test("createStraightPath should create two-point path") {
        val from = Point(0.0, 0.0)
        val to = Point(10.0, 10.0)
        
        val path = LayoutUtils.createStraightPath(from, to)
        
        path.size shouldBe 2
        path[0] shouldBe from
        path[1] shouldBe to
    }
    
    test("createCurvedPath should create three-point path") {
        val from = Point(0.0, 0.0)
        val to = Point(10.0, 0.0)
        
        val path = LayoutUtils.createCurvedPath(from, to)
        
        path.size shouldBe 3
        path[0] shouldBe from
        path[2] shouldBe to
        // Middle point should be offset from the straight line
    }
    
    test("normalizeCoordinates should handle empty list") {
        val points = emptyList<Point>()
        
        val normalized = LayoutUtils.normalizeCoordinates(points)
        
        normalized shouldBe emptyList()
    }
    
    test("normalizeCoordinates should translate to origin") {
        val points = listOf(
            Point(100.0, 200.0),
            Point(150.0, 250.0)
        )
        
        val normalized = LayoutUtils.normalizeCoordinates(points)
        
        normalized[0] shouldBe Point(0.0, 0.0)
        normalized[1] shouldBe Point(50.0, 50.0)
    }
    
    test("rectanglesOverlap should detect overlapping rectangles") {
        val rect1 = Rectangle(0.0, 0.0, 10.0, 10.0)
        val rect2 = Rectangle(5.0, 5.0, 10.0, 10.0)
        val rect3 = Rectangle(20.0, 20.0, 10.0, 10.0)
        
        LayoutUtils.rectanglesOverlap(rect1, rect2) shouldBe true
        LayoutUtils.rectanglesOverlap(rect1, rect3) shouldBe false
    }
    
    test("separationDistance should calculate correct separation") {
        val rect1 = Rectangle(0.0, 0.0, 10.0, 10.0)
        val rect2 = Rectangle(5.0, 0.0, 10.0, 10.0) // Overlapping horizontally
        
        val separation = LayoutUtils.separationDistance(rect1, rect2)
        
        separation.x shouldBe -6.0 // Move rect1 left by overlap + 1
        separation.y shouldBe 0.0
    }
})

class NodeSizeTest : FunSpec({
    
    test("NodeSize should validate dimensions") {
        try {
            NodeSize(-1.0, 10.0)
            throw AssertionError("Expected exception for negative width")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
        
        try {
            NodeSize(10.0, -1.0)
            throw AssertionError("Expected exception for negative height")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }
    
    test("NodeSize.toRectangle should create centered rectangle") {
        val size = NodeSize(20.0, 10.0)
        val center = Point(50.0, 30.0)
        
        val rect = size.toRectangle(center)
        
        rect.x shouldBe 40.0 // center.x - width/2
        rect.y shouldBe 25.0 // center.y - height/2
        rect.width shouldBe 20.0
        rect.height shouldBe 10.0
        rect.center shouldBe center
    }
})