package org.graphviz.kotlin.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GraphDataModelTest {
    
    @Test
    fun testPointOperations() {
        val p1 = Point(1.0, 2.0)
        val p2 = Point(3.0, 4.0)
        
        val sum = p1 + p2
        assertEquals(4.0, sum.x)
        assertEquals(6.0, sum.y)
        
        val distance = p1.distanceTo(p2)
        assertTrue(distance > 0)
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
    }
    
    @Test
    fun testAttributeMap() {
        val attrs = AttributeMap.builder()
            .set(AttributeKey.LABEL, "Test Node")
            .set(AttributeKey.COLOR, Color.Named("red"))
            .set(AttributeKey.WIDTH, 100.0)
            .build()
        
        assertEquals("Test Node", attrs.get(AttributeKey.LABEL))
        assertEquals(Color.Named("red"), attrs.get(AttributeKey.COLOR))
        assertEquals(100.0, attrs.get(AttributeKey.WIDTH))
    }
    
    @Test
    fun testColorConversion() {
        val namedColor = Color.Named("blue")
        assertEquals("blue", namedColor.toDotString())
        
        val hexColor = Color.Hex("#FF0000")
        assertEquals("#FF0000", hexColor.toDotString())
        
        val rgbColor = Color.RGB(255, 128, 0)
        assertEquals("#ff8000", rgbColor.toDotString())
    }
    
    @Test
    fun testNodeCreation() {
        val node = NodeImpl(
            id = "test",
            attributes = AttributeMap.builder()
                .set(AttributeKey.LABEL, "Test Node")
                .build(),
            position = Point(10.0, 20.0)
        )
        
        assertEquals("test", node.id)
        assertEquals("Test Node", node.attributes.get(AttributeKey.LABEL))
        assertNotNull(node.position)
        assertEquals(10.0, node.position!!.x)
        assertEquals(20.0, node.position!!.y)
    }
    
    @Test
    fun testEdgeCreation() {
        val source = NodeImpl("A", AttributeMap.empty(), null)
        val target = NodeImpl("B", AttributeMap.empty(), null)
        
        val edge = EdgeImpl(
            source = source,
            target = target,
            attributes = AttributeMap.builder()
                .set(AttributeKey.LABEL, "A to B")
                .build(),
            controlPoints = listOf(Point(5.0, 5.0))
        )
        
        assertEquals(source, edge.source)
        assertEquals(target, edge.target)
        assertEquals("A to B", edge.attributes.get(AttributeKey.LABEL))
        assertEquals(1, edge.controlPoints.size)
    }
    
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
            attributes = AttributeMap.empty()
        )
        
        assertEquals("test_graph", graph.id)
        assertTrue(graph.isDirected)
        assertEquals(2, graph.nodes.size)
        assertEquals(1, graph.edges.size)
        assertTrue(graph.containsNode(nodeA))
        assertTrue(graph.containsEdge(edge))
    }
}