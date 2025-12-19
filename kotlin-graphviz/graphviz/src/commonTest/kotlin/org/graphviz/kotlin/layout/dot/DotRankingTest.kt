package org.graphviz.kotlin.layout.dot

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.graphviz.kotlin.model.*

class DotRankingTest : FunSpec({
    
    val ranking = DotRanking()
    
    test("should handle empty graph") {
        val graph = createEmptyGraph()
        val result = ranking.assignRanks(graph)
        
        result.shouldBeInstanceOf<RankingResult.Success>()
        result.ranks shouldBe emptyMap()
        result.feedbackEdges shouldBe emptySet()
    }
    
    test("should rank single node") {
        val node = NodeImpl("A", AttributeMap.empty(), null)
        val graph = GraphImpl(
            id = "single",
            isDirected = true,
            nodes = setOf(node),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val result = ranking.assignRanks(graph)
        
        result.shouldBeInstanceOf<RankingResult.Success>()
        result.ranks[node] shouldBe 0
        result.feedbackEdges shouldBe emptySet()
    }
    
    test("should rank simple chain") {
        val (graph, nodes) = createChainGraph(3)
        val result = ranking.assignRanks(graph)
        
        result.shouldBeInstanceOf<RankingResult.Success>()
        result.ranks[nodes[0]] shouldBe 0 // First node
        result.ranks[nodes[1]] shouldBe 1 // Second node
        result.ranks[nodes[2]] shouldBe 2 // Third node
        result.feedbackEdges shouldBe emptySet()
    }
    
    test("should handle diamond graph") {
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
        
        val result = ranking.assignRanks(graph)
        
        result.shouldBeInstanceOf<RankingResult.Success>()
        result.ranks[nodeA] shouldBe 0 // Source
        result.ranks[nodeB] shouldBe 1 // Middle level
        result.ranks[nodeC] shouldBe 1 // Middle level
        result.ranks[nodeD] shouldBe 2 // Sink
        result.feedbackEdges shouldBe emptySet()
    }
    
    test("should detect and handle cycles") {
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
        
        val result = ranking.assignRanks(graph)
        
        result.shouldBeInstanceOf<RankingResult.Success>()
        result.feedbackEdges.size shouldBe 1 // One feedback edge should be identified
        
        // All nodes should have valid ranks
        result.ranks.values.forEach { rank ->
            rank shouldNotBe null
        }
    }
    
    test("should handle disconnected components") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        val nodeD = NodeImpl("D", AttributeMap.empty(), null)
        
        val edges = setOf(
            EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList()),
            EdgeImpl(nodeC, nodeD, AttributeMap.empty(), emptyList())
        )
        
        val graph = GraphImpl(
            id = "disconnected",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC, nodeD),
            edges = edges,
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val result = ranking.assignRanks(graph)
        
        result.shouldBeInstanceOf<RankingResult.Success>()
        
        // Each component should be ranked independently
        result.ranks[nodeA] shouldBe 0
        result.ranks[nodeB] shouldBe 1
        result.ranks[nodeC] shouldBe 0
        result.ranks[nodeD] shouldBe 1
        result.feedbackEdges shouldBe emptySet()
    }
})

class RankingUtilsTest : FunSpec({
    
    test("getNodesAtRank should return correct nodes") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        
        val ranks: Map<Node, Int> = mapOf(
            nodeA to 0,
            nodeB to 1,
            nodeC to 1
        )
        
        val rank0Nodes = RankingUtils.getNodesAtRank(ranks, 0)
        val rank1Nodes = RankingUtils.getNodesAtRank(ranks, 1)
        
        rank0Nodes shouldBe setOf(nodeA)
        rank1Nodes shouldBe setOf(nodeB, nodeC)
    }
    
    test("getMaxRank should return maximum rank") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        
        val ranks: Map<Node, Int> = mapOf(
            nodeA to 0,
            nodeB to 2,
            nodeC to 1
        )
        
        RankingUtils.getMaxRank(ranks) shouldBe 2
    }
    
    test("getMinRank should return minimum rank") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        
        val ranks: Map<Node, Int> = mapOf(
            nodeA to -1,
            nodeB to 2,
            nodeC to 1
        )
        
        RankingUtils.getMinRank(ranks) shouldBe -1
    }
    
    test("normalizeRanks should shift ranks to start from 0") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        
        val ranks: Map<Node, Int> = mapOf(
            nodeA to 5,
            nodeB to 7,
            nodeC to 6
        )
        
        val normalized = RankingUtils.normalizeRanks(ranks)
        
        normalized[nodeA] shouldBe 0
        normalized[nodeB] shouldBe 2
        normalized[nodeC] shouldBe 1
    }
    
    test("getEdgeSpan should calculate correct span") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val edge = EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList())
        
        val ranks: Map<Node, Int> = mapOf(
            nodeA to 1,
            nodeB to 3
        )
        
        val span = RankingUtils.getEdgeSpan(edge, ranks)
        span shouldBe 2
    }
    
    test("getTotalEdgeSpan should sum all edge spans") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        
        val edges = setOf(
            EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList()),
            EdgeImpl(nodeB, nodeC, AttributeMap.empty(), emptyList())
        )
        
        val graph = GraphImpl(
            id = "test",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC),
            edges = edges,
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val ranks: Map<Node, Int> = mapOf(
            nodeA to 0,
            nodeB to 1,
            nodeC to 3
        )
        
        val totalSpan = RankingUtils.getTotalEdgeSpan(graph, ranks)
        totalSpan shouldBe 3 // 1 + 2
    }
})

// Helper functions for creating test graphs
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