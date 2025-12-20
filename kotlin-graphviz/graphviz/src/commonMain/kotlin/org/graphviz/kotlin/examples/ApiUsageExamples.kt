package org.graphviz.kotlin.examples

import org.graphviz.kotlin.GraphvizEngine
import org.graphviz.kotlin.extensions.*
import org.graphviz.kotlin.layout.LayoutOptions
import org.graphviz.kotlin.model.AttributeKey
import org.graphviz.kotlin.model.Color
import org.graphviz.kotlin.renderer.RenderOptions
import org.graphviz.kotlin.workflow.DotToSvgWorkflow
import org.graphviz.kotlin.workflow.GraphBuildWorkflow
import org.graphviz.kotlin.workflow.dotToSvg
import org.graphviz.kotlin.workflow.directedGraphToSvg

/**
 * Comprehensive examples demonstrating the public API usage patterns.
 * These examples show the various ways to use the Kotlin Multiplatform Graphviz library.
 */
object ApiUsageExamples {
    
    // ========================================
    // Basic Usage Examples
    // ========================================
    
    /**
     * Example 1: Simple DOT parsing and SVG rendering
     */
    fun basicDotToSvg(): String {
        val dotContent = """
            digraph G {
                A -> B;
                B -> C;
                A -> C;
            }
        """.trimIndent()
        
        // Method 1: Using GraphvizEngine directly
        val engine = GraphvizEngine.create()
        return engine.parseDotAndRenderSvg(dotContent)
        
        // Method 2: Using extension function
        // return dotContent.dotToSvg()
        
        // Method 3: Using workflow DSL
        // return dotToSvg(dotContent)
    }
    
    /**
     * Example 2: Programmatic graph building
     */
    fun basicGraphBuilding(): String {
        val engine = GraphvizEngine.create()
        
        return engine.buildGraphAndRenderSvg("MyGraph") {
            node("A") {
                label("Node A")
                shape("box")
                color(Color.Named("blue"))
            }
            
            node("B") {
                label("Node B")
                shape("ellipse")
                fillColor(Color.Named("lightblue"))
                style("filled")
            }
            
            edge("A", "B") {
                label("Edge A->B")
                color(Color.Named("red"))
                style("dashed")
            }
        }
    }
    
    // ========================================
    // Advanced Configuration Examples
    // ========================================
    
    /**
     * Example 3: Custom layout and render options
     */
    fun customOptionsExample(): String {
        val dotContent = """
            digraph ComplexGraph {
                rankdir=TB;
                node [shape=box, style=filled, fillcolor=lightblue];
                edge [color=darkblue];
                
                A -> B -> C -> D;
                A -> E -> F -> D;
                B -> F;
                C -> E;
            }
        """.trimIndent()
        
        val engine = GraphvizEngine.create()
        
        // Parse the graph
        val graph = engine.parseDotOrThrow(dotContent)
        
        // Apply layout with custom options
        val layoutOptions = LayoutOptions(
            nodeSpacing = 100.0,
            rankSpacing = 150.0,
            minimizeCrossings = true,
            maxIterations = 200
        )
        val positionedGraph = engine.layoutOrThrow(graph, "dot", layoutOptions)
        
        // Render with custom options
        val renderOptions = RenderOptions(
            width = 800.0,
            height = 600.0,
            margin = 50.0,
            backgroundColor = "white",
            fontFamily = "Helvetica, Arial, sans-serif",
            fontSize = 14.0
        )
        
        return engine.renderSvg(positionedGraph, renderOptions)
    }
    
    /**
     * Example 4: Using workflow builders with configuration
     */
    fun workflowBuilderExample(): String {
        val dotContent = """
            digraph Workflow {
                Start -> Process1;
                Process1 -> Decision;
                Decision -> Process2 [label="Yes"];
                Decision -> End [label="No"];
                Process2 -> End;
            }
        """.trimIndent()
        
        return DotToSvgWorkflow.from(dotContent)
            .withLayout("dot")
            .withLayoutOptions {
                nodeSpacing(75.0)
                rankSpacing(100.0)
                minimizeCrossings(true)
            }
            .withRenderOptions {
                size(600.0, 400.0)
                backgroundColor("white")
                font("Arial", 12.0)
                optimize(true)
            }
            .toSvg()
    }
    
