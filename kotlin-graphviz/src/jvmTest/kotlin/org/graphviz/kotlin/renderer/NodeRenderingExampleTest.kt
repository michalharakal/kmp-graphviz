package org.graphviz.kotlin.renderer

import org.graphviz.kotlin.model.*
import kotlin.test.*

/**
 * Example test to demonstrate the enhanced node rendering capabilities.
 */
class NodeRenderingExampleTest {
    
    @Test
    fun testEnhancedNodeShapes() {
        val renderer = DefaultSvgRenderer()
        val options = RenderOptions(width = 800.0, height = 600.0)
        
        // Create nodes with different shapes
        val nodes = listOf(
            NodeImpl("box", AttributeMap.builder().set(AttributeKey.SHAPE, "box").set(AttributeKey.LABEL, "Box").build(), Point(100.0, 100.0)),
            NodeImpl("diamond", AttributeMap.builder().set(AttributeKey.SHAPE, "diamond").set(AttributeKey.LABEL, "Diamond").build(), Point(200.0, 100.0)),
            NodeImpl("triangle", AttributeMap.builder().set(AttributeKey.SHAPE, "triangle").set(AttributeKey.LABEL, "Triangle").build(), Point(300.0, 100.0)),
            NodeImpl("hexagon", AttributeMap.builder().set(AttributeKey.SHAPE, "hexagon").set(AttributeKey.LABEL, "Hexagon").build(), Point(400.0, 100.0)),
            NodeImpl("star", AttributeMap.builder().set(AttributeKey.SHAPE, "star").set(AttributeKey.LABEL, "Star").build(), Point(500.0, 100.0)),
            NodeImpl("pentagon", AttributeMap.builder().set(AttributeKey.SHAPE, "pentagon").set(AttributeKey.LABEL, "Pentagon").build(), Point(600.0, 100.0))
        )
        
        val graph = GraphImpl(
            id = "shape-demo",
            isDirected = false,
            nodes = nodes.toSet(),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.builder().set(AttributeKey.LABEL, "Node Shape Demo").build()
        )
        
        val svg = renderer.render(graph, options)
        
        // Verify all shapes are rendered
        assertTrue(svg.contains("<rect"), "Should contain rectangle for box shape")
        assertTrue(svg.contains("<polygon"), "Should contain polygons for complex shapes")
        assertTrue(svg.contains("Diamond"), "Should contain diamond label")
        assertTrue(svg.contains("Triangle"), "Should contain triangle label")
        assertTrue(svg.contains("Hexagon"), "Should contain hexagon label")
        assertTrue(svg.contains("Star"), "Should contain star label")
        assertTrue(svg.contains("Pentagon"), "Should contain pentagon label")
        
        // Print the SVG for manual inspection
        println("Generated SVG with enhanced node shapes:")
        println(svg)
    }
    
    @Test
    fun testStyledNodes() {
        val renderer = DefaultSvgRenderer()
        val options = RenderOptions.default()
        
        val styledNode = NodeImpl(
            id = "styled",
            attributes = AttributeMap.builder()
                .set(AttributeKey.SHAPE, "octagon")
                .set(AttributeKey.LABEL, "Styled Octagon")
                .set(AttributeKey.COLOR, Color.Named("red"))
                .set(AttributeKey.FILLCOLOR, Color.Hex("#ffff00"))
                .set(AttributeKey.FONTNAME, "Helvetica")
                .set(AttributeKey.FONTSIZE, 16.0)
                .build(),
            position = Point(150.0, 150.0)
        )
        
        val graph = GraphImpl(
            id = "styled-demo",
            isDirected = false,
            nodes = setOf(styledNode),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val svg = renderer.render(graph, options)
        
        // Verify styling is applied
        assertTrue(svg.contains("stroke=\"red\""))
        assertTrue(svg.contains("fill=\"#ffff00\""))
        assertTrue(svg.contains("font-family=\"Helvetica\""))
        assertTrue(svg.contains("font-size=\"16"))
        assertTrue(svg.contains("Styled Octagon"))
        
        println("Generated SVG with styled octagon:")
        println(svg)
    }
}