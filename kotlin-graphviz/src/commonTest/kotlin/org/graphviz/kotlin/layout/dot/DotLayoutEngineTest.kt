package org.graphviz.kotlin.layout.dot

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.graphviz.kotlin.layout.LayoutOptions
import org.graphviz.kotlin.layout.LayoutResult
import org.graphviz.kotlin.model.*

class DotLayoutEngineTest : FunSpec({
    
    val engine = DotLayoutEngine()
    
    test("should have correct name") {
        engine.name shouldBe "dot"
    }
    
    test("should layout simple chain graph") {
        val (graph, nodes) = createChainGraph(3)
        val result = engine.layout(graph)
        
        result.shouldBeInstanceOf<LayoutResult.Success>()
        val positionedGraph = result.graph
        
        // All nodes should have positions
        positionedGraph.getAllNodes().forEach { node ->
            node.position shouldNotBe null
        }
        
        // All edges should have control points
        positionedGraph.getAllEdges().forEach { edge ->
            edge.controlPoints.isNotEmpty() shouldBe true
        }
        
        // Nodes should be arranged hierarchically
        val positionedNodes = positionedGraph.getAllNodes().toList().sortedBy { it.id }
        val node0Pos = positionedNodes[0].position!!
        val node1Pos = positionedNodes[1].position!!
        val node2Pos = positionedNodes[2].position!!
        
        // Y coordinates should increase (hierarchical layout)
        node0Pos.y shouldBe 0.0
        node1Pos.y shouldBe 75.0 // Default rank spacing
        node2Pos.y shouldBe 150.0
    }
    
    test("should layout diamond graph") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        val nodeD = NodeImpl("D", AttributeMap.empty(), null)
        
        val edges = setOf(
            EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList()),
            EdgeImpl(nodeA, nodeC, AttributeMap.empty(), emptyList()),
            EdgeImpl(nodeB, nodeD, AttributeMap.empty(), emptyList()),
            EdgeImpl(nodeC, nodeD, AttributeMap.empty(), emptyList())
        )
        
        val graph = GraphImpl(
            id = "diamond",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC, nodeD),
            edges = edges,
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val result = engine.layout(graph)
        
        result.shouldBeInstanceOf<LayoutResult.Success>()
        val positionedGraph = result.graph
        
        // All nodes should have positions
        positionedGraph.getAllNodes().forEach { node ->
            node.position shouldNotBe null
        }
        
        // Check hierarchical arrangement by finding nodes by ID
        val positionedNodeA = positionedGraph.getAllNodes().find { it.id == "A" }!!
        val positionedNodeB = positionedGraph.getAllNodes().find { it.id == "B" }!!
        val positionedNodeC = positionedGraph.getAllNodes().find { it.id == "C" }!!
        val positionedNodeD = positionedGraph.getAllNodes().find { it.id == "D" }!!
        
        // A should be at the top (rank 0)
        positionedNodeA.position!!.y shouldBe 0.0
        
        // B and C should be at the middle level (rank 1)
        positionedNodeB.position!!.y shouldBe 75.0
        positionedNodeC.position!!.y shouldBe 75.0
        
        // D should be at the bottom (rank 2)
        positionedNodeD.position!!.y shouldBe 150.0
    }
    
    test("should handle graph with cycles") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        
        val edges = setOf(
            EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList()),
            EdgeImpl(nodeB, nodeC, AttributeMap.empty(), emptyList()),
            EdgeImpl(nodeC, nodeA, AttributeMap.empty(), emptyList()) // Creates cycle
        )
        
        val graph = GraphImpl(
            id = "cycle",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC),
            edges = edges,
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val result = engine.layout(graph)
        
        result.shouldBeInstanceOf<LayoutResult.Success>()
        val positionedGraph = result.graph
        
        // Should still position all nodes despite the cycle
        positionedGraph.getAllNodes().forEach { node ->
            node.position shouldNotBe null
        }
        
        // All edges should have control points (including feedback edge)
        positionedGraph.getAllEdges().forEach { edge ->
            edge.controlPoints.isNotEmpty() shouldBe true
        }
    }
    
    test("should respect layout options") {
        val (graph, _) = createChainGraph(3)
        val options = LayoutOptions(
            rankSpacing = 100.0,
            nodeSpacing = 80.0
        )
        
        val result = engine.layout(graph, options)
        
        result.shouldBeInstanceOf<LayoutResult.Success>()
        val positionedGraph = result.graph
        
        // Check that custom rank spacing is used
        val nodePositions = positionedGraph.getAllNodes().map { it.position!! }
        val yCoordinates = nodePositions.map { it.y }.distinct().sorted()
        
        if (yCoordinates.size > 1) {
            val spacing = yCoordinates[1] - yCoordinates[0]
            spacing shouldBe 100.0 // Custom rank spacing
        }
    }
    
    test("should handle empty graph") {
        val graph = GraphImpl(
            id = "empty",
            isDirected = true,
            nodes = emptySet(),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val result = engine.layout(graph)
        
        // Should fail gracefully for empty graph
        result.shouldBeInstanceOf<LayoutResult.Error>()
    }
    
    test("should handle single node") {
        val node = NodeImpl("A", AttributeMap.empty(), null)
        val graph = GraphImpl(
            id = "single",
            isDirected = true,
            nodes = setOf(node),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val result = engine.layout(graph)
        
        result.shouldBeInstanceOf<LayoutResult.Success>()
        val positionedGraph = result.graph
        
        val positionedNode = positionedGraph.getAllNodes().first()
        positionedNode.position shouldNotBe null
        // Node should be positioned at Y=0 (first rank)
        positionedNode.position!!.y shouldBe 0.0
    }
})

