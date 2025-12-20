# Best Practices Guide

This guide provides best practices for using the Kotlin Multiplatform Graphviz library effectively.

## API Usage Patterns

### 1. Choose the Right API Level

**High-Level API (Recommended for most use cases):**
```kotlin
// Simple and concise
val svg = GraphvizEngine.create().parseDotAndRenderSvg(dotContent)

// Or using extension functions
val svg = dotContent.dotToSvg()
```

**Mid-Level API (For custom configuration):**
```kotlin
val engine = GraphvizEngine.create()
val graph = engine.parseDotOrThrow(dotContent)
val positionedGraph = engine.layoutOrThrow(graph, "dot", customLayoutOptions)
val svg = engine.renderSvg(positionedGraph, customRenderOptions)
```

**Low-Level API (For maximum control):**
```kotlin
val parseResult = DotParser.parse(dotContent)
val layoutResult = DotLayoutEngine().layout(graph, options)
val svg = DefaultSvgRenderer().render(positionedGraph, renderOptions)
```

### 2. Error Handling Strategy

**Use Result Types for Recoverable Errors:**
```kotlin
val parseResult = engine.parseDot(dotContent)
if (parseResult.isError) {
    // Provide fallback or user feedback
    return generateFallbackGraph()
}
```

**Use Exceptions for Programming Errors:**
```kotlin
try {
    val svg = engine.parseDotAndRenderSvg(dotContent)
} catch (e: GraphvizError.ParseError) {
    // Log error and show user-friendly message
    logger.error("Invalid DOT syntax", e)
    showErrorToUser("The graph definition contains syntax errors")
}
```

### 3. Resource Management

**Reuse GraphvizEngine Instances:**
```kotlin
class GraphService {
    private val engine = GraphvizEngine.create()
    
    fun generateGraph(dotContent: String): String {
        return engine.parseDotAndRenderSvg(dotContent)
    }
}
```

**Use Appropriate Layout Options for Graph Size:**
```kotlin
val layoutOptions = when {
    graph.nodes.size > 1000 -> LayoutOptions.forLargeGraphs()
    isHighQualityOutput -> LayoutOptions.forHighQuality()
    else -> LayoutOptions.default()
}
```

## Performance Optimization

### 1. Layout Engine Selection

```kotlin
val layoutEngine = when {
    graph.isDirected && graph.nodes.size < 100 -> "dot"
    !graph.isDirected && graph.nodes.size < 50 -> "neato"
    graph.nodes.size > 1000 -> "sfdp" // When available
    else -> "dot"
}
```

### 2. Rendering Optimization

```kotlin
val renderOptions = RenderOptions(
    optimize = true, // Enable SVG optimization
    includeXmlDeclaration = false, // For web embedding
    additionalStyles = null // Avoid unnecessary styles
)
```

### 3. Memory Management

```kotlin
// For large graphs, process in chunks if possible
fun processLargeGraph(nodes: List<Node>): String {
    return if (nodes.size > 500) {
        // Break into subgraphs
        processInChunks(nodes)
    } else {
        // Process normally
        processDirectly(nodes)
    }
}
```

## Type Safety and Validation

### 1. Use Type-Safe Attributes

```kotlin
// Preferred: Type-safe
node("A") {
    color(Color.Red)
    attribute(AttributeKey.WIDTH, 2.0)
    attribute(AttributeKey.HEIGHT, 1.5)
}

// Avoid: String-based (unless necessary for compatibility)
node("A") {
    attribute("color", "red")
    attribute("width", "2.0")
}
```

### 2. Validate Input Early

```kotlin
fun processUserInput(dotContent: String): String {
    // Validate before processing
    if (!dotContent.isValidDot()) {
        throw IllegalArgumentException("Invalid DOT content")
    }
    
    return dotContent.dotToSvg()
}
```

### 3. Use Builder Validation

```kotlin
val graph = GraphBuilder("MyGraph", directed = true).apply {
    node("A") { label("Node A") }
    node("B") { label("Node B") }
    edge("A", "B")
    
    // Validate before building
    val validation = validate()
    if (!validation.isValid) {
        throw IllegalStateException("Graph validation failed: ${validation.errors}")
    }
}.build()
```

## Platform-Specific Considerations

### JVM Platform

```kotlin
// Use appropriate thread pools for concurrent processing
class GraphService {
    private val executor = Executors.newFixedThreadPool(4)
    private val engine = GraphvizEngine.create()
    
    fun generateGraphAsync(dotContent: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync({
            engine.parseDotAndRenderSvg(dotContent)
        }, executor)
    }
}
```

### Android Platform

