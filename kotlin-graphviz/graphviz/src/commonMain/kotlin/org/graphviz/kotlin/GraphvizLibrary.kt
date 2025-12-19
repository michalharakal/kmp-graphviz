package org.graphviz.kotlin

import org.graphviz.kotlin.layout.LayoutEngineRegistry
import org.graphviz.kotlin.layout.dot.DotLayoutEngine

/**
 * Main entry point for the Kotlin Multiplatform Graphviz library.
 * This class provides the primary API for graph processing, layout, and rendering.
 */
object GraphvizLibrary {
    
    /**
     * Library version information
     */
    const val VERSION = "1.0.0"
    
    /**
     * Supported target platforms
     */
    val SUPPORTED_PLATFORMS = setOf("JVM", "Android", "iOS", "JavaScript")
    
    /**
     * Initialize the library for the current platform.
     * This registers all available layout engines and sets up the library for use.
     */
    fun initialize(): Boolean {
        if (isInitialized()) {
            return true
        }
        
        try {
            // Register all available layout engines
            registerLayoutEngines()
            
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Register all available layout engines with the registry.
     */
    private fun registerLayoutEngines() {
        // Register the dot layout engine
        LayoutEngineRegistry.register(DotLayoutEngine())
        
        // Future layout engines (neato, fdp, etc.) will be registered here
    }
    
    /**
     * Check if the library has been initialized by checking if layout engines are registered.
     */
    fun isInitialized(): Boolean = LayoutEngineRegistry.isRegistered("dot")
    
    /**
     * Get all available layout engine names.
     */
    fun getAvailableLayoutEngines(): Set<String> {
        return LayoutEngineRegistry.getAll().keys
    }
}

