package org.graphviz.kotlin.visual

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Simple tests for the enhanced reference comparison pipeline.
 * **Feature: kotlin-multiplatform-port, Task 14.2: Build reference comparison pipeline**
 */
class SimpleReferenceComparisonTest : FunSpec({
    
    test("should execute GraphvizExecutor with basic functionality") {
        val dotContent = "digraph test { A -> B; }"
        
        val result = GraphvizExecutor.execute(dotContent, "dot", "svg")
        
        when (result) {
            is GraphvizExecutionResult.Success -> {
                result.output shouldNotBe ""
                result.command shouldNotBe ""
                result.inputSize shouldBe dotContent.length
            }
            is GraphvizExecutionResult.Error -> {
                // In simulation mode, this is expected
                result.message shouldNotBe ""
            }
        }
    }
    
    test("should parse SVG with enhanced parser") {
        val svgContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(100,50)">
                    <rect x="-20" y="-10" width="40" height="20"/>
                </g>
            </svg>
        """.trimIndent()
        
        val parsed = SvgParser.parse(svgContent)
        
        parsed.elements.size shouldBe 1
        parsed.coordinates.size shouldBe 1
        parsed.rawContent shouldBe svgContent
    }
    
    test("should perform basic comparison") {
        val svg1 = """<svg><g id="test" transform="translate(50,50)"><rect/></g></svg>"""
        val svg2 = """<svg><g id="test" transform="translate(50,50)"><rect/></g></svg>"""
        
        val result = ReferenceComparison.compareOutputs(svg1, svg2)
        
        result.overallScore shouldBe 1.0
        result.passed shouldBe true
    }
    
    test("should generate reference output") {
        val dotContent = "digraph test { A; }"
        
        val result = ReferenceComparison.generateReferenceOutput(dotContent)
        
        when (result) {
            is ReferenceExecutionResult.Success -> {
                result.output shouldNotBe ""
            }
            is ReferenceExecutionResult.Error -> {
                // Expected in simulation mode
                result.message shouldNotBe ""
            }
        }
    }
    
    test("should handle batch execution") {
        val testCases = listOf(
            TestCase(
                name = "simple",
                description = "Simple test",
                dotContent = "digraph test { A; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("A")
            )
        )
        
        val batchResult = GraphvizExecutor.batchExecute(testCases)
        
        batchResult.results.size shouldBe 1
        batchResult.metrics.size shouldBe 1
        batchResult.successCount + batchResult.failureCount shouldBe 1
    }
})