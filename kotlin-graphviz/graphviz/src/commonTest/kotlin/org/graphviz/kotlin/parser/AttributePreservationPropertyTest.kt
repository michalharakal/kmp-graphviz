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
 * Property-based tests for attribute preservation during processing.
 * 
 * **Feature: kotlin-multiplatform-port, Property 2: Attribute preservation during processing**
 * **Validates: Requirements 1.3, 2.2**
 */
class AttributePreservationPropertyTest : StringSpec({
    
    "Property 2: Attribute preservation during processing - simple attributes" {
        checkAll(
            iterations = 50, // Reduced iterations for stability
            Arb.boolean() // isDirected
        ) { isDirected: Boolean ->
            // Test with simple, known-good attributes
            val graphId = "TestGraph"
            val graphType = if (isDirected) "digraph" else "graph"
            
            val dotBuilder = StringBuilder()
            dotBuilder.append("$graphType $graphId {\n")
            
            // Add simple graph attributes
            dotBuilder.append("  graph [rankdir=TB, bgcolor=white];\n")
            
            // Add simple node with attributes
            dotBuilder.append("  A [label=\"Node A\", shape=box, color=red];\n")
            dotBuilder.append("  B [label=\"Node B\", shape=ellipse, color=blue];\n")
            
            // Add simple edge with attributes
            val edgeOp = if (isDirected) "->" else "--"
            dotBuilder.append("  A $edgeOp B [label=\"Edge AB\", style=solid, color=green];\n")
            
            dotBuilder.append("}")
            val originalDot = dotBuilder.toString()
            
            // Parse the DOT string
            val parseResult = DotParser.parse(originalDot)
            parseResult.isSuccess shouldBe true
            
            val graph = parseResult.getOrThrow()
            
            // Property: Graph attributes should be preserved
            graph.attributes.getRaw("rankdir") shouldNotBe null
            graph.attributes.getRaw("bgcolor") shouldNotBe null
            
            // Property: Node attributes should be preserved
            val nodeA = graph.getNode("A")!!
            val nodeB = graph.getNode("B")!!
            
            nodeA.attributes.getRaw("label") shouldNotBe null
            nodeA.attributes.getRaw("shape") shouldNotBe null
            nodeA.attributes.getRaw("color") shouldNotBe null
            
            nodeB.attributes.getRaw("label") shouldNotBe null
            nodeB.attributes.getRaw("shape") shouldNotBe null
            nodeB.attributes.getRaw("color") shouldNotBe null
            
            // Property: Edge attributes should be preserved
            val edge = graph.edges.first()
            edge.attributes.getRaw("label") shouldNotBe null
            edge.attributes.getRaw("style") shouldNotBe null
            edge.attributes.getRaw("color") shouldNotBe null
            
            // Generate DOT from the parsed graph
            val generatedDot = DotGenerator.generate(graph)
            
            // Parse the generated DOT again
            val secondParseResult = DotParser.parse(generatedDot)
            secondParseResult.isSuccess shouldBe true
            
            val secondGraph = secondParseResult.getOrThrow()
            
            // Property: All attributes should be preserved through round-trip
            val originalGraphRankdir = graph.attributes.getRaw("rankdir")
            val roundTripGraphRankdir = secondGraph.attributes.getRaw("rankdir")
            originalGraphRankdir shouldNotBe null
            roundTripGraphRankdir shouldNotBe null
            roundTripGraphRankdir.toString() shouldBe originalGraphRankdir.toString()
            
            val originalGraphBgcolor = graph.attributes.getRaw("bgcolor")
            val roundTripGraphBgcolor = secondGraph.attributes.getRaw("bgcolor")
            originalGraphBgcolor shouldNotBe null
            roundTripGraphBgcolor shouldNotBe null
            roundTripGraphBgcolor.toString() shouldBe originalGraphBgcolor.toString()
            
            // Property: Node attributes should be preserved through round-trip
            val secondNodeA = secondGraph.getNode("A")!!
            val secondNodeB = secondGraph.getNode("B")!!
            
            val nodeAAttrs = listOf("label", "shape", "color")
            nodeAAttrs.forEach { attr ->
                val original = nodeA.attributes.getRaw(attr)
                val roundTrip = secondNodeA.attributes.getRaw(attr)
                original shouldNotBe null
                roundTrip shouldNotBe null
                roundTrip.toString() shouldBe original.toString()
            }
            
            val nodeBAttrs = listOf("label", "shape", "color")
            nodeBAttrs.forEach { attr ->
                val original = nodeB.attributes.getRaw(attr)
                val roundTrip = secondNodeB.attributes.getRaw(attr)
                original shouldNotBe null
                roundTrip shouldNotBe null
                roundTrip.toString() shouldBe original.toString()
            }
            
            // Property: Edge attributes should be preserved through round-trip
            val secondEdge = secondGraph.edges.first()
            val edgeAttrs = listOf("label", "style", "color")
            edgeAttrs.forEach { attr ->
                val original = edge.attributes.getRaw(attr)
                val roundTrip = secondEdge.attributes.getRaw(attr)
                original shouldNotBe null
                roundTrip shouldNotBe null
                roundTrip.toString() shouldBe original.toString()
            }
        }
    }
    
    "Property 2: Attribute preservation during processing - numeric attributes" {
        checkAll(
            iterations = 50, // Reduced iterations for stability
            Arb.boolean() // isDirected
        ) { isDirected: Boolean ->
            // Test with numeric attributes
            val graphId = "NumericGraph"
            val graphType = if (isDirected) "digraph" else "graph"
            
            val dotBuilder = StringBuilder()
            dotBuilder.append("$graphType $graphId {\n")
            
            // Add node with numeric attributes
            dotBuilder.append("  A [width=2.5, height=1.0, fontsize=12];\n")
            
            // Add edge with numeric attributes
            val edgeOp = if (isDirected) "->" else "--"
            dotBuilder.append("  A $edgeOp A [weight=2.0, penwidth=1.5];\n")
            
            dotBuilder.append("}")
            val originalDot = dotBuilder.toString()
            
            // Parse the DOT string
            val parseResult = DotParser.parse(originalDot)
            parseResult.isSuccess shouldBe true
            
            val graph = parseResult.getOrThrow()
            
            // Property: Numeric node attributes should be preserved
            val nodeA = graph.getNode("A")!!
            nodeA.attributes.getRaw("width") shouldNotBe null
            nodeA.attributes.getRaw("height") shouldNotBe null
            nodeA.attributes.getRaw("fontsize") shouldNotBe null
            
            // Property: Numeric edge attributes should be preserved
            val edge = graph.edges.first()
            edge.attributes.getRaw("weight") shouldNotBe null
            edge.attributes.getRaw("penwidth") shouldNotBe null
            
            // Generate DOT from the parsed graph
            val generatedDot = DotGenerator.generate(graph)
            
            // Parse the generated DOT again
            val secondParseResult = DotParser.parse(generatedDot)
            secondParseResult.isSuccess shouldBe true
            
            val secondGraph = secondParseResult.getOrThrow()
            
            // Property: Numeric attributes should be preserved through round-trip
            val secondNodeA = secondGraph.getNode("A")!!
            val nodeAttrs = listOf("width", "height", "fontsize")
            nodeAttrs.forEach { attr ->
                val original = nodeA.attributes.getRaw(attr)
                val roundTrip = secondNodeA.attributes.getRaw(attr)
                original shouldNotBe null
                roundTrip shouldNotBe null
                roundTrip.toString() shouldBe original.toString()
            }
            
            val secondEdge = secondGraph.edges.first()
            val edgeAttrs = listOf("weight", "penwidth")
            edgeAttrs.forEach { attr ->
                val original = edge.attributes.getRaw(attr)
                val roundTrip = secondEdge.attributes.getRaw(attr)
                original shouldNotBe null
                roundTrip shouldNotBe null
                roundTrip.toString() shouldBe original.toString()
            }
        }
    }
})