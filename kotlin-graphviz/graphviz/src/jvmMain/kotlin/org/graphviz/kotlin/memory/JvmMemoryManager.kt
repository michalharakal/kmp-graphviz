package org.graphviz.kotlin.memory

import org.graphviz.kotlin.Platform
import org.graphviz.kotlin.model.Graph
import org.graphviz.kotlin.collections.JvmOptimizedGraphImpl
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * JVM-specific memory management for large graphs.
 * Provides memory monitoring, optimization, and cleanup strategies.
 */
object JvmMemoryManager {
    
    private val graphReferences = ConcurrentHashMap<String, WeakReference<Graph>>()
    private val memoryThresholds = MemoryThresholds()
    private val cleanupScheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "GraphvizMemoryCleanup").apply { isDaemon = true }
    }
    
    private val totalGraphsCreated = AtomicLong(0)
    private val totalGraphsCollected = AtomicLong(0)
    private val memoryOptimizationCount = AtomicLong(0)
    
    init {
        // Schedule periodic cleanup
        cleanupScheduler.scheduleAtFixedRate(
            { performPeriodicCleanup() },
            30, // Initial delay
            60, // Period
            TimeUnit.SECONDS
        )
        
        // Add shutdown hook to clean up resources
        Runtime.getRuntime().addShutdownHook(Thread {
            shutdown()
        })
    }
    
    /**
     * Register a graph for memory monitoring
     */
    fun registerGraph(graph: Graph) {
        graphReferences[graph.id] = WeakReference(graph)
        totalGraphsCreated.incrementAndGet()
        
        // Check if we need to optimize memory
        checkMemoryPressure()
    }
    
    /**
     * Unregister a graph from memory monitoring
     */
    fun unregisterGraph(graphId: String) {
        graphReferences.remove(graphId)
    }
    
    /**
     * Create a memory-optimized graph based on current memory conditions
     */
    fun createOptimizedGraph(
        id: String,
        isDirected: Boolean,
        estimatedNodes: Int = 1000,
        estimatedEdges: Int = 1000
    ): Graph {
        val memoryInfo = Platform.getMemoryInfo()
        
        return if (memoryInfo.isMemoryPressure || estimatedNodes > 10000) {
            // Use JVM-optimized implementation for large graphs or memory pressure
            JvmOptimizedGraphImpl(id, isDirected)
        } else {
            // Use standard implementation for smaller graphs
            org.graphviz.kotlin.model.GraphImpl(
                id = id,
                isDirected = isDirected,
                nodes = emptySet(),
                edges = emptySet(),
                subgraphs = emptySet(),
                attributes = org.graphviz.kotlin.model.AttributeMap.empty()
            )
        }.also { registerGraph(it) }
    }
    
    /**
     * Optimize memory usage across all registered graphs
     */
    fun optimizeMemory(): MemoryOptimizationResult {
        val startMemory = Platform.getMemoryInfo()
        var optimizedGraphs = 0
        var memoryFreed = 0L
        
        // Clean up dead references
        val deadReferences = mutableListOf<String>()
        graphReferences.forEach { (id, ref) ->
            if (ref.get() == null) {
                deadReferences.add(id)
                totalGraphsCollected.incrementAndGet()
            }
        }
        deadReferences.forEach { graphReferences.remove(it) }
        
        // Optimize living graphs
        graphReferences.values.forEach { ref ->
            val graph = ref.get()
            if (graph is JvmOptimizedGraphImpl) {
                graph.optimizeMemory()
                optimizedGraphs++
            }
        }
        
        // Force garbage collection if memory pressure is high
        if (startMemory.isMemoryPressure) {
            Platform.forceGC()
            Thread.sleep(100) // Give GC time to work
        }
        
        val endMemory = Platform.getMemoryInfo()
        memoryFreed = max(0, startMemory.heapUsed - endMemory.heapUsed)
        memoryOptimizationCount.incrementAndGet()
        
        return MemoryOptimizationResult(
            optimizedGraphs = optimizedGraphs,
            deadReferencesRemoved = deadReferences.size,
            memoryFreedKB = memoryFreed / 1024,
            memoryBeforeKB = startMemory.heapUsed / 1024,
            memoryAfterKB = endMemory.heapUsed / 1024,
            heapUtilizationBefore = startMemory.heapUtilization,
            heapUtilizationAfter = endMemory.heapUtilization
        )
    }
    
    /**
     * Get current memory statistics
     */
    fun getMemoryStatistics(): MemoryStatistics {
        val memoryInfo = Platform.getMemoryInfo()
        val gcInfo = Platform.getGCInfo()
        val activeGraphs = graphReferences.values.count { it.get() != null }
        
        return MemoryStatistics(
            activeGraphs = activeGraphs,
            totalGraphsCreated = totalGraphsCreated.get(),
            totalGraphsCollected = totalGraphsCollected.get(),
            memoryOptimizationCount = memoryOptimizationCount.get(),
            currentMemoryInfo = memoryInfo,
            gcInfo = gcInfo,
            memoryThresholds = memoryThresholds
        )
    }
    
    /**
     * Configure memory thresholds for optimization triggers
     */
    fun configureMemoryThresholds(
        warningThreshold: Double = 0.7,
        criticalThreshold: Double = 0.85,
        maxGraphSize: Int = 100000
    ) {
        memoryThresholds.warningThreshold = warningThreshold
        memoryThresholds.criticalThreshold = criticalThreshold
        memoryThresholds.maxGraphSize = maxGraphSize
    }
    
    /**
     * Check if a graph size is within acceptable limits
     */
    fun isGraphSizeAcceptable(nodeCount: Int, edgeCount: Int): Boolean {
        val totalElements = nodeCount + edgeCount
        val memoryInfo = Platform.getMemoryInfo()
        
        return when {
            totalElements > memoryThresholds.maxGraphSize -> false
            memoryInfo.heapUtilization > memoryThresholds.criticalThreshold -> false
            totalElements > 50000 && memoryInfo.heapUtilization > memoryThresholds.warningThreshold -> false
            else -> true
        }
    }
    
    /**
     * Get memory usage recommendation for a graph
     */
    fun getMemoryRecommendation(nodeCount: Int, edgeCount: Int): MemoryRecommendation {
        val memoryInfo = Platform.getMemoryInfo()
        val estimatedMemoryKB = (nodeCount * 200L + edgeCount * 300L) / 1024
        val availableMemoryKB = (memoryInfo.heapMax - memoryInfo.heapUsed) / 1024
        
        return when {
            estimatedMemoryKB > availableMemoryKB * 0.8 -> {
                MemoryRecommendation.INSUFFICIENT_MEMORY
            }
            memoryInfo.heapUtilization > memoryThresholds.criticalThreshold -> {
                MemoryRecommendation.OPTIMIZE_BEFORE_PROCESSING
            }
            nodeCount + edgeCount > memoryThresholds.maxGraphSize -> {
                MemoryRecommendation.USE_STREAMING_PROCESSING
            }
            memoryInfo.heapUtilization > memoryThresholds.warningThreshold -> {
                MemoryRecommendation.MONITOR_CLOSELY
            }
            else -> {
                MemoryRecommendation.PROCEED_NORMALLY
            }
        }
    }
    
    private fun checkMemoryPressure() {
        val memoryInfo = Platform.getMemoryInfo()
        
        if (memoryInfo.heapUtilization > memoryThresholds.criticalThreshold) {
            // Immediate optimization needed
            optimizeMemory()
        } else if (memoryInfo.heapUtilization > memoryThresholds.warningThreshold) {
            // Schedule optimization
            cleanupScheduler.schedule({ optimizeMemory() }, 5, TimeUnit.SECONDS)
        }
    }
    
    private fun performPeriodicCleanup() {
        try {
            val memoryInfo = Platform.getMemoryInfo()
            
            // Always clean up dead references
            val deadReferences = mutableListOf<String>()
            graphReferences.forEach { (id, ref) ->
                if (ref.get() == null) {
                    deadReferences.add(id)
                    totalGraphsCollected.incrementAndGet()
                }
            }
            deadReferences.forEach { graphReferences.remove(it) }
            
            // Optimize if memory utilization is above warning threshold
            if (memoryInfo.heapUtilization > memoryThresholds.warningThreshold) {
                optimizeMemory()
            }
        } catch (e: Exception) {
            // Log error but don't let it stop the cleanup scheduler
            System.err.println("Error during periodic memory cleanup: ${e.message}")
        }
    }
    
    private fun shutdown() {
        cleanupScheduler.shutdown()
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            cleanupScheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}

