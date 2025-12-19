package org.graphviz.kotlin.demo

import io.kotest.core.spec.style.FunSpec
import org.graphviz.kotlin.GraphvizLibrary
import org.graphviz.kotlin.layout.LayoutEngineRegistry
import org.graphviz.kotlin.layout.LayoutOptions
import org.graphviz.kotlin.layout.LayoutResult
import org.graphviz.kotlin.model.*

/**
 * Demonstration of the complete dot layout engine functionality.
 * This shows how to use the library from initialization to final positioned graph.
 */
class DotLayoutDemo : FunSpec({
    
    test("demonstrate complete dot layout workflow") {
        println("\n=== Dot Layout Engine Demo ===")
        
        // Step 1: Initialize the library
        println("1. Initializing Graphviz library...")
        val initialized = GraphvizLibrary.initialize()
        println("   Library initialized: $initialized")
        println("   Available engines: ${GraphvizLibrary.getAvailableLayoutEngines()}")
        
        // Step 2: Create a sample graph
        println("\n2. Creating sample graph...")
        val graph = createSampleGraph()
        println("   Graph created with ${graph.getAllNodes().size} nodes and ${graph.getAllEdges().size} edges")
        
        // Step 3: Get the dot layout engine
        println("\n3. Getting dot layout engine...")
        val dotEngine = LayoutEngineRegistry.get("dot")
        println("   Dot engine: ${dotEngine?.name}")
        
        // Step 4: Apply layout
        println("\n4. Applying dot layout...")
        val layoutOptions = LayoutOptions(
            rankSpacing = 80.0,
            nodeSpacing = 50.0
        )
        
        val result = dotEngine!!.layout(graph, layoutOptions)
        
        when (result) {
            is LayoutResult.Success -> {
                println("   Layout successful!")
                val positionedGraph = result.graph
                
                // Step 5: Display results
                println("\n5. Layout Results:")
                println("   Node Positions:")
                positionedGraph.getAllNodes().forEach { node ->
                    val pos = node.position!!
                    println("     ${node.id}: (${pos.x}, ${pos.y})")
                }
                
                println("\n   Edge Control Points:")
                positionedGraph.getAllEdges().forEach { edge ->
                    println("     ${edge.source.id} -> ${edge.target.id}: ${edge.controlPoints.size} points")
                }
                
                // Step 6: Verify hierarchical layout
                println("\n6. Verifying hierarchical layout...")
                val nodesByY = positionedGraph.getAllNodes()
                    .groupBy { it.position!!.y }

                println("   Ranks (Y levels):")
                nodesByY.forEach { (y, nodes) ->
                    val nodeIds = nodes.map { it.id }.sorted()
                    println("     Y=$y: $nodeIds")
                }
                
                println("\n=== Demo Complete ===")
            }
            
            is LayoutResult.Error -> {
                println("   Layout failed: ${result.message}")
                result.cause?.let { println("   Cause: ${it.message}") }
            }
        }
    }
})

/**
 * Create a sample graph for demonstration.
 * This creates a diamond-shaped graph: A -> B,C -> D
 */
private fun createSampleGraph(): Graph {
    // Create nodes
    val nodeA = NodeImpl("A", AttributeMap.empty(), null)
    val nodeB = NodeImpl("B", AttributeMap.empty(), null)
    val nodeC = NodeImpl("C", AttributeMap.empty(), null)
    val nodeD = NodeImpl("D", AttributeMap.empty(), null)
    val nodeE = NodeImpl("E", AttributeMap.empty(), null)
    
    // Create edges to form a more complex hierarchy
    val edges = setOf(
        EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList()),
        EdgeImpl(nodeA, nodeC, AttributeMap.empty(), emptyList()),
        EdgeImpl(nodeB, nodeD, AttributeMap.empty(), emptyList()),
        EdgeImpl(nodeC, nodeD, AttributeMap.empty(), emptyList()),
        EdgeImpl(nodeD, nodeE, AttributeMap.empty(), emptyList())
    )
    
    return GraphImpl(
        id = "demo_graph",
        isDirected = true,
        nodes = setOf(nodeA, nodeB, nodeC, nodeD, nodeE),
        edges = edges,
        subgraphs = emptySet(),
        attributes = AttributeMap.empty()
    )
}