    /**
     * Example 5: Programmatic graph building with workflow
     */
    fun programmaticWorkflowExample(): String {
        return GraphBuildWorkflow.directed("NetworkDiagram")
            .withGraph {
                // Create nodes with different shapes and colors
                node("Router") {
                    label("Main Router")
                    shape("box")
                    fillColor(Color.Named("lightgray"))
                    style("filled")
                }
                
                node("Switch1") {
                    label("Switch 1")
                    shape("ellipse")
                    fillColor(Color.Named("lightblue"))
                    style("filled")
                }
                
                node("Switch2") {
                    label("Switch 2")
                    shape("ellipse")
                    fillColor(Color.Named("lightblue"))
                    style("filled")
                }
                
                node("Server") {
                    label("Web Server")
                    shape("box")
                    fillColor(Color.Named("lightgreen"))
                    style("filled")
                }
                
                // Create connections
                edge("Router", "Switch1") {
                    label("1Gbps")
                    color(Color.Named("blue"))
                }
                
                edge("Router", "Switch2") {
                    label("1Gbps")
                    color(Color.Named("blue"))
                }
                
                edge("Switch1", "Server") {
                    label("100Mbps")
                    color(Color.Named("green"))
                }
                
                edge("Switch2", "Server") {
                    label("100Mbps")
                    color(Color.Named("green"))
                    style("dashed")
                }
            }
            .withLayout("dot")
            .withLayoutOptions {
                nodeSpacing(80.0)
                rankSpacing(120.0)
            }
            .withRenderOptions {
                margin(30.0)
                backgroundColor("white")
            }
            .toSvg()
    }
    
    // ========================================
    // Extension Function Examples
    // ========================================
    
    /**
     * Example 6: Using extension functions for fluent API
     */
    fun extensionFunctionExample(): String {
        val dotContent = """
            digraph ExtensionExample {
                A [label="Start", shape=circle, fillcolor=green, style=filled];
                B [label="Process", shape=box];
                C [label="End", shape=circle, fillcolor=red, style=filled];
                
                A -> B -> C;
            }
        """.trimIndent()
        
        // Parse and render in one line
        return dotContent.dotToSvg(
            layoutEngine = "dot",
            layoutOptions = LayoutOptions.forHighQuality(),
            renderOptions = RenderOptions.forWeb()
        )
    }
    
    /**
     * Example 7: Chaining operations with extension functions
     */
    fun chainingExample(): String {
        val dotContent = """
            digraph ChainExample {
                node [shape=box, style=rounded];
                A -> B -> C;
                A -> D -> C;
            }
        """.trimIndent()
        
        return dotContent
            .parseDotOrThrow()
            .layoutOrThrow("dot", LayoutOptions.forLargeGraphs())
            .toSvg(RenderOptions.forPrint())
    }
    
    // ========================================
    // Error Handling Examples
    // ========================================
    
    /**
     * Example 8: Proper error handling
     */
    fun errorHandlingExample(): String {
        val invalidDotContent = """
            digraph InvalidGraph {
                A -> B ->; // Invalid syntax
            }
        """.trimIndent()
        
        val engine = GraphvizEngine.create()
        
        // Method 1: Using Result types
        val parseResult = engine.parseDot(invalidDotContent)
        return when {
            parseResult.isSuccess -> {
                val graph = (parseResult as org.graphviz.kotlin.parser.ParseResult.Success).value
                val layoutResult = engine.layout(graph)
                if (layoutResult.isSuccess) {
                    val positionedGraph = (layoutResult as org.graphviz.kotlin.layout.LayoutResult.Success).graph
                    engine.renderSvg(positionedGraph)
                } else {
                    "Layout failed: ${(layoutResult as org.graphviz.kotlin.layout.LayoutResult.Error).message}"
                }
            }
            else -> {
                val error = parseResult as org.graphviz.kotlin.parser.ParseResult.Error
                "Parse failed at line ${error.line}, column ${error.column}: ${error.message}"
            }
        }
        
        // Method 2: Using extension functions with error handling
        // return invalidDotContent.parseDot()
        //     .getOrElse { error -> 
        //         // Handle parse error and return a simple fallback graph
        //         engine.buildDirectedGraph("ErrorGraph") {
        //             node("Error") {
        //                 label("Parse Error: ${error.message}")
        //                 shape("box")
        //                 color(Color.Red)
        //             }
        //         }
        //     }
        //     .layoutAndRenderSvg()
    }
    
