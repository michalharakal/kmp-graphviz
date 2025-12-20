# Kotlin Multiplatform Graphviz API Reference

This document provides comprehensive API documentation for the Kotlin Multiplatform Graphviz library.

## Table of Contents

1. [Core API](#core-api)
2. [Graph Building](#graph-building)
3. [Layout Engines](#layout-engines)
4. [Rendering](#rendering)
5. [Error Handling](#error-handling)
6. [Extension Functions](#extension-functions)
7. [Workflow Builders](#workflow-builders)
8. [Examples](#examples)

## Core API

### GraphvizEngine

The main entry point for all Graphviz operations.

```kotlin
class GraphvizEngine private constructor(
    private val defaultLayoutEngine: String = "dot",
    private val defaultSvgRenderer: SvgRenderer = DefaultSvgRenderer()
)
```

#### Creation

```kotlin
// Create with default settings
val engine = GraphvizEngine.create()

// Create with custom settings
val engine = GraphvizEngine.create(
    defaultLayoutEngine = "neato",
    defaultSvgRenderer = CustomSvgRenderer()
)

// Create using builder pattern
val engine = graphvizEngine {
    defaultLayoutEngine("dot")
    defaultSvgRenderer(CustomSvgRenderer())
}
```

#### High-Level Convenience Methods

```kotlin
// Parse DOT and render to SVG in one call
fun parseDotAndRenderSvg(dotContent: String): String
fun parseDotAndRenderSvg(dotContent: String, layoutEngine: String): String

// Build graph programmatically and render to SVG
fun buildGraphAndRenderSvg(
    id: String,
    directed: Boolean = true,
    configure: GraphBuilder.() -> Unit
): String
```

#### Parsing Operations

```kotlin
// Parse DOT content (returns Result type)
fun parseDot(dotContent: String): ParseResult<Graph>

// Parse DOT content (throws exception on error)
fun parseDotOrThrow(dotContent: String): Graph
```

#### Graph Building Operations

```kotlin
// Build directed graph
fun buildDirectedGraph(id: String, configure: GraphBuilder.() -> Unit): Graph

// Build undirected graph
fun buildUndirectedGraph(id: String, configure: GraphBuilder.() -> Unit): Graph

// Build graph with specified direction
fun buildGraph(id: String, directed: Boolean = true, configure: GraphBuilder.() -> Unit): Graph
```

#### Layout Operations

```kotlin
// Apply layout with default engine
fun layout(graph: Graph, options: LayoutOptions = LayoutOptions.default()): LayoutResult

// Apply layout with specified engine
fun layout(graph: Graph, layoutEngine: String, options: LayoutOptions = LayoutOptions.default()): LayoutResult

// Apply layout (throws exception on error)
fun layoutOrThrow(
    graph: Graph,
    layoutEngine: String = defaultLayoutEngine,
    options: LayoutOptions = LayoutOptions.default()
): Graph

// Parse and layout in one operation
fun parseAndLayout(
    dotContent: String,
    layoutEngine: String = defaultLayoutEngine,
    options: LayoutOptions = LayoutOptions.default()
): LayoutResult
```

#### Rendering Operations

```kotlin
// Render to SVG with default options
fun renderSvg(graph: Graph): String

// Render to SVG with custom options
fun renderSvg(graph: Graph, options: RenderOptions): String

// Render to SVG file
fun renderSvgToFile(graph: Graph, filePath: String, options: RenderOptions = RenderOptions.default())
```

#### DOT Generation Operations

```kotlin
// Generate DOT language output
fun generateDot(graph: Graph, options: DotGeneratorOptions = DotGeneratorOptions()): String
```

#### Utility Methods

```kotlin
// Get available layout engines
fun getAvailableLayoutEngines(): Set<String>

// Check if layout engine is available
fun isLayoutEngineAvailable(name: String): Boolean

// Get default layout engine
fun getDefaultLayoutEngine(): String

// Get library version
fun getVersion(): String

// Get supported platforms
fun getSupportedPlatforms(): Set<String>
```

## Graph Building

### GraphBuilder

Mutable builder for constructing graphs programmatically.

```kotlin
class GraphBuilder(
    private val id: String,
    private val isDirected: Boolean = true
)
```

#### Node Operations

```kotlin
// Add a node
fun node(id: String, configure: NodeBuilder.() -> Unit = {}): NodeBuilder

// Check if node exists
fun hasNode(id: String): Boolean

// Get all node IDs
val nodeIds: Set<String>

// Get node count
val nodeCount: Int
```

#### Edge Operations

```kotlin
// Add edge between node IDs
fun edge(sourceId: String, targetId: String, configure: EdgeBuilder.() -> Unit = {}): EdgeBuilder

// Add edge between node builders
fun edge(source: NodeBuilder, target: NodeBuilder, configure: EdgeBuilder.() -> Unit = {}): EdgeBuilder

// Get edge count
val edgeCount: Int
```

#### Subgraph Operations

```kotlin
// Add subgraph
fun subgraph(id: String, configure: GraphBuilder.() -> Unit = {}): GraphBuilder

// Get subgraph count
val subgraphCount: Int
```

#### Attribute Operations

```kotlin
// Set typed attribute
fun <T> attribute(key: AttributeKey<T>, value: T): GraphBuilder

// Set string attribute
fun attribute(name: String, value: String): GraphBuilder

// Set multiple attributes
fun attributes(attrs: Map<String, String>): GraphBuilder
```

#### Validation and Building

```kotlin
// Validate graph structure
fun validate(): ValidationResult

// Build immutable graph
fun build(): Graph
```

### NodeBuilder

Builder for constructing individual nodes.

```kotlin
class NodeBuilder(val id: String)
```

#### Position Operations

```kotlin
// Set position
fun position(x: Double, y: Double): NodeBuilder
fun position(point: Point): NodeBuilder
```

#### Attribute Operations

```kotlin
// Set typed attribute
fun <T> attribute(key: AttributeKey<T>, value: T): NodeBuilder

// Set string attribute
fun attribute(name: String, value: String): NodeBuilder

// Set multiple attributes
fun attributes(attrs: Map<String, String>): NodeBuilder
```

#### Convenience Methods

```kotlin
// Common node attributes
fun label(label: String): NodeBuilder
fun shape(shape: String): NodeBuilder
fun color(color: Color): NodeBuilder
fun fillColor(color: Color): NodeBuilder
fun style(style: String): NodeBuilder
```

### EdgeBuilder

Builder for constructing individual edges.

```kotlin
class EdgeBuilder(val sourceId: String, val targetId: String)
```

#### Control Point Operations

```kotlin
// Add control point
fun controlPoint(x: Double, y: Double): EdgeBuilder
fun controlPoint(point: Point): EdgeBuilder

// Set all control points
fun controlPoints(points: List<Point>): EdgeBuilder
```

#### Attribute Operations

```kotlin
// Set typed attribute
fun <T> attribute(key: AttributeKey<T>, value: T): EdgeBuilder

// Set string attribute
fun attribute(name: String, value: String): EdgeBuilder

// Set multiple attributes
fun attributes(attrs: Map<String, String>): EdgeBuilder
```

#### Convenience Methods

```kotlin
// Common edge attributes
fun label(label: String): EdgeBuilder
fun color(color: Color): EdgeBuilder
fun style(style: String): EdgeBuilder
fun penWidth(width: Double): EdgeBuilder
```

### DSL Functions

```kotlin
// Create directed graph
fun digraph(id: String, configure: GraphBuilder.() -> Unit): Graph

// Create undirected graph
fun undirectedGraph(id: String, configure: GraphBuilder.() -> Unit): Graph

// Create graph with specified direction
fun graph(id: String, directed: Boolean = true, configure: GraphBuilder.() -> Unit): Graph
```

## Layout Engines

### LayoutEngine Interface

```kotlin
interface LayoutEngine {
    val name: String
    fun layout(graph: Graph, options: LayoutOptions = LayoutOptions.default()): LayoutResult
}
```

### LayoutOptions

Configuration options for layout algorithms.

```kotlin
data class LayoutOptions(
    val nodeSpacing: Double = 50.0,
    val rankSpacing: Double = 75.0,
    val minNodeWidth: Double = 30.0,
    val minNodeHeight: Double = 20.0,
    val minimizeCrossings: Boolean = true,
    val maxIterations: Int = 100,
    val convergenceThreshold: Double = 1e-6,
    val engineOptions: Map<String, Any> = emptyMap()
)
```

#### Predefined Options

```kotlin
// Default options
LayoutOptions.default()

// Optimized for large graphs
LayoutOptions.forLargeGraphs()

// Optimized for high quality
LayoutOptions.forHighQuality()
```

### LayoutResult

Result of layout operations.

```kotlin
sealed class LayoutResult {
    data class Success(val graph: Graph) : LayoutResult()
    data class Error(val message: String, val cause: Throwable? = null, val partialGraph: Graph? = null) : LayoutResult()
    
    val isSuccess: Boolean
    val isError: Boolean
    
    fun getGraphOrNull(): Graph?
    fun getGraphOrThrow(): Graph
}
```

### Available Layout Engines

| Engine | Status | Description |
|--------|--------|-------------|
| `dot` | ✅ Available | Hierarchical directed graph layout |
| `neato` | 🚧 Planned | Spring-model undirected graph layout |
| `fdp` | 🚧 Planned | Force-directed placement |
| `sfdp` | 🚧 Planned | Scalable force-directed placement |
| `twopi` | 🚧 Planned | Radial layout |
| `circo` | 🚧 Planned | Circular layout |
| `osage` | 🚧 Planned | Array-based layout |
| `patchwork` | 🚧 Planned | Squarified treemap layout |

## Rendering

### SvgRenderer Interface

```kotlin
interface SvgRenderer {
    fun render(graph: Graph, options: RenderOptions = RenderOptions.default()): String
    fun renderToFile(graph: Graph, path: String, options: RenderOptions = RenderOptions.default())
}
```

### RenderOptions

Configuration options for SVG rendering.

```kotlin
data class RenderOptions(
    val width: Double? = null,
    val height: Double? = null,
    val margin: Double = 20.0,
    val backgroundColor: String? = null,
    val fontFamily: String = "Arial, sans-serif",
    val fontSize: Double = 12.0,
    val includeXmlDeclaration: Boolean = true,
    val optimize: Boolean = false,
    val additionalStyles: String? = null,
    val rendererOptions: Map<String, Any> = emptyMap()
)
```

#### Predefined Options

```kotlin
// Default options
RenderOptions.default()

// Optimized for web display
RenderOptions.forWeb()

// Optimized for print
RenderOptions.forPrint()
```

### RenderResult

Result of rendering operations.

```kotlin
sealed class RenderResult {
    data class Success(val svg: String) : RenderResult()
    data class Error(val message: String, val cause: Throwable? = null) : RenderResult()
    
    val isSuccess: Boolean
    val isError: Boolean
    
    fun getSvgOrNull(): String?
    fun getSvgOrThrow(): String
}
```

## Error Handling

### GraphvizError Hierarchy

Base class for all Graphviz errors.

```kotlin
sealed class GraphvizError : Exception {
    abstract val category: ErrorCategory
    abstract val isRecoverable: Boolean
    abstract val context: ErrorContext
    abstract val recoveryActions: List<String>
}
```

#### Error Types

```kotlin
// Parse errors
data class ParseError(
    val line: Int,
    val column: Int,
    val position: Int,
    val parseContext: String,
    val expectedTokens: List<String> = emptyList(),
    val actualToken: String = "",
    override val cause: Throwable? = null
) : GraphvizError()

// Validation errors
data class ValidationError(
    val validationType: ValidationType,
    val elementId: String,
    val attributeName: String? = null,
    val expectedValue: String? = null,
    val actualValue: String? = null,
    val validationRule: String,
    override val cause: Throwable? = null
) : GraphvizError()

// Layout errors
data class LayoutError(
    val algorithm: String,
    val phase: LayoutPhase,
    val nodeCount: Int,
    val edgeCount: Int,
    val layoutMessage: String,
    val fallbackAvailable: Boolean = false,
    override val cause: Throwable? = null
) : GraphvizError()

// Rendering errors
data class RenderError(
    val format: String,
    val renderPhase: RenderPhase,
    val elementType: String? = null,
    val elementId: String? = null,
    val renderMessage: String,
    override val cause: Throwable? = null
) : GraphvizError()

// Platform errors
data class PlatformError(
    val platform: String,
    val feature: String,
    val limitation: String,
    val workaroundAvailable: Boolean = false,
    override val cause: Throwable? = null
) : GraphvizError()

// Resource errors
data class ResourceError(
    val resourceType: ResourceType,
    val requested: String,
    val available: String? = null,
    val threshold: String? = null,
    override val cause: Throwable? = null
) : GraphvizError()

// Configuration errors
data class ConfigurationError(
    val configType: String,
    val configKey: String,
    val configValue: String,
    val validValues: List<String> = emptyList(),
    val configMessage: String,
    override val cause: Throwable? = null
) : GraphvizError()

// Internal errors
data class InternalError(
    val component: String,
    val operation: String,
    val internalMessage: String,
    val debugInfo: Map<String, String> = emptyMap(),
    override val cause: Throwable? = null
) : GraphvizError()
```

### Error Categories

```kotlin
enum class ErrorCategory {
    PARSE,
    VALIDATION,
    LAYOUT,
    RENDER,
    PLATFORM,
    RESOURCE,
    CONFIGURATION,
    INTERNAL
}
```

### Result Types

```kotlin
// Parse results
sealed class ParseResult<T> {
    data class Success<T>(val value: T) : ParseResult<T>()
    data class Error<T>(
        val message: String,
        val line: Int,
        val column: Int,
        val position: Int,
        val context: String? = null,
        val cause: Throwable? = null
    ) : ParseResult<T>()
    
    val isSuccess: Boolean
    val isError: Boolean
}
```

## Extension Functions

### String Extensions

```kotlin
// Parse DOT content
fun String.parseDot(): ParseResult<Graph>
fun String.parseDotOrThrow(): Graph

// Parse and render directly
fun String.dotToSvg(
    layoutEngine: String = "dot",
    layoutOptions: LayoutOptions = LayoutOptions.default(),
    renderOptions: RenderOptions = RenderOptions.default()
): String

// Parse and render to file
fun String.dotToSvgFile(
    filePath: String,
    layoutEngine: String = "dot",
    layoutOptions: LayoutOptions = LayoutOptions.default(),
    renderOptions: RenderOptions = RenderOptions.default()
)

// Validation
fun String.looksLikeDot(): Boolean
fun String.isValidDot(): Boolean
```

### Graph Extensions

```kotlin
// Apply layout
fun Graph.layout(
    layoutEngine: String = "dot",
    options: LayoutOptions = LayoutOptions.default()
): LayoutResult

fun Graph.layoutOrThrow(
    layoutEngine: String = "dot",
    options: LayoutOptions = LayoutOptions.default()
): Graph

// Render to SVG
fun Graph.toSvg(options: RenderOptions = RenderOptions.default()): String
fun Graph.toSvgFile(filePath: String, options: RenderOptions = RenderOptions.default())

// Generate DOT
fun Graph.toDot(): String

// Layout and render in one operation
fun Graph.layoutAndRenderSvg(
    layoutEngine: String = "dot",
    layoutOptions: LayoutOptions = LayoutOptions.default(),
    renderOptions: RenderOptions = RenderOptions.default()
): String
```

### Result Extensions

```kotlin
// Transform results
inline fun <T, R> ParseResult<T>.map(transform: (T) -> R): ParseResult<R>
inline fun <T, R> ParseResult<T>.flatMap(transform: (T) -> ParseResult<R>): ParseResult<R>

// Get values with defaults
fun <T> ParseResult<T>.getOrDefault(default: T): T
inline fun <T> ParseResult<T>.getOrElse(onError: (ParseResult.Error) -> T): T

// Layout result extensions
inline fun <R> LayoutResult.map(transform: (Graph) -> R): R?
inline fun LayoutResult.flatMap(transform: (Graph) -> LayoutResult): LayoutResult
fun LayoutResult.getOrDefault(default: Graph): Graph
inline fun LayoutResult.getOrElse(onError: (LayoutResult.Error) -> Graph): Graph
```

## Workflow Builders

### DotToSvgWorkflow

Fluent API for DOT-to-SVG conversion.

```kotlin
class DotToSvgWorkflow private constructor(
    private val engine: GraphvizEngine,
    private val dotContent: String
)
```

#### Usage

```kotlin
val svg = DotToSvgWorkflow.from(dotContent)
    .withLayout("dot")
    .withLayoutOptions {
        nodeSpacing(75.0)
        rankSpacing(100.0)
    }
    .withRenderOptions {
        size(600.0, 400.0)
        backgroundColor("white")
    }
    .toSvg()
```

### GraphBuildWorkflow

Fluent API for programmatic graph building.

```kotlin
class GraphBuildWorkflow private constructor(
    private val engine: GraphvizEngine,
    private val graphId: String,
    private val directed: Boolean
)
```

#### Usage

```kotlin
val svg = GraphBuildWorkflow.directed("MyGraph")
    .withGraph {
        node("A") { label("Node A") }
        node("B") { label("Node B") }
        edge("A", "B") { label("Edge") }
    }
    .withLayout("dot")
    .withLayoutOptions { nodeSpacing(100.0) }
    .withRenderOptions { margin(30.0) }
    .toSvg()
```

### DSL Functions

```kotlin
// DOT to SVG workflow
fun dotToSvg(dotContent: String, configure: DotToSvgWorkflow.() -> Unit = {}): String

// Directed graph workflow
fun directedGraphToSvg(graphId: String, configure: GraphBuildWorkflow.() -> Unit): String

// Undirected graph workflow
fun undirectedGraphToSvg(graphId: String, configure: GraphBuildWorkflow.() -> Unit): String
```

## Examples

### Basic Usage

```kotlin
// Simple DOT parsing and rendering
val engine = GraphvizEngine.create()
val svg = engine.parseDotAndRenderSvg("""
    digraph G {
        A -> B -> C;
        A -> C;
    }
""")

// Programmatic graph building
val svg = engine.buildGraphAndRenderSvg("MyGraph") {
    node("A") {
        label("Start")
        shape("circle")
        fillColor(Color.Green)
        style("filled")
    }
    
    node("B") {
        label("Process")
        shape("box")
    }
    
    node("C") {
        label("End")
        shape("circle")
        fillColor(Color.Red)
        style("filled")
    }
    
    edge("A", "B") { label("Begin") }
    edge("B", "C") { label("Complete") }
}
```

### Advanced Configuration

```kotlin
val engine = GraphvizEngine.create()

// Custom layout options
val layoutOptions = LayoutOptions(
    nodeSpacing = 100.0,
    rankSpacing = 150.0,
    minimizeCrossings = true,
    maxIterations = 200
)

// Custom render options
val renderOptions = RenderOptions(
    width = 800.0,
    height = 600.0,
    margin = 50.0,
    backgroundColor = "white",
    fontFamily = "Helvetica",
    fontSize = 14.0,
    optimize = true
)

// Process with custom options
val graph = engine.parseDotOrThrow(dotContent)
val positionedGraph = engine.layoutOrThrow(graph, "dot", layoutOptions)
val svg = engine.renderSvg(positionedGraph, renderOptions)
```

### Error Handling

```kotlin
// Exception-based error handling
try {
    val svg = engine.parseDotAndRenderSvg(dotContent)
    // Use SVG
} catch (e: GraphvizError.ParseError) {
    println("Parse error at line ${e.line}: ${e.message}")
} catch (e: GraphvizError.LayoutError) {
    println("Layout error: ${e.message}")
}

// Result-based error handling
val parseResult = engine.parseDot(dotContent)
when {
    parseResult.isSuccess -> {
        val graph = (parseResult as ParseResult.Success).value
        val layoutResult = engine.layout(graph)
        when {
            layoutResult.isSuccess -> {
                val positionedGraph = (layoutResult as LayoutResult.Success).graph
                val svg = engine.renderSvg(positionedGraph)
                // Use SVG
            }
            layoutResult.isError -> {
                val error = layoutResult as LayoutResult.Error
                println("Layout failed: ${error.message}")
            }
        }
    }
    parseResult.isError -> {
        val error = parseResult as ParseResult.Error
        println("Parse failed: ${error.message}")
    }
}
```

### Extension Functions

```kotlin
// Using extension functions for concise code
val svg = dotContent.dotToSvg()

// Chaining operations
val svg = dotContent
    .parseDotOrThrow()
    .layoutOrThrow("dot", LayoutOptions.forHighQuality())
    .toSvg(RenderOptions.forWeb())

// Validation
if (dotContent.isValidDot()) {
    val svg = dotContent.dotToSvg()
}
```

### Workflow Builders

```kotlin
// DOT workflow
val svg = dotToSvg(dotContent) {
    withLayout("dot")
    withLayoutOptions {
        nodeSpacing(75.0)
        minimizeCrossings(true)
    }
    withRenderOptions {
        backgroundColor("white")
        optimize(true)
    }
}

// Graph building workflow
val svg = directedGraphToSvg("Workflow") {
    withGraph {
        node("Start") { shape("circle") }
        node("Process") { shape("box") }
        node("End") { shape("circle") }
        
        edge("Start", "Process")
        edge("Process", "End")
    }
    withLayout("dot")
}
```

This API reference provides comprehensive documentation for all public APIs in the Kotlin Multiplatform Graphviz library. For more examples and usage patterns, see the `ApiUsageExamples.kt` file in the source code.