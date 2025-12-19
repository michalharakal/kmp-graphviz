package org.graphviz.kotlin.renderer

import org.graphviz.kotlin.model.Graph

/**
 * Interface for rendering graphs to SVG format.
 */
interface SvgRenderer {
    /**
     * Render a graph to SVG string format.
     * 
     * @param graph The positioned graph to render
     * @param options Rendering options and parameters
     * @return SVG markup as a string
     */
    fun render(graph: Graph, options: RenderOptions = RenderOptions.default()): String
    
    /**
     * Render a graph to SVG and write to file.
     * 
     * @param graph The positioned graph to render
     * @param path Output file path
     * @param options Rendering options and parameters
     */
    fun renderToFile(graph: Graph, path: String, options: RenderOptions = RenderOptions.default())
}

/**
 * Configuration options for SVG rendering.
 */
data class RenderOptions(
    /**
     * Width of the SVG viewport in pixels.
     */
    val width: Double? = null,
    
    /**
     * Height of the SVG viewport in pixels.
     */
    val height: Double? = null,
    
    /**
     * Margin around the graph content in pixels.
     */
    val margin: Double = 20.0,
    
    /**
     * Background color for the SVG (null for transparent).
     */
    val backgroundColor: String? = null,
    
    /**
     * Default font family for text elements.
     */
    val fontFamily: String = "Arial, sans-serif",
    
    /**
     * Default font size for text elements.
     */
    val fontSize: Double = 12.0,
    
    /**
     * Whether to include XML declaration and DOCTYPE.
     */
    val includeXmlDeclaration: Boolean = true,
    
    /**
     * Whether to optimize SVG output for smaller file size.
     */
    val optimize: Boolean = false,
    
    /**
     * Additional CSS styles to include in the SVG.
     */
    val additionalStyles: String? = null,
    
    /**
     * Renderer-specific options.
     */
    val rendererOptions: Map<String, Any> = emptyMap()
) {
    companion object {
        /**
         * Create default render options.
         */
        fun default(): RenderOptions = RenderOptions()
        
        /**
         * Create render options optimized for web display.
         */
        fun forWeb(): RenderOptions = RenderOptions(
            includeXmlDeclaration = false,
            optimize = true
        )
        
        /**
         * Create render options optimized for print.
         */
        fun forPrint(): RenderOptions = RenderOptions(
            backgroundColor = "white",
            margin = 50.0
        )
    }
}

/**
 * Result of a rendering operation.
 */
sealed class RenderResult {
    /**
     * Successful rendering with SVG content.
     */
    data class Success(val svg: String) : RenderResult()
    
    /**
     * Rendering failed with error information.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : RenderResult()
    
    /**
     * Check if the rendering was successful.
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Check if the rendering failed.
     */
    val isError: Boolean get() = this is Error
    
    /**
     * Get the SVG content if successful, or null if failed.
     */
    fun getSvgOrNull(): String? = when (this) {
        is Success -> svg
        is Error -> null
    }
    
    /**
     * Get the SVG content if successful, or throw if failed.
     */
    fun getSvgOrThrow(): String = when (this) {
        is Success -> svg
        is Error -> throw RenderException(message, cause)
    }
}

/**
 * Exception thrown when rendering operations fail.
 */
class RenderException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)