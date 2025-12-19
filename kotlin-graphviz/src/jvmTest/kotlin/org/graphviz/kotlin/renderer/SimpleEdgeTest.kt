package org.graphviz.kotlin.renderer

import org.graphviz.kotlin.model.*
import kotlin.test.*

/**
 * Simple test for edge rendering functionality.
 */
class SimpleEdgeTest {
    
    @Test
    fun testEdgeRenderingInSvg() {
        // Create a simple graph with an edge
        val nodeA = NodeImpl("A", AttributeMap.empty(), Point(0.0, 0.0))
        val nodeB = NodeImpl("B", AttributeMap.empty(), Point(100.0, 0.0))
        
        val edge = EdgeImpl(
            source = nodeA,
            target = nodeB,
            attributes = AttributeMap.builder()
                .set(AttributeKey.COLOR, Color.Named("blue"))
                .build(),
            controlPoints = listOf(Point(0.0, 0.0), Point(100.0, 0.0))
        )
        
        val graph = GraphImpl(
            id = "simple-test",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        // Render the graph
        val renderer = DefaultSvgRenderer()
        val svg = renderer.render(graph, RenderOptions.default())
        
        // Basic checks
        assertTrue(svg.isNotEmpty())
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("</svg>"))
        
        // Check for edge-related content
        assertTrue(svg.contains("edge") || svg.contains("path"))
        
        println("Generated SVG contains edge rendering:")
        println(svg)
    }
}