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
        
        // Should handle empty graph gracefully by returning the empty graph
        result.shouldBeInstanceOf<LayoutResult.Success>()
        val positionedGraph = result.graph
        positionedGraph.getAllNodes().size shouldBe 0
        positionedGraph.getAllEdges().size shouldBe 0
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
        // Node should be positioned at origin for single node
        positionedNode.position!!.x shouldBe 0.0
        positionedNode.position!!.y shouldBe 0.0
    }
    
    test("should handle single node with self-loop") {
        val node = NodeImpl("A", AttributeMap.empty(), null)
        val selfLoop = EdgeImpl(node, node, AttributeMap.empty(), emptyList())
        val graph = GraphImpl(
            id = "self-loop",
            isDirected = true,
            nodes = setOf(node),
            edges = setOf(selfLoop),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val result = engine.layout(graph)
        
        result.shouldBeInstanceOf<LayoutResult.Success>()
        val positionedGraph = result.graph
        
        val positionedNode = positionedGraph.getAllNodes().first()
        positionedNode.position shouldNotBe null
        
        val positionedEdge = positionedGraph.getAllEdges().first()
        positionedEdge.controlPoints.isNotEmpty() shouldBe true
        // Self-loop should have multiple control points to form a loop
        positionedEdge.controlPoints.size shouldBe 10 // Start + 8 circle points + end
    }
    
    test("should handle disconnected components") {
        // Create two separate components
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        val nodeD = NodeImpl("D", AttributeMap.empty(), null)
        
        val edges = setOf(
            EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList()), // Component 1
            EdgeImpl(nodeC, nodeD, AttributeMap.empty(), emptyList())  // Component 2
        )
        
        val graph = GraphImpl(
            id = "disconnected",
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
        
        // Components should be arranged horizontally
        val nodePositions = positionedGraph.getAllNodes().associateBy { it.id }
        val nodeAPos = nodePositions["A"]!!.position!!
        val nodeBPos = nodePositions["B"]!!.position!!
        val nodeCPos = nodePositions["C"]!!.position!!
        val nodeDPos = nodePositions["D"]!!.position!!
        
        // Nodes in different components should have different X ranges
        val component1MaxX = maxOf(nodeAPos.x, nodeBPos.x)
        val component2MinX = minOf(nodeCPos.x, nodeDPos.x)
        
        // Component 2 should be to the right of component 1
        component2MinX shouldBe component1MaxX + 120.0 // Default spacing * 3
    }
    
    test("should handle graph with only self-loops") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        
        val edges = setOf(
            EdgeImpl(nodeA, nodeA, AttributeMap.empty(), emptyList()),
            EdgeImpl(nodeB, nodeB, AttributeMap.empty(), emptyList()),
            EdgeImpl(nodeC, nodeC, AttributeMap.empty(), emptyList())
        )
        
        val graph = GraphImpl(
            id = "self-loops-only",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC),
            edges = edges,
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val result = engine.layout(graph)
        
        result.shouldBeInstanceOf<LayoutResult.Success>()
        val positionedGraph = result.graph
        
        // All nodes should have positions in a grid layout
        positionedGraph.getAllNodes().forEach { node ->
            node.position shouldNotBe null
        }
        
        // All edges should have self-loop control points
        positionedGraph.getAllEdges().forEach { edge ->
            edge.controlPoints.size shouldBe 10 // Self-loop control points
        }
        
        // Nodes should be arranged in a grid with proper spacing
        val nodePositions = positionedGraph.getAllNodes().map { it.position!! }
        val xCoords = nodePositions.map { it.x }.distinct().sorted()
        val yCoords = nodePositions.map { it.y }.distinct().sorted()
        
        // Should have grid arrangement (2x2 for 3 nodes)
        xCoords.size shouldBe 2 // 2 columns
        yCoords.size shouldBe 2 // 2 rows
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

class DotLayoutEdgeCasesTest : FunSpec({
    
    test("should find connected components correctly") {
        // Create a graph with 3 components
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        val nodeD = NodeImpl("D", AttributeMap.empty(), null)
        val nodeE = NodeImpl("E", AttributeMap.empty(), null)
        
        val edges = setOf(
            EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList()), // Component 1
            EdgeImpl(nodeC, nodeD, AttributeMap.empty(), emptyList()), // Component 2
            // nodeE is isolated (Component 3)
        )
        
        val graph = GraphImpl(
            id = "multi-component",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC, nodeD, nodeE),
            edges = edges,
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val components = DotLayoutEdgeCases.findConnectedComponents(graph)
        
        components.size shouldBe 3
        
        // Check that each component contains the correct nodes
        val componentSizes = components.map { it.size }.sorted()
        componentSizes shouldBe listOf(1, 2, 2) // One isolated node, two pairs
    }
    
    test("should detect multiple edges correctly") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        
        // Graph with multiple edges between same nodes
        val edges = setOf(
            EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList()),
            EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList()) // Duplicate edge
        )
        
        val graph = GraphImpl(
            id = "multi-edge",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = edges,
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val hasMultiple = DotLayoutEdgeCases.hasMultipleEdges(graph)
        hasMultiple shouldBe true
    }
    
    test("should not detect multiple edges in simple graph") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        
        val edges = setOf(
            EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList()),
            EdgeImpl(nodeB, nodeC, AttributeMap.empty(), emptyList())
        )
        
        val graph = GraphImpl(
            id = "simple",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC),
            edges = edges,
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val hasMultiple = DotLayoutEdgeCases.hasMultipleEdges(graph)
        hasMultiple shouldBe false
    }
    
    test("should create self-loop control points") {
        val nodePosition = Point(50.0, 100.0)
        val options = LayoutOptions()
        
        val controlPoints = DotLayoutEdgeCases.createSelfLoopControlPoints(nodePosition, options)
        
        // Should have start point + circle points + end point
        controlPoints.size shouldBe 10
        
        // First and last points should be at the node position
        controlPoints.first() shouldBe nodePosition
        controlPoints.last() shouldBe nodePosition
        
        // Middle points should form a circle around the node
        val middlePoints = controlPoints.drop(1).dropLast(1)
        middlePoints.forEach { point ->
            // All points should be at the same distance from the circle center
            val centerX = nodePosition.x + options.nodeSpacing * 0.8
            val centerY = nodePosition.y - options.nodeSpacing * 0.8
            val distance = kotlin.math.sqrt(
                (point.x - centerX) * (point.x - centerX) + 
                (point.y - centerY) * (point.y - centerY)
            )
            // Distance should be approximately the radius
            kotlin.math.abs(distance - options.nodeSpacing * 0.8) shouldBe 0.0
        }
    }
    
    test("should arrange components horizontally") {
        // Create two simple component graphs
        val nodeA = NodeImpl("A", AttributeMap.empty(), Point(0.0, 0.0))
        val nodeB = NodeImpl("B", AttributeMap.empty(), Point(0.0, 50.0))
        val component1 = GraphImpl(
            id = "comp1",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val nodeC = NodeImpl("C", AttributeMap.empty(), Point(10.0, 0.0))
        val nodeD = NodeImpl("D", AttributeMap.empty(), Point(20.0, 0.0))
        val component2 = GraphImpl(
            id = "comp2",
            isDirected = true,
            nodes = setOf(nodeC, nodeD),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val options = LayoutOptions()
        val arrangedGraph = DotLayoutEdgeCases.arrangeComponentsHorizontally(
            listOf(component1, component2), 
            options
        )
        
        // All nodes should be present
        arrangedGraph.getAllNodes().size shouldBe 4
        
        // Check that components are arranged horizontally
        val nodePositions = arrangedGraph.getAllNodes().associateBy { it.id }
        val nodeAPos = nodePositions["A"]!!.position!!
        val nodeBPos = nodePositions["B"]!!.position!!
        val nodeCPos = nodePositions["C"]!!.position!!
        val nodeDPos = nodePositions["D"]!!.position!!
        
        // Component 1 should start at X=0
        nodeAPos.x shouldBe 0.0
        nodeBPos.x shouldBe 0.0
        
        // Component 2 should be offset to the right
        val expectedOffset = 0.0 + options.nodeSpacing * 3 // Component width + spacing
        nodeCPos.x shouldBe expectedOffset + 10.0 // Original position + offset
        nodeDPos.x shouldBe expectedOffset + 20.0
    }
})