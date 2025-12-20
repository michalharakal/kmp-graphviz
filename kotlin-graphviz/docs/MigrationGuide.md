# Migration Guide: From Original Graphviz to Kotlin Multiplatform Graphviz

This guide helps you migrate from the original C/C++ Graphviz library to the Kotlin Multiplatform port.

## Overview

The Kotlin Multiplatform Graphviz library provides a modern, type-safe API that maintains compatibility with the original Graphviz functionality while offering improved developer experience through Kotlin's language features.

## Key Differences

### 1. Language and Platform Support

**Original Graphviz:**
- C/C++ implementation
- Command-line tools (dot, neato, fdp, etc.)
- Language bindings via SWIG
- Platform-specific builds

**Kotlin Multiplatform Graphviz:**
- Pure Kotlin implementation
- Runs on JVM, Android, iOS, JavaScript
- Native Kotlin API with type safety
- Single codebase for all platforms

### 2. API Design Philosophy

**Original Graphviz:**
- Procedural C API
- String-based attribute handling
- Manual memory management
- Error handling via return codes

**Kotlin Multiplatform Graphviz:**
- Object-oriented Kotlin API
- Type-safe attribute system
- Automatic memory management
- Exception-based error handling

## Migration Patterns

### Command-Line Tool Usage

**Original Graphviz:**
```bash
# Generate SVG from DOT file
dot -Tsvg input.dot -o output.svg

# Use different layout engine
neato -Tsvg input.dot -o output.svg

# Custom options
dot -Tsvg -Gdpi=300 -Nshape=box input.dot -o output.svg
```

**Kotlin Multiplatform Graphviz:**
```kotlin
// Basic usage
val engine = GraphvizEngine.create()
val svg = engine.parseDotAndRenderSvg(dotContent)

// Different layout engine
val svg = engine.parseDotAndRenderSvg(dotContent, "neato")

// Custom options
val layoutOptions = LayoutOptions(
    nodeSpacing = 100.0,
    rankSpacing = 150.0
)
val renderOptions = RenderOptions(
    width = 800.0,
    height = 600.0
)
val graph = engine.parseDotOrThrow(dotContent)
val positionedGraph = engine.layoutOrThrow(graph, "dot", layoutOptions)
val svg = engine.renderSvg(positionedGraph, renderOptions)
```

### Programmatic Graph Creation

**Original Graphviz (C API):**
```c
#include <graphviz/gvc.h>
#include <graphviz/cgraph.h>

GVC_t *gvc = gvContext();
Agraph_t *g = agopen("G", Agdirected, 0);

Agnode_t *n1 = agnode(g, "A", 1);
Agnode_t *n2 = agnode(g, "B", 1);
Agedge_t *e = agedge(g, n1, n2, 0, 1);

agsafeset(n1, "label", "Node A", "");
agsafeset(n2, "label", "Node B", "");
agsafeset(e, "label", "Edge A->B", "");

gvLayout(gvc, g, "dot");
gvRender(gvc, g, "svg", stdout);

gvFreeLayout(gvc, g);
agclose(g);
gvFreeContext(gvc);
```

**Kotlin Multiplatform Graphviz:**
```kotlin
val engine = GraphvizEngine.create()

val svg = engine.buildGraphAndRenderSvg("G") {
    node("A") {
        label("Node A")
    }
    
    node("B") {
        label("Node B")
    }
    
    edge("A", "B") {
        label("Edge A->B")
    }
}
```

### Language Bindings

**Original Graphviz (Python binding):**
```python
import graphviz

dot = graphviz.Digraph()
dot.node('A', 'Node A')
dot.node('B', 'Node B')
dot.edge('A', 'B', 'Edge A->B')

svg = dot.pipe(format='svg', encoding='utf-8')
```

**Kotlin Multiplatform Graphviz:**
```kotlin
// Direct Kotlin API - no bindings needed
val svg = directedGraphToSvg("G") {
    withGraph {
        node("A") { label("Node A") }
        node("B") { label("Node B") }
        edge("A", "B") { label("Edge A->B") }
    }
}
```

## Feature Mapping

### Layout Engines

| Original Graphviz | Kotlin Multiplatform | Status |
|-------------------|----------------------|---------|
| `dot` | `"dot"` | ✅ Implemented |
| `neato` | `"neato"` | 🚧 Planned |
| `fdp` | `"fdp"` | 🚧 Planned |
| `sfdp` | `"sfdp"` | 🚧 Planned |
| `twopi` | `"twopi"` | 🚧 Planned |
| `circo` | `"circo"` | 🚧 Planned |
| `osage` | `"osage"` | 🚧 Planned |
| `patchwork` | `"patchwork"` | 🚧 Planned |

