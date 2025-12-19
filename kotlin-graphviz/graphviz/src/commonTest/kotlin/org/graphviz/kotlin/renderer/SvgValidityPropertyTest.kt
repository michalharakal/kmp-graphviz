package org.graphviz.kotlin.renderer

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.graphviz.kotlin.builder.*
import org.graphviz.kotlin.model.*

/**
 * Property-based tests for SVG validity and structure.
 * 
 * **Feature: kotlin-multiplatform-port, Property 5: SVG validity and structure**
 * **Validates: Requirements 4.1, 4.4**
 */
class SvgValidityPropertyTest : StringSpec({
    
    "Property 5: SVG validity and structure - basic test" {
        // Create a simple test graph
        val builder = GraphBuilder("test-graph", true)
        
        builder.node("A") {
            position(0.0, 0.0)
            label("Node A")
            shape("ellipse")
        }
        builder.node("B") {
            position(100.0, 50.0)
            label("Node B")
            shape("box")
        }
        
        builder.edge("A", "B") {
            controlPoints(listOf(Point(0.0, 0.0), Point(100.0, 50.0)))
            label("Edge A->B")
        }
        
        val graph = builder.build()
        val renderer = DefaultSvgRenderer()
        val options = RenderOptions.default()
        
        val svg = renderer.render(graph, options)
        
        // Property: SVG should be valid XML structure
        svg shouldContain "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        svg shouldContain "<!DOCTYPE svg"
        svg shouldContain "<svg"
        svg shouldContain "</svg>"
        
        // Property: SVG should have proper namespaces
        svg shouldContain "xmlns=\"http://www.w3.org/2000/svg\""
        svg shouldContain "xmlns:xlink=\"http://www.w3.org/1999/xlink\""
        svg shouldContain "version=\"1.1\""
        
        // Property: SVG should have proper viewport definition
        svg shouldContain "width=\""
        svg shouldContain "height=\""
        svg shouldContain "viewBox=\""
        
        // Property: SVG should contain node elements
        svg shouldContain "id=\"node-A\""
        svg shouldContain "id=\"node-B\""
        
        // Property: SVG should contain edge elements
        svg shouldContain "id=\"edge-A-B\""
    }
    
    "Property 5: SVG validity and structure - property test with multiple graphs" {
        checkAll(
            iterations = 100,
            Arb.int(1..5), // nodeCount
            Arb.int(0..3), // edgeCount  
            Arb.boolean()  // isDirected
        ) { nodeCount, edgeCount, isDirected ->
            // Create a test graph with the generated parameters
            val builder = GraphBuilder("test-graph", isDirected)
            
            // Add nodes with positions
            val nodeIds = mutableListOf<String>()
            repeat(nodeCount) { i ->
                val nodeId = "node_$i"
                nodeIds.add(nodeId)
                
                builder.node(nodeId) {
                    position(i * 100.0, i * 50.0)
                    label("Label $i")
                    shape("ellipse")
                }
            }
            
            // Add edges with control points
            repeat(minOf(edgeCount, nodeIds.size - 1)) { i ->
                if (nodeIds.size >= 2) {
                    val sourceId = nodeIds[i % nodeIds.size]
                    val targetId = nodeIds[(i + 1) % nodeIds.size]
                    
                    builder.edge(sourceId, targetId) {
                        controlPoints(listOf(
                            Point(i * 100.0, i * 50.0), 
                            Point((i + 1) * 100.0, (i + 1) * 50.0)
                        ))
                        label("Edge $i")
                    }
                }
            }
            
            val graph = builder.build()
            val renderer = DefaultSvgRenderer()
            val options = RenderOptions.default()
            
            val svg = renderer.render(graph, options)
            
            // Property: SVG should be valid XML structure
            svg shouldContain "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            svg shouldContain "<svg"
            svg shouldContain "</svg>"
            
            // Property: SVG should have proper namespaces
            svg shouldContain "xmlns=\"http://www.w3.org/2000/svg\""
            svg shouldContain "version=\"1.1\""
            
            // Property: SVG should have proper viewport definition
            svg shouldContain "width=\""
            svg shouldContain "height=\""
            
            // Property: ViewBox should have valid format (x y width height)
            val viewBoxRegex = """viewBox="([^"]+)"""".toRegex()
            viewBoxRegex.find(svg)?.let { match ->
                val viewBoxStr = match.groupValues[1]
                val parts = viewBoxStr.split(" ")
                parts.size shouldBe 4
                
                // Width and height should be positive
                val viewBoxWidth = parts[2].toDouble()
                val viewBoxHeight = parts[3].toDouble()
                (viewBoxWidth > 0.0) shouldBe true
                (viewBoxHeight > 0.0) shouldBe true
            }
        }
    }
})