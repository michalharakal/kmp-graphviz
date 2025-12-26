package org.graphviz.kotlin.visual

import kotlin.test.*

/**
 * Test suite for Task 15.1: Execute comprehensive visual consistency validation.
 * Tests the comprehensive validation system that runs complete test suite against
 * original Graphviz reference outputs and validates consistency requirements.
 */
class Task15_1_ComprehensiveValidationTest {
    
    @Test
    fun testComprehensiveValidationExecution() {
        // Mock Kotlin renderer for testing
        val mockRenderer: (String) -> String = { dotContent ->
            // Generate mock SVG output based on DOT content
            generateMockSvgOutput(dotContent)
        }
        
        // Execute comprehensive validation with strict config
        val result = ComprehensiveVisualConsistencyValidation.executeComprehensiveValidation(
            kotlinRenderer = mockRenderer,
            config = ValidationConfig.STRICT
        )
        
        // Verify validation was executed
        assertNotNull(result)
        assertNotNull(result.suiteResult)
        assertNotNull(result.consistencyAnalysis)
        assertNotNull(result.structuralValidation)
        assertNotNull(result.coordinateValidation)
        assertNotNull(result.attributeValidation)
        assertNotNull(result.textValidation)
        assertNotNull(result.report)
        
        // Verify test suite was comprehensive
        assertTrue(result.suiteResult.results.isNotEmpty(), "Should have executed test cases")
        assertTrue(result.totalTime > 0, "Should have recorded execution time")
        
        println("Comprehensive validation executed successfully")
        println("Total tests: ${result.suiteResult.summary.totalTests}")
        println("Success rate: ${String.format("%.2f", result.suiteResult.summary.successRate * 100)}%")
        println("Consistency score: ${String.format("%.3f", result.consistencyAnalysis.consistencyScore)}")
    }
    
    @Test
    fun testValidationConfigurationOptions() {
        val mockRenderer: (String) -> String = { generateMockSvgOutput(it) }
        
        // Test with different validation configurations
        val strictResult = ComprehensiveVisualConsistencyValidation.executeComprehensiveValidation(
            kotlinRenderer = mockRenderer,
            config = ValidationConfig.STRICT
        )
        
        val defaultResult = ComprehensiveVisualConsistencyValidation.executeComprehensiveValidation(
            kotlinRenderer = mockRenderer,
            config = ValidationConfig.DEFAULT
        )
        
        val lenientResult = ComprehensiveVisualConsistencyValidation.executeComprehensiveValidation(
            kotlinRenderer = mockRenderer,
            config = ValidationConfig.LENIENT
        )
        
        // Verify different configurations produce different requirements
        assertTrue(strictResult.structuralValidation.requiredSimilarity >= defaultResult.structuralValidation.requiredSimilarity)
        assertTrue(defaultResult.structuralValidation.requiredSimilarity >= lenientResult.structuralValidation.requiredSimilarity)
        
        assertTrue(strictResult.coordinateValidation.requiredMaxDeviation <= defaultResult.coordinateValidation.requiredMaxDeviation)
        assertTrue(defaultResult.coordinateValidation.requiredMaxDeviation <= lenientResult.coordinateValidation.requiredMaxDeviation)
        
        println("Validation configuration options tested successfully")
        println("Strict similarity requirement: ${strictResult.structuralValidation.requiredSimilarity}")
        println("Default similarity requirement: ${defaultResult.structuralValidation.requiredSimilarity}")
        println("Lenient similarity requirement: ${lenientResult.structuralValidation.requiredSimilarity}")
    }
    
    // Helper methods for creating mock test data
    
    private fun generateMockSvgOutput(dotContent: String): String {
        // Generate a simple mock SVG based on DOT content
        val hasNodes = dotContent.contains("->") || dotContent.contains("--")
        val hasLabels = dotContent.contains("label=")
        val hasColors = dotContent.contains("color=") || dotContent.contains("fillcolor=")
        
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="no"?>""")
            appendLine("""<svg width="200pt" height="150pt" viewBox="0.00 0.00 200.00 150.00" xmlns="http://www.w3.org/2000/svg">""")
            appendLine("""<g id="graph0" class="graph" transform="scale(1 1) rotate(0) translate(4 146)">""")
            
            if (hasNodes) {
                appendLine("""<g id="node1" class="node">""")
                appendLine("""<ellipse fill="none" stroke="black" cx="50" cy="75" rx="27" ry="18"/>""")
                if (hasLabels) {
                    appendLine("""<text text-anchor="middle" x="50" y="79" font-family="Times,serif" font-size="14.00">A</text>""")
                }
                appendLine("""</g>""")
                
                appendLine("""<g id="node2" class="node">""")
                appendLine("""<ellipse fill="none" stroke="black" cx="150" cy="75" rx="27" ry="18"/>""")
                if (hasLabels) {
                    appendLine("""<text text-anchor="middle" x="150" y="79" font-family="Times,serif" font-size="14.00">B</text>""")
                }
                appendLine("""</g>""")
                
                appendLine("""<g id="edge1" class="edge">""")
                appendLine("""<path fill="none" stroke="black" d="M77,-75C85,-75 94,-75 103,-75"/>""")
                appendLine("""<polygon fill="black" stroke="black" points="103,-78.5 113,-75 103,-71.5 103,-78.5"/>""")
                appendLine("""</g>""")
            }
            
            appendLine("""</g>""")
            appendLine("""</svg>""")
        }
    }
}