```kotlin
class GraphActivity : AppCompatActivity() {
    private val engine = GraphvizEngine.create()
    
    private fun generateGraph(dotContent: String) {
        lifecycleScope.launch {
            try {
                val svg = withContext(Dispatchers.Default) {
                    engine.parseDotAndRenderSvg(dotContent)
                }
                displaySvg(svg)
            } catch (e: GraphvizError) {
                showError("Failed to generate graph: ${e.message}")
            }
        }
    }
}
```

### JavaScript Platform

```kotlin
// Use suspending functions for non-blocking operations
suspend fun generateGraphForWeb(dotContent: String): String {
    return withContext(Dispatchers.Default) {
        GraphvizEngine.create().parseDotAndRenderSvg(dotContent)
    }
}
```

## Code Organization

### 1. Separate Concerns

```kotlin
// Graph definition
class GraphDefinition {
    fun createWorkflowGraph(): Graph {
        return digraph("Workflow") {
            node("Start") { shape("circle") }
            node("Process") { shape("box") }
            node("End") { shape("circle") }
            
            edge("Start", "Process")
            edge("Process", "End")
        }
    }
}

// Graph rendering
class GraphRenderer {
    private val engine = GraphvizEngine.create()
    
    fun renderToSvg(graph: Graph, options: RenderOptions = RenderOptions.default()): String {
        val positionedGraph = engine.layoutOrThrow(graph)
        return engine.renderSvg(positionedGraph, options)
    }
}
```

### 2. Use Configuration Objects

```kotlin
data class GraphConfig(
    val layoutEngine: String = "dot",
    val layoutOptions: LayoutOptions = LayoutOptions.default(),
    val renderOptions: RenderOptions = RenderOptions.default()
) {
    companion object {
        fun forWeb() = GraphConfig(
            renderOptions = RenderOptions.forWeb()
        )
        
        fun forPrint() = GraphConfig(
            layoutOptions = LayoutOptions.forHighQuality(),
            renderOptions = RenderOptions.forPrint()
        )
    }
}

class ConfigurableGraphService(private val config: GraphConfig) {
    private val engine = GraphvizEngine.create()
    
    fun generateGraph(dotContent: String): String {
        val graph = engine.parseDotOrThrow(dotContent)
        val positionedGraph = engine.layoutOrThrow(graph, config.layoutEngine, config.layoutOptions)
        return engine.renderSvg(positionedGraph, config.renderOptions)
    }
}
```

## Testing Strategies

### 1. Unit Testing

```kotlin
class GraphServiceTest {
    private val engine = GraphvizEngine.create()
    
    @Test
    fun `should generate valid SVG from DOT content`() {
        val dotContent = "digraph G { A -> B; }"
        val svg = engine.parseDotAndRenderSvg(dotContent)
        
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("</svg>"))
    }
    
    @Test
    fun `should handle parse errors gracefully`() {
        val invalidDot = "invalid dot content"
        
        assertThrows<GraphvizError.ParseError> {
            engine.parseDotOrThrow(invalidDot)
        }
    }
}
```

### 2. Integration Testing

```kotlin
class GraphIntegrationTest {
    @Test
    fun `should process complete workflow`() {
        val graph = digraph("TestGraph") {
            node("A") { label("Start") }
            node("B") { label("End") }
            edge("A", "B")
        }
        
        val svg = graph.layoutAndRenderSvg()
        
        // Verify SVG structure
        assertTrue(svg.contains("Start"))
        assertTrue(svg.contains("End"))
    }
}
```

### 3. Property-Based Testing

```kotlin
class GraphPropertyTest {
    @Test
    fun `round trip property - parse then generate should preserve structure`() {
        forAll(validDotGraphs()) { originalDot ->
            val graph = originalDot.parseDotOrThrow()
            val regeneratedDot = graph.toDot()
            val reparsedGraph = regeneratedDot.parseDotOrThrow()
            
            // Verify structural equivalence
            graph.nodes.size == reparsedGraph.nodes.size &&
            graph.edges.size == reparsedGraph.edges.size
        }
    }
}
```

## Common Pitfalls and Solutions

### 1. Memory Leaks

**Problem:** Creating new GraphvizEngine instances repeatedly
```kotlin
// Avoid this
fun generateGraph(dotContent: String): String {
    return GraphvizEngine.create().parseDotAndRenderSvg(dotContent) // Creates new instance each time
}
```

**Solution:** Reuse instances
```kotlin
class GraphService {
    private val engine = GraphvizEngine.create() // Reuse instance
    
    fun generateGraph(dotContent: String): String {
        return engine.parseDotAndRenderSvg(dotContent)
    }
}
```

### 2. Blocking UI Thread

