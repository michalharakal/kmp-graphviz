package org.graphviz.kotlin.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DotParserDebugTest : FunSpec({
    
    test("debug complex graph parsing") {
        val input = """
            strict digraph ComplexGraph {
                graph [rankdir=TB, bgcolor="lightgray"];
                node [shape=box, style=filled];
                edge [color=blue];
                
                // Main nodes
                Start [label="Start Node", fillcolor=green];
                End [label="End Node", fillcolor=red];
                
                // Process subgraph
                subgraph cluster_process {
                    label = "Processing";
                    Process1 -> Process2 -> Process3;
                }
                
                // Connect everything
                Start -> Process1 [label="begin"];
                Process3 -> End [label="finish", style=bold];
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        
        if (result.isError) {
            val error = result as ParseResult.Error
            println("Parse error: ${error.message}")
            println("Line: ${error.line}, Column: ${error.column}")
            println("Context: ${error.context}")
        } else {
            val graph = result.getOrThrow()
            println("Graph parsed successfully!")
            println("ID: ${graph.id}")
            println("Directed: ${graph.isDirected}")
            println("Nodes: ${graph.nodes.size}")
            println("Edges: ${graph.edges.size}")
            println("Subgraphs: ${graph.subgraphs.size}")
            
            graph.nodes.forEach { node ->
                println("Node: ${node.id}")
            }
            
            graph.subgraphs.forEach { subgraph ->
                println("Subgraph: ${subgraph.id} (nodes: ${subgraph.nodes.size}, edges: ${subgraph.edges.size})")
            }
        }
        
        // Just print debug info, don't fail
        println("Test completed")
    }
})