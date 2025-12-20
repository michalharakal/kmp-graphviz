package org.graphviz.kotlin

import org.graphviz.kotlin.builder.GraphBuilder
import org.graphviz.kotlin.builder.digraph
import org.graphviz.kotlin.builder.graph
import org.graphviz.kotlin.builder.undirectedGraph
import org.graphviz.kotlin.error.GraphvizError
import org.graphviz.kotlin.error.LayoutPhase
import org.graphviz.kotlin.generator.DotGenerator
import org.graphviz.kotlin.generator.DotGeneratorOptions
import org.graphviz.kotlin.layout.LayoutEngine
import org.graphviz.kotlin.layout.LayoutEngineRegistry
import org.graphviz.kotlin.layout.LayoutOptions
import org.graphviz.kotlin.layout.LayoutResult
import org.graphviz.kotlin.model.Graph
import org.graphviz.kotlin.parser.DotParser
import org.graphviz.kotlin.parser.ParseResult
import org.graphviz.kotlin.renderer.DefaultSvgRenderer
import org.graphviz.kotlin.renderer.RenderOptions
import org.graphviz.kotlin.renderer.RenderResult
import org.graphviz.kotlin.renderer.SvgRenderer

/**
 * Main facade for the Kotlin Multiplatform Graphviz library.
 * 
 * This class provides a clean, high-level API that coordinates parsing, layout, and rendering
 * operations. It implements builder patterns for common use cases and provides convenience
 * methods for typical workflows.
 * 
 * ## Basic Usage
 * 
 * ```kotlin
 * // Initialize the engine
 * val engine = GraphvizEngine.create()
 * 
 * // Parse DOT file and render to SVG
 * val svg = engine.parseDotAndRenderSvg(dotContent)
 * 
 * // Build graph programmatically and render
 * val graph = engine.buildGraph("MyGraph") {
 *     node("A") { label("Node A") }
 *     node("B") { label("Node B") }
 *     edge("A", "B") { label("Edge A->B") }
 * }
 * val svg = engine.renderSvg(graph)
 * ```
 * 
 * ## Advanced Usage
 * 
 * ```kotlin
 * // Custom layout and render options
 * val result = engine.parseAndLayout(dotContent, "dot", LayoutOptions.forHighQuality())
 * val svg = engine.render(result.graph, RenderOptions.forWeb())
 * ```
 */