class DotLayoutStatisticsTest : FunSpec({
    
    test("should calculate quality score") {
        val stats = DotLayoutStatistics(
            nodeCount = 4,
            edgeCount = 4,
            rankCount = 3,
            crossings = 2,
            totalEdgeSpan = 5,
            totalEdgeLength = 150.0,
            boundingBox = BoundingBox(0.0, 0.0, 100.0, 200.0),
            hasOverlaps = false
        )
        
        val score = stats.calculateQualityScore()
        
        // Score should include penalties for crossings and edge span
        // 2 crossings * 100 + 5 edge span * 10 + bounding box area * 0.01
        val expectedScore = 2 * 100.0 + 5 * 10.0 + 100.0 * 200.0 * 0.01
        score shouldBe expectedScore
    }
    
    test("should penalize overlaps heavily") {
        val statsWithOverlaps = DotLayoutStatistics(
            nodeCount = 2,
            edgeCount = 1,
            rankCount = 2,
            crossings = 0,
            totalEdgeSpan = 1,
            totalEdgeLength = 50.0,
            boundingBox = BoundingBox(0.0, 0.0, 50.0, 100.0),
            hasOverlaps = true
        )
        
        val statsWithoutOverlaps = statsWithOverlaps.copy(hasOverlaps = false)
        
        val scoreWithOverlaps = statsWithOverlaps.calculateQualityScore()
        val scoreWithoutOverlaps = statsWithoutOverlaps.calculateQualityScore()
        
        scoreWithOverlaps shouldBe scoreWithoutOverlaps + 1000.0
    }
    
    test("should generate readable string representation") {
        val stats = DotLayoutStatistics(
            nodeCount = 3,
            edgeCount = 2,
            rankCount = 2,
            crossings = 0,
            totalEdgeSpan = 2,
            totalEdgeLength = 100.0,
            boundingBox = BoundingBox(0.0, 0.0, 80.0, 75.0),
            hasOverlaps = false
        )
        
        val string = stats.toString()
        
        string.contains("Nodes: 3") shouldBe true
        string.contains("Edges: 2") shouldBe true
        string.contains("Crossings: 0") shouldBe true
        string.contains("Bounding Box: 80.0 x 75.0") shouldBe true
    }
})

// Helper function to create a chain graph for testing
private fun createChainGraph(length: Int): Pair<Graph, List<Node>> {
    val nodes = (0 until length).map { i ->
        NodeImpl("node$i", AttributeMap.empty(), null)
    }
    
    val edges = (0 until length - 1).map { i ->
        EdgeImpl(nodes[i], nodes[i + 1], AttributeMap.empty(), emptyList())
    }.toSet()
    
    val graph = GraphImpl(
        id = "chain",
        isDirected = true,
        nodes = nodes.toSet(),
        edges = edges,
        subgraphs = emptySet(),
        attributes = AttributeMap.empty()
    )
    
    return Pair(graph, nodes)
}