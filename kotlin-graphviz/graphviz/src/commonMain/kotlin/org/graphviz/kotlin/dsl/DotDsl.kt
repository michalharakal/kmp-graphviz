package org.graphviz.kotlin.dsl

import org.graphviz.kotlin.builder.EdgeBuilder
import org.graphviz.kotlin.builder.GraphBuilder
import org.graphviz.kotlin.builder.digraph as builderDigraph
import org.graphviz.kotlin.builder.graph as builderGraph
import org.graphviz.kotlin.builder.undirectedGraph as builderUndirectedGraph
import org.graphviz.kotlin.model.Graph

/**
 * Lightweight DSL facade that delegates to the core GraphBuilder API,
 * keeping a concise syntax for simple graph definitions.
 */

// Re-expose core DSL entry points in a friendlier package
fun graph(id: String, directed: Boolean = true, configure: GraphBuilder.() -> Unit): Graph =
    builderGraph(id, directed, configure)

// Anonymous graph helpers (default id "G") for a simpler DSL
fun graph(directed: Boolean = true, configure: GraphBuilder.() -> Unit): Graph =
    builderGraph("G", directed, configure)

fun digraph(id: String, configure: GraphBuilder.() -> Unit): Graph =
    builderDigraph(id, configure)

fun undirectedGraph(id: String, configure: GraphBuilder.() -> Unit): Graph =
    builderUndirectedGraph(id, configure)

fun digraph(configure: GraphBuilder.() -> Unit): Graph =
    builderDigraph("G", configure)

fun undirectedGraph(configure: GraphBuilder.() -> Unit): Graph =
    builderUndirectedGraph("G", configure)

// Convenience attribute helpers for simple String-based styling
fun org.graphviz.kotlin.builder.NodeBuilder.color(color: String) = attribute("color", color)
fun org.graphviz.kotlin.builder.NodeBuilder.fillColor(color: String) = attribute("fillcolor", color)
fun EdgeBuilder.color(color: String) = attribute("color", color)
