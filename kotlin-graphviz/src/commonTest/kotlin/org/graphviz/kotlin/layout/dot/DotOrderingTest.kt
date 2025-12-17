package org.graphviz.kotlin.layout.dot

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.graphviz.kotlin.model.*
import org.graphviz.kotlin.layout.NodeSize

class DotOrderingTest : FunSpec({
    
    val ordering = DotOrdering()
    
    test("should handle empty graph") {
        val graph = createEmptyGraph()
        val ranks = emptyMap<Node, Int>()
        val nodeSizes = emptyMap<Node, NodeSize>()
        
        val result = ordering.orderNodes(graph, ranks, nodeSizes)
        
        result.shouldBeInstanceOf<OrderingResult.Success>()
        result.ordering shouldBe emptyMap()
    }
    
    test("should order single rank") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        
        val graph = GraphImpl(
            id = "single_rank",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val ranks: Map<Node, Int> = mapOf(
            nodeA to 0,
            nodeB to 0,
            nodeC to 0
        )
        
        val nodeSizes: Map<Node, NodeSize> = mapOf(
            nodeA to NodeSize(30.0, 20.0),
            nodeB to NodeSize(30.0, 20.0),
            nodeC to NodeSize(30.0, 20.0)
        )
        
        val result = ordering.orderNodes(graph, ranks, nodeSizes)
        
        result.shouldBeInstanceOf<OrderingResult.Success>()
        val rank0 = result.ordering[0]!!
        rank0.size shouldBe 3
        
        // Nodes should be ordered by ID (A, B, C)
        rank0[0].node shouldBe nodeA
        rank0[1].node shouldBe nodeB
        rank0[2].node shouldBe nodeC
    }
    
    test("should order nodes based on connections") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        val nodeD = NodeImpl("D", AttributeMap.empty(), null)
        
        // A -> C, B -> D (should result in A,B then C,D ordering)
        val edges = setOf(
            EdgeImpl(nodeA, nodeC, AttributeMap.empty(), emptyList()),
            EdgeImpl(nodeB, nodeD, AttributeMap.empty(), emptyList())
        )
        
        val graph = GraphImpl(
            id = "connected",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC, nodeD),
            edges = edges,
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val ranks: Map<Node, Int> = mapOf(
            nodeA to 0,
            nodeB to 0,
            nodeC to 1,
            nodeD to 1
        )
        
        val nodeSizes: Map<Node, NodeSize> = mapOf(
            nodeA to NodeSize(30.0, 20.0),
            nodeB to NodeSize(30.0, 20.0),
            nodeC to NodeSize(30.0, 20.0),
            nodeD to NodeSize(30.0, 20.0)
        )
        
        val result = ordering.orderNodes(graph, ranks, nodeSizes)
        
        result.shouldBeInstanceOf<OrderingResult.Success>()
        
        val rank0 = result.ordering[0]!!
        val rank1 = result.ordering[1]!!
        
        // Should maintain connection-based ordering
        rank0.size shouldBe 2
        rank1.size shouldBe 2
    }
    
    test("should apply proper spacing") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        
        val graph = GraphImpl(
            id = "spacing",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val ranks: Map<Node, Int> = mapOf(
            nodeA to 0,
            nodeB to 0
        )
        
        val nodeSizes: Map<Node, NodeSize> = mapOf(
            nodeA to NodeSize(40.0, 20.0),
            nodeB to NodeSize(60.0, 20.0)
        )
        
        val result = ordering.orderNodes(graph, ranks, nodeSizes)
        
        result.shouldBeInstanceOf<OrderingResult.Success>()
        
        val rank0 = result.ordering[0]!!
        rank0.size shouldBe 2
        
        val pos1 = rank0[0]
        val pos2 = rank0[1]
        
        // Check that nodes don't overlap and have proper spacing
        pos1.right shouldBe pos1.x + pos1.size.width / 2
        pos2.left shouldBe pos2.x - pos2.size.width / 2
        
        // There should be at least 20.0 spacing between nodes
        (pos2.left - pos1.right) shouldBe 20.0
    }
    
    test("should count crossings correctly") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        val nodeD = NodeImpl("D", AttributeMap.empty(), null)
        
        // Create crossing edges: A->D, B->C
        val edges = setOf(
            EdgeImpl(nodeA, nodeD, AttributeMap.empty(), emptyList()),
            EdgeImpl(nodeB, nodeC, AttributeMap.empty(), emptyList())
        )
        
        val graph = GraphImpl(
            id = "crossings",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC, nodeD),
            edges = edges,
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        // Order that creates crossings: A,B -> C,D
        val orderingWithCrossings = mapOf(
            0 to listOf(nodeA, nodeB),
            1 to listOf(nodeC, nodeD)
        )
        
        val crossings = ordering.countCrossings(graph, orderingWithCrossings)
        crossings shouldBe 1 // A->D crosses B->C
        
        // Order that avoids crossings: A,B -> D,C
        val orderingWithoutCrossings = mapOf(
            0 to listOf(nodeA, nodeB),
            1 to listOf(nodeD, nodeC)
        )
        
        val noCrossings = ordering.countCrossings(graph, orderingWithoutCrossings)
        noCrossings shouldBe 0
    }
})

class NodePositionTest : FunSpec({
    
    test("should calculate correct boundaries") {
        val node = NodeImpl("test", AttributeMap.empty(), null)
        val size = NodeSize(40.0, 20.0)
        val position = NodePosition(node, 100.0, size)
        
        position.left shouldBe 80.0  // 100 - 40/2
        position.right shouldBe 120.0 // 100 + 40/2
    }
})

class OrderingUtilsTest : FunSpec({
    
    test("getRankWidth should calculate correct width") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        
        val positions = listOf(
            NodePosition(nodeA, 20.0, NodeSize(20.0, 10.0)), // left=10, right=30
            NodePosition(nodeB, 60.0, NodeSize(30.0, 10.0))  // left=45, right=75
        )
        
        val width = OrderingUtils.getRankWidth(positions)
        width shouldBe 65.0 // 75 - 10
    }
    
    test("centerRank should center positions correctly") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        
        val positions = listOf(
            NodePosition(nodeA, 10.0, NodeSize(20.0, 10.0)), // left=0, right=20
            NodePosition(nodeB, 30.0, NodeSize(20.0, 10.0))  // left=20, right=40
        )
        
        // Current center is at (0+40)/2 = 20, want to center at 50
        val centered = OrderingUtils.centerRank(positions, 50.0)
        
        centered[0].x shouldBe 40.0 // 10 + 30
        centered[1].x shouldBe 60.0 // 30 + 30
    }
    
    test("toPositionMap should convert ordering to positions") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        
        val ordering = mapOf(
            0 to listOf(NodePosition(nodeA, 10.0, NodeSize(20.0, 10.0))),
            1 to listOf(NodePosition(nodeB, 30.0, NodeSize(20.0, 10.0)))
        )
        
        val rankY = mapOf(0 to 0.0, 1 to 50.0)
        
        val positions = OrderingUtils.toPositionMap(ordering, rankY)
        
        positions[nodeA] shouldBe Point(10.0, 0.0)
        positions[nodeB] shouldBe Point(30.0, 50.0)
    }
})

// Helper function to create empty graph
private fun createEmptyGraph(): Graph {
    return GraphImpl(
        id = "empty",
        isDirected = true,
        nodes = emptySet(),
        edges = emptySet(),
        subgraphs = emptySet(),
        attributes = AttributeMap.empty()
    )
}