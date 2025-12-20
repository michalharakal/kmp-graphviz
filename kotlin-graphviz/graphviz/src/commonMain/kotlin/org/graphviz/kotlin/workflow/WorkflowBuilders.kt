package org.graphviz.kotlin.workflow

import org.graphviz.kotlin.GraphvizEngine
import org.graphviz.kotlin.builder.GraphBuilder
import org.graphviz.kotlin.layout.LayoutOptions
import org.graphviz.kotlin.model.Graph
import org.graphviz.kotlin.renderer.RenderOptions

/**
 * Builder for common DOT-to-SVG workflow.
 * Provides a fluent API for parsing DOT content and rendering to SVG.
 */
class DotToSvgWorkflow private constructor(
    private val engine: GraphvizEngine,
    private val dotContent: String
) {
    private var layoutEngine: String = "dot"
    private var layoutOptions: LayoutOptions = LayoutOptions.default()
    private var renderOptions: RenderOptions = RenderOptions.default()
    
    companion object {
        /**
         * Start a DOT-to-SVG workflow.
         */
        fun from(dotContent: String, engine: GraphvizEngine = GraphvizEngine.create()): DotToSvgWorkflow {
            return DotToSvgWorkflow(engine, dotContent)
        }
    }
    
    /**
     * Specify the layout engine to use.
     */
    fun withLayout(engine: String): DotToSvgWorkflow {
        this.layoutEngine = engine
        return this
    }
    
    /**
     * Specify layout options.
     */
    fun withLayoutOptions(options: LayoutOptions): DotToSvgWorkflow {
        this.layoutOptions = options
        return this
    }
    
    /**
     * Configure layout options using a builder.
     */
    fun withLayoutOptions(configure: LayoutOptionsBuilder.() -> Unit): DotToSvgWorkflow {
        val builder = LayoutOptionsBuilder()
        builder.configure()
        this.layoutOptions = builder.build()
        return this
    }
    
    /**
     * Specify render options.
     */
    fun withRenderOptions(options: RenderOptions): DotToSvgWorkflow {
        this.renderOptions = options
        return this
    }
    
    /**
     * Configure render options using a builder.
     */
    fun withRenderOptions(configure: RenderOptionsBuilder.() -> Unit): DotToSvgWorkflow {
        val builder = RenderOptionsBuilder()
        builder.configure()
        this.renderOptions = builder.build()
        return this
    }
    
    /**
     * Execute the workflow and return SVG string.
     */
    fun toSvg(): String {
        val parseResult = engine.parseDot(dotContent)
        if (parseResult.isError) {
            throw RuntimeException("Parse failed: ${(parseResult as org.graphviz.kotlin.parser.ParseResult.Error).message}")
        }
        
        val graph = (parseResult as org.graphviz.kotlin.parser.ParseResult.Success).value
        val layoutResult = engine.layout(graph, layoutEngine, layoutOptions)
        if (layoutResult.isError) {
            throw RuntimeException("Layout failed: ${(layoutResult as org.graphviz.kotlin.layout.LayoutResult.Error).message}")
        }
        
        val positionedGraph = (layoutResult as org.graphviz.kotlin.layout.LayoutResult.Success).graph
        return engine.renderSvg(positionedGraph, renderOptions)
    }
    
    /**
     * Execute the workflow and save SVG to file.
     */
    fun toSvgFile(filePath: String) {
        val parseResult = engine.parseDot(dotContent)
        if (parseResult.isError) {
            throw RuntimeException("Parse failed: ${(parseResult as org.graphviz.kotlin.parser.ParseResult.Error).message}")
        }
        
        val graph = (parseResult as org.graphviz.kotlin.parser.ParseResult.Success).value
        val layoutResult = engine.layout(graph, layoutEngine, layoutOptions)
        if (layoutResult.isError) {
            throw RuntimeException("Layout failed: ${(layoutResult as org.graphviz.kotlin.layout.LayoutResult.Error).message}")
        }
        
        val positionedGraph = (layoutResult as org.graphviz.kotlin.layout.LayoutResult.Success).graph
        engine.renderSvgToFile(positionedGraph, filePath, renderOptions)
    }
}

/**
 * Builder for programmatic graph construction workflow.
 * Provides a fluent API for building graphs and rendering to SVG.
 */
