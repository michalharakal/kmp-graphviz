package org.graphviz.kotlin.parser

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.graphviz.kotlin.generator.DotGenerator
import org.graphviz.kotlin.model.*

/**
 * Property-based tests for hierarchical structure preservation.
 * 
 * **Feature: kotlin-multiplatform-port, Property 3: Hierarchical structure preservation**
 * **Validates: Requirements 1.4, 2.2**
 */
class HierarchicalStructurePropertyTest : StringSpec({
    
    "Property 3: Hierarchical structure preservation - single level subgraphs" {
        checkAll(
            iterations = 100,
            Arb.boolean(), // isDirected
            Arb.int(1..3), // number of subgraphs
            Arb.int(1..3)  // nodes per subgraph
        ) { isDirected, subgraphCount, nodesPerSubgraph ->
            // Generate a graph with subgraphs
            val graphId = "MainGraph"
            val graphType = if (isDirected) "digraph" else "graph"
            val edgeOp = if (isDirected) "->" else "--"
            
            val dotBuilder = StringBuilder()
            dotBuilder.append("$graphType $graphId {\n")
            
            // Add main graph nodes
            dotBuilder.append("  main_node_1;\n")
            dotBuilder.append("  main_node_2;\n")
            
            // Add subgraphs
            repeat(subgraphCount) { subgraphIndex ->
                val subgraphId = "cluster_$subgraphIndex"
                dotBuilder.append("  subgraph $subgraphId {\n")
                dotBuilder.append("    label=\"Subgraph $subgraphIndex\";\n")
                
                // Add nodes to subgraph
                val subgraphNodes = (1..nodesPerSubgraph).map { "sub${subgraphIndex}_node$it" }
                subgraphNodes.forEach { nodeId ->
                    dotBuilder.append("    $nodeId;\n")
                }
                
                // Add edges within subgraph
                if (subgraphNodes.size > 1) {
                    dotBuilder.append("    ${subgraphNodes[0]} $edgeOp ${subgraphNodes[1]};\n")
                }
                
                dotBuilder.append("  }\n")
            }
            
            dotBuilder.append("}")
            val originalDot = dotBuilder.toString()
            
            // Parse the DOT string
            val parseResult = DotParser.parse(originalDot)
            parseResult.isSuccess shouldBe true
            
            val graph = parseResult.getOrThrow()
            
            // Property: Subgraph count should be preserved
            graph.subgraphs.size shouldBe subgraphCount
            
            // Property: Each subgraph should have the correct number of nodes
            graph.subgraphs.forEachIndexed { index, subgraph ->
                subgraph.nodes.size shouldBe nodesPerSubgraph
                subgraph.id shouldBe "cluster_$index"
                
                // Property: Subgraph attributes should be preserved
                subgraph.attributes.getRaw("label") shouldNotBe null
            }
            
            // Property: Main graph nodes should be preserved (only the explicitly declared ones)
            graph.nodes.size shouldBe 2
            graph.getNode("main_node_1") shouldNotBe null
            graph.getNode("main_node_2") shouldNotBe null
            
            // Generate DOT from the parsed graph
            val generatedDot = DotGenerator.generate(graph)
            
            // Parse the generated DOT again
            val secondParseResult = DotParser.parse(generatedDot)
            secondParseResult.isSuccess shouldBe true
            
            val secondGraph = secondParseResult.getOrThrow()
            
            // Property: Hierarchical structure should be preserved through round-trip
            secondGraph.subgraphs.size shouldBe graph.subgraphs.size
            secondGraph.nodes.size shouldBe graph.nodes.size
            
            // Property: Subgraph structure should match
            val originalSubgraphIds = graph.subgraphs.map { it.id }.toSet()
            val roundTripSubgraphIds = secondGraph.subgraphs.map { it.id }.toSet()
            roundTripSubgraphIds shouldBe originalSubgraphIds
            
            // Property: Each subgraph's node count should be preserved
            graph.subgraphs.forEach { originalSubgraph ->
                val roundTripSubgraph = secondGraph.subgraphs.find { it.id == originalSubgraph.id }
                roundTripSubgraph shouldNotBe null
                roundTripSubgraph!!.nodes.size shouldBe originalSubgraph.nodes.size
                
                // Property: Subgraph node IDs should be preserved
                val originalNodeIds = originalSubgraph.nodes.map { it.id }.toSet()
                val roundTripNodeIds = roundTripSubgraph.nodes.map { it.id }.toSet()
                roundTripNodeIds shouldBe originalNodeIds
            }
        }
    }
    
    "Property 3: Hierarchical structure preservation - nested subgraphs" {
        checkAll(
            iterations = 50, // Fewer iterations for complex structures
            Arb.boolean() // isDirected
        ) { isDirected ->
            // Generate a graph with nested subgraphs
            val graphId = "NestedGraph"
            val graphType = if (isDirected) "digraph" else "graph"
            val edgeOp = if (isDirected) "->" else "--"
            
            val dotBuilder = StringBuilder()
            dotBuilder.append("$graphType $graphId {\n")
            
            // Add main graph node
            dotBuilder.append("  root;\n")
            
            // Add outer subgraph
            dotBuilder.append("  subgraph cluster_outer {\n")
            dotBuilder.append("    label=\"Outer\";\n")
            dotBuilder.append("    outer_node;\n")
            
            // Add nested inner subgraph
            dotBuilder.append("    subgraph cluster_inner {\n")
            dotBuilder.append("      label=\"Inner\";\n")
            dotBuilder.append("      inner_node_1;\n")
            dotBuilder.append("      inner_node_2;\n")
            dotBuilder.append("      inner_node_1 $edgeOp inner_node_2;\n")
            dotBuilder.append("    }\n")
            
            dotBuilder.append("    outer_node $edgeOp inner_node_1;\n")
            dotBuilder.append("  }\n")
            
            dotBuilder.append("  root $edgeOp outer_node;\n")
            dotBuilder.append("}")
            val originalDot = dotBuilder.toString()
            
            // Parse the DOT string
            val parseResult = DotParser.parse(originalDot)
            parseResult.isSuccess shouldBe true
            
            val graph = parseResult.getOrThrow()
            
            // Property: Outer subgraph should exist
            graph.subgraphs.size shouldBe 1
            val outerSubgraph = graph.subgraphs.first()
            outerSubgraph.id shouldBe "cluster_outer"
            
            // Property: Nested subgraph should exist
            outerSubgraph.subgraphs.size shouldBe 1
            val innerSubgraph = outerSubgraph.subgraphs.first()
            innerSubgraph.id shouldBe "cluster_inner"
            
            // Property: Inner subgraph should have correct nodes
            innerSubgraph.nodes.size shouldBe 2
            innerSubgraph.getNode("inner_node_1") shouldNotBe null
            innerSubgraph.getNode("inner_node_2") shouldNotBe null
            
            // Property: Inner subgraph should have correct edges
            innerSubgraph.edges.size shouldBe 1
            
            // Generate DOT from the parsed graph
            val generatedDot = DotGenerator.generate(graph)
            
            // Parse the generated DOT again
            val secondParseResult = DotParser.parse(generatedDot)
            secondParseResult.isSuccess shouldBe true
            
            val secondGraph = secondParseResult.getOrThrow()
            
            // Property: Nested structure should be preserved through round-trip
            secondGraph.subgraphs.size shouldBe graph.subgraphs.size
            
            val secondOuterSubgraph = secondGraph.subgraphs.first()
            secondOuterSubgraph.id shouldBe outerSubgraph.id
            secondOuterSubgraph.subgraphs.size shouldBe outerSubgraph.subgraphs.size
            
            val secondInnerSubgraph = secondOuterSubgraph.subgraphs.first()
            secondInnerSubgraph.id shouldBe innerSubgraph.id
            secondInnerSubgraph.nodes.size shouldBe innerSubgraph.nodes.size
            secondInnerSubgraph.edges.size shouldBe innerSubgraph.edges.size
            
            // Property: Node IDs at each level should be preserved
            val originalInnerNodeIds = innerSubgraph.nodes.map { it.id }.toSet()
            val roundTripInnerNodeIds = secondInnerSubgraph.nodes.map { it.id }.toSet()
            roundTripInnerNodeIds shouldBe originalInnerNodeIds
        }
    }
    
    "Property 3: Hierarchical structure preservation - subgraph attributes" {
        checkAll(
            iterations = 100,
            Arb.boolean(), // isDirected
            Arb.int(1..3)  // number of subgraphs
        ) { isDirected, subgraphCount ->
            // Generate a graph with subgraphs that have various attributes
            val graphId = "AttrGraph"
            val graphType = if (isDirected) "digraph" else "graph"
            
            val dotBuilder = StringBuilder()
            dotBuilder.append("$graphType $graphId {\n")
            
            // Add subgraphs with attributes
            repeat(subgraphCount) { index ->
                val subgraphId = "cluster_$index"
                dotBuilder.append("  subgraph $subgraphId {\n")
                dotBuilder.append("    label=\"Subgraph $index\";\n")
                dotBuilder.append("    style=filled;\n")
                dotBuilder.append("    color=lightgrey;\n")
                dotBuilder.append("    node_$index;\n")
                dotBuilder.append("  }\n")
            }
            
            dotBuilder.append("}")
            val originalDot = dotBuilder.toString()
            
            // Parse the DOT string
            val parseResult = DotParser.parse(originalDot)
            parseResult.isSuccess shouldBe true
            
            val graph = parseResult.getOrThrow()
            
            // Property: Subgraph attributes should be preserved
            graph.subgraphs.forEach { subgraph ->
                subgraph.attributes.getRaw("label") shouldNotBe null
                subgraph.attributes.getRaw("style") shouldNotBe null
                subgraph.attributes.getRaw("color") shouldNotBe null
            }
            
            // Generate DOT from the parsed graph
            val generatedDot = DotGenerator.generate(graph)
            
            // Parse the generated DOT again
            val secondParseResult = DotParser.parse(generatedDot)
            secondParseResult.isSuccess shouldBe true
            
            val secondGraph = secondParseResult.getOrThrow()
            
            // Property: Subgraph attributes should be preserved through round-trip
            graph.subgraphs.forEach { originalSubgraph ->
                val roundTripSubgraph = secondGraph.subgraphs.find { it.id == originalSubgraph.id }
                roundTripSubgraph shouldNotBe null
                
                // Check each attribute
                val attributeKeys = listOf("label", "style", "color")
                attributeKeys.forEach { key ->
                    val originalValue = originalSubgraph.attributes.getRaw(key)
                    val roundTripValue = roundTripSubgraph!!.attributes.getRaw(key)
                    
                    originalValue shouldNotBe null
                    roundTripValue shouldNotBe null
                    roundTripValue.toString() shouldBe originalValue.toString()
                }
            }
        }
    }
    
    "Property 3: Hierarchical structure preservation - edges across hierarchy levels" {
        checkAll(
            iterations = 100,
            Arb.boolean() // isDirected
        ) { isDirected ->
            // Generate a graph with edges connecting nodes at different hierarchy levels
            val graphId = "CrossLevelGraph"
            val graphType = if (isDirected) "digraph" else "graph"
            val edgeOp = if (isDirected) "->" else "--"
            
            val dotBuilder = StringBuilder()
            dotBuilder.append("$graphType $graphId {\n")
            
            // Main graph nodes
            dotBuilder.append("  main_a;\n")
            dotBuilder.append("  main_b;\n")
            
            // Subgraph with nodes
            dotBuilder.append("  subgraph cluster_sub {\n")
            dotBuilder.append("    label=\"Sub\";\n")
            dotBuilder.append("    sub_a;\n")
            dotBuilder.append("    sub_b;\n")
            dotBuilder.append("    sub_a $edgeOp sub_b;\n")
            dotBuilder.append("  }\n")
            
            // Edges connecting main graph to subgraph
            dotBuilder.append("  main_a $edgeOp sub_a;\n")
            dotBuilder.append("  sub_b $edgeOp main_b;\n")
            
            // Edge within main graph
            dotBuilder.append("  main_a $edgeOp main_b;\n")
            
            dotBuilder.append("}")
            val originalDot = dotBuilder.toString()
            
            // Parse the DOT string
            val parseResult = DotParser.parse(originalDot)
            parseResult.isSuccess shouldBe true
            
            val graph = parseResult.getOrThrow()
            
            // Property: All nodes should be accessible
            val allNodes = graph.getAllNodes()
            allNodes.size shouldBe 4
            
            // Property: All edges should be preserved
            val allEdges = graph.getAllEdges()
            allEdges.size shouldBe 4
            
            // Property: Edges should connect correct nodes
            val edgeConnections = allEdges.map { "${it.source.id}-${it.target.id}" }.toSet()
            edgeConnections.contains("main_a-sub_a") shouldBe true
            edgeConnections.contains("sub_b-main_b") shouldBe true
            edgeConnections.contains("main_a-main_b") shouldBe true
            edgeConnections.contains("sub_a-sub_b") shouldBe true
            
            // Generate DOT from the parsed graph
            val generatedDot = DotGenerator.generate(graph)
            
            // Parse the generated DOT again
            val secondParseResult = DotParser.parse(generatedDot)
            secondParseResult.isSuccess shouldBe true
            
            val secondGraph = secondParseResult.getOrThrow()
            
            // Property: All nodes should be preserved through round-trip
            val secondAllNodes = secondGraph.getAllNodes()
            secondAllNodes.size shouldBe allNodes.size
            
            val originalNodeIds = allNodes.map { it.id }.toSet()
            val roundTripNodeIds = secondAllNodes.map { it.id }.toSet()
            roundTripNodeIds shouldBe originalNodeIds
            
            // Property: All edges should be preserved through round-trip
            val secondAllEdges = secondGraph.getAllEdges()
            secondAllEdges.size shouldBe allEdges.size
            
            val secondEdgeConnections = secondAllEdges.map { "${it.source.id}-${it.target.id}" }.toSet()
            secondEdgeConnections shouldBe edgeConnections
        }
    }
    
    "Property 3: Hierarchical structure preservation - empty subgraphs" {
        checkAll(
            iterations = 100,
            Arb.boolean() // isDirected
        ) { isDirected ->
            // Test edge case: empty subgraphs
            val graphId = "EmptySubGraph"
            val graphType = if (isDirected) "digraph" else "graph"
            
            val dotBuilder = StringBuilder()
            dotBuilder.append("$graphType $graphId {\n")
            dotBuilder.append("  main_node;\n")
            dotBuilder.append("  subgraph cluster_empty {\n")
            dotBuilder.append("    label=\"Empty\";\n")
            dotBuilder.append("  }\n")
            dotBuilder.append("}")
            val originalDot = dotBuilder.toString()
            
            // Parse the DOT string
            val parseResult = DotParser.parse(originalDot)
            parseResult.isSuccess shouldBe true
            
            val graph = parseResult.getOrThrow()
            
            // Property: Empty subgraph should exist
            graph.subgraphs.size shouldBe 1
            val subgraph = graph.subgraphs.first()
            subgraph.id shouldBe "cluster_empty"
            subgraph.nodes.size shouldBe 0
            subgraph.edges.size shouldBe 0
            
            // Property: Subgraph attributes should be preserved
            subgraph.attributes.getRaw("label") shouldNotBe null
            
            // Generate DOT from the parsed graph
            val generatedDot = DotGenerator.generate(graph)
            
            // Parse the generated DOT again
            val secondParseResult = DotParser.parse(generatedDot)
            secondParseResult.isSuccess shouldBe true
            
            val secondGraph = secondParseResult.getOrThrow()
            
            // Property: Empty subgraph structure should be preserved
            secondGraph.subgraphs.size shouldBe graph.subgraphs.size
            val secondSubgraph = secondGraph.subgraphs.first()
            secondSubgraph.id shouldBe subgraph.id
            secondSubgraph.nodes.size shouldBe subgraph.nodes.size
            secondSubgraph.edges.size shouldBe subgraph.edges.size
            
            // Property: Empty subgraph attributes should be preserved
            val originalLabel = subgraph.attributes.getRaw("label")
            val roundTripLabel = secondSubgraph.attributes.getRaw("label")
            originalLabel shouldNotBe null
            roundTripLabel shouldNotBe null
            roundTripLabel.toString() shouldBe originalLabel.toString()
        }
    }
})
