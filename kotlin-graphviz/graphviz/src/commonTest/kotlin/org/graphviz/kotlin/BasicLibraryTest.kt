package org.graphviz.kotlin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Basic tests using kotlin.test that work across all platforms.
 * These tests verify the core library functionality without relying on Kotest.
 */
class BasicLibraryTest {
    
    @Test
    fun testLibraryInitialization() {
        assertTrue(GraphvizLibrary.initialize(), "Library should initialize successfully")
    }
    
    @Test
    fun testLibraryVersion() {
        assertEquals("1.0.0", GraphvizLibrary.VERSION, "Version should match expected value")
    }
    
    @Test
    fun testSupportedPlatforms() {
        val platforms = GraphvizLibrary.SUPPORTED_PLATFORMS
        assertTrue(platforms.contains("JVM"), "Should support JVM")
        assertTrue(platforms.contains("Android"), "Should support Android")
        assertTrue(platforms.contains("iOS"), "Should support iOS")
        assertTrue(platforms.contains("JavaScript"), "Should support JavaScript")
    }
}