### Output Formats

| Original Graphviz | Kotlin Multiplatform | Status |
|-------------------|----------------------|---------|
| SVG | SVG | ✅ Implemented |
| PNG | PNG | 🚧 Planned |
| PDF | PDF | 🚧 Planned |
| PostScript | PostScript | 🚧 Planned |
| DOT | DOT | ✅ Implemented |

### Attribute System

**Original Graphviz:**
```c
// String-based attributes
agsafeset(node, "color", "red", "");
agsafeset(node, "shape", "box", "");
agsafeset(node, "width", "2.0", "");
```

**Kotlin Multiplatform Graphviz:**
```kotlin
// Type-safe attributes
node("A") {
    color(Color.Red)
    shape("box")
    attribute(AttributeKey.WIDTH, 2.0)
}

// Or string-based for compatibility
node("A") {
    attribute("color", "red")
    attribute("shape", "box")
    attribute("width", "2.0")
}
```

## Common Migration Scenarios

### 1. Simple DOT File Processing

**Before (command line):**
```bash
dot -Tsvg graph.dot -o output.svg
```

**After (Kotlin):**
```kotlin
val dotContent = File("graph.dot").readText()
val svg = GraphvizEngine.create().parseDotAndRenderSvg(dotContent)
File("output.svg").writeText(svg)
```

### 2. Programmatic Graph Generation

**Before (Python):**
```python
import graphviz

g = graphviz.Digraph('G', comment='The Graph')
g.attr(rankdir='TB', size='8,6')
g.attr('node', shape='doublecircle')

g.node('LR_0')
g.node('LR_3')
g.node('LR_4')
g.node('LR_8')

g.attr('node', shape='circle')
g.edge('LR_0', 'LR_2', label='SS(B)')
g.edge('LR_0', 'LR_1', label='SS(S)')

print(g.source)
```

**After (Kotlin):**
```kotlin
val svg = GraphvizEngine.create().buildGraphAndRenderSvg("G") {
    // Graph attributes
    attribute("rankdir", "TB")
    attribute("size", "8,6")
    
    // Double circle nodes
    node("LR_0") { shape("doublecircle") }
    node("LR_3") { shape("doublecircle") }
    node("LR_4") { shape("doublecircle") }
    node("LR_8") { shape("doublecircle") }
    
    // Edges with labels
    edge("LR_0", "LR_2") { label("SS(B)") }
    edge("LR_0", "LR_1") { label("SS(S)") }
}
```

### 3. Custom Layout Options

**Before (command line):**
```bash
dot -Tsvg -Grankdir=LR -Nshape=box -Earrowhead=diamond graph.dot
```

**After (Kotlin):**
```kotlin
val engine = GraphvizEngine.create()
val graph = engine.parseDotOrThrow(dotContent)

// Apply custom layout options
val layoutOptions = LayoutOptions(
    nodeSpacing = 75.0,
    rankSpacing = 100.0
)

val renderOptions = RenderOptions(
    margin = 30.0,
    backgroundColor = "white"
)

val positionedGraph = engine.layoutOrThrow(graph, "dot", layoutOptions)
val svg = engine.renderSvg(positionedGraph, renderOptions)
```

## Error Handling Migration

### Original Graphviz Error Handling

**C API:**
```c
if (gvLayout(gvc, g, "dot") != 0) {
    fprintf(stderr, "Layout failed\n");
    return 1;
}
```

**Python binding:**
```python
try:
    svg = dot.pipe(format='svg')
except graphviz.ExecutableNotFound:
    print("Graphviz not installed")
except graphviz.CalledProcessError as e:
    print(f"Graphviz error: {e}")
```

### Kotlin Multiplatform Error Handling

**Exception-based:**
```kotlin
try {
    val svg = engine.parseDotAndRenderSvg(dotContent)
} catch (e: GraphvizError.ParseError) {
    println("Parse error at line ${e.line}, column ${e.column}: ${e.message}")
} catch (e: GraphvizError.LayoutError) {
    println("Layout error in ${e.algorithm}: ${e.message}")
}
```

**Result-based:**
```kotlin
val parseResult = engine.parseDot(dotContent)
when {
    parseResult.isSuccess -> {
        val graph = (parseResult as ParseResult.Success).value
        // Continue processing
    }
    parseResult.isError -> {
        val error = parseResult as ParseResult.Error
        println("Parse failed: ${error.message}")
    }
}
```

## Platform-Specific Considerations

### JVM Platform

**Migration from Java/Scala:**
```kotlin
// Replace Java Graphviz bindings
// Old: GraphViz graphViz = new GraphViz();
val engine = GraphvizEngine.create()

// Old: graphViz.addln(graphViz.start_graph());
val svg = engine.buildGraphAndRenderSvg("G") {
    // Graph construction
}
```

