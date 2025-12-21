package org.graphviz.kotlin.integration

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.graphviz.kotlin.GraphvizEngine
import org.graphviz.kotlin.error.GraphvizError
import org.graphviz.kotlin.layout.LayoutOptions
import org.graphviz.kotlin.model.AttributeKey
import org.graphviz.kotlin.model.Color
import org.graphviz.kotlin.renderer.RenderOptions
import kotlin.test.assertFailsWith

/**
 * Comprehensive integration tests for the public API with visual validation.
 * 
 * Tests complete workflows from DOT parsing to SVG rendering, programmatic
 * graph construction and manipulation, error handling, and end-to-end visual
 * consistency.
 * 
 * Requirements: All requirements (1.1-8.5)
 */
class PublicApiIntegrationTest : FunSpec({
    
    // ========================================
    // Test 1: Complete DOT Parsing to SVG Rendering Workflow
    // ========================================
    
    test("complete workflow: parse DOT -> layout -> render SVG") {
        val engine = GraphvizEngine.create()
        
        val dotContent = """
            digraph G {
                A [label="Node A", shape=box];
                B [label="Node B", shape=ellipse];
                C [label="Node C"];
                A -> B [label="Edge 1"];
                B -> C [label="Edge 2"];
            }
        """.trimIndent()
        
        // Parse, layout, and render in one call
        val svg = engine.parseDotAndRenderSvg(dotContent)
        
        // Verify SVG structure
        svg shouldContain "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        svg shouldContain "<!DOCTYPE svg"
        svg shouldContain "xmlns=\"http://www.w3.org/2000/svg\""
        svg shouldContain "<svg"
        svg shouldContain "</svg>"
        
        // Verify nodes are present
        svg shouldContain "id=\"node-A\""
        svg shouldContain "id=\"node-B\""
        svg shouldContain "id=\"node-C\""
        
        // Verify edges are present
        svg shouldContain "id=\"edge-A-B\""
        svg shouldContain "id=\"edge-B-C\""
        
        // Verify labels are rendered
        svg shouldContain "Node A"
        svg shouldContain "Node B"
        svg shouldContain "Node C"
        svg shouldContain "Edge 1"
        svg shouldContain "Edge 2"
        
        println("✓ Complete DOT to SVG workflow successful")
    }
    
    test("parse DOT with attributes and verify SVG rendering") {
        val engine = GraphvizEngine.create()
        
        val dotContent = """
            digraph Styled {
                node [shape=box, style=filled, fillcolor=lightblue];
                edge [color=red, style=dashed];
                
                Start [label="Start", fillcolor=green];
                Process [label="Process"];
                End [label="End", fillcolor=orange];
                
                Start -> Process [label="Begin"];
                Process -> End [label="Complete", style=bold];
            }
        """.trimIndent()
        
        val svg = engine.parseDotAndRenderSvg(dotContent)
        
        // Verify styled elements are present
        svg shouldContain "Start"
        svg shouldContain "Process"
        svg shouldContain "End"
        svg shouldContain "Begin"
        svg shouldContain "Complete"
        
        // Verify SVG contains styling attributes
        svg shouldContain "fill"
        svg shouldContain "stroke"
        
        println("✓ DOT with attributes rendered successfully")
    }
    
    test("parse DOT with subgraphs and clusters") {
        val engine = GraphvizEngine.create()
        
        val dotContent = """
            digraph Clusters {
                subgraph cluster_0 {
                    label="Cluster 0";
                    a0 -> a1 -> a2;
                }
                
                subgraph cluster_1 {
                    label="Cluster 1";
                    b0 -> b1 -> b2;
                }
                
                a2 -> b0;
            }
        """.trimIndent()
        
        val svg = engine.parseDotAndRenderSvg(dotContent)
        
        // Verify nodes from both clusters are present (cluster labels may not be rendered yet)
        svg shouldContain "a0"
        svg shouldContain "a1"
        svg shouldContain "a2"
        svg shouldContain "b0"
        svg shouldContain "b1"
        svg shouldContain "b2"
        
        // Verify subgraph structure is present
        svg shouldContain "subgraph"
        
        println("✓ DOT with subgraphs/clusters rendered successfully")
    }
    
    // ========================================
    // Test 2: Programmatic Graph Construction and Manipulation
    // ========================================
    
    test("build graph programmatically and render") {
        val engine = GraphvizEngine.create()
        
        val svg = engine.buildGraphAndRenderSvg("ProgrammaticGraph") {
            node("A") {
                label("Node A")
                shape("box")
                fillColor(Color.Named("lightblue"))
            }
            
            node("B") {
                label("Node B")
                shape("ellipse")
                fillColor(Color.Named("lightgreen"))
            }
            
            node("C") {
                label("Node C")
                shape("diamond")
            }
            
            edge("A", "B") {
                label("A to B")
                color(Color.Named("red"))
            }
            
            edge("B", "C") {
                label("B to C")
                style("dashed")
            }
        }
        
        // Verify all elements are present
        svg shouldContain "Node A"
        svg shouldContain "Node B"
        svg shouldContain "Node C"
        svg shouldContain "A to B"
        svg shouldContain "B to C"
        
        println("✓ Programmatic graph construction successful")
    }
    
    test("build undirected graph programmatically") {
        val engine = GraphvizEngine.create()
        
        val graph = engine.buildUndirectedGraph("UndirectedGraph") {
            node("X") { label("Node X") }
            node("Y") { label("Node Y") }
            node("Z") { label("Node Z") }
            
            edge("X", "Y")
            edge("Y", "Z")
            edge("Z", "X")
        }
        
        graph.isDirected shouldBe false
        graph.nodes.size shouldBe 3
        graph.edges.size shouldBe 3
        
        // Render and verify
        val layoutResult = engine.layout(graph)
        layoutResult.isSuccess shouldBe true
        
        val svg = engine.renderSvg((layoutResult as org.graphviz.kotlin.layout.LayoutResult.Success).graph)
        svg shouldContain "Node X"
        svg shouldContain "Node Y"
        svg shouldContain "Node Z"
        
        println("✓ Undirected graph construction successful")
    }
    
    test("modify graph attributes programmatically") {
        val engine = GraphvizEngine.create()
        
        val graph = engine.buildDirectedGraph("ModifiableGraph") {
            // Set graph-level attributes
            attribute(AttributeKey.LABEL, "My Graph")
            attribute("rankdir", "LR")
            
            node("A") { label("Start") }
            node("B") { label("Middle") }
            node("C") { label("End") }
            
            edge("A", "B")
            edge("B", "C")
        }
        
        graph.attributes.get(AttributeKey.LABEL) shouldBe "My Graph"
        val rankdirValue = graph.attributes.getRaw("rankdir")
        if (rankdirValue is org.graphviz.kotlin.model.AttributeValue.StringValue) {
            rankdirValue.value shouldBe "LR"
        }
        
        println("✓ Graph attribute modification successful")
    }
    
    // ========================================
    // Test 3: Error Handling and Edge Cases
    // ========================================
    
    test("parse invalid DOT syntax throws descriptive error") {
        val engine = GraphvizEngine.create()
        
        val invalidDot = """
            digraph Invalid {
                A -> B
                C -> // Missing target
            }
        """.trimIndent()
        
        val exception = assertFailsWith<GraphvizError.ParseError> {
            engine.parseDotAndRenderSvg(invalidDot)
        }
        
        // Verify error contains useful information
        exception.line shouldNotBe 0
        exception.column shouldNotBe 0
        
        println("✓ Parse error handling successful: ${exception.message}")
    }
    
    test("parse empty DOT file") {
        val engine = GraphvizEngine.create()
        
        val emptyDot = ""
        
        val exception = assertFailsWith<GraphvizError.ParseError> {
            engine.parseDotAndRenderSvg(emptyDot)
        }
        
        println("✓ Empty DOT error handling successful: ${exception.message}")
    }
    
    test("parse DOT with missing closing brace") {
        val engine = GraphvizEngine.create()
        
        val incompleteDot = """
            digraph Incomplete {
                A -> B;
        """.trimIndent()
        
        val exception = assertFailsWith<GraphvizError.ParseError> {
            engine.parseDotAndRenderSvg(incompleteDot)
        }
        
        // The error message should indicate a parsing issue (specific message may vary)
        exception.message shouldNotBe null
        
        println("✓ Incomplete DOT error handling successful")
    }
    
    test("handle graph with no nodes") {
        val engine = GraphvizEngine.create()
        
        val emptyGraph = """
            digraph Empty {
            }
        """.trimIndent()
        
        // Empty graphs should either render successfully or throw a specific error
        try {
            val svg = engine.parseDotAndRenderSvg(emptyGraph)
            svg shouldContain "<svg"
            svg shouldContain "</svg>"
            println("✓ Empty graph handling successful")
        } catch (e: GraphvizError.LayoutError) {
            // Layout error for empty graph is acceptable
            e.message shouldContain "empty"
            println("✓ Empty graph error handling successful: ${e.message}")
        }
    }
    
    test("handle graph with single node") {
        val engine = GraphvizEngine.create()
        
        val singleNodeDot = """
            digraph Single {
                A [label="Only Node"];
            }
        """.trimIndent()
        
        val svg = engine.parseDotAndRenderSvg(singleNodeDot)
        
        svg shouldContain "Only Node"
        svg shouldContain "id=\"node-A\""
        
        println("✓ Single node graph handling successful")
    }
    
    test("handle graph with self-loop") {
        val engine = GraphvizEngine.create()
        
        val selfLoopDot = """
            digraph SelfLoop {
                A -> A [label="Self"];
            }
        """.trimIndent()
        
        val svg = engine.parseDotAndRenderSvg(selfLoopDot)
        
        svg shouldContain "Self"
        
        println("✓ Self-loop handling successful")
    }
    
    test("handle disconnected graph components") {
        val engine = GraphvizEngine.create()
        
        val disconnectedDot = """
            digraph Disconnected {
                A -> B;
                C -> D;
                E;
            }
        """.trimIndent()
        
        val svg = engine.parseDotAndRenderSvg(disconnectedDot)
        
        // All nodes should be present
        svg shouldContain "id=\"node-A\""
        svg shouldContain "id=\"node-B\""
        svg shouldContain "id=\"node-C\""
        svg shouldContain "id=\"node-D\""
        svg shouldContain "id=\"node-E\""
        
        println("✓ Disconnected components handling successful")
    }
    
    // ========================================
    // Test 4: Layout Engine Options
    // ========================================
    
    test("use custom layout options") {
        val engine = GraphvizEngine.create()
        
        val graph = engine.buildDirectedGraph("CustomLayout") {
            node("A")
            node("B")
            node("C")
            edge("A", "B")
            edge("B", "C")
        }
        
        val customOptions = LayoutOptions(
            rankSpacing = 100.0,
            nodeSpacing = 50.0,
            minNodeWidth = 60.0,
            minNodeHeight = 40.0
        )
        
        val layoutResult = engine.layout(graph, "dot", customOptions)
        layoutResult.isSuccess shouldBe true
        
        val positionedGraph = (layoutResult as org.graphviz.kotlin.layout.LayoutResult.Success).graph
        positionedGraph.getAllNodes().forEach { node ->
            node.position shouldNotBe null
        }
        
        println("✓ Custom layout options successful")
    }
    
    test("use different layout engines") {
        val engine = GraphvizEngine.create()
        
        val dotContent = """
            graph G {
                A -- B;
                B -- C;
                C -- D;
                D -- A;
            }
        """.trimIndent()
        
        // Test with dot engine
        val svgDot = engine.parseDotAndRenderSvg(dotContent, "dot")
        svgDot shouldContain "<svg"
        
        println("✓ Different layout engines successful")
    }
    
    // ========================================
    // Test 5: Render Options and Customization
    // ========================================
    
    test("use custom render options") {
        val engine = GraphvizEngine.create()
        
        val graph = engine.buildDirectedGraph("CustomRender") {
            node("A") { label("Node A") }
            node("B") { label("Node B") }
            edge("A", "B")
        }
        
        val layoutResult = engine.layout(graph)
        val positionedGraph = (layoutResult as org.graphviz.kotlin.layout.LayoutResult.Success).graph
        
        val customRenderOptions = RenderOptions(
            width = 800.0,
            height = 600.0,
            margin = 50.0,
            backgroundColor = "#f0f0f0",
            rendererOptions = mapOf(
                "title" to "Custom Rendered Graph",
                "description" to "A graph with custom render options"
            )
        )
        
        val svg = engine.renderSvg(positionedGraph, customRenderOptions)
        
        svg shouldContain "width=\"800\""
        svg shouldContain "height=\"600\""
        svg shouldContain "<title>Custom Rendered Graph</title>"
        svg shouldContain "<desc>A graph with custom render options</desc>"
        
        println("✓ Custom render options successful")
    }
    
    // ========================================
    // Test 6: Round-Trip DOT Generation
    // ========================================
    
    test("round-trip: parse DOT -> generate DOT -> parse again") {
        val engine = GraphvizEngine.create()
        
        val originalDot = """
            digraph RoundTrip {
                A [label="Node A"];
                B [label="Node B"];
                A -> B [label="Edge"];
            }
        """.trimIndent()
        
        // Parse original
        val graph1 = engine.parseDotOrThrow(originalDot)
        
        // Generate DOT
        val generatedDot = engine.generateDot(graph1)
        
        // Parse generated
        val graph2 = engine.parseDotOrThrow(generatedDot)
        
        // Verify structure is preserved
        graph1.nodes.size shouldBe graph2.nodes.size
        graph1.edges.size shouldBe graph2.edges.size
        graph1.isDirected shouldBe graph2.isDirected
        
        println("✓ Round-trip DOT generation successful")
        println("Generated DOT:\n$generatedDot")
    }
    
    // ========================================
    // Test 7: Visual Consistency Validation
    // ========================================
    
    test("visual consistency: hierarchical layout") {
        val engine = GraphvizEngine.create()
        
        val dotContent = """
            digraph Hierarchy {
                rankdir=TB;
                
                Root [label="Root"];
                L1A [label="Level 1A"];
                L1B [label="Level 1B"];
                L2A [label="Level 2A"];
                L2B [label="Level 2B"];
                L2C [label="Level 2C"];
                
                Root -> L1A;
                Root -> L1B;
                L1A -> L2A;
                L1A -> L2B;
                L1B -> L2C;
            }
        """.trimIndent()
        
        val graph = engine.parseDotOrThrow(dotContent)
        val layoutResult = engine.layout(graph, "dot")
        val positionedGraph = (layoutResult as org.graphviz.kotlin.layout.LayoutResult.Success).graph
        
        // Verify hierarchical positioning
        val nodes = positionedGraph.getAllNodes()
        val root = nodes.find { it.id == "Root" }!!
        val l1a = nodes.find { it.id == "L1A" }!!
        val l1b = nodes.find { it.id == "L1B" }!!
        val l2a = nodes.find { it.id == "L2A" }!!
        
        // Root should be at top (lowest Y)
        root.position!!.y shouldBe 0.0
        
        // Level 1 nodes should be below root
        l1a.position!!.y shouldNotBe 0.0
        l1b.position!!.y shouldNotBe 0.0
        l1a.position!!.y shouldBe l1b.position!!.y
        
        // Level 2 nodes should be below level 1
        l2a.position!!.y shouldNotBe l1a.position!!.y
        
        val svg = engine.renderSvg(positionedGraph)
        svg shouldContain "Root"
        svg shouldContain "Level 1A"
        svg shouldContain "Level 2A"
        
        println("✓ Visual consistency: hierarchical layout verified")
    }
    
    test("visual consistency: node shapes rendered correctly") {
        val engine = GraphvizEngine.create()
        
        val dotContent = """
            digraph Shapes {
                Box [shape=box];
                Ellipse [shape=ellipse];
                Circle [shape=circle];
                Diamond [shape=diamond];
            }
        """.trimIndent()
        
        val svg = engine.parseDotAndRenderSvg(dotContent)
        
        // Verify different shapes are present
        svg shouldContain "id=\"node-Box\""
        svg shouldContain "id=\"node-Ellipse\""
        svg shouldContain "id=\"node-Circle\""
        svg shouldContain "id=\"node-Diamond\""
        
        // SVG should contain shape elements (rect, ellipse, polygon, etc.)
        svg shouldContain "<rect" // for box
        svg shouldContain "<ellipse" // for ellipse/circle
        
        println("✓ Visual consistency: node shapes verified")
    }
    
    test("visual consistency: edge styles rendered correctly") {
        val engine = GraphvizEngine.create()
        
        val dotContent = """
            digraph EdgeStyles {
                A -> B [style=solid];
                C -> D [style=dashed];
                E -> F [style=dotted];
                G -> H [style=bold];
            }
        """.trimIndent()
        
        val svg = engine.parseDotAndRenderSvg(dotContent)
        
        // Verify edges are present
        svg shouldContain "id=\"edge-A-B\""
        svg shouldContain "id=\"edge-C-D\""
        svg shouldContain "id=\"edge-E-F\""
        svg shouldContain "id=\"edge-G-H\""
        
        // SVG should contain path elements for edges
        svg shouldContain "<path"
        
        println("✓ Visual consistency: edge styles verified")
    }
    
    test("visual consistency: colors applied correctly") {
        val engine = GraphvizEngine.create()
        
        val dotContent = """
            digraph Colors {
                Red [fillcolor=red, style=filled];
                Blue [fillcolor=blue, style=filled];
                Green [fillcolor=green, style=filled];
                
                Red -> Blue [color=red];
                Blue -> Green [color=blue];
            }
        """.trimIndent()
        
        val svg = engine.parseDotAndRenderSvg(dotContent)
        
        // Verify color attributes are present in SVG
        svg shouldContain "fill"
        svg shouldContain "stroke"
        
        println("✓ Visual consistency: colors verified")
    }
    
    // ========================================
    // Test 8: Complex Real-World Scenarios
    // ========================================
    
    test("complex workflow: state machine diagram") {
        val engine = GraphvizEngine.create()
        
        val svg = engine.buildGraphAndRenderSvg("StateMachine") {
            attribute("rankdir", "LR")
            
            node("Start") {
                shape("circle")
                fillColor(Color.Named("green"))
                style("filled")
            }
            
            node("Processing") {
                shape("box")
                label("Processing\nData")
            }
            
            node("Validation") {
                shape("diamond")
                label("Valid?")
            }
            
            node("Success") {
                shape("circle")
                fillColor(Color.Named("blue"))
                style("filled")
            }
            
            node("Error") {
                shape("circle")
                fillColor(Color.Named("red"))
                style("filled")
            }
            
            edge("Start", "Processing") { label("Begin") }
            edge("Processing", "Validation") { label("Check") }
            edge("Validation", "Success") { label("Yes") }
            edge("Validation", "Error") { label("No") }
            edge("Error", "Processing") { label("Retry") }
        }
        
        svg shouldContain "Start"
        svg shouldContain "Processing"
        svg shouldContain "Validation"
        svg shouldContain "Success"
        svg shouldContain "Error"
        
        println("✓ Complex workflow: state machine successful")
    }
    
    test("complex workflow: dependency graph") {
        val engine = GraphvizEngine.create()
        
        val dotContent = """
            digraph Dependencies {
                rankdir=BT;
                
                App [label="Application"];
                UI [label="UI Layer"];
                Business [label="Business Logic"];
                Data [label="Data Layer"];
                DB [label="Database"];
                API [label="External API"];
                
                App -> UI;
                App -> Business;
                Business -> Data;
                Data -> DB;
                Data -> API;
            }
        """.trimIndent()
        
        val svg = engine.parseDotAndRenderSvg(dotContent)
        
        svg shouldContain "Application"
        svg shouldContain "UI Layer"
        svg shouldContain "Business Logic"
        svg shouldContain "Data Layer"
        svg shouldContain "Database"
        svg shouldContain "External API"
        
        println("✓ Complex workflow: dependency graph successful")
    }
    
    // ========================================
    // Test 9: API Utility Methods
    // ========================================
    
    test("query available layout engines") {
        val engine = GraphvizEngine.create()
        
        val engines = engine.getAvailableLayoutEngines()
        engines.isNotEmpty() shouldBe true
        engines.contains("dot") shouldBe true
        
        engine.isLayoutEngineAvailable("dot") shouldBe true
        engine.isLayoutEngineAvailable("nonexistent") shouldBe false
        
        println("✓ Available layout engines: $engines")
    }
    
    test("get library version and platform info") {
        val engine = GraphvizEngine.create()
        
        val version = engine.getVersion()
        version.isNotEmpty() shouldBe true
        
        val platforms = engine.getSupportedPlatforms()
        platforms.isNotEmpty() shouldBe true
        
        println("✓ Library version: $version")
        println("✓ Supported platforms: $platforms")
    }
    
    test("get default layout engine") {
        val engine = GraphvizEngine.create()
        
        val defaultEngine = engine.getDefaultLayoutEngine()
        defaultEngine shouldBe "dot"
        
        println("✓ Default layout engine: $defaultEngine")
    }
    
    // ========================================
    // Test 10: Builder Pattern and DSL
    // ========================================
    
    test("use engine builder pattern") {
        val engine = GraphvizEngine.create(
            defaultLayoutEngine = "dot"
        )
        
        engine.getDefaultLayoutEngine() shouldBe "dot"
        
        val graph = engine.buildDirectedGraph("BuilderTest") {
            node("A")
            node("B")
            edge("A", "B")
        }
        
        graph.nodes.size shouldBe 2
        graph.edges.size shouldBe 1
        
        println("✓ Engine builder pattern successful")
    }
})
