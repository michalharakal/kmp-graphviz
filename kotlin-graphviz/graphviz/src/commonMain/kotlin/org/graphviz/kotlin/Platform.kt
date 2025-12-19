package org.graphviz.kotlin

/**
 * Platform-specific utilities and optimizations.
 * Each platform provides its own implementation.
 */
expect object Platform {
    val name: String
}