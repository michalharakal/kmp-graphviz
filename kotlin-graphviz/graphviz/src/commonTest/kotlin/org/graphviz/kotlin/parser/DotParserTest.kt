package org.graphviz.kotlin.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.graphviz.kotlin.model.AttributeKey

class DotParserTest : FunSpec({
    
    test("should parse simple directed graph") {
        val input = """
            digraph G {
                A -> B;
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isSuccess shouldBe true
        
        val graph = result.getOrThrow()
        graph.id shouldBe "G"
        graph.isDirected shouldBe true
        graph.nodes shouldHaveSize 2
        graph.edges shouldHaveSize 1
        
        val nodeA = graph.getNode("A")
        val nodeB = graph.getNode("B")
        nodeA shouldNotBe null
        nodeB shouldNotBe null
        
        val edge = graph.edges.first()
        edge.source shouldBe nodeA
        edge.target shouldBe nodeB
    }
    
    test("should parse simple undirected graph") {
        val input = """
            graph G {
                A -- B;
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isSuccess shouldBe true
        
        val graph = result.getOrThrow()
        graph.id shouldBe "G"
        graph.isDirected shouldBe false
        graph.nodes shouldHaveSize 2
        graph.edges shouldHaveSize 1
    }
    
    test("should parse graph with node attributes") {
        val input = """
            digraph G {
                A [label="Node A", shape=box];
                B [color=red];
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isSuccess shouldBe true
        
        val graph = result.getOrThrow()
        val nodeA = graph.getNode("A")!!
        val nodeB = graph.getNode("B")!!
        
        nodeA.attributes.get(AttributeKey.LABEL) shouldBe "Node A"
        nodeA.attributes.get(AttributeKey.SHAPE) shouldBe "box"
        nodeB.attributes.getRaw("color")?.toString() shouldBe "StringValue(value=red)"
    }
    
    test("should parse graph with edge attributes") {
        val input = """
            digraph G {
                A -> B [label="edge AB", color=blue];
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isSuccess shouldBe true
        
        val graph = result.getOrThrow()
        val edge = graph.edges.first()
        
        edge.attributes.get(AttributeKey.LABEL) shouldBe "edge AB"
        edge.attributes.getRaw("color")?.toString() shouldBe "StringValue(value=blue)"
    }
    
    test("should parse graph with multiple edges") {
        val input = """
            digraph G {
                A -> B -> C;
                A -> D;
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isSuccess shouldBe true
        
        val graph = result.getOrThrow()
        graph.nodes shouldHaveSize 4
        graph.edges shouldHaveSize 3
        
        val nodeIds = graph.nodes.map { it.id }.toSet()
        nodeIds shouldContain "A"
        nodeIds shouldContain "B"
        nodeIds shouldContain "C"
        nodeIds shouldContain "D"
    }
    
    test("should parse graph with subgraph") {
        val input = """
            digraph G {
                A -> B;
                subgraph cluster_0 {
                    C -> D;
                }
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isSuccess shouldBe true
        
        val graph = result.getOrThrow()
        graph.nodes shouldHaveSize 2 // A, B (C, D are in subgraph)
        graph.edges shouldHaveSize 1 // A -> B (C -> D is in subgraph)
        graph.subgraphs shouldHaveSize 1
        
        val subgraph = graph.subgraphs.first()
        subgraph.id shouldBe "cluster_0"
        subgraph.nodes shouldHaveSize 2
        subgraph.edges shouldHaveSize 1
    }
    
    test("should parse graph with graph attributes") {
        val input = """
            digraph G {
                graph [rankdir=LR, bgcolor=white];
                A -> B;
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isSuccess shouldBe true
        
        val graph = result.getOrThrow()
        graph.attributes.getRaw("rankdir")?.toString() shouldBe "StringValue(value=LR)"
        graph.attributes.getRaw("bgcolor")?.toString() shouldBe "StringValue(value=white)"
    }
    
    test("should parse strict graph") {
        val input = """
            strict digraph G {
                A -> B;
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isSuccess shouldBe true
        
        val graph = result.getOrThrow()
        graph.id shouldBe "G"
        graph.isDirected shouldBe true
    }
    
    test("should parse graph without explicit ID") {
        val input = """
            digraph {
                A -> B;
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isSuccess shouldBe true
        
        val graph = result.getOrThrow()
        graph.id shouldBe "G" // default name
    }
    
    test("should parse graph with numeric attributes") {
        val input = """
            digraph G {
                A [width=2.5, height=1.0];
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isSuccess shouldBe true
        
        val graph = result.getOrThrow()
        val nodeA = graph.getNode("A")!!
        
        nodeA.attributes.get(AttributeKey.WIDTH) shouldBe 2.5
        nodeA.attributes.get(AttributeKey.HEIGHT) shouldBe 1.0
    }
    
    test("should parse graph with comments") {
        val input = """
            // This is a test graph
            digraph G {
                /* Multi-line
                   comment */
                A -> B; // Edge comment
                # Hash comment
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isSuccess shouldBe true
        
        val graph = result.getOrThrow()
        graph.nodes shouldHaveSize 2
        graph.edges shouldHaveSize 1
    }
    
    test("should handle syntax error - missing brace") {
        val input = """
            digraph G 
                A -> B;
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isError shouldBe true
        
        val error = result as ParseResult.Error
        error.message shouldBe "Expected '{' to start graph body"
    }
    
    test("should handle syntax error - invalid edge operator") {
        val input = """
            graph G {
                A -> B;
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isError shouldBe true
        
        // This should fail because we're using -> in an undirected graph
        val error = result as ParseResult.Error
        error.message shouldBe "Expected statement (node, edge, subgraph, or attribute)"
    }
    
    test("should handle syntax error - missing attribute value") {
        val input = """
            digraph G {
                A [label=];
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isError shouldBe true
        
        val error = result as ParseResult.Error
        error.message shouldBe "Expected attribute value (identifier, string, or number)"
    }
    
    test("should handle empty graph") {
        val input = """
            digraph G {
            }
        """.trimIndent()
        
        val result = DotParser.parse(input)
        result.isSuccess shouldBe true
        
        val graph = result.getOrThrow()
        graph.nodes shouldHaveSize 0
        graph.edges shouldHaveSize 0
    }
    
    test("should parse complex graph with all features") {
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
        result.isSuccess shouldBe true
        
        val graph = result.getOrThrow()
        graph.id shouldBe "ComplexGraph"
        graph.isDirected shouldBe true
        
        // Check main graph nodes (Start, End, Process1, Process3 due to cross-references)
        graph.nodes shouldHaveSize 4
        graph.getNode("Start") shouldNotBe null
        graph.getNode("End") shouldNotBe null
        graph.getNode("Process1") shouldNotBe null
        graph.getNode("Process3") shouldNotBe null
        
        // Check subgraph
        graph.subgraphs shouldHaveSize 1
        val subgraph = graph.subgraphs.first()
        subgraph.nodes shouldHaveSize 3
        subgraph.edges shouldHaveSize 2
        
        // Check graph attributes
        graph.attributes.getRaw("rankdir")?.toString() shouldBe "StringValue(value=TB)"
        graph.attributes.getRaw("bgcolor")?.toString() shouldBe "StringValue(value=lightgray)"
    }
})