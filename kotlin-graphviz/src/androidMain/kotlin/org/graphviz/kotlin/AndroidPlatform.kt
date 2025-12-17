package org.graphviz.kotlin

/**
 * Android-specific platform utilities and optimizations
 */
actual object Platform {
    actual val name: String = "Android"
    
    /**
     * Android-specific initialization
     */
    fun initializeAndroid() {
        // Android-specific setup will be implemented here
    }
}