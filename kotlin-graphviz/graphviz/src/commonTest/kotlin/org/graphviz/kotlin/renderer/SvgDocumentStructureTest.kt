package org.graphviz.kotlin.renderer

import org.graphviz.kotlin.model.*
import kotlin.test.*

/**
 * Tests for SVG document structure generation functionality.
 * 
 * This test class verifies that task 8.2 requirements are met:
 * - Generate valid SVG XML with proper DOCTYPE and namespaces
 * - Calculate and set appropriate viewport and coordinate system
 * - Handle coordinate transformations and scaling
 */
class SvgDocumentStructureTest {
    
    @Test
    fun testBasicSvgDocumentStructure() {
        // Create a simple graph with positioned nodes
        val node1 = NodeImpl("A", AttributeMap.empty(), Point(0.0, 0.0))
        val node2 = NodeImpl("B", AttributeMap.empty(), Point(100.0, 50.0))
        val edge = EdgeImpl(node1, node2, AttributeMap.empty(), listOf(Point(0.0, 0.0), Point(100.0, 50.0)))
        
        val graph = GraphImpl(
            id = "test-graph",
            isDirected = true,
            nodes = setOf(node1, node2),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        // Create SVG document
        val boundingBox = graph.getBoundingBox()!!
        val document = SvgDocument.create(boundingBox, RenderOptions.default())
        
        // Generate SVG
        val svg = document.toSvg(RenderOptions.default())
        
        // Verify basic structure
        assertTrue(svg.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(svg.contains("<!DOCTYPE svg"))
        assertTrue(svg.contains("xmlns=\"http://www.w3.org/2000/svg\""))
        assertTrue(svg.contains("xmlns:xlink=\"http://www.w3.org/1999/xlink\""))
        assertTrue(svg.contains("version=\"1.1\""))
        assertTrue(svg.contains("viewBox="))
        assertTrue(svg.contains("width="))
        assertTrue(svg.contains("height="))
    }
    
    @Test
    fun testSvgDocumentWithoutXmlDeclaration() {
        val boundingBox = BoundingBox(0.0, 0.0, 100.0, 100.0)
        val document = SvgDocument.create(boundingBox, RenderOptions.forWeb())
        
        val svg = document.toSvg(RenderOptions.forWeb())
        
        // Should not include XML declaration for web
        assertFalse(svg.contains("<?xml"))
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("xmlns=\"http://www.w3.org/2000/svg\""))
    }
    
    @Test
    fun testViewportCalculation() {
        val boundingBox = BoundingBox(-50.0, -25.0, 150.0, 75.0)
        val options = RenderOptions(width = 400.0, height = 300.0, margin = 20.0)
        val document = SvgDocument.create(boundingBox, options)
        
        val svg = document.toSvg(options)
        
        // Should have specified dimensions
        assertTrue(svg.contains("width=\"400\""))
        assertTrue(svg.contains("height=\"300\""))
        
        // Should have viewBox that includes margin
        assertTrue(svg.contains("viewBox="))
    }
    
    @Test
    fun testCoordinateSystemTransformation() {
        val boundingBox = BoundingBox(0.0, 0.0, 200.0, 100.0)
        val viewport = Viewport(400.0, 200.0, ViewBox.fromBoundingBox(boundingBox, 10.0))
        val coordinateSystem = CoordinateSystem.fitToViewport(boundingBox, viewport, 10.0)
        
        // Test point transformation
        val originalPoint = Point(100.0, 50.0) // Center of bounding box
        val transformedPoint = coordinateSystem.transformPoint(originalPoint)
        
        // Transformed point should be within viewport
        assertTrue(transformedPoint.x >= 0.0)
        assertTrue(transformedPoint.x <= viewport.width)
        assertTrue(transformedPoint.y >= 0.0)
        assertTrue(transformedPoint.y <= viewport.height)

        // Ensure text would be counter-flipped (transform on <text>) when group Y-flip is applied
        val doc = SvgDocument.createWithCoordinateSystem(coordinateSystem, viewport)
        val renderer = DefaultSvgRenderer()

        // Minimal graph with a labeled node to produce <text>
        val attrs = AttributeMap.builder().set(AttributeKey.LABEL, "Hello").build()
        val n = NodeImpl("N", attrs, Point(100.0, 50.0))
        val g = GraphImpl(
            id = "g1",
            isDirected = true,
            nodes = setOf(n),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )

        // Render using full pipeline to let renderer attach counter-transform
        val svg = renderer.render(g, RenderOptions.default())
        // Check that a <text ... transform="... scale(1, -1) ..."> appears
        assertTrue(svg.contains("<text") && svg.contains("scale(1, -1)"))
    }
    
    @Test
    fun testSvgDocumentWithMetadata() {
        val boundingBox = BoundingBox(0.0, 0.0, 100.0, 100.0)
        val document = SvgDocument.create(boundingBox, RenderOptions.default())
        
        document.addMetadata("generator", "Graphviz Kotlin Test")
        document.addMetadata("created", "2024-01-01")
        
        val options = RenderOptions(
            rendererOptions = mapOf(
                "title" to "Test Graph",
                "description" to "A test graph for SVG generation"
            )
        )
        
        val svg = document.toSvg(options)
        
        // Should contain metadata
        assertTrue(svg.contains("<metadata>"))
        assertTrue(svg.contains("<generator>Graphviz Kotlin Test</generator>"))
        assertTrue(svg.contains("<created>2024-01-01</created>"))
        assertTrue(svg.contains("<title>Test Graph</title>"))
        assertTrue(svg.contains("<desc>A test graph for SVG generation</desc>"))
    }
    
    @Test
    fun testSvgDocumentWithStyles() {
        val boundingBox = BoundingBox(0.0, 0.0, 100.0, 100.0)
        val document = SvgDocument.create(boundingBox, RenderOptions.default())
        
        document.addStyle(".custom-node { fill: red; }")
        document.addStyle(".custom-edge { stroke: blue; }")
        
        val options = RenderOptions(
            additionalStyles = ".graph-title { font-size: 16px; }"
        )
        
        val svg = document.toSvg(options)
        
        // Should contain styles
        assertTrue(svg.contains("<style type=\"text/css\">"))
        assertTrue(svg.contains(".custom-node { fill: red; }"))
        assertTrue(svg.contains(".custom-edge { stroke: blue; }"))
        assertTrue(svg.contains(".graph-title { font-size: 16px; }"))
        assertTrue(svg.contains("/* Default Graphviz styles */"))
    }
    
    @Test
    fun testSvgDocumentWithDefinitions() {
        val boundingBox = BoundingBox(0.0, 0.0, 100.0, 100.0)
        val document = SvgDocument.create(boundingBox, RenderOptions.default())
        
        // Add a gradient definition
        val gradient = SvgGroup()
        gradient.setAttribute("id", "testGradient")
        document.addDefinition(gradient)
        
        val svg = document.toSvg(RenderOptions.default())
        
        // Should contain definitions section
        assertTrue(svg.contains("<defs>"))
        assertTrue(svg.contains("id=\"testGradient\""))
        assertTrue(svg.contains("</defs>"))
    }
    
    @Test
    fun testCoordinateSystemScaling() {
        val largeBoundingBox = BoundingBox(0.0, 0.0, 1000.0, 1000.0)
        val smallViewport = Viewport(200.0, 200.0, null)
        val coordinateSystem = CoordinateSystem.fitToViewport(largeBoundingBox, smallViewport, 10.0)
        
        // Should require scaling
        assertTrue(coordinateSystem.requiresScaling())
        
        // Scale should be less than 1 to fit large graph in small viewport
        assertTrue(coordinateSystem.getUniformScale() < 1.0)
        
        // Transform matrix should include scaling
        val transformMatrix = coordinateSystem.getTransformMatrix()
        // Accept either functional or matrix form
        assertTrue(transformMatrix.contains("scale") || transformMatrix.contains("matrix"))
    }
    
    @Test
    fun testXmlEscaping() {
        val text = "Test & <special> \"characters\" 'here'"
        val escaped = SvgUtils.escapeXml(text)
        
        assertEquals("Test &amp; &lt;special&gt; &quot;characters&quot; &apos;here&apos;", escaped)
    }
    
    @Test
    fun testNumberFormatting() {
        assertEquals("100", SvgUtils.formatNumber(100.0))
        assertEquals("100.5", SvgUtils.formatNumber(100.5))
        assertEquals("0.123", SvgUtils.formatNumber(0.12345))
    }
    
    @Test
    fun testDimensionFormatting() {
        assertEquals("200", SvgUtils.formatDimension(200.0))
        assertEquals("200.5", SvgUtils.formatDimension(200.5))
        assertEquals("0.12", SvgUtils.formatDimension(0.12345))
    }
}