**Problem:** Performing graph operations on UI thread
```kotlin
// Avoid this on Android/UI platforms
button.setOnClickListener {
    val svg = engine.parseDotAndRenderSvg(largeDotContent) // Blocks UI
    displaySvg(svg)
}
```

**Solution:** Use background threads
```kotlin
button.setOnClickListener {
    lifecycleScope.launch {
        val svg = withContext(Dispatchers.Default) {
            engine.parseDotAndRenderSvg(largeDotContent)
        }
        displaySvg(svg)
    }
}
```

### 3. Ignoring Error Handling

**Problem:** Not handling errors properly
```kotlin
// Avoid this
val svg = engine.parseDotAndRenderSvg(userInput) // May throw exception
```

**Solution:** Handle errors appropriately
```kotlin
try {
    val svg = engine.parseDotAndRenderSvg(userInput)
    displaySvg(svg)
} catch (e: GraphvizError.ParseError) {
    showUserFriendlyError("Invalid graph syntax at line ${e.line}")
} catch (e: GraphvizError.LayoutError) {
    showUserFriendlyError("Unable to layout graph: ${e.message}")
}
```

### 4. Inefficient Layout Options

**Problem:** Using inappropriate layout options for graph size
```kotlin
// Avoid this for large graphs
val options = LayoutOptions(
    minimizeCrossings = true, // Expensive for large graphs
    maxIterations = 1000      // Too many iterations
)
```

**Solution:** Choose appropriate options
```kotlin
val options = when {
    graph.nodes.size > 500 -> LayoutOptions.forLargeGraphs()
    isHighQualityNeeded -> LayoutOptions.forHighQuality()
    else -> LayoutOptions.default()
}
```

## Debugging and Troubleshooting

### 1. Enable Detailed Error Information

```kotlin
try {
    val svg = engine.parseDotAndRenderSvg(dotContent)
} catch (e: GraphvizError) {
    logger.error("Graph processing failed", e)
    logger.debug("Error context: ${e.context}")
    logger.debug("Recovery actions: ${e.recoveryActions}")
}
```

### 2. Validate Intermediate Results

```kotlin
fun debugGraphProcessing(dotContent: String): String {
    // Step 1: Parse
    val parseResult = engine.parseDot(dotContent)
    if (parseResult.isError) {
        logger.debug("Parse failed: ${parseResult}")
        throw RuntimeException("Parse failed")
    }
    
    val graph = (parseResult as ParseResult.Success).value
    logger.debug("Parsed graph: ${graph.nodes.size} nodes, ${graph.edges.size} edges")
    
    // Step 2: Layout
    val layoutResult = engine.layout(graph)
    if (layoutResult.isError) {
        logger.debug("Layout failed: ${layoutResult}")
        throw RuntimeException("Layout failed")
    }
    
    val positionedGraph = (layoutResult as LayoutResult.Success).graph
    logger.debug("Layout complete: positioned ${positionedGraph.nodes.size} nodes")
    
    // Step 3: Render
    val svg = engine.renderSvg(positionedGraph)
    logger.debug("Rendered SVG: ${svg.length} characters")
    
    return svg
}
```

### 3. Use Validation Tools

```kotlin
fun validateGraphStructure(graph: Graph): List<String> {
    val issues = mutableListOf<String>()
    
    // Check for disconnected components
    if (hasDisconnectedComponents(graph)) {
        issues.add("Graph has disconnected components")
    }
    
    // Check for cycles in directed graphs
    if (graph.isDirected && hasCycles(graph)) {
        issues.add("Directed graph contains cycles")
    }
    
    // Check for missing node references
    graph.edges.forEach { edge ->
        if (!graph.nodes.contains(edge.source)) {
            issues.add("Edge references missing source node: ${edge.source.id}")
        }
        if (!graph.nodes.contains(edge.target)) {
            issues.add("Edge references missing target node: ${edge.target.id}")
        }
    }
    
    return issues
}
```

## Migration from Other Libraries

### From Original Graphviz

See the [Migration Guide](MigrationGuide.md) for detailed migration instructions.

### From Other Kotlin Graph Libraries

```kotlin
// If migrating from other Kotlin graph libraries
fun migrateFromOtherLibrary(otherGraph: OtherGraphType): Graph {
    return digraph("MigratedGraph") {
        // Convert nodes
        otherGraph.vertices.forEach { vertex ->
            node(vertex.id) {
                label(vertex.label)
                // Map other attributes
            }
        }
        
        // Convert edges
        otherGraph.edges.forEach { edge ->
            edge(edge.source.id, edge.target.id) {
                label(edge.label)
                // Map other attributes
            }
        }
    }
}
```

Following these best practices will help you build robust, maintainable applications using the Kotlin Multiplatform Graphviz library.