class GraphvizEngine private constructor(
    private val defaultLayoutEngine: String = "dot",
    private val defaultSvgRenderer: SvgRenderer = DefaultSvgRenderer()
) {
    
    companion object {
        /**
         * Create a new GraphvizEngine instance with default configuration.
         * Automatically initializes the library if not already initialized.
         */
        fun create(): GraphvizEngine {
            if (!GraphvizLibrary.isInitialized()) {
                GraphvizLibrary.initialize()
            }
            return GraphvizEngine()
        }
        
        /**
         * Create a new GraphvizEngine instance with custom configuration.
         */
        fun create(
            defaultLayoutEngine: String = "dot",
            defaultSvgRenderer: SvgRenderer = DefaultSvgRenderer()
        ): GraphvizEngine {
            if (!GraphvizLibrary.isInitialized()) {
                GraphvizLibrary.initialize()
            }
            return GraphvizEngine(defaultLayoutEngine, defaultSvgRenderer)
        }
    }
    
    // ========================================
    // High-Level Convenience Methods
    // ========================================
    
    /**
     * Parse DOT content and render directly to SVG using default options.
     * This is the most common workflow for simple use cases.
     * 
     * @param dotContent DOT language content to parse
     * @return SVG string or throws exception on error
     */
    fun parseDotAndRenderSvg(dotContent: String): String {
        return parseDotAndRenderSvg(dotContent, defaultLayoutEngine)
    }
    
    /**
     * Parse DOT content and render to SVG using specified layout engine.
     * 
     * @param dotContent DOT language content to parse
     * @param layoutEngine Name of layout engine to use
     * @return SVG string or throws exception on error
     */
    fun parseDotAndRenderSvg(dotContent: String, layoutEngine: String): String {
        val parseResult = parseDot(dotContent)
        if (parseResult.isError) {
            val error = parseResult as ParseResult.Error
            throw GraphvizError.ParseError(
                line = error.line,
                column = error.column,
                position = error.position,
                parseContext = error.context ?: "",
                cause = error.cause
            )
        }
        
        val graph = (parseResult as ParseResult.Success).value
        val layoutResult = layout(graph, layoutEngine)
        if (layoutResult.isError) {
            val error = layoutResult as LayoutResult.Error
            throw GraphvizError.LayoutError(
                algorithm = layoutEngine,
                phase = LayoutPhase.INITIALIZATION,
                nodeCount = graph.nodes.size,
                edgeCount = graph.edges.size,
                layoutMessage = error.message,
                fallbackAvailable = false,
                cause = error.cause
            )
        }
        
        val positionedGraph = (layoutResult as LayoutResult.Success).graph
        return renderSvg(positionedGraph)
    }
    
    /**
     * Build a graph programmatically and render to SVG.
     * 
     * @param id Graph identifier
     * @param directed Whether the graph is directed (default: true)
     * @param configure Builder configuration block
     * @return SVG string or throws exception on error
     */
    fun buildGraphAndRenderSvg(
        id: String,
        directed: Boolean = true,
        configure: GraphBuilder.() -> Unit
    ): String {
        val graph = buildGraph(id, directed, configure)
        val layoutResult = layout(graph, defaultLayoutEngine)
        if (layoutResult.isError) {
            val error = layoutResult as LayoutResult.Error
            throw GraphvizError.LayoutError(
                algorithm = defaultLayoutEngine,
                phase = LayoutPhase.INITIALIZATION,
                nodeCount = graph.nodes.size,
                edgeCount = graph.edges.size,
                layoutMessage = error.message,
                fallbackAvailable = false,
                cause = error.cause
            )
        }
        
        val positionedGraph = (layoutResult as LayoutResult.Success).graph
        return renderSvg(positionedGraph)
    }
    
    // ========================================
    // Parsing Operations
    // ========================================
    
    /**
     * Parse DOT language content into a graph structure.
     * 
     * @param dotContent DOT language content
     * @return Parse result containing graph or error information
     */
    fun parseDot(dotContent: String): ParseResult<Graph> {
        return DotParser.parse(dotContent)
    }
    
    /**
     * Parse DOT content and throw exception on error.
     * 
     * @param dotContent DOT language content
     * @return Parsed graph
     * @throws GraphvizError.ParseError if parsing fails
     */
    fun parseDotOrThrow(dotContent: String): Graph {
        val result = parseDot(dotContent)
        if (result.isError) {
            val error = result as ParseResult.Error
            throw GraphvizError.ParseError(
                line = error.line,
                column = error.column,
                position = error.position,
                parseContext = error.context ?: "",
                cause = error.cause
            )
        }
        return (result as ParseResult.Success).value
    }
    
    // ========================================
    // Graph Building Operations
    // ========================================
    
    /**
     * Build a directed graph programmatically.
     * 
     * @param id Graph identifier
     * @param configure Builder configuration block
     * @return Built graph
     */
    fun buildDirectedGraph(id: String, configure: GraphBuilder.() -> Unit): Graph {
        return digraph(id, configure)
    }
    
    /**
     * Build an undirected graph programmatically.
     * 
     * @param id Graph identifier
     * @param configure Builder configuration block
     * @return Built graph
     */
    fun buildUndirectedGraph(id: String, configure: GraphBuilder.() -> Unit): Graph {
        return undirectedGraph(id, configure)
    }
    
    /**
     * Build a graph programmatically with specified direction.
     * 
     * @param id Graph identifier
     * @param directed Whether the graph is directed
     * @param configure Builder configuration block
     * @return Built graph
     */
    fun buildGraph(id: String, directed: Boolean = true, configure: GraphBuilder.() -> Unit): Graph {
        return graph(id, directed, configure)
    }
    
    // ========================================
    // Layout Operations
    // ========================================
    
    /**
     * Apply layout algorithm to a graph using the default layout engine.
     * 
     * @param graph Input graph
     * @param options Layout options
     * @return Layout result with positioned graph or error
     */
    fun layout(graph: Graph, options: LayoutOptions = LayoutOptions.default()): LayoutResult {
        return layout(graph, defaultLayoutEngine, options)
    }
    
    /**
     * Apply layout algorithm to a graph using specified layout engine.
     * 
     * @param graph Input graph
     * @param layoutEngine Name of layout engine to use
     * @param options Layout options
     * @return Layout result with positioned graph or error
     */
    fun layout(graph: Graph, layoutEngine: String, options: LayoutOptions = LayoutOptions.default()): LayoutResult {
        val engine = LayoutEngineRegistry.get(layoutEngine)
            ?: return LayoutResult.Error("Layout engine '$layoutEngine' not found")
        
        return engine.layout(graph, options)
    }
    
    /**
     * Apply layout and throw exception on error.
     * 
     * @param graph Input graph
     * @param layoutEngine Name of layout engine to use
     * @param options Layout options
     * @return Positioned graph
     * @throws GraphvizError.LayoutError if layout fails
     */
    fun layoutOrThrow(
        graph: Graph,
        layoutEngine: String = defaultLayoutEngine,
        options: LayoutOptions = LayoutOptions.default()
    ): Graph {
        val result = layout(graph, layoutEngine, options)
        if (result.isError) {
            val error = result as LayoutResult.Error
            throw GraphvizError.LayoutError(
                algorithm = layoutEngine,
                phase = LayoutPhase.INITIALIZATION,
                nodeCount = graph.nodes.size,
                edgeCount = graph.edges.size,
                layoutMessage = error.message,
                fallbackAvailable = false,
                cause = error.cause
            )
        }
        return (result as LayoutResult.Success).graph
    }
    
    /**
     * Parse DOT content and apply layout in one operation.
     * 
     * @param dotContent DOT language content
     * @param layoutEngine Name of layout engine to use
     * @param options Layout options
     * @return Layout result with positioned graph or error
     */
    fun parseAndLayout(
        dotContent: String,
        layoutEngine: String = defaultLayoutEngine,
        options: LayoutOptions = LayoutOptions.default()
    ): LayoutResult {
        val parseResult = parseDot(dotContent)
        if (parseResult.isError) {
            val error = parseResult as ParseResult.Error
            return LayoutResult.Error("Parse error: ${error.message}")
        }
        
        val graph = (parseResult as ParseResult.Success).value
        return layout(graph, layoutEngine, options)
    }
    
    // ========================================
    // Rendering Operations
    // ========================================
    
    /**
     * Render a positioned graph to SVG using default options.
     * 
     * @param graph Positioned graph to render
     * @return SVG string
     */
    fun renderSvg(graph: Graph): String {
        return renderSvg(graph, RenderOptions.default())
    }
    
    /**
     * Render a positioned graph to SVG with specified options.
     * 
     * @param graph Positioned graph to render
     * @param options Render options
     * @return SVG string
     */
    fun renderSvg(graph: Graph, options: RenderOptions): String {
        return defaultSvgRenderer.render(graph, options)
    }
    
    /**
     * Render a positioned graph to SVG file.
     * 
     * @param graph Positioned graph to render
     * @param filePath Output file path
     * @param options Render options
     */
    fun renderSvgToFile(graph: Graph, filePath: String, options: RenderOptions = RenderOptions.default()) {
        defaultSvgRenderer.renderToFile(graph, filePath, options)
    }
    
    // ========================================
    // DOT Generation Operations
    // ========================================
    
    /**
     * Generate DOT language output from a graph.
     * 
     * @param graph Graph to serialize
     * @param options DOT generation options
     * @return DOT language string
     */
    fun generateDot(graph: Graph, options: DotGeneratorOptions = DotGeneratorOptions()): String {
        return DotGenerator.generate(graph, options)
    }
    
    // ========================================
    // Utility and Information Methods
    // ========================================
    
    /**
     * Get all available layout engine names.
     */
    fun getAvailableLayoutEngines(): Set<String> {
        return LayoutEngineRegistry.getAll().keys
    }
    
    /**
     * Check if a layout engine is available.
     */
    fun isLayoutEngineAvailable(name: String): Boolean {
        return LayoutEngineRegistry.isRegistered(name)
    }
    
    /**
     * Get the default layout engine name.
     */
    fun getDefaultLayoutEngine(): String = defaultLayoutEngine
    
    /**
     * Get library version information.
     */
    fun getVersion(): String = GraphvizLibrary.VERSION
    
    /**
     * Get supported platforms.
     */
    fun getSupportedPlatforms(): Set<String> = GraphvizLibrary.SUPPORTED_PLATFORMS
}

/**
 * Builder class for creating GraphvizEngine instances with custom configuration.
 */
class GraphvizEngineBuilder {
    private var defaultLayoutEngine: String = "dot"
    private var defaultSvgRenderer: SvgRenderer = DefaultSvgRenderer()
    
    /**
     * Set the default layout engine.
     */
    fun defaultLayoutEngine(engine: String): GraphvizEngineBuilder {
        this.defaultLayoutEngine = engine
        return this
    }
    
    /**
     * Set the default SVG renderer.
     */
    fun defaultSvgRenderer(renderer: SvgRenderer): GraphvizEngineBuilder {
        this.defaultSvgRenderer = renderer
        return this
    }
    
    /**
     * Build the GraphvizEngine instance.
     */
    fun build(): GraphvizEngine {
        return GraphvizEngine.create(defaultLayoutEngine, defaultSvgRenderer)
    }
}

/**
 * DSL function for creating GraphvizEngine with builder pattern.
 */
fun graphvizEngine(configure: GraphvizEngineBuilder.() -> Unit = {}): GraphvizEngine {
    val builder = GraphvizEngineBuilder()
    builder.configure()
    return builder.build()
}