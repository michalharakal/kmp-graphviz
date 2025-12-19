package org.graphviz.kotlin.renderer

import org.graphviz.kotlin.model.*
import kotlin.test.*

/**
 * Test for edge rendering functionality (Task 8.4).
 */
class EdgeRenderingTest {
    
    @Test
    fun testBasicEdgeRendering() {
        // Create nodes
        val nodeA = NodeImpl(
            id = "A",
            attributes = AttributeMap.empty(),
            position = Point(0.0, 0.0)
        )
        
        val nodeB = NodeImpl(
            id = "B", 
            attributes = AttributeMap.empty(),
            position = Point(100.0, 0.0)
        )
        
        // Create edge with control points and attributes
        val edge = EdgeImpl(
            source = nodeA,
            target = nodeB,
            attributes = AttributeMap.builder()
                .set(AttributeKey.COLOR, Color.Named("red"))
                .set(AttributeKey.PENWIDTH, 2.0)
                .set(AttributeKey.STYLE, "dashed")
                .build(),
            controlPoints = listOf(Point(0.0, 0.0), Point(50.0, 10.0), Point(100.0, 0.0))
        )
        
        // Create graph
        val graph = GraphImpl(
            id = "test-graph",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        // Render complete graph
        val renderer = DefaultSvgRenderer()
        val options = RenderOptions.default()
        val svg = renderer.render(graph, options)
        
        // Should contain edge with proper attributes
        assertTrue(svg.contains("id=\"edge-A-B\""))
        assertTrue(svg.contains("stroke=\"red\""))
        assertTrue(svg.contains("stroke-width=\"2.0\""))
        assertTrue(svg.contains("stroke-dasharray=\"5,5\""))
        assertTrue(svg.contains("class=\"edge\""))
        assertTrue(svg.contains("fill=\"none\""))
    }
    
    @Test
    fun testEdgeWithLabel() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), Point(0.0, 0.0))
        val nodeB = NodeImpl("B", AttributeMap.empty(), Point(100.0, 0.0))
        
        val edge = EdgeImpl(
            source = nodeA,
            target = nodeB,
            attributes = AttributeMap.builder()
                .set(AttributeKey.LABEL, "Edge Label")
                .set(AttributeKey.FONTSIZE, 14.0)
                .build(),
            controlPoints = listOf(Point(0.0, 0.0), Point(100.0, 0.0))
        )
        
        val graph = GraphImpl(
            id = "test-graph",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val renderer = DefaultSvgRenderer()
        val svg = renderer.render(graph, RenderOptions.default())
        
        // Should contain edge label
        assertTrue(svg.contains("Edge Label"))
        assertTrue(svg.contains("font-size=\"14.0\""))
        assertTrue(svg.contains("class=\"edge-label\""))
        assertTrue(svg.contains("id=\"edge-A-B-label\""))
    }
    
    @Test
    fun testArrowheadRendering() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), Point(0.0, 0.0))
        val nodeB = NodeImpl("B", AttributeMap.empty(), Point(100.0, 0.0))
        
        val edge = EdgeImpl(
            source = nodeA,
            target = nodeB,
            attributes = AttributeMap.builder()
                .setRaw("arrowhead", AttributeValue.StringValue("normal"))
                .setRaw("arrowsize", AttributeValue.NumberValue(1.5))
                .build(),
            controlPoints = listOf(Point(0.0, 0.0), Point(100.0, 0.0))
        )
        
        val graph = GraphImpl(
            id = "test-graph",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val renderer = DefaultSvgRenderer()
        val svg = renderer.render(graph, RenderOptions.default())
        
        // Should contain arrowhead
        assertTrue(svg.contains("class=\"arrowhead\""))
    }
    
    @Test
    fun testDifferentArrowheadTypes() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), Point(0.0, 0.0))
        val nodeB = NodeImpl("B", AttributeMap.empty(), Point(100.0, 0.0))
        
        val arrowTypes = listOf("normal", "inv", "dot", "diamond", "box", "vee", "tee")
        
        for (arrowType in arrowTypes) {
            val edge = EdgeImpl(
                source = nodeA,
                target = nodeB,
                attributes = AttributeMap.builder()
                    .setRaw("arrowhead", AttributeValue.StringValue(arrowType))
                    .build(),
                controlPoints = listOf(Point(0.0, 0.0), Point(100.0, 0.0))
            )
            
            val graph = GraphImpl(
                id = "test-graph-$arrowType",
                isDirected = true,
                nodes = setOf(nodeA, nodeB),
                edges = setOf(edge),
                subgraphs = emptySet(),
                attributes = AttributeMap.empty()
            )
            
            val renderer = DefaultSvgRenderer()
            val svg = renderer.render(graph, RenderOptions.default())
            
            // Should have arrowhead for each type
            assertTrue(svg.contains("class=\"arrowhead\""), "Arrow type $arrowType should produce an arrowhead")
        }
    }
    
    @Test
    fun testEdgeWithoutArrowhead() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), Point(0.0, 0.0))
        val nodeB = NodeImpl("B", AttributeMap.empty(), Point(100.0, 0.0))
        
        val edge = EdgeImpl(
            source = nodeA,
            target = nodeB,
            attributes = AttributeMap.builder()
                .setRaw("arrowhead", AttributeValue.StringValue("none"))
                .build(),
            controlPoints = listOf(Point(0.0, 0.0), Point(100.0, 0.0))
        )
        
        val graph = GraphImpl(
            id = "test-graph",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val renderer = DefaultSvgRenderer()
        val svg = renderer.render(graph, RenderOptions.default())
        
        // Should not have arrowhead
        assertFalse(svg.contains("class=\"arrowhead\""))
    }
    
    @Test
    fun testEdgeStyleAttributes() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), Point(0.0, 0.0))
        val nodeB = NodeImpl("B", AttributeMap.empty(), Point(100.0, 0.0))
        
        val styles = mapOf(
            "dashed" to "stroke-dasharray=\"5,5\"",
            "dotted" to "stroke-dasharray=\"2,2\"",
            "bold" to "stroke-width=\"2\"",
            "solid" to "stroke-dasharray=\"none\""
        )
        
        for ((style, expectedSvg) in styles) {
            val edge = EdgeImpl(
                source = nodeA,
                target = nodeB,
                attributes = AttributeMap.builder()
                    .set(AttributeKey.STYLE, style)
                    .build(),
                controlPoints = listOf(Point(0.0, 0.0), Point(100.0, 0.0))
            )
            
            val graph = GraphImpl(
                id = "test-graph-$style",
                isDirected = true,
                nodes = setOf(nodeA, nodeB),
                edges = setOf(edge),
                subgraphs = emptySet(),
                attributes = AttributeMap.empty()
            )
            
            val renderer = DefaultSvgRenderer()
            val svg = renderer.render(graph, RenderOptions.default())
            
            assertTrue(svg.contains(expectedSvg), "Style $style should produce $expectedSvg")
        }
    }
    
    @Test
    fun testEdgeColorAndWidth() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), Point(0.0, 0.0))
        val nodeB = NodeImpl("B", AttributeMap.empty(), Point(100.0, 0.0))
        
        val edge = EdgeImpl(
            source = nodeA,
            target = nodeB,
            attributes = AttributeMap.builder()
                .set(AttributeKey.COLOR, Color.Hex("#ff0000"))
                .set(AttributeKey.PENWIDTH, 3.5)
                .build(),
            controlPoints = listOf(Point(0.0, 0.0), Point(100.0, 0.0))
        )
        
        val graph = GraphImpl(
            id = "test-graph",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val renderer = DefaultSvgRenderer()
        val svg = renderer.render(graph, RenderOptions.default())
        
        // Should contain color and width attributes
        assertTrue(svg.contains("stroke=\"#ff0000\""))
        assertTrue(svg.contains("stroke-width=\"3.5\""))
    }
}