/**
 * Memory thresholds configuration
 */
data class MemoryThresholds(
    var warningThreshold: Double = 0.7,
    var criticalThreshold: Double = 0.85,
    var maxGraphSize: Int = 100000
)

/**
 * Result of memory optimization operation
 */
data class MemoryOptimizationResult(
    val optimizedGraphs: Int,
    val deadReferencesRemoved: Int,
    val memoryFreedKB: Long,
    val memoryBeforeKB: Long,
    val memoryAfterKB: Long,
    val heapUtilizationBefore: Double,
    val heapUtilizationAfter: Double
) {
    val memoryFreedPercent: Double get() = if (memoryBeforeKB > 0) (memoryFreedKB.toDouble() / memoryBeforeKB) * 100 else 0.0
    
    override fun toString(): String {
        return "MemoryOptimizationResult(optimized=$optimizedGraphs graphs, " +
               "removed=$deadReferencesRemoved dead refs, " +
               "freed=${memoryFreedKB}KB (${String.format("%.1f", memoryFreedPercent)}%), " +
               "heap: ${String.format("%.1f", heapUtilizationBefore * 100)}% → ${String.format("%.1f", heapUtilizationAfter * 100)}%)"
    }
}

/**
 * Memory statistics
 */
data class MemoryStatistics(
    val activeGraphs: Int,
    val totalGraphsCreated: Long,
    val totalGraphsCollected: Long,
    val memoryOptimizationCount: Long,
    val currentMemoryInfo: org.graphviz.kotlin.MemoryInfo,
    val gcInfo: List<org.graphviz.kotlin.GCInfo>,
    val memoryThresholds: MemoryThresholds
) {
    val graphCollectionRate: Double get() = if (totalGraphsCreated > 0) totalGraphsCollected.toDouble() / totalGraphsCreated else 0.0
    
    override fun toString(): String {
        return "MemoryStatistics(active=$activeGraphs graphs, " +
               "created=$totalGraphsCreated, collected=$totalGraphsCollected, " +
               "optimizations=$memoryOptimizationCount, " +
               "heap=${String.format("%.1f", currentMemoryInfo.heapUtilization * 100)}%, " +
               "collection_rate=${String.format("%.1f", graphCollectionRate * 100)}%)"
    }
}

/**
 * Memory usage recommendations
 */
enum class MemoryRecommendation(val message: String) {
    PROCEED_NORMALLY("Memory usage is within acceptable limits"),
    MONITOR_CLOSELY("Memory usage is elevated, monitor closely"),
    OPTIMIZE_BEFORE_PROCESSING("Memory pressure detected, optimize before processing"),
    USE_STREAMING_PROCESSING("Graph is very large, consider streaming processing"),
    INSUFFICIENT_MEMORY("Insufficient memory available for this operation")
}