package org.graphviz.kotlin.integration

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.graphviz.kotlin.GraphvizLibrary
import org.graphviz.kotlin.layout.LayoutEngineRegistry
import org.graphviz.kotlin.layout.LayoutOptions
import org.graphviz.kotlin.layout.LayoutResult
import org.graphviz.kotlin.model.*

/**
 * Integration tests for the complete dot layout engine workflow.
 */
class DotLayoutIntegrationTest : FunSpec({
    
    beforeTest {
        // Ensure library is initialized for each test
        GraphvizLibrary.initialize()
    }
    
    test("complete workflow: create graph -> layout with dot -> verify positions") {
        // Create a simple directed graph
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
            id = "integration_test",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC, nodeD),
            edges = edges,
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        // Get the dot layout engine from the registry
        val dotEngine = LayoutEngineRegistry.get("dot")
        dotEngine shouldNotBe null
        
        // Apply layout
        val result = dotEngine!!.layout(graph, LayoutOptions())
        
        // Verify successful layout
        result.shouldBeInstanceOf<LayoutResult.Success>()
        val positionedGraph = result.graph
        
        // Verify all nodes have positions
        val positionedNodes = positionedGraph.getAllNodes()
        positionedNodes.size shouldBe 4
        
        positionedNodes.forEach { node ->
            node.position shouldNotBe null
            println("Node ${node.id}: ${node.position}")
        }
        
        // Verify all edges have control points
        val positionedEdges = positionedGraph.getAllEdges()
        positionedEdges.size shouldBe 4
        
        positionedEdges.forEach { edge ->
            edge.controlPoints.isNotEmpty() shouldBe true
            println("Edge ${edge.source.id} -> ${edge.target.id}: ${edge.controlPoints}")
        }
        
        // Verify hierarchical arrangement
        val nodeAPos = positionedNodes.find { it.id == "A" }!!.position!!
        val nodeBPos = positionedNodes.find { it.id == "B" }!!.position!!
        val nodeCPos = positionedNodes.find { it.id == "C" }!!.position!!
        val nodeDPos = positionedNodes.find { it.id == "D" }!!.position!!
        
        // A should be at the top (lowest Y)
        nodeAPos.y shouldBe 0.0
        
        // B and C should be at the same level (middle)
        nodeBPos.y shouldBe nodeCPos.y
        nodeBPos.y shouldBe 75.0 // Default rank spacing
        
        // D should be at the bottom (highest Y)
        nodeDPos.y shouldBe 150.0
        
        println("Layout completed successfully!")
        println("Node A: $nodeAPos")
        println("Node B: $nodeBPos") 
        println("Node C: $nodeCPos")
        println("Node D: $nodeDPos")
    }
    
    test("dot engine should handle complex graph with cycles") {
        // Create a graph with a cycle
        val nodes = (1..5).map { i ->
            NodeImpl("node$i", AttributeMap.empty(), null)
        }
        
        val edges = setOf(
            EdgeImpl(nodes[0], nodes[1], AttributeMap.empty(), emptyList()),
            EdgeImpl(nodes[1], nodes[2], AttributeMap.empty(), emptyList()),
            EdgeImpl(nodes[2], nodes[3], AttributeMap.empty(), emptyList()),
            EdgeImpl(nodes[3], nodes[4], AttributeMap.empty(), emptyList()),
            EdgeImpl(nodes[4], nodes[1], AttributeMap.empty(), emptyList()) // Creates cycle
        )
        
        val graph = GraphImpl(
            id = "cycle_test",
            isDirected = true,
            nodes = nodes.toSet(),
            edges = edges,
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val dotEngine = LayoutEngineRegistry.get("dot")!!
        val result = dotEngine.layout(graph, LayoutOptions())
        
        result.shouldBeInstanceOf<LayoutResult.Success>()
        val positionedGraph = result.graph
        
        // Should successfully position all nodes despite the cycle
        positionedGraph.getAllNodes().forEach { node ->
            node.position shouldNotBe null
        }
        
        // Should route all edges including the feedback edge
        positionedGraph.getAllEdges().forEach { edge ->
            edge.controlPoints.isNotEmpty() shouldBe true
        }
        
        println("Cycle graph layout completed successfully!")
    }
    
    test("dot engine should respect custom layout options") {
        val (graph, _) = createSimpleChain(3)
        
        val customOptions = LayoutOptions(
            rankSpacing = 120.0,
            nodeSpacing = 60.0,
            minNodeWidth = 50.0,
            minNodeHeight = 30.0
        )
        
        val dotEngine = LayoutEngineRegistry.get("dot")!!
        val result = dotEngine.layout(graph, customOptions)
        
        result.shouldBeInstanceOf<LayoutResult.Success>()
        val positionedGraph = result.graph
        
        // Check that custom rank spacing is applied
        val positions = positionedGraph.getAllNodes().map { it.position!! }
        val yCoordinates = positions.map { it.y }.distinct().sorted()
        
        if (yCoordinates.size > 1) {
            val spacing = yCoordinates[1] - yCoordinates[0]
            spacing shouldBe 120.0 // Custom rank spacing
        }
        
        println("Custom options layout completed successfully!")
    }
})

// Helper function to create a simple chain graph
private fun createSimpleChain(length: Int): Pair<Graph, List<Node>> {
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