class GraphBuildWorkflow private constructor(
    private val engine: GraphvizEngine,
    private val graphId: String,
    private val directed: Boolean
) {
    private var layoutEngine: String = "dot"
    private var layoutOptions: LayoutOptions = LayoutOptions.default()
    private var renderOptions: RenderOptions = RenderOptions.default()
    private var graphBuilder: (GraphBuilder.() -> Unit)? = null
    
    companion object {
        /**
         * Start a graph building workflow for a directed graph.
         */
        fun directed(graphId: String, engine: GraphvizEngine = GraphvizEngine.create()): GraphBuildWorkflow {
            return GraphBuildWorkflow(engine, graphId, true)
        }
        
        /**
         * Start a graph building workflow for an undirected graph.
         */
        fun undirected(graphId: String, engine: GraphvizEngine = GraphvizEngine.create()): GraphBuildWorkflow {
            return GraphBuildWorkflow(engine, graphId, false)
        }
    }
    
    /**
     * Configure the graph structure.
     */
    fun withGraph(configure: GraphBuilder.() -> Unit): GraphBuildWorkflow {
        this.graphBuilder = configure
        return this
    }
    
    /**
     * Specify the layout engine to use.
     */
    fun withLayout(engine: String): GraphBuildWorkflow {
        this.layoutEngine = engine
        return this
    }
    
    /**
     * Specify layout options.
     */
    fun withLayoutOptions(options: LayoutOptions): GraphBuildWorkflow {
        this.layoutOptions = options
        return this
    }
    
    /**
     * Configure layout options using a builder.
     */
    fun withLayoutOptions(configure: LayoutOptionsBuilder.() -> Unit): GraphBuildWorkflow {
        val builder = LayoutOptionsBuilder()
        builder.configure()
        this.layoutOptions = builder.build()
        return this
    }
    
    /**
     * Specify render options.
     */
    fun withRenderOptions(options: RenderOptions): GraphBuildWorkflow {
        this.renderOptions = options
        return this
    }
    
    /**
     * Configure render options using a builder.
     */
    fun withRenderOptions(configure: RenderOptionsBuilder.() -> Unit): GraphBuildWorkflow {
        val builder = RenderOptionsBuilder()
        builder.configure()
        this.renderOptions = builder.build()
        return this
    }
    
    /**
     * Build the graph and return it without layout.
     */
    fun buildGraph(): Graph {
        val builder = graphBuilder ?: throw IllegalStateException("Graph configuration not provided")
        return engine.buildGraph(graphId, directed, builder)
    }
    
    /**
     * Execute the workflow and return SVG string.
     */
    fun toSvg(): String {
        val graph = buildGraph()
        val layoutResult = engine.layout(graph, layoutEngine, layoutOptions)
        if (layoutResult.isError) {
            throw RuntimeException("Layout failed: ${(layoutResult as org.graphviz.kotlin.layout.LayoutResult.Error).message}")
        }
        
        val positionedGraph = (layoutResult as org.graphviz.kotlin.layout.LayoutResult.Success).graph
        return engine.renderSvg(positionedGraph, renderOptions)
    }
    
    /**
     * Execute the workflow and save SVG to file.
     */
    fun toSvgFile(filePath: String) {
        val graph = buildGraph()
        val layoutResult = engine.layout(graph, layoutEngine, layoutOptions)
        if (layoutResult.isError) {
            throw RuntimeException("Layout failed: ${(layoutResult as org.graphviz.kotlin.layout.LayoutResult.Error).message}")
        }
        
        val positionedGraph = (layoutResult as org.graphviz.kotlin.layout.LayoutResult.Success).graph
        engine.renderSvgToFile(positionedGraph, filePath, renderOptions)
    }
}

/**
 * Builder for LayoutOptions with fluent API.
 */
class LayoutOptionsBuilder {
    private var nodeSpacing: Double = 50.0
    private var rankSpacing: Double = 75.0
    private var minNodeWidth: Double = 30.0
    private var minNodeHeight: Double = 20.0
    private var minimizeCrossings: Boolean = true
    private var maxIterations: Int = 100
    private var convergenceThreshold: Double = 1e-6
    private var engineOptions: Map<String, Any> = emptyMap()
    
    fun nodeSpacing(spacing: Double): LayoutOptionsBuilder {
        this.nodeSpacing = spacing
        return this
    }
    
