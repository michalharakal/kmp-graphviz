package org.graphviz.kotlin

/**
 * iOS-specific platform utilities and optimizations
 */
actual object Platform {
    actual val name: String = "iOS"
    
    /**
     * iOS-specific initialization
     */
    fun initializeIos() {
        // iOS-specific setup will be implemented here
    }
}