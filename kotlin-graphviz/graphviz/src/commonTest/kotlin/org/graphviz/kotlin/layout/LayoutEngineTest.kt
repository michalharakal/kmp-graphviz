package org.graphviz.kotlin.layout

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.graphviz.kotlin.model.*

class LayoutEngineTest : FunSpec({
    
    test("LayoutOptions should have sensible defaults") {
        val options = LayoutOptions.default()
        
        options.nodeSpacing shouldBe 50.0
        options.rankSpacing shouldBe 75.0
        options.minNodeWidth shouldBe 30.0
        options.minNodeHeight shouldBe 20.0
        options.minimizeCrossings shouldBe true
        options.maxIterations shouldBe 100
        options.convergenceThreshold shouldBe 1e-6
        options.engineOptions shouldBe emptyMap()
    }
    
    test("LayoutOptions.forLargeGraphs should optimize for performance") {
        val options = LayoutOptions.forLargeGraphs()
        
        options.nodeSpacing shouldBe 30.0
        options.rankSpacing shouldBe 50.0
        options.minimizeCrossings shouldBe false
        options.maxIterations shouldBe 50
    }
    
    test("LayoutOptions.forHighQuality should optimize for quality") {
        val options = LayoutOptions.forHighQuality()
        
        options.nodeSpacing shouldBe 75.0
        options.rankSpacing shouldBe 100.0
        options.minimizeCrossings shouldBe true
        options.maxIterations shouldBe 200
    }
    
    test("LayoutResult.Success should indicate success") {
        val graph = createSimpleGraph()
        val result = LayoutResult.Success(graph)
        
        result.isSuccess shouldBe true
        result.isError shouldBe false
        result.getGraphOrNull() shouldBe graph
        result.getGraphOrThrow() shouldBe graph
    }
    
    test("LayoutResult.Error should indicate failure") {
        val result = LayoutResult.Error("Test error")
        
        result.isSuccess shouldBe false
        result.isError shouldBe true
        result.getGraphOrNull() shouldBe null
    }
    
    test("LayoutResult.Error with partial graph should return partial result") {
        val partialGraph = createSimpleGraph()
        val result = LayoutResult.Error("Test error", partialGraph = partialGraph)
        
        result.getGraphOrNull() shouldBe partialGraph
    }
    
    test("LayoutException should be thrown when getting graph from error result") {
        val result = LayoutResult.Error("Test error")
        
        try {
            result.getGraphOrThrow()
            throw AssertionError("Expected LayoutException to be thrown")
        } catch (e: LayoutException) {
            e.message shouldBe "Test error"
        }
    }
})

