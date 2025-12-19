package org.graphviz.kotlin

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import org.graphviz.kotlin.layout.LayoutEngineRegistry

/**
 * Basic tests for GraphvizLibrary to verify multiplatform setup
 */
class GraphvizLibraryTest : StringSpec({
    
    "library should initialize successfully" {
        GraphvizLibrary.initialize() shouldBe true
    }
    
    "library should have correct version" {
        GraphvizLibrary.VERSION shouldBe "1.0.0"
    }
    
    "library should support all required platforms" {
        GraphvizLibrary.SUPPORTED_PLATFORMS shouldContain "JVM"
        GraphvizLibrary.SUPPORTED_PLATFORMS shouldContain "Android"
        GraphvizLibrary.SUPPORTED_PLATFORMS shouldContain "iOS"
        GraphvizLibrary.SUPPORTED_PLATFORMS shouldContain "JavaScript"
    }
    
    "library should register dot layout engine after initialization" {
        GraphvizLibrary.initialize()
        
        val availableEngines = GraphvizLibrary.getAvailableLayoutEngines()
        availableEngines shouldContain "dot"
        
        val dotEngine = LayoutEngineRegistry.get("dot")
        (dotEngine is org.graphviz.kotlin.layout.dot.DotLayoutEngine) shouldBe true
    }
    
    "library should track initialization state" {
        // Clear any previous initialization for this test
        LayoutEngineRegistry.clear()
        
        GraphvizLibrary.isInitialized() shouldBe false
        GraphvizLibrary.initialize() shouldBe true
        GraphvizLibrary.isInitialized() shouldBe true
        
        // Second initialization should still return true
        GraphvizLibrary.initialize() shouldBe true
    }
    
    "property test example - string operations should work" {
        checkAll(Arb.string(1..10)) { str ->
            str.length >= 1
        }
    }
    
    "property test configuration should use minimum 100 iterations" {
        var iterationCount = 0
        checkAll(Arb.string(1..5)) { _ ->
            iterationCount++
            true
        }
        // Verify that at least 100 iterations were run
        iterationCount shouldBe 100
    }
})