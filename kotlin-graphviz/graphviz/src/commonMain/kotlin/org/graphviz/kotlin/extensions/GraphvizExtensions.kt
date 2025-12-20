package org.graphviz.kotlin.extensions

import org.graphviz.kotlin.GraphvizEngine
import org.graphviz.kotlin.layout.LayoutOptions
import org.graphviz.kotlin.layout.LayoutResult
import org.graphviz.kotlin.model.Graph
import org.graphviz.kotlin.parser.ParseResult
import org.graphviz.kotlin.renderer.RenderOptions

/**
 * Extension functions to make the Graphviz API more convenient and Kotlin-idiomatic.
 */

// ========================================
// String Extensions for DOT Parsing
// ========================================

/**
 * Parse this string as DOT language content.
 */
fun String.parseDot(): ParseResult<Graph> {
    return GraphvizEngine.create().parseDot(this)
}

/**
 * Parse this string as DOT language content and throw on error.
 */
fun String.parseDotOrThrow(): Graph {
    return GraphvizEngine.create().parseDotOrThrow(this)
}

/**
 * Parse this string as DOT and render directly to SVG.
 */
fun String.dotToSvg(
    layoutEngine: String = "dot",
    layoutOptions: LayoutOptions = LayoutOptions.default(),
    renderOptions: RenderOptions = RenderOptions.default()
): String {
    val engine = GraphvizEngine.create()
    val graph = engine.parseDotOrThrow(this)
    val positionedGraph = engine.layoutOrThrow(graph, layoutEngine, layoutOptions)
    return engine.renderSvg(positionedGraph, renderOptions)
}

/**
 * Parse this string as DOT and render to SVG file.
 */
fun String.dotToSvgFile(
    filePath: String,
    layoutEngine: String = "dot",
    layoutOptions: LayoutOptions = LayoutOptions.default(),
    renderOptions: RenderOptions = RenderOptions.default()
) {
    val engine = GraphvizEngine.create()
    val graph = engine.parseDotOrThrow(this)
    val positionedGraph = engine.layoutOrThrow(graph, layoutEngine, layoutOptions)
    engine.renderSvgToFile(positionedGraph, filePath, renderOptions)
}

// ========================================
// Graph Extensions for Layout and Rendering
// ========================================

/**
 * Apply layout to this graph using the specified engine.
 */
fun Graph.layout(
    layoutEngine: String = "dot",
    options: LayoutOptions = LayoutOptions.default()
): LayoutResult {
    return GraphvizEngine.create().layout(this, layoutEngine, options)
}

/**
 * Apply layout to this graph and throw on error.
 */
fun Graph.layoutOrThrow(
    layoutEngine: String = "dot",
    options: LayoutOptions = LayoutOptions.default()
): Graph {
    return GraphvizEngine.create().layoutOrThrow(this, layoutEngine, options)
}

/**
 * Render this positioned graph to SVG.
 */
fun Graph.toSvg(options: RenderOptions = RenderOptions.default()): String {
    return GraphvizEngine.create().renderSvg(this, options)
}

/**
 * Render this positioned graph to SVG file.
 */
fun Graph.toSvgFile(filePath: String, options: RenderOptions = RenderOptions.default()) {
    GraphvizEngine.create().renderSvgToFile(this, filePath, options)
}

/**
 * Generate DOT language output from this graph.
 */
fun Graph.toDot(): String {
    return GraphvizEngine.create().generateDot(this)
}

/**
 * Apply layout and render to SVG in one operation.
 */
fun Graph.layoutAndRenderSvg(
    layoutEngine: String = "dot",
    layoutOptions: LayoutOptions = LayoutOptions.default(),
    renderOptions: RenderOptions = RenderOptions.default()
): String {
    val engine = GraphvizEngine.create()
    val positionedGraph = engine.layoutOrThrow(this, layoutEngine, layoutOptions)
    return engine.renderSvg(positionedGraph, renderOptions)
}

// ========================================
// Result Extensions for Better Error Handling
// ========================================

/**
 * Transform a ParseResult using the provided function if successful.
 */
inline fun <T, R> ParseResult<T>.map(transform: (T) -> R): ParseResult<R> {
    return when (this) {
        is ParseResult.Success -> ParseResult.Success(transform(value))
        is ParseResult.Error -> this
    }
}

/**
 * Transform a ParseResult using the provided function if successful, or return the error.
 */
inline fun <T, R> ParseResult<T>.flatMap(transform: (T) -> ParseResult<R>): ParseResult<R> {
    return when (this) {
        is ParseResult.Success -> transform(value)
        is ParseResult.Error -> this
    }
}

/**
 * Get the value if successful, or return the provided default.
 */
fun <T> ParseResult<T>.getOrDefault(default: T): T {
    return when (this) {
        is ParseResult.Success -> value
        is ParseResult.Error -> default
    }
}

/**
 * Get the value if successful, or compute a default using the error.
 */
inline fun <T> ParseResult<T>.getOrElse(onError: (ParseResult.Error) -> T): T {
    return when (this) {
        is ParseResult.Success -> value
        is ParseResult.Error -> onError(this)
    }
}

/**
 * Transform a LayoutResult using the provided function if successful.
 */
inline fun <R> LayoutResult.map(transform: (Graph) -> R): R? {
    return when (this) {
        is LayoutResult.Success -> transform(graph)
        is LayoutResult.Error -> null
    }
}

/**
 * Transform a LayoutResult using the provided function if successful, or return the error.
 */
inline fun LayoutResult.flatMap(transform: (Graph) -> LayoutResult): LayoutResult {
    return when (this) {
        is LayoutResult.Success -> transform(graph)
        is LayoutResult.Error -> this
    }
}

/**
 * Get the graph if successful, or return the provided default.
 */
fun LayoutResult.getOrDefault(default: Graph): Graph {
    return when (this) {
        is LayoutResult.Success -> graph
        is LayoutResult.Error -> default
    }
}

/**
 * Get the graph if successful, or compute a default using the error.
 */
inline fun LayoutResult.getOrElse(onError: (LayoutResult.Error) -> Graph): Graph {
    return when (this) {
        is LayoutResult.Success -> graph
        is LayoutResult.Error -> onError(this)
    }
}

// ========================================
// Utility Extensions
// ========================================

/**
 * Check if this string looks like DOT language content.
 */
fun String.looksLikeDot(): Boolean {
    val trimmed = this.trim()
    return trimmed.startsWith("graph ") || 
           trimmed.startsWith("digraph ") || 
           trimmed.startsWith("strict graph ") || 
           trimmed.startsWith("strict digraph ")
}

/**
 * Validate that this string is valid DOT content.
 */
fun String.isValidDot(): Boolean {
    return try {
        this.parseDot().isSuccess
    } catch (e: Exception) {
        false
    }
}