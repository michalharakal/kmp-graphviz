package org.graphviz.kotlin.layout.dot

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.graphviz.kotlin.model.*
import org.graphviz.kotlin.layout.LayoutOptions
import org.graphviz.kotlin.layout.NodeSize

class DotCoordinatesTest : FunSpec({
    
    val coordinates = DotCoordinates()
    
    test("should assign coordinates to simple graph") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val edge = EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList())
        
        val graph = GraphImpl(
            id = "simple",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val ranks: Map<Node, Int> = mapOf(nodeA to 0, nodeB to 1)
        val ordering = mapOf(
            0 to listOf(NodePosition(nodeA, 50.0, NodeSize(30.0, 20.0))),
            1 to listOf(NodePosition(nodeB, 50.0, NodeSize(30.0, 20.0)))
        )
        val options = LayoutOptions.default()
        
        val result = coordinates.assignCoordinates(graph, ranks, ordering, options)
        
        result.shouldBeInstanceOf<CoordinateResult.Success>()
        
        val nodePositions = result.nodePositions
        val edgeControlPoints = result.edgeControlPoints
        
        // Nodes should have positions
        nodePositions[nodeA] shouldNotBe null
        nodePositions[nodeB] shouldNotBe null
        
        // Edge should have control points
        edgeControlPoints[edge] shouldNotBe null
        edgeControlPoints[edge]!!.size shouldBe 2 // Start and end points
    }
    
    test("should handle long edges spanning multiple ranks") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        val edge = EdgeImpl(nodeA, nodeC, AttributeMap.empty(), emptyList()) // Spans 2 ranks
        
        val graph = GraphImpl(
            id = "long_edge",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val ranks: Map<Node, Int> = mapOf(nodeA to 0, nodeB to 1, nodeC to 2)
        val ordering = mapOf(
            0 to listOf(NodePosition(nodeA, 50.0, NodeSize(30.0, 20.0))),
            1 to listOf(NodePosition(nodeB, 50.0, NodeSize(30.0, 20.0))),
            2 to listOf(NodePosition(nodeC, 50.0, NodeSize(30.0, 20.0)))
        )
        val options = LayoutOptions.default()
        
        val result = coordinates.assignCoordinates(graph, ranks, ordering, options)
        
        result.shouldBeInstanceOf<CoordinateResult.Success>()
        
        val edgeControlPoints = result.edgeControlPoints[edge]!!
        
        // Long edge should have intermediate control points
        edgeControlPoints.size shouldBe 3 // Start, intermediate, end
    }
    
    test("should calculate bounding box correctly") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        
        val nodePositions: Map<Node, Point> = mapOf(
            nodeA to Point(10.0, 20.0),
            nodeB to Point(50.0, 60.0)
        )
        
        val nodeSizes: Map<Node, NodeSize> = mapOf(
            nodeA to NodeSize(20.0, 10.0), // left=0, right=20, top=15, bottom=25
            nodeB to NodeSize(30.0, 20.0)  // left=35, right=65, top=50, bottom=70
        )
        
        val boundingBox = coordinates.calculateBoundingBox(nodePositions, nodeSizes)
        
        boundingBox shouldNotBe null
        boundingBox!!.minX shouldBe 0.0   // leftmost edge of nodeA
        boundingBox.maxX shouldBe 65.0    // rightmost edge of nodeB
        boundingBox.minY shouldBe 15.0    // topmost edge of nodeA
        boundingBox.maxY shouldBe 70.0    // bottommost edge of nodeB
    }
    
    test("should adjust for overlaps") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        
        // Position nodes so they overlap
        val nodePositions: Map<Node, Point> = mapOf(
            nodeA to Point(10.0, 10.0),
            nodeB to Point(15.0, 10.0) // Overlaps with nodeA
        )
        
        val nodeSizes: Map<Node, NodeSize> = mapOf(
            nodeA to NodeSize(20.0, 10.0),
            nodeB to NodeSize(20.0, 10.0)
        )
        
        val adjustedPositions = coordinates.adjustForOverlaps(nodePositions, nodeSizes)
        
        // Nodes should no longer overlap
        val posA = adjustedPositions[nodeA]!!
        val posB = adjustedPositions[nodeB]!!
        
        val rectA = nodeSizes[nodeA]!!.toRectangle(posA)
        val rectB = nodeSizes[nodeB]!!.toRectangle(posB)
        
        rectA.intersects(rectB) shouldBe false
    }
})

class CoordinateUtilsTest : FunSpec({
    
    test("calculateCenter should find correct center") {
        val positions = listOf(
            Point(0.0, 0.0),
            Point(10.0, 0.0),
            Point(5.0, 10.0)
        )
        
        val center = CoordinateUtils.calculateCenter(positions)
        
        center.x shouldBe 5.0  // (0 + 10 + 5) / 3
        center.y shouldBe 10.0 / 3  // (0 + 0 + 10) / 3
    }
    
    test("scalePositions should scale all positions") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        
        val positions: Map<Node, Point> = mapOf(
            nodeA to Point(10.0, 20.0),
            nodeB to Point(30.0, 40.0)
        )
        
        val scaled = CoordinateUtils.scalePositions(positions, 2.0)
        
        scaled[nodeA] shouldBe Point(20.0, 40.0)
        scaled[nodeB] shouldBe Point(60.0, 80.0)
    }
    
    test("translatePositions should translate all positions") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        
        val positions: Map<Node, Point> = mapOf(
            nodeA to Point(10.0, 20.0),
            nodeB to Point(30.0, 40.0)
        )
        
        val translated = CoordinateUtils.translatePositions(positions, Point(5.0, -10.0))
        
        translated[nodeA] shouldBe Point(15.0, 10.0)
        translated[nodeB] shouldBe Point(35.0, 30.0)
    }
    
    test("hasOverlaps should detect overlapping nodes") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        
        val overlappingPositions: Map<Node, Point> = mapOf(
            nodeA to Point(10.0, 10.0),
            nodeB to Point(15.0, 10.0), // Overlaps with A
            nodeC to Point(50.0, 10.0)  // No overlap
        )
        
        val nonOverlappingPositions: Map<Node, Point> = mapOf(
            nodeA to Point(10.0, 10.0),
            nodeB to Point(40.0, 10.0), // No overlap
            nodeC to Point(70.0, 10.0)  // No overlap
        )
        
        val nodeSizes: Map<Node, NodeSize> = mapOf(
            nodeA to NodeSize(20.0, 10.0),
            nodeB to NodeSize(20.0, 10.0),
            nodeC to NodeSize(20.0, 10.0)
        )
        
        CoordinateUtils.hasOverlaps(overlappingPositions, nodeSizes) shouldBe true
        CoordinateUtils.hasOverlaps(nonOverlappingPositions, nodeSizes) shouldBe false
    }
})