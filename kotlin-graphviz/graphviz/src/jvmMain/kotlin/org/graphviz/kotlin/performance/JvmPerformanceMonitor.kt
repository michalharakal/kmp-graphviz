package org.graphviz.kotlin.performance

import org.graphviz.kotlin.Platform
import org.graphviz.kotlin.model.Graph
import java.lang.management.ManagementFactory
import java.lang.management.ThreadMXBean
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime

/**
 * JVM-specific performance monitoring for graph operations.
 * Provides detailed metrics on memory usage, CPU time, and operation performance.
 */
object JvmPerformanceMonitor {
    
    private val threadBean: ThreadMXBean = ManagementFactory.getThreadMXBean()
    private val operationMetrics = ConcurrentHashMap<String, OperationMetrics>()
    private val isEnabled = AtomicLong(1) // 1 = enabled, 0 = disabled
    
    init {
        // Enable CPU time measurement if supported
        if (threadBean.isThreadCpuTimeSupported && !threadBean.isThreadCpuTimeEnabled) {
            threadBean.isThreadCpuTimeEnabled = true
        }
    }
    
    /**
     * Enable or disable performance monitoring
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled.set(if (enabled) 1 else 0)
    }
    
    /**
     * Check if performance monitoring is enabled
     */
    fun isEnabled(): Boolean = isEnabled.get() == 1L
    
    /**
     * Monitor a graph operation with detailed performance metrics
     */
    fun <T> monitorOperation(
        operationName: String,
        graph: Graph? = null,
        block: () -> T
    ): MonitoredResult<T> {
        if (!isEnabled()) {
            return MonitoredResult(block(), null)
        }
        
        val startMemory = Platform.getMemoryInfo()
        val startCpuTime = getCurrentThreadCpuTime()
        val startTime = System.nanoTime()
        
        var result: T
        val wallTime = measureNanoTime {
            result = block()
        }
        
        val endTime = System.nanoTime()
        val endCpuTime = getCurrentThreadCpuTime()
        val endMemory = Platform.getMemoryInfo()
        
        val metrics = PerformanceSnapshot(
            operationName = operationName,
            wallTimeNanos = wallTime,
            cpuTimeNanos = endCpuTime - startCpuTime,
            memoryDeltaBytes = endMemory.heapUsed - startMemory.heapUsed,
            startMemoryBytes = startMemory.heapUsed,
            endMemoryBytes = endMemory.heapUsed,
            heapUtilizationBefore = startMemory.heapUtilization,
            heapUtilizationAfter = endMemory.heapUtilization,
            graphNodeCount = graph?.getAllNodes()?.size ?: 0,
            graphEdgeCount = graph?.getAllEdges()?.size ?: 0
        )
        
        // Record metrics for aggregation
        recordOperationMetrics(operationName, metrics)
        
        return MonitoredResult(result, metrics)
    }
    
    /**
     * Monitor graph parsing performance
     */
    fun <T> monitorParsing(dotContent: String, block: () -> T): MonitoredResult<T> {
        return monitorOperation("graph_parsing") {
            Platform.withPerformanceMonitoring("graph_parsing") {
                block()
            }
        }
    }
    
    /**
     * Monitor graph layout performance
     */
    fun <T> monitorLayout(graph: Graph, layoutEngine: String, block: () -> T): MonitoredResult<T> {
        return monitorOperation("graph_layout_$layoutEngine", graph) {
            Platform.withPerformanceMonitoring("graph_layout") {
                block()
            }
        }
    }
    
    /**
     * Monitor graph rendering performance
     */
    fun <T> monitorRendering(graph: Graph, format: String, block: () -> T): MonitoredResult<T> {
        return monitorOperation("graph_rendering_$format", graph) {
            Platform.withPerformanceMonitoring("graph_rendering") {
                block()
            }
        }
    }
    
    /**
     * Get aggregated metrics for all operations
     */
    fun getAllMetrics(): Map<String, OperationMetrics> {
        return operationMetrics.toMap()
    }
    
    /**
     * Get metrics for a specific operation
     */
    fun getMetrics(operationName: String): OperationMetrics? {
        return operationMetrics[operationName]
    }
    
    /**
     * Clear all recorded metrics
     */
    fun clearMetrics() {
        operationMetrics.clear()
    }
    
