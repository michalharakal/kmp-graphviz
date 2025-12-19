package org.graphviz.kotlin.layout

import org.graphviz.kotlin.model.Graph

/**
 * Interface for graph layout engines that calculate node and edge positions.
 */
interface LayoutEngine {
    /**
     * The name of this layout engine (e.g., "dot", "neato", "fdp").
     */
    val name: String
    
    /**
     * Apply layout algorithm to the given graph with specified options.
     * 
     * @param graph The input graph to layout
     * @param options Layout-specific options and parameters
     * @return Result containing the positioned graph or error information
     */
    fun layout(graph: Graph, options: LayoutOptions = LayoutOptions.default()): LayoutResult
}

/**
 * Configuration options for layout algorithms.
 */
data class LayoutOptions(
    /**
     * Spacing between nodes in the same rank (horizontal spacing for dot layout).
     */
    val nodeSpacing: Double = 50.0,
    
    /**
     * Spacing between ranks (vertical spacing for dot layout).
     */
    val rankSpacing: Double = 75.0,
    
    /**
     * Minimum node width for layout calculations.
     */
    val minNodeWidth: Double = 30.0,
    
    /**
     * Minimum node height for layout calculations.
     */
    val minNodeHeight: Double = 20.0,
    
    /**
     * Whether to optimize for minimal edge crossings.
     */
    val minimizeCrossings: Boolean = true,
    
    /**
     * Maximum number of iterations for iterative algorithms.
     */
    val maxIterations: Int = 100,
    
    /**
     * Convergence threshold for iterative algorithms.
     */
    val convergenceThreshold: Double = 1e-6,
    
    /**
     * Additional engine-specific options.
     */
    val engineOptions: Map<String, Any> = emptyMap()
) {
    companion object {
        /**
         * Create default layout options.
         */
        fun default(): LayoutOptions = LayoutOptions()
        
        /**
         * Create layout options optimized for large graphs.
         */
        fun forLargeGraphs(): LayoutOptions = LayoutOptions(
            nodeSpacing = 30.0,
            rankSpacing = 50.0,
            minimizeCrossings = false,
            maxIterations = 50
        )
        
        /**
         * Create layout options optimized for high-quality output.
         */
        fun forHighQuality(): LayoutOptions = LayoutOptions(
            nodeSpacing = 75.0,
            rankSpacing = 100.0,
            minimizeCrossings = true,
            maxIterations = 200
        )
    }
}

/**
 * Result of a layout operation.
 */
sealed class LayoutResult {
    /**
     * Successful layout with positioned graph.
     */
    data class Success(val graph: Graph) : LayoutResult()
    
    /**
     * Layout failed with error information.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val partialGraph: Graph? = null
    ) : LayoutResult()
    
    /**
     * Check if the layout was successful.
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Check if the layout failed.
     */
    val isError: Boolean get() = this is Error
    
    /**
     * Get the resulting graph if successful, or null if failed.
     */
    fun getGraphOrNull(): Graph? = when (this) {
        is Success -> graph
        is Error -> partialGraph
    }
    
    /**
     * Get the resulting graph if successful, or throw if failed.
     */
    fun getGraphOrThrow(): Graph = when (this) {
        is Success -> graph
        is Error -> throw LayoutException(message, cause)
    }
}

/**
 * Exception thrown when layout operations fail.
 */
class LayoutException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)