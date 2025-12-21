package org.graphviz.kotlin.visual

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan

/**
 * Tests for the reference comparison pipeline.
 * **Feature: kotlin-multiplatform-port, Task 14.2: Build reference comparison pipeline**
 */
class ReferenceComparisonTest : FunSpec({
    
    test("should generate reference output") {
        val dotContent = """
            digraph test {
                A -> B;
            }
        """.trimIndent()
        
        val result = ReferenceComparison.generateReferenceOutput(dotContent)
        
        result shouldBe ReferenceExecutionResult.Success::class
        val successResult = result as ReferenceExecutionResult.Success
        successResult.output.shouldNotBe("")
        successResult.output shouldContain "<?xml"
        successResult.output shouldContain "<svg"
    }
    
    test("should compare identical outputs") {
        val svgContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(50,50)">
                    <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                </g>
            </svg>
        """.trimIndent()
        
        val result = ReferenceComparison.compareOutputs(svgContent, svgContent)
        
        result.structuralSimilarity.similarity shouldBe 1.0
        result.coordinateDeviations.maxDeviation shouldBe 0.0
        result.attributeMatches.matchPercentage shouldBe 1.0
        result.overallScore shouldBe 1.0
        result.passed shouldBe true
    }
    
    test("should detect structural differences") {
        val kotlinSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(50,50)">
                    <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                </g>
            </svg>
        """.trimIndent()
        
        val referenceSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(50,50)">
                    <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                </g>
                <g id="node-B" transform="translate(150,50)">
                    <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                </g>
            </svg>
        """.trimIndent()
        
        val result = ReferenceComparison.compareOutputs(kotlinSvg, referenceSvg)
        
        result.structuralSimilarity.similarity shouldBeLessThan 1.0
        result.structuralSimilarity.missingElements.shouldNotBeEmpty()
        result.passed shouldBe false
    }
    
    test("should detect coordinate deviations") {
        val kotlinSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(50,50)">
                    <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                </g>
            </svg>
        """.trimIndent()
        
        val referenceSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(55,55)">
                    <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                </g>
            </svg>
        """.trimIndent()
        
        val result = ReferenceComparison.compareOutputs(kotlinSvg, referenceSvg)
        
        result.coordinateDeviations.maxDeviation shouldBeGreaterThan 0.0
        result.coordinateDeviations.deviations.shouldNotBeEmpty()
        result.coordinateDeviations.deviations.first().deviation shouldBeGreaterThan 0.0
    }
    
    test("should detect attribute differences") {
        val kotlinSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(50,50)">
                    <ellipse cx="0" cy="0" rx="30" ry="20" fill="red" stroke="black"/>
                </g>
            </svg>
        """.trimIndent()
        
        val referenceSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(50,50)">
                    <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                </g>
            </svg>
        """.trimIndent()
        
        val result = ReferenceComparison.compareOutputs(kotlinSvg, referenceSvg)
        
        result.attributeMatches.matchPercentage shouldBeLessThan 1.0
        val fillMatch = result.attributeMatches.matches.find { it.attributeName == "fill" }
        fillMatch shouldNotBe null
        fillMatch!!.matches shouldBe false
        fillMatch.kotlinValue shouldBe "red"
        fillMatch.referenceValue shouldBe "white"
    }
    
    test("should generate comparison report") {
        val testCase = TestCase(
            name = "test_comparison",
            description = "Test comparison report generation",
            dotContent = "digraph test { A -> B; }",
            category = TestCategory.NODE_SHAPES,
            expectedElements = listOf("A", "B")
        )
        
        val kotlinOutput = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(50,50)">
                    <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                </g>
            </svg>
        """.trimIndent()
        
        val referenceOutput = kotlinOutput // Same for this test
        
        val comparisonResult = ReferenceComparison.compareOutputs(kotlinOutput, referenceOutput)
        val report = ReferenceComparison.generateComparisonReport(testCase, kotlinOutput, referenceOutput, comparisonResult)
        
        report.testCase shouldBe testCase
        report.kotlinOutputSize shouldBe kotlinOutput.length
        report.referenceOutputSize shouldBe referenceOutput.length
        report.comparisonResult shouldBe comparisonResult
        report.timestamp shouldNotBe null
        report.recommendations.shouldNotBeEmpty()
    }
    
    test("should handle batch comparison") {
        val testCases = listOf(
            TestCase(
                name = "test1",
                description = "First test",
                dotContent = "digraph test1 { A; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("A")
            ),
            TestCase(
                name = "test2",
                description = "Second test",
                dotContent = "digraph test2 { B; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("B")
            )
        )
        
        val kotlinOutputs = mapOf(
            "test1" to """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                    <g id="node-A" transform="translate(50,50)">
                        <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                    </g>
                </svg>
            """.trimIndent(),
            "test2" to """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                    <g id="node-B" transform="translate(50,50)">
                        <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                    </g>
                </svg>
            """.trimIndent()
        )
        
        val batchResult = ReferenceComparison.batchCompare(testCases, kotlinOutputs)
        
        batchResult.reports.size shouldBe 2
        batchResult.errors.size shouldBe 0
        batchResult.summary.totalTests shouldBe 2
        batchResult.summary.successRate shouldBeGreaterThan 0.0
    }
    
    test("should handle missing kotlin output in batch") {
        val testCases = listOf(
            TestCase(
                name = "test1",
                description = "Test with missing output",
                dotContent = "digraph test1 { A; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("A")
            )
        )
        
        val kotlinOutputs = emptyMap<String, String>() // Missing output
        
        val batchResult = ReferenceComparison.batchCompare(testCases, kotlinOutputs)
        
        batchResult.reports.size shouldBe 0
        batchResult.errors.size shouldBe 1
        batchResult.errors.first() shouldContain "Missing Kotlin output for test: test1"
    }
    
    test("should use different tolerance levels") {
        val kotlinSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(50.5,50.5)">
                    <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                </g>
            </svg>
        """.trimIndent()
        
        val referenceSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(50,50)">
                    <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                </g>
            </svg>
        """.trimIndent()
        
        // With strict tolerances, should fail
        val strictResult = ReferenceComparison.compareOutputs(kotlinSvg, referenceSvg, ComparisonTolerances.STRICT)
        strictResult.coordinateDeviations.withinTolerance shouldBe false
        
        // With lenient tolerances, should pass
        val lenientResult = ReferenceComparison.compareOutputs(kotlinSvg, referenceSvg, ComparisonTolerances.LENIENT)
        lenientResult.coordinateDeviations.withinTolerance shouldBe true
    }
})