    /**
     * Generate a performance report
     */
    fun generateReport(): PerformanceReport {
        val allMetrics = getAllMetrics()
        val totalOperations = allMetrics.values.sumOf { it.executionCount }
        val totalWallTime = allMetrics.values.sumOf { it.totalWallTimeMs }
        val totalCpuTime = allMetrics.values.sumOf { it.totalCpuTimeMs }
        val totalMemoryAllocated = allMetrics.values.sumOf { it.totalMemoryAllocatedKB }
        
        val currentMemory = Platform.getMemoryInfo()
        val gcInfo = Platform.getGCInfo()
        
        return PerformanceReport(
            totalOperations = totalOperations,
            totalWallTimeMs = totalWallTime,
            totalCpuTimeMs = totalCpuTime,
            totalMemoryAllocatedKB = totalMemoryAllocated,
            currentMemoryInfo = currentMemory,
            gcInfo = gcInfo,
            operationMetrics = allMetrics,
            reportTimestamp = System.currentTimeMillis()
        )
    }
    
    private fun getCurrentThreadCpuTime(): Long {
        return if (threadBean.isThreadCpuTimeSupported) {
            threadBean.getCurrentThreadCpuTime()
        } else {
            0L
        }
    }
    
    private fun recordOperationMetrics(operationName: String, snapshot: PerformanceSnapshot) {
        val metrics = operationMetrics.computeIfAbsent(operationName) { 
            OperationMetrics(operationName) 
        }
        metrics.recordExecution(snapshot)
    }
}

/**
 * Result of a monitored operation
 */
data class MonitoredResult<T>(
    val result: T,
    val metrics: PerformanceSnapshot?
)

/**
 * Snapshot of performance metrics for a single operation execution
 */
data class PerformanceSnapshot(
    val operationName: String,
    val wallTimeNanos: Long,
    val cpuTimeNanos: Long,
    val memoryDeltaBytes: Long,
    val startMemoryBytes: Long,
    val endMemoryBytes: Long,
    val heapUtilizationBefore: Double,
    val heapUtilizationAfter: Double,
    val graphNodeCount: Int,
    val graphEdgeCount: Int
) {
    val wallTimeMs: Double get() = wallTimeNanos / 1_000_000.0
    val cpuTimeMs: Double get() = cpuTimeNanos / 1_000_000.0
    val memoryDeltaKB: Double get() = memoryDeltaBytes / 1024.0
    val cpuEfficiency: Double get() = if (wallTimeNanos > 0) cpuTimeNanos.toDouble() / wallTimeNanos else 0.0
    
    override fun toString(): String {
        return "PerformanceSnapshot(op=$operationName, " +
               "wall=${String.format("%.2f", wallTimeMs)}ms, " +
               "cpu=${String.format("%.2f", cpuTimeMs)}ms, " +
               "memory=${String.format("%.2f", memoryDeltaKB)}KB, " +
               "nodes=$graphNodeCount, edges=$graphEdgeCount)"
    }
}

/**
 * Aggregated metrics for an operation type
 */
class OperationMetrics(val operationName: String) {
    private val executions = mutableListOf<PerformanceSnapshot>()
    
    @Synchronized
    fun recordExecution(snapshot: PerformanceSnapshot) {
        executions.add(snapshot)
        
        // Keep only the last 1000 executions to prevent memory leaks
        if (executions.size > 1000) {
            executions.removeAt(0)
        }
    }
    
    val executionCount: Int get() = executions.size
    
    val totalWallTimeMs: Double get() = executions.sumOf { it.wallTimeMs }
    val averageWallTimeMs: Double get() = if (executionCount > 0) totalWallTimeMs / executionCount else 0.0
    val maxWallTimeMs: Double get() = executions.maxOfOrNull { it.wallTimeMs } ?: 0.0
    val minWallTimeMs: Double get() = executions.minOfOrNull { it.wallTimeMs } ?: 0.0
    
    val totalCpuTimeMs: Double get() = executions.sumOf { it.cpuTimeMs }
    val averageCpuTimeMs: Double get() = if (executionCount > 0) totalCpuTimeMs / executionCount else 0.0
    val maxCpuTimeMs: Double get() = executions.maxOfOrNull { it.cpuTimeMs } ?: 0.0
    
    val totalMemoryAllocatedKB: Double get() = executions.filter { it.memoryDeltaKB > 0 }.sumOf { it.memoryDeltaKB }
    val averageMemoryAllocatedKB: Double get() = if (executionCount > 0) totalMemoryAllocatedKB / executionCount else 0.0
    val maxMemoryAllocatedKB: Double get() = executions.maxOfOrNull { it.memoryDeltaKB } ?: 0.0
    
    val averageCpuEfficiency: Double get() = if (executionCount > 0) executions.sumOf { it.cpuEfficiency } / executionCount else 0.0
    
    val averageGraphSize: Pair<Int, Int> get() {
        if (executionCount == 0) return Pair(0, 0)
        val avgNodes = executions.sumOf { it.graphNodeCount } / executionCount
        val avgEdges = executions.sumOf { it.graphEdgeCount } / executionCount
        return Pair(avgNodes, avgEdges)
    }
    