    // ========================================
    // Complex Workflow Examples
    // ========================================
    
    /**
     * Example 9: Multi-step workflow with validation
     */
    fun complexWorkflowExample(): String {
        val dotContent = """
            digraph ComplexWorkflow {
                subgraph cluster_input {
                    label="Input Processing";
                    style=filled;
                    fillcolor=lightgray;
                    
                    Input -> Validate -> Parse;
                }
                
                subgraph cluster_processing {
                    label="Core Processing";
                    style=filled;
                    fillcolor=lightblue;
                    
                    Process -> Transform -> Optimize;
                }
                
                subgraph cluster_output {
                    label="Output Generation";
                    style=filled;
                    fillcolor=lightgreen;
                    
                    Format -> Render -> Output;
                }
                
                Parse -> Process;
                Optimize -> Format;
            }
        """.trimIndent()
        
        val engine = GraphvizEngine.create()
        
        // Validate DOT content first
        if (!dotContent.isValidDot()) {
            throw IllegalArgumentException("Invalid DOT content")
        }
        
        // Parse with error handling
        val graph = dotContent.parseDot().getOrElse { error ->
            throw RuntimeException("Parse failed: ${error.message}")
        }
        
        // Apply layout with high-quality settings
        val positionedGraph = graph.layoutOrThrow("dot", LayoutOptions.forHighQuality())
        
        // Render with print-quality settings
        return positionedGraph.toSvg(RenderOptions.forPrint())
    }
    
    /**
     * Example 10: Dynamic graph generation
     */
    fun dynamicGraphExample(nodeCount: Int = 5): String {
        val engine = GraphvizEngine.create()
        
        return engine.buildGraphAndRenderSvg("DynamicGraph") {
            // Generate nodes dynamically
            for (i in 1..nodeCount) {
                node("Node$i") {
                    label("Node $i")
                    shape(if (i % 2 == 0) "box" else "ellipse")
                    fillColor(if (i % 3 == 0) Color.Named("lightblue") else Color.Named("lightgray"))
                    style("filled")
                }
            }
            
            // Generate edges dynamically
            for (i in 1 until nodeCount) {
                edge("Node$i", "Node${i + 1}") {
                    label("Edge $i->${i + 1}")
                    color(if (i % 2 == 0) Color.Named("blue") else Color.Named("red"))
                }
            }
            
            // Add some cross-connections
            if (nodeCount > 3) {
                edge("Node1", "Node$nodeCount") {
                    label("Wrap around")
                    style("dashed")
                    color(Color.Named("green"))
                }
            }
        }
    }
    
    // ========================================
    // Utility Functions for Examples
    // ========================================
    
    /**
     * Save an example to a file (platform-specific implementation needed)
     */
    fun saveExampleToFile(example: () -> String, filename: String) {
        val svg = example()
        // Platform-specific file writing would go here
        println("Generated SVG for $filename (${svg.length} characters)")
    }
    
    /**
     * Run all examples and return a summary
     */
    fun runAllExamples(): Map<String, String> {
        return mapOf(
            "basic_dot_to_svg" to basicDotToSvg(),
            "basic_graph_building" to basicGraphBuilding(),
            "custom_options" to customOptionsExample(),
            "workflow_builder" to workflowBuilderExample(),
            "programmatic_workflow" to programmaticWorkflowExample(),
            "extension_functions" to extensionFunctionExample(),
            "chaining" to chainingExample(),
            "error_handling" to errorHandlingExample(),
            "complex_workflow" to complexWorkflowExample(),
            "dynamic_graph" to dynamicGraphExample(7)
        )
    }
}