package org.graphviz.kotlin.renderer

import org.graphviz.kotlin.model.*
import kotlin.test.*

/**
 * Test for node rendering functionality (Task 8.3).
 */
class NodeRenderingTest {
    
    @Test
    fun testBasicNodeShapes() {
        val renderer = DefaultSvgRenderer()
        val options = RenderOptions.default()
        
        // Test different node shapes
        val shapes = listOf("box", "ellipse", "circle", "diamond", "triangle", "hexagon", "star")
        
        for (shape in shapes) {
            val node = NodeImpl(
                id = "test-$shape",
                attributes = AttributeMap.builder()
                    .set(AttributeKey.SHAPE, shape)
                    .set(AttributeKey.LABEL, "Test $shape")
                    .build(),
                position = Point(50.0, 50.0)
            )
            
            val graph = GraphImpl(
                id = "test-graph",
                isDirected = true,
                nodes = setOf(node),
                edges = emptySet(),
                subgraphs = emptySet(),
                attributes = AttributeMap.empty()
            )
            
            val svg = renderer.render(graph, options)
            
            // Should contain the node ID
            assertTrue(svg.contains("id=\"node-test-$shape\""), "SVG should contain node ID for $shape")
            
            // Should contain the label text
            assertTrue(svg.contains("Test $shape"), "SVG should contain label text for $shape")
        }
    }
    
    @Test
    fun testNodeAttributes() {
        val renderer = DefaultSvgRenderer()
        val options = RenderOptions.default()
        
        val node = NodeImpl(
            id = "styled-node",
            attributes = AttributeMap.builder()
                .set(AttributeKey.SHAPE, "box")
                .set(AttributeKey.COLOR, Color.Named("blue"))
                .set(AttributeKey.FILLCOLOR, Color.Hex("#ff0000"))
                .set(AttributeKey.LABEL, "Styled Node")
                .set(AttributeKey.FONTSIZE, 14.0)
                .build(),
            position = Point(100.0, 100.0)
        )
        
        val graph = GraphImpl(
            id = "test-graph",
            isDirected = true,
            nodes = setOf(node),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val svg = renderer.render(graph, options)
        
        // Should apply color attributes
        assertTrue(svg.contains("stroke=\"blue\"") || svg.contains("stroke='blue'"))
        assertTrue(svg.contains("fill=\"#ff0000\"") || svg.contains("fill='#ff0000'"))
        assertTrue(svg.contains("Styled Node"))
    }
    
    @Test
    fun testPolygonShapes() {
        val renderer = DefaultSvgRenderer()
        val options = RenderOptions.default()
        
        val polygonShapes = listOf("diamond", "triangle", "hexagon", "octagon", "star", "pentagon")
        
        for (shape in polygonShapes) {
            val node = NodeImpl(
                id = "polygon-$shape",
                attributes = AttributeMap.builder()
                    .set(AttributeKey.SHAPE, shape)
                    .build(),
                position = Point(50.0, 50.0)
            )
            
            val graph = GraphImpl(
                id = "test-graph",
                isDirected = true,
                nodes = setOf(node),
                edges = emptySet(),
                subgraphs = emptySet(),
                attributes = AttributeMap.empty()
            )
            
            val svg = renderer.render(graph, options)
            
            // Polygon shapes should render as <polygon> elements
            assertTrue(svg.contains("<polygon"), "Shape $shape should render as polygon")
            assertTrue(svg.contains("points="), "Polygon should have points attribute")
        }
    }
    
    @Test
    fun testTextPositioning() {
        val renderer = DefaultSvgRenderer()
        val options = RenderOptions.default()
        
        val node = NodeImpl(
            id = "text-node",
            attributes = AttributeMap.builder()
                .set(AttributeKey.LABEL, "Test Label")
                .set(AttributeKey.FONTNAME, "Arial")
                .set(AttributeKey.FONTSIZE, 12.0)
                .build(),
            position = Point(75.0, 75.0)
        )
        
        val graph = GraphImpl(
            id = "test-graph",
            isDirected = true,
            nodes = setOf(node),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val svg = renderer.render(graph, options)
        
        // Should have text element with proper attributes
        assertTrue(svg.contains("Test Label"), "SVG should contain label text")
        assertTrue(svg.contains("text-anchor=\"middle\""), "Text should be center-aligned")
        assertTrue(svg.contains("font-family=\"Arial\""), "Text should use specified font")
        assertTrue(svg.contains("font-size=\"12"), "Text should use specified font size")
    }
}