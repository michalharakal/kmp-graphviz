package org.graphviz.kotlin.visual

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan

/**
 * Tests for the visual testing automation system.
 * **Feature: kotlin-multiplatform-port, Task 14.3: Create visual testing automation**
 */
class VisualTestAutomationTest : FunSpec({
    
    test("should run single visual regression test") {
        val testCase = TestCase(
            name = "test_simple_node",
            description = "Simple node test",
            dotContent = "digraph test { A; }",
            category = TestCategory.NODE_SHAPES,
            expectedElements = listOf("A")
        )
        
        val mockRenderer: (String) -> String = { dotContent ->
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                    <g id="node-A" transform="translate(100,50)">
                        <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                    </g>
                </svg>
            """.trimIndent()
        }
        
        val result = VisualTestAutomation.runSingleTest(testCase, mockRenderer, TestSuiteConfig.DEFAULT)
        
        result.testCase shouldBe testCase
        result.kotlinOutput.shouldNotBe("")
        result.referenceOutput shouldNotBe null
        result.executionTime shouldBeGreaterThan 0L
        result.performanceMetrics shouldNotBe null
    }
    
    test("should run complete test suite") {
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
        
        val mockRenderer: (String) -> String = { dotContent ->
            val nodeId = if (dotContent.contains("A")) "A" else "B"
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                    <g id="node-$nodeId" transform="translate(100,50)">
                        <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                    </g>
                </svg>
            """.trimIndent()
        }
        
        val suiteResult = VisualTestAutomation.runTestSuite(testCases, mockRenderer)
        
        suiteResult.results.size shouldBe 2
        suiteResult.errors.size shouldBe 0
        suiteResult.totalTime shouldBeGreaterThan 0L
        suiteResult.summary.totalTests shouldBe 2
        suiteResult.summary.successRate shouldBeGreaterThan 0.0
    }
    
    test("should handle test errors gracefully") {
        val testCase = TestCase(
            name = "failing_test",
            description = "Test that throws exception",
            dotContent = "digraph test { A; }",
            category = TestCategory.NODE_SHAPES,
            expectedElements = listOf("A")
        )
        
        val failingRenderer: (String) -> String = { _ ->
            throw RuntimeException("Renderer failed")
        }
        
        val suiteResult = VisualTestAutomation.runTestSuite(listOf(testCase), failingRenderer)
        
        suiteResult.results.size shouldBe 0
        suiteResult.errors.size shouldBe 1
        suiteResult.errors.first().testCase shouldBe testCase
        suiteResult.errors.first().message shouldContain "Renderer failed"
    }
    
    test("should generate HTML report") {
        val testResult = TestResult(
            testCase = TestCase(
                name = "test_html_report",
                description = "Test HTML report generation",
                dotContent = "digraph test { A; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("A")
            ),
            passed = true,
            kotlinOutput = "<svg>test</svg>",
            referenceOutput = "<svg>test</svg>",
            comparison = ComparisonResult(
                structuralSimilarity = StructuralSimilarity(1.0, 1, emptyList(), emptyList()),
                coordinateDeviations = CoordinateDeviations(emptyList(), 0.0, 0.0, true),
                attributeMatches = AttributeMatches(emptyList(), 0, 0, 1.0),
                overallScore = 1.0,
                passed = true
            ),
            executionTime = 100L,
            performanceMetrics = PerformanceMetrics(100L, 100, 1.0)
        )
        
        val suiteResult = TestSuiteResult(
            results = listOf(testResult),
            errors = emptyList(),
            totalTime = 100L,
            summary = TestSummary(1, 1, 0, 1.0)
        )
        
        val htmlReport = VisualTestAutomation.generateHtmlReport(suiteResult)
        
        htmlReport shouldContain "<!DOCTYPE html>"
        htmlReport shouldContain "Visual Regression Test Report"
        htmlReport shouldContain "Total Tests: 1"
        htmlReport shouldContain "Passed: 1"
        htmlReport shouldContain "test_html_report"
        htmlReport shouldContain "PASSED"
    }
    
    test("should generate JSON report") {
        val testResult = TestResult(
            testCase = TestCase(
                name = "test_json_report",
                description = "Test JSON report generation",
                dotContent = "digraph test { A; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("A")
            ),
            passed = true,
            kotlinOutput = "<svg>test</svg>",
            referenceOutput = "<svg>test</svg>",
            comparison = null,
            executionTime = 100L,
            performanceMetrics = null
        )
        
        val suiteResult = TestSuiteResult(
            results = listOf(testResult),
            errors = emptyList(),
            totalTime = 100L,
            summary = TestSummary(1, 1, 0, 1.0)
        )
        
        val jsonReport = VisualTestAutomation.generateJsonReport(suiteResult)
        
        jsonReport shouldContain "\"totalTests\": 1"
        jsonReport shouldContain "\"passedTests\": 1"
        jsonReport shouldContain "\"failedTests\": 0"
        jsonReport shouldContain "\"successRate\": 1.0"
        jsonReport shouldContain "\"testName\": \"test_json_report\""
        jsonReport shouldContain "\"passed\": true"
    }
    
    test("should benchmark performance") {
        val testCases = listOf(
            TestCase(
                name = "perf_test",
                description = "Performance test",
                dotContent = "digraph test { A -> B; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("A", "B")
            )
        )
        
        val mockRenderer: (String) -> String = { _ ->
            // Simulate some processing time
            Thread.sleep(10)
            "<svg>mock output</svg>"
        }
        
        val benchmark = VisualTestAutomation.benchmarkPerformance(testCases, mockRenderer)
        
        benchmark.kotlinAverageTime shouldBeGreaterThan 0.0
        benchmark.referenceAverageTime shouldBeGreaterThan 0.0
        benchmark.kotlinMedianTime shouldBeGreaterThan 0.0
        benchmark.referenceMedianTime shouldBeGreaterThan 0.0
        benchmark.speedRatio shouldBeGreaterThan 0.0
    }
    
    test("should detect regressions") {
        val testResult1 = TestResult(
            testCase = TestCase("test1", "Test 1", "digraph { A; }", TestCategory.NODE_SHAPES, listOf("A")),
            passed = true,
            kotlinOutput = "",
            referenceOutput = "",
            comparison = ComparisonResult(
                StructuralSimilarity(1.0, 1, emptyList(), emptyList()),
                CoordinateDeviations(emptyList(), 0.0, 0.0, true),
                AttributeMatches(emptyList(), 0, 0, 1.0),
                1.0, true
            ),
            executionTime = 100L,
            performanceMetrics = null
        )
        
        val testResult2 = TestResult(
            testCase = TestCase("test1", "Test 1", "digraph { A; }", TestCategory.NODE_SHAPES, listOf("A")),
            passed = false,
            kotlinOutput = "",
            referenceOutput = "",
            comparison = ComparisonResult(
                StructuralSimilarity(0.5, 1, listOf("missing"), emptyList()),
                CoordinateDeviations(emptyList(), 0.0, 0.0, true),
                AttributeMatches(emptyList(), 0, 0, 0.5),
                0.5, false
            ),
            executionTime = 100L,
            performanceMetrics = null
        )
        
        val previousResults = TestSuiteResult(
            results = listOf(testResult1),
            errors = emptyList(),
            totalTime = 100L,
            summary = TestSummary(1, 1, 0, 1.0)
        )
        
        val currentResults = TestSuiteResult(
            results = listOf(testResult2),
            errors = emptyList(),
            totalTime = 100L,
            summary = TestSummary(1, 0, 1, 0.0)
        )
        
        val regressionReport = VisualTestAutomation.checkForRegressions(currentResults, previousResults)
        
        regressionReport.hasRegressions shouldBe true
        regressionReport.regressions.shouldNotBeEmpty()
        regressionReport.regressions.first().testName shouldBe "test1"
        regressionReport.regressions.first().previousScore shouldBe 1.0
        regressionReport.regressions.first().currentScore shouldBe 0.5
    }
    
    test("should detect improvements") {
        val testResult1 = TestResult(
            testCase = TestCase("test1", "Test 1", "digraph { A; }", TestCategory.NODE_SHAPES, listOf("A")),
            passed = false,
            kotlinOutput = "",
            referenceOutput = "",
            comparison = ComparisonResult(
                StructuralSimilarity(0.5, 1, listOf("missing"), emptyList()),
                CoordinateDeviations(emptyList(), 0.0, 0.0, true),
                AttributeMatches(emptyList(), 0, 0, 0.5),
                0.5, false
            ),
            executionTime = 100L,
            performanceMetrics = null
        )
        
        val testResult2 = TestResult(
            testCase = TestCase("test1", "Test 1", "digraph { A; }", TestCategory.NODE_SHAPES, listOf("A")),
            passed = true,
            kotlinOutput = "",
            referenceOutput = "",
            comparison = ComparisonResult(
                StructuralSimilarity(1.0, 1, emptyList(), emptyList()),
                CoordinateDeviations(emptyList(), 0.0, 0.0, true),
                AttributeMatches(emptyList(), 0, 0, 1.0),
                1.0, true
            ),
            executionTime = 100L,
            performanceMetrics = null
        )
        
        val previousResults = TestSuiteResult(
            results = listOf(testResult1),
            errors = emptyList(),
            totalTime = 100L,
            summary = TestSummary(1, 0, 1, 0.0)
        )
        
        val currentResults = TestSuiteResult(
            results = listOf(testResult2),
            errors = emptyList(),
            totalTime = 100L,
            summary = TestSummary(1, 1, 0, 1.0)
        )
        
        val regressionReport = VisualTestAutomation.checkForRegressions(currentResults, previousResults)
        
        regressionReport.hasRegressions shouldBe false
        regressionReport.improvements.shouldNotBeEmpty()
        regressionReport.improvements.first().testName shouldBe "test1"
        regressionReport.improvements.first().previousScore shouldBe 0.5
        regressionReport.improvements.first().currentScore shouldBe 1.0
    }
})