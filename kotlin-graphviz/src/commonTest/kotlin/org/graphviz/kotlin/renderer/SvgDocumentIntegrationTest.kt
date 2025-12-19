package org.graphviz.kotlin.renderer

import org.graphviz.kotlin.model.*
import kotlin.test.*

/**
 * Integration test to verify the complete SVG document generation workflow.
 */
class SvgDocumentIntegrationTest {
    
    @Test
    fun testCompleteWorkflow() {
        // Create a simple graph
        val nodeA = NodeImpl(
            id = "A",
            attributes = AttributeMap.builder()
                .set(AttributeKey.LABEL, "Node A")
                .set(AttributeKey.SHAPE, "ellipse")
                .set(AttributeKey.COLOR, Color.Named("blue"))
                .build(),
            position = Point(50.0, 100.0)
        )
        
        val nodeB = NodeImpl(
            id = "B", 
            attributes = AttributeMap.builder()
                .set(AttributeKey.LABEL, "Node B")
                .set(AttributeKey.SHAPE, "box")
                .set(AttributeKey.FILLCOLOR, Color.Hex("#ff0000"))
                .build(),
            position = Point(150.0, 100.0)
        )
        
        val edge = EdgeImpl(
            source = nodeA,
            target = nodeB,
            attributes = AttributeMap.builder()
                .set(AttributeKey.LABEL, "Edge A->B")
                .set(AttributeKey.COLOR, Color.Named("green"))
                .build(),
            controlPoints = listOf(Point(50.0, 100.0), Point(100.0, 100.0), Point(150.0, 100.0))
        )
        
        val graph = GraphImpl(
            id = "test-graph",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.builder()
                .set(AttributeKey.LABEL, "Test Graph")
                .build()
        )
        
        // Render using DefaultSvgRenderer
        val renderer = DefaultSvgRenderer()
        val options = RenderOptions(
            width = 300.0,
            height = 200.0,
            margin = 20.0,
            backgroundColor = "white",
            rendererOptions = mapOf(
                "title" to "Integration Test Graph",
                "description" to "A test graph demonstrating SVG document structure generation"
            )
        )
        
        val svg = renderer.render(graph, options)
        
        // Verify the SVG contains expected elements
        assertTrue(svg.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(svg.contains("<!DOCTYPE svg"))
        assertTrue(svg.contains("xmlns=\"http://www.w3.org/2000/svg\""))
        assertTrue(svg.contains("width=\"300\""))
        assertTrue(svg.contains("height=\"200\""))
        assertTrue(svg.contains("viewBox="))
        assertTrue(svg.contains("<title>Integration Test Graph</title>"))
        assertTrue(svg.contains("<desc>A test graph demonstrating SVG document structure generation</desc>"))
        assertTrue(svg.contains("<metadata>"))
        assertTrue(svg.contains("generator"))
        assertTrue(svg.contains("Graphviz Kotlin Multiplatform"))
        assertTrue(svg.contains("id=\"node-A\""))
        assertTrue(svg.contains("id=\"node-B\""))
        assertTrue(svg.contains("id=\"edge-A-B\""))
        
        // Verify coordinate transformation is applied
        assertTrue(svg.contains("transform=") || svg.contains("viewBox="))
        
        println("Generated SVG:")
        println(svg)
    }
    
    @Test
    fun testCoordinateSystemIntegration() {
        // Create a graph with a large coordinate space
        val nodeA = NodeImpl("A", AttributeMap.empty(), Point(0.0, 0.0))
        val nodeB = NodeImpl("B", AttributeMap.empty(), Point(1000.0, 1000.0))
        val edge = EdgeImpl(nodeA, nodeB, AttributeMap.empty(), listOf(Point(0.0, 0.0), Point(1000.0, 1000.0)))
        
        val graph = GraphImpl(
            id = "large-graph",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        // Render to a small viewport
        val renderer = DefaultSvgRenderer()
        val options = RenderOptions(width = 200.0, height = 200.0, margin = 10.0)
        
        val svg = renderer.render(graph, options)
        
        // Should contain scaling transformation
        assertTrue(svg.contains("transform=") && svg.contains("scale"))
        
        // Should fit within the specified dimensions
        assertTrue(svg.contains("width=\"200\""))
        assertTrue(svg.contains("height=\"200\""))
    }
}