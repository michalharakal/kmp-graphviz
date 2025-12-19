package org.graphviz.kotlin

/**
 * JVM-specific platform utilities and optimizations
 */
actual object Platform {
    actual val name: String = "JVM"
    
    /**
     * JVM-specific initialization
     */
    fun initializeJvm() {
        // JVM-specific setup will be implemented here
    }
}