    fun rankSpacing(spacing: Double): LayoutOptionsBuilder {
        this.rankSpacing = spacing
        return this
    }
    
    fun minNodeSize(width: Double, height: Double): LayoutOptionsBuilder {
        this.minNodeWidth = width
        this.minNodeHeight = height
        return this
    }
    
    fun minimizeCrossings(minimize: Boolean): LayoutOptionsBuilder {
        this.minimizeCrossings = minimize
        return this
    }
    
    fun maxIterations(iterations: Int): LayoutOptionsBuilder {
        this.maxIterations = iterations
        return this
    }
    
    fun convergenceThreshold(threshold: Double): LayoutOptionsBuilder {
        this.convergenceThreshold = threshold
        return this
    }
    
    fun engineOptions(options: Map<String, Any>): LayoutOptionsBuilder {
        this.engineOptions = options
        return this
    }
    
    fun build(): LayoutOptions {
        return LayoutOptions(
            nodeSpacing = nodeSpacing,
            rankSpacing = rankSpacing,
            minNodeWidth = minNodeWidth,
            minNodeHeight = minNodeHeight,
            minimizeCrossings = minimizeCrossings,
            maxIterations = maxIterations,
            convergenceThreshold = convergenceThreshold,
            engineOptions = engineOptions
        )
    }
}

/**
 * Builder for RenderOptions with fluent API.
 */
class RenderOptionsBuilder {
    private var width: Double? = null
    private var height: Double? = null
    private var margin: Double = 20.0
    private var backgroundColor: String? = null
    private var fontFamily: String = "Arial, sans-serif"
    private var fontSize: Double = 12.0
    private var includeXmlDeclaration: Boolean = true
    private var optimize: Boolean = false
    private var additionalStyles: String? = null
    private var rendererOptions: Map<String, Any> = emptyMap()
    
    fun size(width: Double, height: Double): RenderOptionsBuilder {
        this.width = width
        this.height = height
        return this
    }
    
    fun width(width: Double): RenderOptionsBuilder {
        this.width = width
        return this
    }
    
    fun height(height: Double): RenderOptionsBuilder {
        this.height = height
        return this
    }
    
    fun margin(margin: Double): RenderOptionsBuilder {
        this.margin = margin
        return this
    }
    
    fun backgroundColor(color: String): RenderOptionsBuilder {
        this.backgroundColor = color
        return this
    }
    
    fun font(family: String, size: Double): RenderOptionsBuilder {
        this.fontFamily = family
        this.fontSize = size
        return this
    }
    
    fun includeXmlDeclaration(include: Boolean): RenderOptionsBuilder {
        this.includeXmlDeclaration = include
        return this
    }
    
    fun optimize(optimize: Boolean): RenderOptionsBuilder {
        this.optimize = optimize
        return this
    }
    
    fun additionalStyles(styles: String): RenderOptionsBuilder {
        this.additionalStyles = styles
        return this
    }
    
    fun rendererOptions(options: Map<String, Any>): RenderOptionsBuilder {
        this.rendererOptions = options
        return this
    }
    
    fun build(): RenderOptions {
        return RenderOptions(
            width = width,
            height = height,
            margin = margin,
            backgroundColor = backgroundColor,
            fontFamily = fontFamily,
            fontSize = fontSize,
            includeXmlDeclaration = includeXmlDeclaration,
            optimize = optimize,
            additionalStyles = additionalStyles,
            rendererOptions = rendererOptions
        )
    }
}

// ========================================
// DSL Functions for Workflow Builders
// ========================================

/**
 * DSL function for DOT-to-SVG workflow.
 */
fun dotToSvg(dotContent: String, configure: DotToSvgWorkflow.() -> Unit = {}): String {
    val workflow = DotToSvgWorkflow.from(dotContent)
    workflow.configure()
    return workflow.toSvg()
}

/**
 * DSL function for directed graph building workflow.
 */
fun directedGraphToSvg(graphId: String, configure: GraphBuildWorkflow.() -> Unit): String {
    val workflow = GraphBuildWorkflow.directed(graphId)
    workflow.configure()
    return workflow.toSvg()
}

/**
 * DSL function for undirected graph building workflow.
 */
fun undirectedGraphToSvg(graphId: String, configure: GraphBuildWorkflow.() -> Unit): String {
    val workflow = GraphBuildWorkflow.undirected(graphId)
    workflow.configure()
    return workflow.toSvg()
}