    /**
     * Get performance percentiles
     */
    fun getWallTimePercentiles(): PerformancePercentiles {
        val sortedTimes = executions.map { it.wallTimeMs }.sorted()
        return PerformancePercentiles.fromSortedList(sortedTimes)
    }
    
    fun getMemoryPercentiles(): PerformancePercentiles {
        val sortedMemory = executions.map { it.memoryDeltaKB }.sorted()
        return PerformancePercentiles.fromSortedList(sortedMemory)
    }
    
    override fun toString(): String {
        return "OperationMetrics(op=$operationName, count=$executionCount, " +
               "avgWall=${String.format("%.2f", averageWallTimeMs)}ms, " +
               "avgCpu=${String.format("%.2f", averageCpuTimeMs)}ms, " +
               "avgMemory=${String.format("%.2f", averageMemoryAllocatedKB)}KB, " +
               "cpuEff=${String.format("%.2f", averageCpuEfficiency * 100)}%)"
    }
}

/**
 * Performance percentiles for analysis
 */
data class PerformancePercentiles(
    val p50: Double,
    val p90: Double,
    val p95: Double,
    val p99: Double
) {
    companion object {
        fun fromSortedList(sortedValues: List<Double>): PerformancePercentiles {
            if (sortedValues.isEmpty()) {
                return PerformancePercentiles(0.0, 0.0, 0.0, 0.0)
            }
            
            fun percentile(p: Double): Double {
                val index = ((sortedValues.size - 1) * p).toInt()
                return sortedValues[index.coerceIn(0, sortedValues.size - 1)]
            }
            
            return PerformancePercentiles(
                p50 = percentile(0.5),
                p90 = percentile(0.9),
                p95 = percentile(0.95),
                p99 = percentile(0.99)
            )
        }
    }
}

/**
 * Comprehensive performance report
 */
data class PerformanceReport(
    val totalOperations: Int,
    val totalWallTimeMs: Double,
    val totalCpuTimeMs: Double,
    val totalMemoryAllocatedKB: Double,
    val currentMemoryInfo: org.graphviz.kotlin.MemoryInfo,
    val gcInfo: List<org.graphviz.kotlin.GCInfo>,
    val operationMetrics: Map<String, OperationMetrics>,
    val reportTimestamp: Long
) {
    fun toFormattedString(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Graphviz JVM Performance Report ===")
        sb.appendLine("Generated: ${java.time.Instant.ofEpochMilli(reportTimestamp)}")
        sb.appendLine()
        
        sb.appendLine("Overall Statistics:")
        sb.appendLine("  Total Operations: $totalOperations")
        sb.appendLine("  Total Wall Time: ${String.format("%.2f", totalWallTimeMs)}ms")
        sb.appendLine("  Total CPU Time: ${String.format("%.2f", totalCpuTimeMs)}ms")
        sb.appendLine("  Total Memory Allocated: ${String.format("%.2f", totalMemoryAllocatedKB)}KB")
        sb.appendLine()
        
        sb.appendLine("Current Memory Status:")
        sb.appendLine("  Heap Used: ${currentMemoryInfo.heapUsed / 1024 / 1024}MB")
        sb.appendLine("  Heap Max: ${currentMemoryInfo.heapMax / 1024 / 1024}MB")
        sb.appendLine("  Heap Utilization: ${String.format("%.1f", currentMemoryInfo.heapUtilization * 100)}%")
        sb.appendLine("  Memory Pressure: ${currentMemoryInfo.isMemoryPressure}")
        sb.appendLine()
        
        if (gcInfo.isNotEmpty()) {
            sb.appendLine("Garbage Collection:")
            gcInfo.forEach { gc ->
                sb.appendLine("  ${gc.name}: ${gc.collectionCount} collections, " +
                             "${gc.collectionTime}ms total, " +
                             "${String.format("%.2f", gc.averageCollectionTime)}ms avg")
            }
            sb.appendLine()
        }
        
        if (operationMetrics.isNotEmpty()) {
            sb.appendLine("Operation Metrics:")
            operationMetrics.values.sortedByDescending { it.totalWallTimeMs }.forEach { metrics ->
                sb.appendLine("  $metrics")
                val wallPercentiles = metrics.getWallTimePercentiles()
                sb.appendLine("    Wall Time Percentiles: P50=${String.format("%.2f", wallPercentiles.p50)}ms, " +
                             "P90=${String.format("%.2f", wallPercentiles.p90)}ms, " +
                             "P95=${String.format("%.2f", wallPercentiles.p95)}ms, " +
                             "P99=${String.format("%.2f", wallPercentiles.p99)}ms")
            }
        }
        
        return sb.toString()
    }
}