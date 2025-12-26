package org.graphviz.kotlin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.graphviz.kotlin.collections.JvmOptimizedGraphImpl
import org.graphviz.kotlin.factory.JvmGraphFactory
import org.graphviz.kotlin.memory.JvmMemoryManager
import org.graphviz.kotlin.memory.MemoryRecommendation
import org.graphviz.kotlin.performance.JvmPerformanceMonitor

class JvmOptimizationsTest : FunSpec({
    
    beforeTest {
        // Initialize JVM platform
        Platform.initializeJvm()
        JvmPerformanceMonitor.setEnabled(true)
        JvmPerformanceMonitor.clearMetrics()
    }
    
    test("Platform should provide JVM-specific functionality") {
        Platform.name shouldBe "JVM"
        
        val memoryInfo = Platform.getMemoryInfo()
        memoryInfo.heapUsed shouldBeGreaterThan 0L
        memoryInfo.heapMax shouldBeGreaterThan 0L
        
        val gcInfo = Platform.getGCInfo()
        gcInfo.shouldNotBe(emptyList<GCInfo>())
    }
    
    test("Performance monitoring should track operations") {
        val result = Platform.withPerformanceMonitoring("test_operation") {
            Thread.sleep(10) // Simulate some work
            "test_result"
        }
        
        result shouldBe "test_result"
        
        val metrics = Platform.getPerformanceMetrics()
        metrics.shouldNotBe(emptyMap<String, PerformanceMetric>())
        metrics["test_operation"]?.count shouldBe 1L
    }
    
    test("JvmGraphFactory should create appropriate graph implementations") {
        // Small graph should use standard implementation
        val smallGraph = JvmGraphFactory.createGraph("small", true, 100, 200)
        smallGraph.id shouldBe "small"
        smallGraph.isDirected shouldBe true
        
        // Large graph should use optimized implementation
        val largeGraph = JvmGraphFactory.createGraph("large", false, 10000, 20000)
        largeGraph.shouldBeInstanceOf<JvmOptimizedGraphImpl>()
        largeGraph.id shouldBe "large"
        largeGraph.isDirected shouldBe false
    }
    
    test("JvmOptimizedGraphImpl should handle large graphs efficiently") {
        val graph = JvmOptimizedGraphImpl("test", true)
        
        // Add many nodes
        val nodes = (1..1000).map { i ->
            NodeImpl("node_$i", AttributeMap.empty(), null)
        }
        
        val edges = (1..999).map { i ->
            EdgeImpl(nodes[i-1], nodes[i], AttributeMap.empty(), emptyList())
        }
        
        val graphWithData = JvmOptimizedGraphImpl(
            "test_large",
            true,
            nodes.toSet(),
            edges.toSet(),
            emptySet(),
            AttributeMap.empty()
        )
        
        graphWithData.nodes.size shouldBe 1000
        graphWithData.edges.size shouldBe 999
        
        // Test efficient lookups
        graphWithData.getNode("node_500") shouldNotBe null
        graphWithData.getNode("node_500")?.id shouldBe "node_500"
        
        // Test memory stats
        val memoryStats = graphWithData.getMemoryStats()
        memoryStats.nodeCount shouldBe 1000
        memoryStats.edgeCount shouldBe 999
        memoryStats.estimatedMemoryUsageKB shouldBeGreaterThan 0L
    }
    
    test("Memory manager should provide recommendations") {
        val smallRecommendation = JvmMemoryManager.getMemoryRecommendation(100, 200)
        smallRecommendation shouldBe MemoryRecommendation.PROCEED_NORMALLY
        
        val largeRecommendation = JvmMemoryManager.getMemoryRecommendation(200000, 400000)
        largeRecommendation shouldBe MemoryRecommendation.USE_STREAMING_PROCESSING
    }
    
    test("Memory manager should track graph statistics") {
        val initialStats = JvmMemoryManager.getMemoryStatistics()
        val initialActiveGraphs = initialStats.activeGraphs
        
        // Create some graphs
        val graph1 = JvmMemoryManager.createOptimizedGraph("test1", true, 100, 200)
        val graph2 = JvmMemoryManager.createOptimizedGraph("test2", false, 500, 1000)
        
        val newStats = JvmMemoryManager.getMemoryStatistics()
        newStats.activeGraphs shouldBe initialActiveGraphs + 2
        newStats.totalGraphsCreated shouldBeGreaterThan initialStats.totalGraphsCreated
    }
    
    test("Performance monitor should track detailed metrics") {
        JvmPerformanceMonitor.clearMetrics()
        
        val result = JvmPerformanceMonitor.monitorOperation("test_parsing") {
            // Simulate parsing work
            Thread.sleep(50)
            "parsed_result"
        }
        
        result.result shouldBe "parsed_result"
        result.metrics shouldNotBe null
        result.metrics?.operationName shouldBe "test_parsing"
        result.metrics?.wallTimeMs?.let { it shouldBeGreaterThan 40.0 }
        
        val allMetrics = JvmPerformanceMonitor.getAllMetrics()
        allMetrics["test_parsing"] shouldNotBe null
        allMetrics["test_parsing"]?.executionCount shouldBe 1L
    }
    
    test("Graph builder should handle incremental construction") {
        val builder = JvmGraphFactory.createBuilder("incremental", true, 1000, 2000)
        
        // Add nodes incrementally
        repeat(100) { i ->
            val node = JvmGraphFactory.createNode("node_$i")
            builder.addNode(node)
        }
        
        // Add edges
        repeat(99) { i ->
            val source = NodeImpl("node_$i", AttributeMap.empty(), null)
            val target = NodeImpl("node_${i+1}", AttributeMap.empty(), null)
            val edge = JvmGraphFactory.createEdge(source, target)
            builder.addEdge(edge)
        }
        
        val stats = builder.getBuildStatistics()
        stats.currentNodes shouldBe 100
        stats.currentEdges shouldBe 99
        stats.estimatedMemoryUsageKB shouldBeGreaterThan 0L
        
        val graph = builder.build()
        graph.nodes.size shouldBe 100
        graph.edges.size shouldBe 99
    }
    
    test("Memory optimization should reduce memory usage") {
        // Create some graphs and let them go out of scope
        repeat(10) { i ->
            JvmMemoryManager.createOptimizedGraph("temp_$i", true, 1000, 2000)
        }
        
        // Force optimization
        val result = JvmMemoryManager.optimizeMemory()
        
        result.optimizedGraphs shouldBeGreaterThan 0
        // Note: deadReferencesRemoved might be 0 if GC hasn't run yet
    }
    
    test("Performance report should provide comprehensive metrics") {
        JvmPerformanceMonitor.clearMetrics()
        
        // Generate some operations
        repeat(5) { i ->
            JvmPerformanceMonitor.monitorOperation("test_op_$i") {
                Thread.sleep(10 + i * 5)
                "result_$i"
            }
        }
        
        val report = JvmPerformanceMonitor.generateReport()
        report.totalOperations shouldBe 5
        report.totalWallTimeMs shouldBeGreaterThan 0.0
        report.operationMetrics.size shouldBe 5
        
        val reportString = report.toFormattedString()
        reportString shouldContain "Performance Report"
        reportString shouldContain "Total Operations: 5"
    }
})