class BaseLayoutEngineTest : FunSpec({
    
    val testEngine = object : BaseLayoutEngine() {
        override val name = "test"
        
        override fun layout(graph: Graph, options: LayoutOptions): LayoutResult {
            val preResult = preLayout(graph, options)
            return when (preResult) {
                is PreLayoutResult.Error -> handleError(preResult.message)
                is PreLayoutResult.Success -> {
                    // Simple test layout: position nodes in a line
                    val nodePositions = graph.getAllNodes().mapIndexed { index, node ->
                        node to Point(index * 100.0, 0.0)
                    }.toMap()
                    
                    val edgeControlPoints = graph.getAllEdges().associateWith { edge ->
                        listOf(
                            nodePositions[edge.source] ?: Point.ORIGIN,
                            nodePositions[edge.target] ?: Point.ORIGIN
                        )
                    }
                    
                    val positionedGraph = applyPositions(graph, nodePositions, edgeControlPoints)
                    success(positionedGraph)
                }
            }
        }
        
        // Expose protected methods for testing
        fun testValidateGraph(graph: Graph) = validateGraph(graph)
        fun testCalculateNodeSizes(graph: Graph, options: LayoutOptions) = calculateNodeSizes(graph, options)
        fun testPreLayout(graph: Graph, options: LayoutOptions) = preLayout(graph, options)
    }
    
    test("validateGraph should accept valid graphs") {
        val graph = createSimpleGraph()
        val result = testEngine.testValidateGraph(graph)
        
        result shouldBe ValidationResult.Valid
    }
    
    test("validateGraph should reject empty graphs") {
        val graph = GraphImpl(
            id = "empty",
            isDirected = true,
            nodes = emptySet(),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val result = testEngine.testValidateGraph(graph)
        result.shouldBeInstanceOf<ValidationResult.Invalid>()
        result.errors.any { it.contains("empty graph") } shouldBe true
    }
    
    test("calculateNodeSizes should use minimum sizes") {
        val graph = createSimpleGraph()
        val options = LayoutOptions.default()
        val sizes = testEngine.testCalculateNodeSizes(graph, options)
        
        sizes.values.forEach { size ->
            size.width shouldBe options.minNodeWidth
            size.height shouldBe options.minNodeHeight
        }
    }
    
    test("calculateNodeSizes should account for labels") {
        val node = NodeImpl(
            id = "labeled",
            attributes = AttributeMap.builder()
                .set(AttributeKey.LABEL, "Long Label Text")
                .build(),
            position = null
        )
        val graph = GraphImpl(
            id = "test",
            isDirected = true,
            nodes = setOf(node),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val options = LayoutOptions.default()
        val sizes = testEngine.testCalculateNodeSizes(graph, options)
        
        val nodeSize = sizes[node]!!
        nodeSize.width shouldNotBe options.minNodeWidth // Should be larger due to label
        nodeSize.height shouldBeGreaterThan options.minNodeHeight // Should be larger due to text height + padding
    }
    
    test("layout should position nodes and edges") {
        val graph = createSimpleGraph()
        val result = testEngine.layout(graph)
        
        result.shouldBeInstanceOf<LayoutResult.Success>()
        val positionedGraph = result.graph
        
        // All nodes should have positions
        positionedGraph.getAllNodes().forEach { node ->
            node.position shouldNotBe null
        }
        
        // All edges should have control points
        positionedGraph.getAllEdges().forEach { edge ->
            edge.controlPoints.size shouldBe 2 // Start and end points
        }
    }
    
    test("preLayout should validate graph and calculate sizes") {
        val graph = createSimpleGraph()
        val options = LayoutOptions.default()
        val result = testEngine.testPreLayout(graph, options)
        
        result.shouldBeInstanceOf<BaseLayoutEngine.PreLayoutResult.Success>()
        result.nodeSizes.size shouldBe graph.getAllNodes().size
    }
})

class LayoutEngineRegistryTest : FunSpec({
    
    beforeEach {
        LayoutEngineRegistry.clear()
    }
    
    test("should register and retrieve layout engines") {
        val engine = object : LayoutEngine {
            override val name = "test"
            override fun layout(graph: Graph, options: LayoutOptions) = 
                LayoutResult.Error("Not implemented")
        }
        
        LayoutEngineRegistry.register(engine)
        LayoutEngineRegistry.get("test") shouldBe engine
        LayoutEngineRegistry.isRegistered("test") shouldBe true
    }
    
    test("should return null for unregistered engines") {
        LayoutEngineRegistry.get("nonexistent") shouldBe null
        LayoutEngineRegistry.isRegistered("nonexistent") shouldBe false
    }
    
    test("should unregister engines") {
        val engine = object : LayoutEngine {
            override val name = "test"
            override fun layout(graph: Graph, options: LayoutOptions) = 
                LayoutResult.Error("Not implemented")
        }
        
        LayoutEngineRegistry.register(engine)
        LayoutEngineRegistry.unregister("test") shouldBe engine
        LayoutEngineRegistry.isRegistered("test") shouldBe false
    }
    
    test("should list all registered engines") {
        val engine1 = object : LayoutEngine {
            override val name = "test1"
            override fun layout(graph: Graph, options: LayoutOptions) = 
                LayoutResult.Error("Not implemented")
        }
        val engine2 = object : LayoutEngine {
            override val name = "test2"
            override fun layout(graph: Graph, options: LayoutOptions) = 
                LayoutResult.Error("Not implemented")
        }
        
        LayoutEngineRegistry.register(engine1)
        LayoutEngineRegistry.register(engine2)
        
        val all = LayoutEngineRegistry.getAll()
        all.size shouldBe 2
        all["test1"] shouldBe engine1
        all["test2"] shouldBe engine2
    }
})

// Helper function to create a simple test graph
private fun createSimpleGraph(): Graph {
    val node1 = NodeImpl("1", AttributeMap.empty(), null)
    val node2 = NodeImpl("2", AttributeMap.empty(), null)
    val edge = EdgeImpl(node1, node2, AttributeMap.empty(), emptyList())
    
    return GraphImpl(
        id = "test",
        isDirected = true,
        nodes = setOf(node1, node2),
        edges = setOf(edge),
        subgraphs = emptySet(),
        attributes = AttributeMap.empty()
    )
}