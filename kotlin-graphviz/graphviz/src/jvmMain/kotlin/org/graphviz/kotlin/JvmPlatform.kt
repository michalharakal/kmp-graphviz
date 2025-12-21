package org.graphviz.kotlin

import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.GarbageCollectorMXBean
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * JVM-specific platform utilities and optimizations
 */
actual object Platform {
    actual val name: String = "JVM"
    
    private val memoryBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
    private val gcBeans: List<GarbageCollectorMXBean> = ManagementFactory.getGarbageCollectorMXBeans()
    
    // Performance monitoring
    private val performanceMetrics = ConcurrentHashMap<String, PerformanceMetric>()
    
    /**
     * JVM-specific initialization with memory optimization settings
     */
    fun initializeJvm() {
        // Suggest GC tuning for large graphs
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", 
                          Runtime.getRuntime().availableProcessors().toString())
        
        // Initialize performance monitoring
        initializePerformanceMonitoring()
    }
    
    /**
     * Get current memory usage information
     */
    fun getMemoryInfo(): MemoryInfo {
        val heapMemory = memoryBean.heapMemoryUsage
        val nonHeapMemory = memoryBean.nonHeapMemoryUsage
        
        return MemoryInfo(
            heapUsed = heapMemory.used,
            heapMax = heapMemory.max,
            heapCommitted = heapMemory.committed,
            nonHeapUsed = nonHeapMemory.used,
            nonHeapMax = nonHeapMemory.max,
            nonHeapCommitted = nonHeapMemory.committed
        )
    }
    
    /**
     * Get garbage collection statistics
     */
    fun getGCInfo(): List<GCInfo> {
        return gcBeans.map { bean ->
            GCInfo(
                name = bean.name,
                collectionCount = bean.collectionCount,
                collectionTime = bean.collectionTime
            )
        }
    }
    
    /**
     * Force garbage collection (use sparingly)
     */
    fun forceGC() {
        System.gc()
    }
    
    /**
     * Start performance monitoring for an operation
     */
    fun startPerformanceMonitoring(operationName: String): PerformanceToken {
        val startTime = System.nanoTime()
        val startMemory = getMemoryInfo()
        return PerformanceToken(operationName, startTime, startMemory)
    }
    
    /**
     * End performance monitoring and record metrics
     */
    fun endPerformanceMonitoring(token: PerformanceToken) {
        val endTime = System.nanoTime()
        val endMemory = getMemoryInfo()
        val duration = endTime - token.startTime
        val memoryDelta = endMemory.heapUsed - token.startMemory.heapUsed
        
        val metric = performanceMetrics.computeIfAbsent(token.operationName) { 
            PerformanceMetric(token.operationName) 
        }
        metric.recordExecution(duration, memoryDelta)
    }
    
    /**
     * Get performance metrics for all monitored operations
     */
    fun getPerformanceMetrics(): Map<String, PerformanceMetric> {
        return performanceMetrics.toMap()
    }
    
    /**
     * Clear all performance metrics
     */
    fun clearPerformanceMetrics() {
        performanceMetrics.clear()
    }
    
    /**
     * Execute a block with performance monitoring
     */
    inline fun <T> withPerformanceMonitoring(operationName: String, block: () -> T): T {
        val token = startPerformanceMonitoring(operationName)
        try {
            return block()
        } finally {
            endPerformanceMonitoring(token)
        }
    }
    
    /**
     * Get optimal collection size hint based on available memory
     */
    fun getOptimalCollectionSize(estimatedElementSize: Int): Int {
        val memoryInfo = getMemoryInfo()
        val availableMemory = memoryInfo.heapMax - memoryInfo.heapUsed
        val safeMemoryLimit = (availableMemory * 0.7).toLong() // Use 70% of available memory
        
        return (safeMemoryLimit / estimatedElementSize).toInt().coerceAtLeast(1000)
    }
    
    private fun initializePerformanceMonitoring() {
        // Pre-register common operations
        performanceMetrics["graph_parsing"] = PerformanceMetric("graph_parsing")
        performanceMetrics["graph_layout"] = PerformanceMetric("graph_layout")
        performanceMetrics["graph_rendering"] = PerformanceMetric("graph_rendering")
        performanceMetrics["attribute_processing"] = PerformanceMetric("attribute_processing")
    }
}

/**
 * Memory usage information
 */
data class MemoryInfo(
    val heapUsed: Long,
    val heapMax: Long,
    val heapCommitted: Long,
    val nonHeapUsed: Long,
    val nonHeapMax: Long,
    val nonHeapCommitted: Long
) {
    val heapUtilization: Double get() = if (heapMax > 0) heapUsed.toDouble() / heapMax else 0.0
    val isMemoryPressure: Boolean get() = heapUtilization > 0.8
}

/**
 * Garbage collection information
 */
data class GCInfo(
    val name: String,
    val collectionCount: Long,
    val collectionTime: Long
) {
    val averageCollectionTime: Double get() = if (collectionCount > 0) collectionTime.toDouble() / collectionCount else 0.0
}

/**
 * Performance monitoring token
 */
data class PerformanceToken(
    val operationName: String,
    val startTime: Long,
    val startMemory: MemoryInfo
)

/**
 * Performance metrics for an operation
 */
class PerformanceMetric(val operationName: String) {
    private val executionCount = AtomicLong(0)
    private val totalDuration = AtomicLong(0)
    private val totalMemoryDelta = AtomicLong(0)
    private val maxDuration = AtomicLong(0)
    private val maxMemoryDelta = AtomicLong(0)
    
    fun recordExecution(durationNanos: Long, memoryDelta: Long) {
        executionCount.incrementAndGet()
        totalDuration.addAndGet(durationNanos)
        totalMemoryDelta.addAndGet(memoryDelta)
        
        // Update max values atomically
        var currentMax = maxDuration.get()
        while (durationNanos > currentMax && !maxDuration.compareAndSet(currentMax, durationNanos)) {
            currentMax = maxDuration.get()
        }
        
        currentMax = maxMemoryDelta.get()
        while (memoryDelta > currentMax && !maxMemoryDelta.compareAndSet(currentMax, memoryDelta)) {
            currentMax = maxMemoryDelta.get()
        }
    }
    
    val count: Long get() = executionCount.get()
    val averageDurationMs: Double get() = if (count > 0) totalDuration.get() / 1_000_000.0 / count else 0.0
    val maxDurationMs: Double get() = maxDuration.get() / 1_000_000.0
    val averageMemoryDeltaKB: Double get() = if (count > 0) totalMemoryDelta.get() / 1024.0 / count else 0.0
    val maxMemoryDeltaKB: Double get() = maxMemoryDelta.get() / 1024.0
    val totalMemoryDeltaKB: Double get() = totalMemoryDelta.get() / 1024.0
    
    override fun toString(): String {
        return "PerformanceMetric(operation=$operationName, count=$count, " +
               "avgDuration=${String.format("%.2f", averageDurationMs)}ms, " +
               "maxDuration=${String.format("%.2f", maxDurationMs)}ms, " +
               "avgMemory=${String.format("%.2f", averageMemoryDeltaKB)}KB, " +
               "maxMemory=${String.format("%.2f", maxMemoryDeltaKB)}KB)"
    }
}