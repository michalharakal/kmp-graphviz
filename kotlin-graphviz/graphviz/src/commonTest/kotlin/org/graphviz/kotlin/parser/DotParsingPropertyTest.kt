package org.graphviz.kotlin.parser

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.graphviz.kotlin.generator.DotGenerator
import org.graphviz.kotlin.model.*

/**
 * Property-based tests for DOT parsing completeness.
 * 
 * **Feature: kotlin-multiplatform-port, Property 1: DOT parsing round-trip consistency**
 * **Validates: Requirements 1.1, 2.4**
 */
class DotParsingPropertyTest : StringSpec({
    
    "Property 1: DOT parsing round-trip consistency - basic graphs" {
        checkAll(
            iterations = 100,
            Arb.boolean(), // isDirected
            Arb.int(1..5), // nodeCount
            Arb.int(0..3)  // edgeCount
        ) { isDirected, nodeCount, edgeCount ->
            // Generate a simple graph structure
            val graphId = "TestGraph"
            val nodeIds = (1..nodeCount).map { "node$it" }
            
            // Create DOT string manually for a simple graph
            val graphType = if (isDirected) "digraph" else "graph"
            val edgeOp = if (isDirected) "->" else "--"
            
            val dotBuilder = StringBuilder()
            dotBuilder.append("$graphType $graphId {\n")
            
            // Add nodes
            nodeIds.forEach { nodeId ->
                dotBuilder.append("  $nodeId;\n")
            }
            
            // Add edges (connect consecutive nodes)
            val actualEdgeCount = minOf(edgeCount, nodeIds.size - 1)
            repeat(actualEdgeCount) { i ->
                val source = nodeIds[i]
                val target = nodeIds[i + 1]
                dotBuilder.append("  $source $edgeOp $target;\n")
            }
            
            dotBuilder.append("}")
            val originalDot = dotBuilder.toString()
            
            // Parse the DOT string
            val parseResult = DotParser.parse(originalDot)
            parseResult.isSuccess shouldBe true
            
            val graph = parseResult.getOrThrow()
            
            // Verify basic graph properties
            graph.id shouldBe graphId
            graph.isDirected shouldBe isDirected
            graph.nodes.size shouldBe nodeCount
            graph.edges.size shouldBe actualEdgeCount
            
            // Generate DOT from the parsed graph
            val generatedDot = DotGenerator.generate(graph)
            
            // Parse the generated DOT again
            val secondParseResult = DotParser.parse(generatedDot)
            secondParseResult.isSuccess shouldBe true
            
            val secondGraph = secondParseResult.getOrThrow()
            
            // Property: Round-trip should preserve graph structure
            secondGraph.id shouldBe graph.id
            secondGraph.isDirected shouldBe graph.isDirected
            secondGraph.nodes.size shouldBe graph.nodes.size
            secondGraph.edges.size shouldBe graph.edges.size
            
            // Property: Node IDs should be preserved
            val originalNodeIds = graph.nodes.map { it.id }.toSet()
            val roundTripNodeIds = secondGraph.nodes.map { it.id }.toSet()
            roundTripNodeIds shouldBe originalNodeIds
            
            // Property: Edge connections should be preserved
            val originalEdges = graph.edges.map { "${it.source.id}-${it.target.id}" }.toSet()
            val roundTripEdges = secondGraph.edges.map { "${it.source.id}-${it.target.id}" }.toSet()
            roundTripEdges shouldBe originalEdges
        }
    }
    
    "Property 1: DOT parsing round-trip consistency - graphs with attributes" {
        checkAll(
            iterations = 100,
            Arb.boolean(), // isDirected
            Arb.int(1..3), // nodeCount
            Arb.int(0..2)  // edgeCount
        ) { isDirected, nodeCount, edgeCount ->
            // Generate a graph with attributes
            val graphId = "AttrGraph"
            val nodeIds = (1..nodeCount).map { "node$it" }
            
            val graphType = if (isDirected) "digraph" else "graph"
            val edgeOp = if (isDirected) "->" else "--"
            
            val dotBuilder = StringBuilder()
            dotBuilder.append("$graphType $graphId {\n")
            
            // Add graph attributes
            dotBuilder.append("  graph [rankdir=TB, bgcolor=white];\n")
            
            // Add nodes with attributes
            nodeIds.forEachIndexed { index, nodeId ->
                val shape = if (index % 2 == 0) "box" else "ellipse"
                val color = if (index % 2 == 0) "red" else "blue"
                dotBuilder.append("  $nodeId [shape=$shape, color=$color, label=\"Label $index\"];\n")
            }
            
            // Add edges with attributes
            val actualEdgeCount = minOf(edgeCount, nodeIds.size - 1)
            repeat(actualEdgeCount) { i ->
                val source = nodeIds[i]
                val target = nodeIds[i + 1]
                val style = if (i % 2 == 0) "solid" else "dashed"
                dotBuilder.append("  $source $edgeOp $target [style=$style, label=\"Edge $i\"];\n")
            }
            
            dotBuilder.append("}")
            val originalDot = dotBuilder.toString()
            
            // Parse the DOT string
            val parseResult = DotParser.parse(originalDot)
            parseResult.isSuccess shouldBe true
            
            val graph = parseResult.getOrThrow()
            
            // Generate DOT from the parsed graph
            val generatedDot = DotGenerator.generate(graph)
            
            // Parse the generated DOT again
            val secondParseResult = DotParser.parse(generatedDot)
            secondParseResult.isSuccess shouldBe true
            
            val secondGraph = secondParseResult.getOrThrow()
            
            // Property: Graph attributes should be preserved
            graph.attributes.getRaw("rankdir") shouldNotBe null
            graph.attributes.getRaw("bgcolor") shouldNotBe null
            secondGraph.attributes.getRaw("rankdir") shouldNotBe null
            secondGraph.attributes.getRaw("bgcolor") shouldNotBe null
            
            // Property: Node attributes should be preserved
            graph.nodes.forEach { node ->
                node.attributes.getRaw("shape") shouldNotBe null
                node.attributes.getRaw("color") shouldNotBe null
                node.attributes.getRaw("label") shouldNotBe null
            }
            
            secondGraph.nodes.forEach { node ->
                node.attributes.getRaw("shape") shouldNotBe null
                node.attributes.getRaw("color") shouldNotBe null
                node.attributes.getRaw("label") shouldNotBe null
            }
            
            // Property: Edge attributes should be preserved
            if (graph.edges.isNotEmpty()) {
                graph.edges.forEach { edge ->
                    edge.attributes.getRaw("style") shouldNotBe null
                    edge.attributes.getRaw("label") shouldNotBe null
                }
                
                secondGraph.edges.forEach { edge ->
                    edge.attributes.getRaw("style") shouldNotBe null
                    edge.attributes.getRaw("label") shouldNotBe null
                }
            }
        }
    }
    
    "Property 1: DOT parsing round-trip consistency - graphs with subgraphs" {
        checkAll(
            iterations = 50, // Fewer iterations for complex structures
            Arb.boolean() // isDirected
        ) { isDirected ->
            // Generate a graph with subgraphs
            val graphId = "MainGraph"
            val graphType = if (isDirected) "digraph" else "graph"
            val edgeOp = if (isDirected) "->" else "--"
            
            val dotBuilder = StringBuilder()
            dotBuilder.append("$graphType $graphId {\n")
            
            // Add main graph nodes
            dotBuilder.append("  A;\n")
            dotBuilder.append("  B;\n")
            
            // Add subgraph
            dotBuilder.append("  subgraph cluster_0 {\n")
            dotBuilder.append("    label=\"Subgraph\";\n")
            dotBuilder.append("    C;\n")
            dotBuilder.append("    D;\n")
            dotBuilder.append("    C $edgeOp D;\n")
            dotBuilder.append("  }\n")
            
            // Connect main graph to subgraph
            dotBuilder.append("  A $edgeOp C;\n")
            dotBuilder.append("  D $edgeOp B;\n")
            
            dotBuilder.append("}")
            val originalDot = dotBuilder.toString()
            
            // Parse the DOT string
            val parseResult = DotParser.parse(originalDot)
            parseResult.isSuccess shouldBe true
            
            val graph = parseResult.getOrThrow()
            
            // Verify subgraph structure
            graph.subgraphs.size shouldBe 1
            val subgraph = graph.subgraphs.first()
            subgraph.id shouldBe "cluster_0"
            subgraph.nodes.size shouldBe 2
            subgraph.edges.size shouldBe 1
            
            // Generate DOT from the parsed graph
            val generatedDot = DotGenerator.generate(graph)
            
            // Parse the generated DOT again
            val secondParseResult = DotParser.parse(generatedDot)
            secondParseResult.isSuccess shouldBe true
            
            val secondGraph = secondParseResult.getOrThrow()
            
            // Property: Subgraph structure should be preserved
            secondGraph.subgraphs.size shouldBe graph.subgraphs.size
            
            if (secondGraph.subgraphs.isNotEmpty()) {
                val secondSubgraph = secondGraph.subgraphs.first()
                secondSubgraph.nodes.size shouldBe subgraph.nodes.size
                secondSubgraph.edges.size shouldBe subgraph.edges.size
                
                // Property: Subgraph node IDs should be preserved
                val originalSubgraphNodeIds = subgraph.nodes.map { it.id }.toSet()
                val roundTripSubgraphNodeIds = secondSubgraph.nodes.map { it.id }.toSet()
                roundTripSubgraphNodeIds shouldBe originalSubgraphNodeIds
            }
        }
    }
    
    "Property 1: DOT parsing round-trip consistency - edge cases" {
        checkAll(
            iterations = 100,
            Arb.boolean() // isDirected
        ) { isDirected ->
            // Test edge cases like empty graphs, single nodes, etc.
            val graphType = if (isDirected) "digraph" else "graph"
            
            // Test empty graph
            val emptyDot = "$graphType EmptyGraph {}"
            val emptyParseResult = DotParser.parse(emptyDot)
            emptyParseResult.isSuccess shouldBe true
            
            val emptyGraph = emptyParseResult.getOrThrow()
            emptyGraph.nodes.size shouldBe 0
            emptyGraph.edges.size shouldBe 0
            
            // Round-trip empty graph
            val emptyGeneratedDot = DotGenerator.generate(emptyGraph)
            val emptySecondParseResult = DotParser.parse(emptyGeneratedDot)
            emptySecondParseResult.isSuccess shouldBe true
            
            val emptySecondGraph = emptySecondParseResult.getOrThrow()
            emptySecondGraph.nodes.size shouldBe emptyGraph.nodes.size
            emptySecondGraph.edges.size shouldBe emptyGraph.edges.size
            
            // Test single node graph
            val singleNodeDot = "$graphType SingleNode { A; }"
            val singleParseResult = DotParser.parse(singleNodeDot)
            singleParseResult.isSuccess shouldBe true
            
            val singleGraph = singleParseResult.getOrThrow()
            singleGraph.nodes.size shouldBe 1
            singleGraph.edges.size shouldBe 0
            
            // Round-trip single node graph
            val singleGeneratedDot = DotGenerator.generate(singleGraph)
            val singleSecondParseResult = DotParser.parse(singleGeneratedDot)
            singleSecondParseResult.isSuccess shouldBe true
            
            val singleSecondGraph = singleSecondParseResult.getOrThrow()
            singleSecondGraph.nodes.size shouldBe singleGraph.nodes.size
            singleSecondGraph.edges.size shouldBe singleGraph.edges.size
            
            // Property: Node ID should be preserved
            val originalNodeId = singleGraph.nodes.first().id
            val roundTripNodeId = singleSecondGraph.nodes.first().id
            roundTripNodeId shouldBe originalNodeId
        }
    }
})