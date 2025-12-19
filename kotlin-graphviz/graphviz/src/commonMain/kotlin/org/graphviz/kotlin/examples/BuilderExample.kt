package org.graphviz.kotlin.examples

import org.graphviz.kotlin.builder.*
import org.graphviz.kotlin.model.*

/**
 * Example demonstrating the graph builder API.
 */
object BuilderExample {
    
    fun createSimpleGraph(): Graph {
        return digraph("example") {
            // Add nodes with attributes
            node("A") {
                label("Node A")
                shape("box")
                color(Color.Named("red"))
            }
            
            node("B") {
                label("Node B")
                shape("ellipse")
                color(Color.Named("blue"))
            }
            
            node("C") {
                label("Node C")
                position(100.0, 50.0)
            }
            
            // Add edges
            edge("A", "B") {
                label("A to B")
                color(Color.Named("green"))
            }
            
            edge("B", "C") {
                style("dashed")
            }
            
            // Set graph attributes
            attribute("rankdir", "TB")
            attribute("bgcolor", "white")
        }
    }
    
    fun createGraphWithSubgraph(): Graph {
        return digraph("main") {
            node("start")
            node("end")
            
            subgraph("cluster_0") {
                attribute("label", "Process")
                attribute("style", "filled")
                attribute("color", "lightgrey")
                
                node("p1") {
                    label("Process 1")
                }
                
                node("p2") {
                    label("Process 2")
                }
                
                edge("p1", "p2")
            }
            
            edge("start", "p1")
            edge("p2", "end")
        }
    }
}