### Android Platform

**Considerations:**
- No native dependencies required
- Smaller APK size compared to native bindings
- Better integration with Android lifecycle

```kotlin
class GraphActivity : AppCompatActivity() {
    private val graphvizEngine = GraphvizEngine.create()
    
    private fun generateGraph() {
        lifecycleScope.launch {
            val svg = withContext(Dispatchers.Default) {
                graphvizEngine.parseDotAndRenderSvg(dotContent)
            }
            // Display SVG in WebView or convert to bitmap
        }
    }
}
```

### JavaScript Platform

**Web Integration:**
```kotlin
// Kotlin/JS
fun generateGraphForWeb(dotContent: String): String {
    val engine = GraphvizEngine.create()
    return engine.parseDotAndRenderSvg(dotContent)
}

// Can be called from JavaScript
external fun displayGraph(svg: String)

fun main() {
    val svg = generateGraphForWeb(dotContent)
    displayGraph(svg)
}
```

## Performance Considerations

### Memory Usage

**Original Graphviz:**
- Manual memory management
- Potential memory leaks if not properly cleaned up

**Kotlin Multiplatform:**
- Automatic garbage collection
- Immutable data structures reduce memory issues

### Processing Speed

**Original Graphviz:**
- Highly optimized C implementation
- Decades of performance tuning

**Kotlin Multiplatform:**
- Pure Kotlin implementation
- May be slower for very large graphs
- Better for typical use cases

### Optimization Tips

```kotlin
// For large graphs, use appropriate layout options
val layoutOptions = LayoutOptions.forLargeGraphs()

// For high-quality output
val layoutOptions = LayoutOptions.forHighQuality()

// Optimize SVG output
val renderOptions = RenderOptions(optimize = true)
```

## Best Practices

### 1. Use Type-Safe APIs When Possible

```kotlin
// Preferred: Type-safe
node("A") {
    color(Color.Red)
    shape("box")
}

// Acceptable: String-based for compatibility
node("A") {
    attribute("color", "red")
    attribute("shape", "box")
}
```

### 2. Handle Errors Appropriately

```kotlin
// Use Result types for recoverable errors
val parseResult = engine.parseDot(dotContent)
if (parseResult.isError) {
    // Handle error gracefully
    return fallbackGraph()
}

// Use exceptions for programming errors
try {
    val svg = engine.parseDotAndRenderSvg(invalidDot)
} catch (e: GraphvizError) {
    // Log error and provide user feedback
}
```

### 3. Leverage Kotlin Features

```kotlin
// Use extension functions
val svg = dotContent.dotToSvg()

// Use DSL builders
val svg = directedGraphToSvg("MyGraph") {
    withGraph {
        node("A") { label("Node A") }
        node("B") { label("Node B") }
        edge("A", "B")
    }
}

// Use workflow builders
val svg = DotToSvgWorkflow.from(dotContent)
    .withLayout("dot")
    .withLayoutOptions { nodeSpacing(100.0) }
    .withRenderOptions { backgroundColor("white") }
    .toSvg()
```

## Troubleshooting

### Common Issues

1. **Missing Layout Engine**
   ```kotlin
   // Check available engines
   val engines = engine.getAvailableLayoutEngines()
   println("Available: $engines")
   ```

2. **Parse Errors**
   ```kotlin
   // Get detailed error information
   val result = engine.parseDot(dotContent)
   if (result.isError) {
       val error = result as ParseResult.Error
       println("Error at line ${error.line}: ${error.message}")
       println("Context: ${error.context}")
   }
   ```

3. **Layout Failures**
   ```kotlin
   // Try fallback layout
   val result = engine.layout(graph, "dot")
   if (result.isError) {
       // Try simpler layout
       val fallbackResult = engine.layout(graph, "neato")
   }
   ```

## Getting Help

- **Documentation**: Check KDoc comments in the API
- **Examples**: See `ApiUsageExamples.kt` for comprehensive examples
- **Issues**: Report bugs and feature requests on the project repository
- **Community**: Join discussions about Kotlin Multiplatform development

## Future Roadmap

The Kotlin Multiplatform Graphviz library is actively developed with plans for:

1. **Additional Layout Engines**: neato, fdp, sfdp, twopi, circo
2. **More Output Formats**: PNG, PDF, PostScript
3. **Performance Optimizations**: Parallel processing, streaming
4. **Advanced Features**: Custom shapes, plugins, interactive graphs
5. **Platform Integrations**: Compose UI, SwiftUI, React components

This migration guide will be updated as new features are added and the API evolves.