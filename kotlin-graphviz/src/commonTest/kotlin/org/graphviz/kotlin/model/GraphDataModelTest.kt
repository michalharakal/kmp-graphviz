package org.graphviz.kotlin.model

import kotlin.test.*

/**
 * Comprehensive unit tests for the graph data model.
 * Tests graph construction and modification operations, attribute validation and type safety,
 * and subgraph and hierarchy management.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
class GraphDataModelTest {
    
    // ========== Coordinate System Tests ==========
    
    @Test
    fun testPointOperations() {
        val p1 = Point(1.0, 2.0)
        val p2 = Point(3.0, 4.0)
        
        // Test addition
        val sum = p1 + p2
        assertEquals(4.0, sum.x)
        assertEquals(6.0, sum.y)
        
        // Test subtraction
        val diff = p2 - p1
        assertEquals(2.0, diff.x)
        assertEquals(2.0, diff.y)
        
        // Test scalar multiplication
        val scaled = p1 * 2.0
        assertEquals(2.0, scaled.x)
        assertEquals(4.0, scaled.y)
        
        // Test distance calculation
        val distance = p1.distanceTo(p2)
        assertEquals(2.8284271247461903, distance, 0.0001)
    }
    
    @Test
    fun testRectangleCreation() {
        val rect = Rectangle(0.0, 0.0, 10.0, 20.0)
        assertEquals(0.0, rect.left)
        assertEquals(10.0, rect.right)
        assertEquals(0.0, rect.top)
        assertEquals(20.0, rect.bottom)
        
        val center = rect.center
        assertEquals(5.0, center.x)
        assertEquals(10.0, center.y)
        
        // Test point containment
        assertTrue(rect.contains(Point(5.0, 10.0)))
        assertFalse(rect.contains(Point(15.0, 10.0)))
        
        // Test rectangle intersection
        val other = Rectangle(5.0, 5.0, 10.0, 10.0)
        assertTrue(rect.intersects(other))
    }
    
    @Test
    fun testRectangleValidation() {
        // Test negative dimensions validation
        assertFailsWith<IllegalArgumentException> {
            Rectangle(0.0, 0.0, -5.0, 10.0)
        }
        
        assertFailsWith<IllegalArgumentException> {
            Rectangle(0.0, 0.0, 10.0, -5.0)
        }
    }
    
    @Test
    fun testBoundingBoxFromPoints() {
        val points = listOf(
            Point(1.0, 1.0),
            Point(5.0, 3.0),
            Point(2.0, 7.0)
        )
        
        val bbox = BoundingBox.fromPoints(points)
        assertEquals(1.0, bbox.minX)
        assertEquals(1.0, bbox.minY)
        assertEquals(5.0, bbox.maxX)
        assertEquals(7.0, bbox.maxY)
        
        // Test width and height
        assertEquals(4.0, bbox.width)
        assertEquals(6.0, bbox.height)
        
        // Test center
        val center = bbox.center
        assertEquals(3.0, center.x)
        assertEquals(4.0, center.y)
    }
    
    @Test
    fun testBoundingBoxValidation() {
        // Test invalid bounds
        assertFailsWith<IllegalArgumentException> {
            BoundingBox(5.0, 0.0, 1.0, 10.0) // minX > maxX
        }
        
        assertFailsWith<IllegalArgumentException> {
            BoundingBox(0.0, 5.0, 10.0, 1.0) // minY > maxY
        }
        
        // Test empty points collection
        assertFailsWith<IllegalArgumentException> {
            BoundingBox.fromPoints(emptyList())
        }
    }
    
    @Test
    fun testBoundingBoxOperations() {
        val bbox1 = BoundingBox(0.0, 0.0, 5.0, 5.0)
        val bbox2 = BoundingBox(3.0, 3.0, 8.0, 8.0)
        
        // Test intersection
        assertTrue(bbox1.intersects(bbox2))
        
        // Test containment
        val smallBox = BoundingBox(1.0, 1.0, 4.0, 4.0)
        assertTrue(bbox1.contains(smallBox))
        assertFalse(smallBox.contains(bbox1))
        
        // Test union
        val union = bbox1.union(bbox2)
        assertEquals(0.0, union.minX)
        assertEquals(0.0, union.minY)
        assertEquals(8.0, union.maxX)
        assertEquals(8.0, union.maxY)
    }
    
    // ========== Attribute System Tests ==========
    
    @Test
    fun testAttributeMapBasicOperations() {
        val attrs = AttributeMap.builder()
            .set(AttributeKey.LABEL, "Test Node")
            .set(AttributeKey.COLOR, Color.Named("red"))
            .set(AttributeKey.WIDTH, 100.0)
            .build()
        
        assertEquals("Test Node", attrs.get(AttributeKey.LABEL))
        assertEquals(Color.Named("red"), attrs.get(AttributeKey.COLOR))
        assertEquals(100.0, attrs.get(AttributeKey.WIDTH))
        
        // Test contains
        assertTrue(attrs.contains("label"))
        assertTrue(attrs.contains("color"))
        assertFalse(attrs.contains("nonexistent"))
        
        // Test size and empty
        assertEquals(3, attrs.size)
        assertFalse(attrs.isEmpty)
    }
    
    @Test
    fun testAttributeMapImmutability() {
        val original = AttributeMap.builder()
            .set(AttributeKey.LABEL, "Original")
            .build()
        
        val modified = original.set(AttributeKey.COLOR, Color.Named("blue"))
        
        // Original should be unchanged
        assertEquals("Original", original.get(AttributeKey.LABEL))
        assertNull(original.get(AttributeKey.COLOR))
        
        // Modified should have both attributes
        assertEquals("Original", modified.get(AttributeKey.LABEL))
        assertEquals(Color.Named("blue"), modified.get(AttributeKey.COLOR))
    }
    
    @Test
    fun testAttributeMapMerging() {
        val attrs1 = AttributeMap.builder()
            .set(AttributeKey.LABEL, "Node1")
            .set(AttributeKey.COLOR, Color.Named("red"))
            .build()
        
        val attrs2 = AttributeMap.builder()
            .set(AttributeKey.LABEL, "Node2") // Should override
            .set(AttributeKey.SHAPE, "box")
            .build()
        
        val merged = attrs1.merge(attrs2)
        
        assertEquals("Node2", merged.get(AttributeKey.LABEL)) // Overridden
        assertEquals(Color.Named("red"), merged.get(AttributeKey.COLOR)) // Preserved
        assertEquals("box", merged.get(AttributeKey.SHAPE)) // Added
    }
    
    @Test
    fun testAttributeMapRemoval() {
        val attrs = AttributeMap.builder()
            .set(AttributeKey.LABEL, "Test")
            .set(AttributeKey.COLOR, Color.Named("red"))
            .build()
        
        val removed = attrs.remove("label")
        
        assertNull(removed.get(AttributeKey.LABEL))
        assertEquals(Color.Named("red"), removed.get(AttributeKey.COLOR))
        assertEquals(1, removed.size)
    }
    
    @Test
    fun testAttributeValidation() {
        val attrs = AttributeMap.builder()
            .set(AttributeKey.LABEL, "Valid Label")
            .set(AttributeKey.WIDTH, 100.0)
            .build()
        
        val validation = attrs.validate()
        assertTrue(validation.isValid)
    }
    
    @Test
    fun testAttributeValidationWithInvalidNumber() {
        val attrs = AttributeMap.fromValues(mapOf(
            "width" to AttributeValue.NumberValue(Double.POSITIVE_INFINITY)
        ))
        
        val validation = attrs.validate()
        assertFalse(validation.isValid)
        assertTrue(validation is ValidationResult.Invalid)
        val errors = (validation as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("invalid number value") })
    }
    
    @Test
    fun testColorValidation() {
        // Test valid colors
        val namedColor = Color.Named("blue")
        assertEquals("blue", namedColor.toDotString())
        
        val hexColor = Color.Hex("#FF0000")
        assertEquals("#FF0000", hexColor.toDotString())
        
        val rgbColor = Color.RGB(255, 128, 0)
        assertEquals("#ff8000", rgbColor.toDotString())
        
        // Test invalid hex color
        assertFailsWith<IllegalArgumentException> {
            Color.Hex("FF0000") // Missing #
        }
        
        assertFailsWith<IllegalArgumentException> {
            Color.Hex("#GG0000") // Invalid hex characters
        }
        
        // Test invalid RGB values
        assertFailsWith<IllegalArgumentException> {
            Color.RGB(-1, 128, 0) // Negative red
        }
        
        assertFailsWith<IllegalArgumentException> {
            Color.RGB(255, 256, 0) // Green > 255
        }
    }
    
    @Test
    fun testAttributeTypeSafety() {
        val key = AttributeKey("test", AttributeType.STRING)
        val attrs = AttributeMap.builder()
            .set(key, "string value")
            .build()
        
        assertEquals("string value", attrs.get(key))
        
        // Test type mismatch handling
        val numberKey = AttributeKey("number", AttributeType.NUMBER)
        assertNull(attrs.get(numberKey)) // Should return null for non-existent key
    }
    
    // ========== Node Tests ==========
    
    @Test
    fun testNodeCreation() {
        val node = NodeImpl(
            id = "test_node",
            attributes = AttributeMap.builder()
                .set(AttributeKey.LABEL, "Test Node")
                .set(AttributeKey.SHAPE, "box")
                .build(),
            position = Point(10.0, 20.0)
        )
        
        assertEquals("test_node", node.id)
        assertEquals("Test Node", node.attributes.get(AttributeKey.LABEL))
        assertEquals("box", node.attributes.get(AttributeKey.SHAPE))
        assertNotNull(node.position)
        assertEquals(10.0, node.position?.x)
        assertEquals(20.0, node.position?.y)
    }
    
    @Test
    fun testNodeValidation() {
        // Test blank ID validation
        assertFailsWith<IllegalArgumentException> {
            NodeImpl("", AttributeMap.empty(), null)
        }
        
        assertFailsWith<IllegalArgumentException> {
            NodeImpl("   ", AttributeMap.empty(), null)
        }
    }
    
    @Test
    fun testNodeImmutability() {
        val original = NodeImpl("test", AttributeMap.empty(), null)
        
        // Test position modification
        val positioned = original.withPosition(Point(5.0, 10.0))
        assertNull(original.position)
        assertEquals(Point(5.0, 10.0), positioned.position)
        
        // Test attribute modification
        val withAttrs = original.withAttribute(AttributeKey.LABEL, "New Label")
        assertNull(original.attributes.get(AttributeKey.LABEL))
        assertEquals("New Label", withAttrs.attributes.get(AttributeKey.LABEL))
    }
    
    // ========== Edge Tests ==========
    
    @Test
    fun testEdgeCreation() {
        val source = NodeImpl("A", AttributeMap.empty(), null)
        val target = NodeImpl("B", AttributeMap.empty(), null)
        
        val edge = EdgeImpl(
            source = source,
            target = target,
            attributes = AttributeMap.builder()
                .set(AttributeKey.LABEL, "A to B")
                .set(AttributeKey.COLOR, Color.Named("blue"))
                .build(),
            controlPoints = listOf(Point(5.0, 5.0), Point(10.0, 10.0))
        )
        
        assertEquals(source, edge.source)
        assertEquals(target, edge.target)
        assertEquals("A to B", edge.attributes.get(AttributeKey.LABEL))
        assertEquals(Color.Named("blue"), edge.attributes.get(AttributeKey.COLOR))
        assertEquals(2, edge.controlPoints.size)
        assertEquals(Point(5.0, 5.0), edge.controlPoints[0])
        assertEquals(Point(10.0, 10.0), edge.controlPoints[1])
    }
    
    @Test
    fun testEdgeImmutability() {
        val source = NodeImpl("A", AttributeMap.empty(), null)
        val target = NodeImpl("B", AttributeMap.empty(), null)
        val original = EdgeImpl(source, target, AttributeMap.empty(), emptyList())
        
        // Test control points modification
        val newPoints = listOf(Point(1.0, 1.0), Point(2.0, 2.0))
        val withPoints = original.withControlPoints(newPoints)
        assertTrue(original.controlPoints.isEmpty())
        assertEquals(2, withPoints.controlPoints.size)
        
        // Test attribute modification
        val withAttrs = original.withAttribute(AttributeKey.STYLE, "dashed")
        assertNull(original.attributes.get(AttributeKey.STYLE))
        assertEquals("dashed", withAttrs.attributes.get(AttributeKey.STYLE))
    }
    
    @Test
    fun testEdgeNodeConnection() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        
        val edge1 = EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList())
        val edge2 = EdgeImpl(nodeB, nodeA, AttributeMap.empty(), emptyList())
        val edge3 = EdgeImpl(nodeA, nodeC, AttributeMap.empty(), emptyList())
        
        // Test connectsSameNodes
        assertTrue(edge1.connectsSameNodes(edge2))
        assertFalse(edge1.connectsSameNodes(edge3))
    }
    
    // ========== Graph Construction and Modification Tests ==========
    
    @Test
    fun testGraphCreation() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val edge = EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList())
        
        val graph = GraphImpl(
            id = "test_graph",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.builder()
                .set(AttributeKey.LABEL, "Test Graph")
                .build()
        )
        
        assertEquals("test_graph", graph.id)
        assertTrue(graph.isDirected)
        assertEquals(2, graph.nodes.size)
        assertEquals(1, graph.edges.size)
        assertEquals("Test Graph", graph.attributes.get(AttributeKey.LABEL))
        assertTrue(graph.containsNode(nodeA))
        assertTrue(graph.containsNode(nodeB))
        assertTrue(graph.containsEdge(edge))
    }
    
    @Test
    fun testGraphNodeLookup() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        
        val graph = GraphImpl(
            id = "lookup_test",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        assertEquals(nodeA, graph.getNode("A"))
        assertEquals(nodeB, graph.getNode("B"))
        assertNull(graph.getNode("C"))
    }
    
    @Test
    fun testGraphEdgeQueries() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        
        val edge1 = EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList())
        val edge2 = EdgeImpl(nodeB, nodeC, AttributeMap.empty(), emptyList())
        val edge3 = EdgeImpl(nodeA, nodeC, AttributeMap.empty(), emptyList())
        
        val graph = GraphImpl(
            id = "edge_test",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC),
            edges = setOf(edge1, edge2, edge3),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        // Test getEdges for a node
        val edgesFromA = graph.getEdges(nodeA)
        assertEquals(2, edgesFromA.size)
        assertTrue(edgesFromA.contains(edge1))
        assertTrue(edgesFromA.contains(edge3))
        
        // Test getEdges between specific nodes
        val edgesAtoB = graph.getEdges(nodeA, nodeB)
        assertEquals(1, edgesAtoB.size)
        assertTrue(edgesAtoB.contains(edge1))
        
        val edgesBtoA = graph.getEdges(nodeB, nodeA)
        assertTrue(edgesBtoA.isEmpty()) // Directed graph
    }
    
    @Test
    fun testUndirectedGraphEdgeQueries() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        
        val edge = EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList())
        
        val graph = GraphImpl(
            id = "undirected_test",
            isDirected = false,
            nodes = setOf(nodeA, nodeB),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        // In undirected graph, edge should be found in both directions
        val edgesAtoB = graph.getEdges(nodeA, nodeB)
        val edgesBtoA = graph.getEdges(nodeB, nodeA)
        
        assertEquals(1, edgesAtoB.size)
        assertEquals(1, edgesBtoA.size)
        assertTrue(edgesAtoB.contains(edge))
        assertTrue(edgesBtoA.contains(edge))
    }
    
    @Test
    fun testGraphValidation() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val nodeC = NodeImpl("C", AttributeMap.empty(), null)
        
        // Test edge referencing non-existent node
        val invalidEdge = EdgeImpl(nodeA, nodeC, AttributeMap.empty(), emptyList())
        
        assertFailsWith<IllegalArgumentException> {
            GraphImpl(
                id = "invalid_graph",
                isDirected = true,
                nodes = setOf(nodeA, nodeB), // nodeC not included
                edges = setOf(invalidEdge), // but edge references nodeC
                subgraphs = emptySet(),
                attributes = AttributeMap.empty()
            )
        }
    }
    
    // ========== Subgraph and Hierarchy Management Tests ==========
    
    @Test
    fun testGraphWithSubgraphs() {
        // Create nodes for main graph
        val mainNodeA = NodeImpl("main_A", AttributeMap.empty(), null)
        val mainNodeB = NodeImpl("main_B", AttributeMap.empty(), null)
        
        // Create nodes for subgraph
        val subNodeX = NodeImpl("sub_X", AttributeMap.empty(), null)
        val subNodeY = NodeImpl("sub_Y", AttributeMap.empty(), null)
        val subEdge = EdgeImpl(subNodeX, subNodeY, AttributeMap.empty(), emptyList())
        
        // Create subgraph
        val subgraph = GraphImpl(
            id = "subgraph_1",
            isDirected = true,
            nodes = setOf(subNodeX, subNodeY),
            edges = setOf(subEdge),
            subgraphs = emptySet(),
            attributes = AttributeMap.builder()
                .set(AttributeKey.LABEL, "Subgraph 1")
                .build()
        )
        
        // Create main graph with subgraph
        val mainGraph = GraphImpl(
            id = "main_graph",
            isDirected = true,
            nodes = setOf(mainNodeA, mainNodeB),
            edges = emptySet(),
            subgraphs = setOf(subgraph),
            attributes = AttributeMap.empty()
        )
        
        assertEquals("main_graph", mainGraph.id)
        assertEquals(2, mainGraph.nodes.size) // Only direct nodes
        assertEquals(0, mainGraph.edges.size) // No direct edges
        assertEquals(1, mainGraph.subgraphs.size)
        
        // Test getAllNodes includes subgraph nodes
        val allNodes = mainGraph.getAllNodes()
        assertEquals(4, allNodes.size) // main_A, main_B, sub_X, sub_Y
        assertTrue(allNodes.contains(mainNodeA))
        assertTrue(allNodes.contains(mainNodeB))
        assertTrue(allNodes.contains(subNodeX))
        assertTrue(allNodes.contains(subNodeY))
        
        // Test getAllEdges includes subgraph edges
        val allEdges = mainGraph.getAllEdges()
        assertEquals(1, allEdges.size)
        assertTrue(allEdges.contains(subEdge))
    }
    
    @Test
    fun testNestedSubgraphs() {
        // Create deeply nested structure
        val leafNode = NodeImpl("leaf", AttributeMap.empty(), null)
        val leafGraph = GraphImpl(
            id = "leaf_graph",
            isDirected = true,
            nodes = setOf(leafNode),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val middleNode = NodeImpl("middle", AttributeMap.empty(), null)
        val middleGraph = GraphImpl(
            id = "middle_graph",
            isDirected = true,
            nodes = setOf(middleNode),
            edges = emptySet(),
            subgraphs = setOf(leafGraph),
            attributes = AttributeMap.empty()
        )
        
        val rootNode = NodeImpl("root", AttributeMap.empty(), null)
        val rootGraph = GraphImpl(
            id = "root_graph",
            isDirected = true,
            nodes = setOf(rootNode),
            edges = emptySet(),
            subgraphs = setOf(middleGraph),
            attributes = AttributeMap.empty()
        )
        
        // Test that getAllNodes traverses the entire hierarchy
        val allNodes = rootGraph.getAllNodes()
        assertEquals(3, allNodes.size)
        assertTrue(allNodes.contains(rootNode))
        assertTrue(allNodes.contains(middleNode))
        assertTrue(allNodes.contains(leafNode))
    }
    
    @Test
    fun testGraphBoundingBox() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), Point(0.0, 0.0))
        val nodeB = NodeImpl("B", AttributeMap.empty(), Point(10.0, 5.0))
        val nodeC = NodeImpl("C", AttributeMap.empty(), null) // No position
        
        val graph = GraphImpl(
            id = "bbox_test",
            isDirected = true,
            nodes = setOf(nodeA, nodeB, nodeC),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val bbox = graph.getBoundingBox()
        assertNotNull(bbox)
        assertEquals(0.0, bbox.minX)
        assertEquals(0.0, bbox.minY)
        assertEquals(10.0, bbox.maxX)
        assertEquals(5.0, bbox.maxY)
    }
    
    @Test
    fun testGraphBoundingBoxWithNoPositions() {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        
        val graph = GraphImpl(
            id = "no_positions",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val bbox = graph.getBoundingBox()
        assertNull(bbox)
    }
    
    @Test
    fun testGraphBoundingBoxWithSubgraphs() {
        // Main graph node
        val mainNode = NodeImpl("main", AttributeMap.empty(), Point(0.0, 0.0))
        
        // Subgraph nodes
        val subNode1 = NodeImpl("sub1", AttributeMap.empty(), Point(20.0, 10.0))
        val subNode2 = NodeImpl("sub2", AttributeMap.empty(), Point(-5.0, 15.0))
        
        val subgraph = GraphImpl(
            id = "subgraph",
            isDirected = true,
            nodes = setOf(subNode1, subNode2),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val mainGraph = GraphImpl(
            id = "main_with_sub",
            isDirected = true,
            nodes = setOf(mainNode),
            edges = emptySet(),
            subgraphs = setOf(subgraph),
            attributes = AttributeMap.empty()
        )
        
        val bbox = mainGraph.getBoundingBox()
        assertNotNull(bbox)
        assertEquals(-5.0, bbox.minX) // From subNode2
        assertEquals(0.0, bbox.minY)  // From mainNode
        assertEquals(20.0, bbox.maxX) // From subNode1
        assertEquals(15.0, bbox.maxY) // From subNode2
    }
    
    // ========== Graph Consistency Tests ==========
    
    @Test
    fun testGraphConsistencyAfterModification() {
        // This test verifies that graph operations maintain referential integrity
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        val edge = EdgeImpl(nodeA, nodeB, AttributeMap.empty(), emptyList())
        
        val graph = GraphImpl(
            id = "consistency_test",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = setOf(edge),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        // Verify all nodes referenced by edges exist in the graph
        for (graphEdge in graph.getAllEdges()) {
            assertTrue(graph.getAllNodes().contains(graphEdge.source))
            assertTrue(graph.getAllNodes().contains(graphEdge.target))
        }
    }
    
    @Test
    fun testAttributeMapDotConversion() {
        val attrs = AttributeMap.builder()
            .set(AttributeKey.LABEL, "Test Label")
            .set(AttributeKey.COLOR, Color.Named("red"))
            .set(AttributeKey.WIDTH, 100.0)
            .build()
        
        val dotMap = attrs.toDotMap()
        assertEquals("\"Test Label\"", dotMap["label"]) // Should be quoted
        assertEquals("red", dotMap["color"])
        assertEquals("100.0", dotMap["width"])
    }
}