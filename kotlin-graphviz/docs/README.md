# Kotlin Multiplatform Graphviz Documentation

Welcome to the comprehensive documentation for the Kotlin Multiplatform Graphviz library. This documentation provides everything you need to effectively use the library in your projects.

## Quick Start

```kotlin
// Add dependency to your build.gradle.kts
dependencies {
    implementation("org.graphviz.kotlin:graphviz:1.0.0")
}

// Basic usage
val engine = GraphvizEngine.create()
val svg = engine.parseDotAndRenderSvg("""
    digraph G {
        A -> B -> C;
        A -> C;
    }
""")
```

## Documentation Structure

### 📚 Core Documentation

- **[API Reference](ApiReference.md)** - Complete API documentation with examples
- **[Migration Guide](MigrationGuide.md)** - Migrate from original Graphviz to Kotlin Multiplatform
- **[Best Practices](BestPractices.md)** - Recommended patterns and practices

### 🚀 Getting Started

1. **Installation**: Add the library to your project dependencies
2. **Basic Usage**: Start with simple DOT parsing and SVG generation
3. **Advanced Features**: Explore programmatic graph building and custom layouts

### 📖 Key Concepts

#### GraphvizEngine
The main entry point providing high-level APIs for common operations:
- DOT parsing and validation
- Graph layout calculation
- SVG rendering
- Programmatic graph building

#### Graph Model
Immutable data structures representing graphs:
- `Graph`: Container for nodes, edges, and attributes
- `Node`: Graph vertices with positioning and styling
- `Edge`: Connections between nodes with routing information
- `AttributeMap`: Type-safe attribute management

#### Layout Engines
Algorithms for positioning graph elements:
- `dot`: Hierarchical directed graph layout (available)
- `neato`: Spring-model undirected layout (planned)
- `fdp`: Force-directed placement (planned)
- Additional engines coming soon

#### Rendering System
SVG generation with customizable options:
- Coordinate system management
- Style and theme support
- Optimization for different use cases

## Usage Patterns

### 1. Simple DOT Processing

```kotlin
// Parse DOT and render to SVG
val svg = GraphvizEngine.create().parseDotAndRenderSvg(dotContent)

// Using extension functions
val svg = dotContent.dotToSvg()
```

### 2. Programmatic Graph Building

```kotlin
val svg = GraphvizEngine.create().buildGraphAndRenderSvg("MyGraph") {
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
    
    edge("A", "B") {
        label("Begin")
        color(Color.Blue)
    }
}
```

### 3. Custom Configuration

```kotlin
val engine = GraphvizEngine.create()

val layoutOptions = LayoutOptions(
    nodeSpacing = 100.0,
    rankSpacing = 150.0,
    minimizeCrossings = true
)

val renderOptions = RenderOptions(
    width = 800.0,
    height = 600.0,
    backgroundColor = "white",
    optimize = true
)

val graph = engine.parseDotOrThrow(dotContent)
val positionedGraph = engine.layoutOrThrow(graph, "dot", layoutOptions)
val svg = engine.renderSvg(positionedGraph, renderOptions)
```

### 4. Workflow Builders

```kotlin
// DOT-to-SVG workflow
val svg = DotToSvgWorkflow.from(dotContent)
    .withLayout("dot")
    .withLayoutOptions { nodeSpacing(75.0) }
    .withRenderOptions { backgroundColor("white") }
    .toSvg()

// Graph building workflow
val svg = GraphBuildWorkflow.directed("Workflow")
    .withGraph {
        node("Start") { shape("circle") }
        node("End") { shape("circle") }
        edge("Start", "End")
    }
    .withLayout("dot")
    .toSvg()
```

## Platform Support

The library supports all Kotlin Multiplatform targets:

| Platform | Status | Notes |
|----------|--------|-------|
| JVM | ✅ Full Support | Java 8+ |
| Android | ✅ Full Support | API 21+ |
| iOS | ✅ Full Support | iOS 12+ |
| JavaScript | ✅ Full Support | Node.js & Browser |
| Native | 🚧 Planned | Linux, Windows, macOS |

