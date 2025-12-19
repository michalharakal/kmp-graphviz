package org.graphviz.kotlin.builder

import org.graphviz.kotlin.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GraphBuilderTest {
    
    @Test
    fun testSimpleGraphBuilding() {
        val graph = digraph("test") {
            node("A") {
                label("Node A")
                shape("box")
            }
            
            node("B") {
                label("Node B")
                shape("ellipse")
            }
            
            edge("A", "B") {
                label("A to B")
            }
        }
        
        assertEquals("test", graph.id)
        assertTrue(graph.isDirected)
        assertEquals(2, graph.nodes.size)
        assertEquals(1, graph.edges.size)
        
        val nodeA = graph.getNode("A")!!
        assertEquals("Node A", nodeA.attributes.get(AttributeKey.LABEL))
        assertEquals("box", nodeA.attributes.get(AttributeKey.SHAPE))
    }
    
    @Test
    fun testUndirectedGraphBuilding() {
        val graph = undirectedGraph("undirected") {
            node("X")
            node("Y")
            edge("X", "Y")
        }
        
        assertEquals("undirected", graph.id)
        assertEquals(false, graph.isDirected)
        assertEquals(2, graph.nodes.size)
        assertEquals(1, graph.edges.size)
    }
    
    @Test
    fun testGraphWithSubgraph() {
        val graph = digraph("main") {
            node("start")
            node("end")
            
            subgraph("cluster_0") {
                node("p1")
                node("p2")
                edge("p1", "p2")
            }
        }
        
        assertEquals("main", graph.id)
        assertEquals(2, graph.nodes.size) // Only direct nodes: start, end
        assertEquals(0, graph.edges.size) // No direct edges
        assertEquals(1, graph.subgraphs.size)
        
        // Check that getAllNodes includes subgraph nodes
        val allNodes = graph.getAllNodes()
        assertEquals(4, allNodes.size) // start, end (main) + p1, p2 (subgraph)
        
        val allEdges = graph.getAllEdges()
        assertEquals(1, allEdges.size) // p1->p2 from subgraph
    }
    
    @Test
    fun testNodePositioning() {
        val graph = digraph("positioned") {
            node("A") {
                position(10.0, 20.0)
                label("Positioned Node")
            }
        }
        
        val nodeA = graph.getNode("A")!!
        assertEquals(Point(10.0, 20.0), nodeA.position)
        assertEquals("Positioned Node", nodeA.attributes.get(AttributeKey.LABEL))
    }
    
    @Test
    fun testEdgeControlPoints() {
        val graph = digraph("with_control_points") {
            node("A")
            node("B")
            
            edge("A", "B") {
                controlPoint(5.0, 5.0)
                controlPoint(10.0, 10.0)
            }
        }
        
        val edge = graph.edges.first()
        assertEquals(2, edge.controlPoints.size)
        assertEquals(Point(5.0, 5.0), edge.controlPoints[0])
        assertEquals(Point(10.0, 10.0), edge.controlPoints[1])
    }
    
    @Test
    fun testGraphValidation() {
        val builder = GraphBuilder("test")
        
        // Add nodes and edges
        builder.node("A")
        builder.node("B")
        builder.edge("A", "B")
        
        val validation = builder.validate()
        assertTrue(validation.isValid)
        
        // Test building
        val graph = builder.build()
        assertEquals("test", graph.id)
        assertEquals(2, graph.nodes.size)
        assertEquals(1, graph.edges.size)
    }
    
    @Test
    fun testBuilderConvenienceMethods() {
        val graph = digraph("convenience") {
            node("styled") {
                label("Styled Node")
                shape("diamond")
                color(Color.Named("red"))
                fillColor(Color.RGB(255, 255, 0))
                style("filled")
            }
            
            node("target")
            
            edge("styled", "target") {
                label("Styled Edge")
                color(Color.Hex("#00FF00"))
                style("dashed")
                penWidth(2.0)
            }
        }
        
        val styledNode = graph.getNode("styled")!!
        assertEquals("Styled Node", styledNode.attributes.get(AttributeKey.LABEL))
        assertEquals("diamond", styledNode.attributes.get(AttributeKey.SHAPE))
        assertEquals(Color.Named("red"), styledNode.attributes.get(AttributeKey.COLOR))
        assertEquals(Color.RGB(255, 255, 0), styledNode.attributes.get(AttributeKey.FILLCOLOR))
        assertEquals("filled", styledNode.attributes.get(AttributeKey.STYLE))
        
        val edge = graph.edges.first()
        assertEquals("Styled Edge", edge.attributes.get(AttributeKey.LABEL))
        assertEquals(Color.Hex("#00FF00"), edge.attributes.get(AttributeKey.COLOR))
        assertEquals("dashed", edge.attributes.get(AttributeKey.STYLE))
        assertEquals(2.0, edge.attributes.get(AttributeKey.PENWIDTH))
    }
}