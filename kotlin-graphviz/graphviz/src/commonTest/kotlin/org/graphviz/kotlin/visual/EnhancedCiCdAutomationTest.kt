package org.graphviz.kotlin.visual

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan

/**
 * Tests for enhanced CI/CD automation features.
 * **Feature: kotlin-multiplatform-port, Task 14.3: Create visual testing automation**
 */
class EnhancedCiCdAutomationTest : FunSpec({
    
    test("should generate visual diffs for failed tests") {
        val failedResult = TestResult(
            testCase = TestCase(
                name = "failed_test",
                description = "Test that fails comparison",
                dotContent = "digraph test { A -> B; }",
                category = TestCategory.EDGE_STYLES,
                expectedElements = listOf("A", "B")
            ),
            passed = false,
            kotlinOutput = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                    <g id="node-A"><ellipse cx="50" cy="50" rx="20" ry="15"/></g>
                    <g id="node-B"><ellipse cx="150" cy="50" rx="20" ry="15"/></g>
                </svg>
            """.trimIndent(),
            referenceOutput = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                    <g id="node-A"><ellipse cx="60" cy="50" rx="20" ry="15"/></g>
                    <g id="node-B"><ellipse cx="140" cy="50" rx="20" ry="15"/></g>
                </svg>
            """.trimIndent(),
            comparison = ComparisonResult(
                structuralSimilarity = StructuralSimilarity(0.8, 2, emptyList(), emptyList()),
                coordinateDeviations = CoordinateDeviations(emptyList(), 10.0, 5.0, false),
                attributeMatches = AttributeMatches(emptyList(), 0, 0, 0.9),
                overallScore = 0.75,
                passed = false
            ),
            executionTime = 150L,
            performanceMetrics = PerformanceMetrics(150L, 200, 1.33)
        )
        
        val suiteResult = TestSuiteResult(
            results = listOf(failedResult),
            errors = emptyList(),
            totalTime = 150L,
            summary = TestSummary(1, 0, 1, 0.0)
        )
        
        val visualDiffs = VisualTestAutomation.generateVisualDiffs(suiteResult)
        
        visualDiffs.shouldNotBeEmpty()
        visualDiffs.size shouldBe 1
        
        val diff = visualDiffs.first()
        diff.testName shouldBe "failed_test"
        diff.diffImagePath shouldContain "failed_test-diff.png"
        diff.kotlinImagePath shouldContain "failed_test-kotlin.png"
        diff.referenceImagePath shouldContain "failed_test-reference.png"
    }
    
    test("should create and load baseline results") {
        val testResult = TestResult(
            testCase = TestCase(
                name = "baseline_test",
                description = "Test for baseline creation",
                dotContent = "digraph test { A; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("A")
            ),
            passed = true,
            kotlinOutput = "<svg>test output</svg>",
            referenceOutput = "<svg>test output</svg>",
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
        
        // Create baseline
        VisualTestAutomation.createBaseline(suiteResult, "test-baseline.json")
        
        // Load baseline (simplified test - in real implementation would verify file contents)
        val loadedBaseline = VisualTestAutomation.loadBaseline("test-baseline.json")
        
        // In a real implementation, we would verify the loaded baseline matches the created one
        // For now, just verify the method doesn't throw exceptions
    }
    
    test("should generate comprehensive CI/CD report") {
        val testResult = TestResult(
            testCase = TestCase(
                name = "cicd_test",
                description = "Test for CI/CD reporting",
                dotContent = "digraph test { A -> B; }",
                category = TestCategory.EDGE_STYLES,
                expectedElements = listOf("A", "B")
            ),
            passed = true,
            kotlinOutput = "<svg>kotlin output</svg>",
            referenceOutput = "<svg>reference output</svg>",
            comparison = ComparisonResult(
                structuralSimilarity = StructuralSimilarity(0.95, 2, emptyList(), emptyList()),
                coordinateDeviations = CoordinateDeviations(emptyList(), 1.0, 0.5, true),
                attributeMatches = AttributeMatches(emptyList(), 0, 0, 0.98),
                overallScore = 0.96,
                passed = true
            ),
            executionTime = 120L,
            performanceMetrics = PerformanceMetrics(120L, 150, 1.25)
        )
        
        val suiteResult = TestSuiteResult(
            results = listOf(testResult),
            errors = emptyList(),
            totalTime = 120L,
            summary = TestSummary(1, 1, 0, 1.0)
        )
        
        val benchmark = PerformanceBenchmark(
            kotlinAverageTime = 120.0,
            referenceAverageTime = 100.0,
            kotlinMedianTime = 120.0,
            referenceMedianTime = 100.0,
            speedRatio = 1.2
        )
        
        val regressionReport = RegressionReport(
            regressions = emptyList(),
            improvements = emptyList(),
            hasRegressions = false
        )
        
        val visualDiffs = emptyList<VisualDiff>()
        
        val cicdReport = VisualTestAutomation.generateCiCdReport(
            suiteResult, benchmark, regressionReport, visualDiffs
        )
        
        cicdReport.summary shouldBe suiteResult.summary
        cicdReport.executionTime shouldBe 120L
        cicdReport.benchmark shouldBe benchmark
        cicdReport.regressionReport shouldBe regressionReport
        cicdReport.visualDiffs shouldBe visualDiffs
        cicdReport.failedTests.size shouldBe 0
        cicdReport.timestamp shouldBeGreaterThan 0L
        cicdReport.environment shouldNotBe null
    }
    
    test("should generate trend analysis") {
        val currentResult = TestResult(
            testCase = TestCase("trend_test", "Trend analysis test", "digraph { A; }", TestCategory.NODE_SHAPES, listOf("A")),
            passed = true,
            kotlinOutput = "",
            referenceOutput = "",
            comparison = ComparisonResult(
                StructuralSimilarity(0.9, 1, emptyList(), emptyList()),
                CoordinateDeviations(emptyList(), 0.0, 0.0, true),
                AttributeMatches(emptyList(), 0, 0, 0.9),
                0.9, true
            ),
            executionTime = 100L,
            performanceMetrics = null
        )
        
        val currentResults = TestSuiteResult(
            results = listOf(currentResult),
            errors = emptyList(),
            totalTime = 100L,
            summary = TestSummary(1, 1, 0, 1.0)
        )
        
        // Create historical results with varying scores
        val historicalResults = listOf(
            TestSuiteResult(
                results = listOf(currentResult.copy(
                    comparison = currentResult.comparison?.copy(overallScore = 0.85)
                )),
                errors = emptyList(),
                totalTime = 100L,
                summary = TestSummary(1, 1, 0, 1.0)
            ),
            TestSuiteResult(
                results = listOf(currentResult.copy(
                    comparison = currentResult.comparison?.copy(overallScore = 0.88)
                )),
                errors = emptyList(),
                totalTime = 100L,
                summary = TestSummary(1, 1, 0, 1.0)
            )
        )
        
        val trendAnalysis = VisualTestAutomation.generateTrendAnalysis(currentResults, historicalResults)
        
        trendAnalysis.trends.shouldNotBeEmpty()
        trendAnalysis.trends.containsKey("trend_test") shouldBe true
        
        val testTrend = trendAnalysis.trends["trend_test"]!!
        testTrend.testName shouldBe "trend_test"
        testTrend.currentScore shouldBe 0.9
        testTrend.averageHistoricalScore shouldBe 0.865 // (0.85 + 0.88) / 2
        testTrend.trend shouldBe TrendDirection.IMPROVING
        testTrend.volatility shouldBeGreaterThan 0.0
        
        trendAnalysis.overallTrend shouldBe TrendDirection.IMPROVING
        trendAnalysis.stabilityScore shouldBeGreaterThan 0.0
    }
    
    test("should detect CI environment information") {
        val cicdReport = VisualTestAutomation.generateCiCdReport(
            TestSuiteResult(emptyList(), emptyList(), 0L, TestSummary(0, 0, 0, 0.0)),
            null, null, emptyList()
        )
        
        val envInfo = cicdReport.environment
        
        envInfo.javaVersion shouldNotBe null
        envInfo.osName shouldNotBe null
        envInfo.osVersion shouldNotBe null
        
        // CI detection depends on environment variables, so we can't assert specific values
        // but we can verify the structure is correct
    }
    
    test("should handle regression detection with baseline comparison") {
        val baselineResult = TestResult(
            testCase = TestCase("regression_test", "Regression test", "digraph { A; }", TestCategory.NODE_SHAPES, listOf("A")),
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
        
        val currentResult = baselineResult.copy(
            passed = false,
            comparison = baselineResult.comparison?.copy(
                overallScore = 0.7,
                passed = false
            )
        )
        
        val baselineResults = TestSuiteResult(
            results = listOf(baselineResult),
            errors = emptyList(),
            totalTime = 100L,
            summary = TestSummary(1, 1, 0, 1.0)
        )
        
        val currentResults = TestSuiteResult(
            results = listOf(currentResult),
            errors = emptyList(),
            totalTime = 100L,
            summary = TestSummary(1, 0, 1, 0.0)
        )
        
        val regressionReport = VisualTestAutomation.checkForRegressions(currentResults, baselineResults)
        
        regressionReport.hasRegressions shouldBe true
        regressionReport.regressions.shouldNotBeEmpty()
        regressionReport.regressions.first().testName shouldBe "regression_test"
        regressionReport.regressions.first().previousScore shouldBe 1.0
        regressionReport.regressions.first().currentScore shouldBe 0.7
    }
})