## Error Handling

The library provides comprehensive error handling with detailed context:

```kotlin
try {
    val svg = engine.parseDotAndRenderSvg(dotContent)
} catch (e: GraphvizError.ParseError) {
    println("Parse error at line ${e.line}, column ${e.column}: ${e.message}")
    println("Context: ${e.parseContext}")
    println("Recovery actions: ${e.recoveryActions}")
} catch (e: GraphvizError.LayoutError) {
    println("Layout error in ${e.algorithm}: ${e.layoutMessage}")
    if (e.fallbackAvailable) {
        // Try alternative approach
    }
}
```

## Performance Considerations

### Layout Engine Selection

Choose the appropriate layout engine based on your graph characteristics:

```kotlin
val layoutEngine = when {
    graph.isDirected && graph.nodes.size < 100 -> "dot"
    !graph.isDirected && graph.nodes.size < 50 -> "neato" // When available
    graph.nodes.size > 1000 -> "sfdp" // When available
    else -> "dot"
}
```

### Memory Management

Reuse GraphvizEngine instances for better performance:

```kotlin
class GraphService {
    private val engine = GraphvizEngine.create() // Reuse instance
    
    fun generateGraph(dotContent: String): String {
        return engine.parseDotAndRenderSvg(dotContent)
    }
}
```

### Optimization Options

```kotlin
// For large graphs
val layoutOptions = LayoutOptions.forLargeGraphs()

// For high-quality output
val layoutOptions = LayoutOptions.forHighQuality()

// For web display
val renderOptions = RenderOptions.forWeb()

// For print output
val renderOptions = RenderOptions.forPrint()
```

## Testing

The library includes comprehensive testing utilities:

```kotlin
// Unit testing
@Test
fun `should generate valid SVG`() {
    val svg = GraphvizEngine.create().parseDotAndRenderSvg("digraph G { A -> B; }")
    assertTrue(svg.contains("<svg"))
    assertTrue(svg.contains("</svg>"))
}

// Property-based testing
@Test
fun `round trip property`() {
    forAll(validDotGraphs()) { originalDot ->
        val graph = originalDot.parseDotOrThrow()
        val regeneratedDot = graph.toDot()
        val reparsedGraph = regeneratedDot.parseDotOrThrow()
        
        graph.nodes.size == reparsedGraph.nodes.size
    }
}
```

## Examples

The library includes comprehensive examples in `ApiUsageExamples.kt`:

- Basic DOT parsing and rendering
- Programmatic graph construction
- Custom layout and render options
- Error handling patterns
- Platform-specific usage
- Performance optimization
- Testing strategies

## Contributing

We welcome contributions! Please see the main project README for contribution guidelines.

### Documentation Contributions

- Improve existing documentation
- Add new examples and use cases
- Translate documentation to other languages
- Report documentation issues

## Support

- **Issues**: Report bugs and feature requests on GitHub
- **Discussions**: Join community discussions
- **Documentation**: This comprehensive documentation
- **Examples**: See `ApiUsageExamples.kt` for code examples

## Roadmap

### Current Status (v1.0.0)
- ✅ DOT language parsing and generation
- ✅ Dot layout engine
- ✅ SVG rendering
- ✅ Programmatic graph building
- ✅ Type-safe attribute system
- ✅ Comprehensive error handling
- ✅ Multiplatform support (JVM, Android, iOS, JS)

### Planned Features
- 🚧 Additional layout engines (neato, fdp, sfdp, twopi, circo)
- 🚧 More output formats (PNG, PDF, PostScript)
- 🚧 Performance optimizations
- 🚧 Interactive graph features
- 🚧 Custom shape support
- 🚧 Plugin system

## License

This library is released under the same license as the original Graphviz project. See the LICENSE file for details.

---

For detailed API documentation, see [API Reference](ApiReference.md).
For migration from original Graphviz, see [Migration Guide](MigrationGuide.md).
For best practices and patterns, see [Best Practices](BestPractices.md).