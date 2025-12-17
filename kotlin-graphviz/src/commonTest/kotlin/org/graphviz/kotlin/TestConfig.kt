package org.graphviz.kotlin

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.property.PropertyTesting

/**
 * Global test configuration for the Kotlin Graphviz library.
 * 
 * This configuration ensures that property-based tests run with a minimum of 100 iterations
 * as specified in the design document for comprehensive testing coverage.
 */
object TestConfig : AbstractProjectConfig() {
    
    init {
        // Configure property-based testing to run minimum 100 iterations
        PropertyTesting.defaultIterationCount = 100
        
        // Set reasonable timeout for property tests
        PropertyTesting.defaultShrinkingMode = io.kotest.property.ShrinkingMode.Bounded(1000)
    }
}