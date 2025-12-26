package org.graphviz.kotlin.visual

import org.graphviz.kotlin.model.Point
import kotlin.test.*

/**
 * Tests for Task 14.4: Generate comprehensive test reports.
 * **Feature: kotlin-multiplatform-port, Task 14.4: Generate comprehensive test reports**
 */
class Task14_4_ComprehensiveReportingTest {
    
    @Test
    fun testEnhancedHtmlReportGeneration() {
        // Create sample test results
        val testResults = listOf(
            createSampleTestResult("test1", true, 0.95),
            createSampleTestResult("test2", false, 0.65),
            createSampleTestResult("test3", true, 0.88)
        )
        
        val suiteResult = TestSuiteResult(
            results = testResults,
            errors = emptyList(),
            totalTime = 5000L,
            summary = TestSummary(3, 2, 1, 0.67)
        )
        
        val performanceBenchmark = PerformanceBenchmark(
            kotlinAverageTime = 150.0,
            referenceAverageTime = 120.0,
            kotlinMedianTime = 140.0,
            referenceMedianTime = 115.0,
            speedRatio = 1.25
        )
        
        // Generate enhanced HTML report
        val htmlReport = ComprehensiveReporting.generateEnhancedHtmlReport(
            suiteResult = suiteResult,
            historicalResults = emptyList(),
            performanceBenchmark = performanceBenchmark
        )
        
        // Verify HTML report structure
        assertNotNull(htmlReport)
        assertTrue(htmlReport.content.contains("<!DOCTYPE html>"))
        assertTrue(htmlReport.content.contains("Visual Regression Test Report"))
        assertTrue(htmlReport.content.contains("Executive Summary"))
        assertTrue(htmlReport.content.contains("Detailed Test Results"))
        
        // Verify summary data
        assertEquals(3, htmlReport.summary.totalTests)
        assertEquals(2, htmlReport.summary.passedTests)
        assertEquals(1, htmlReport.summary.failedTests)
        
        // Verify failure analysis
        assertNotNull(htmlReport.failureAnalysis)
        assertEquals(1, htmlReport.failureAnalysis.totalFailures)
        
        // Verify performance analysis
        assertNotNull(htmlReport.performanceAnalysis)
        assertTrue(htmlReport.performanceAnalysis.metrics.averageExecutionTime > 0)
    }
    
    @Test
    fun testFailureAnalysisWithCategorization() {
        // Create failed test results with different failure types
        val failedResults = listOf(
            createFailedTestResult("layout_test", 15.0, 0.9, 0.95), // Layout deviation
            createFailedTestResult("attribute_test", 2.0, 0.95, 0.6), // Attribute mismatch
            createFailedTestResult("structure_test", 1.0, 0.7, 0.9), // Structural difference
            createExecutionErrorResult("error_test") // Execution error
        )
        
        val failureAnalysis = ComprehensiveReporting.analyzeFailures(failedResults)
        
        // Verify failure analysis
        assertEquals(4, failureAnalysis.totalFailures)
        assertTrue(failureAnalysis.categorizedFailures.isNotEmpty())
        
        // Verify categorization
        val categories = failureAnalysis.categorizedFailures.keys
        assertTrue(categories.contains(FailureCategory.LAYOUT_DEVIATION))
        assertTrue(categories.contains(FailureCategory.ATTRIBUTE_MISMATCH))
        assertTrue(categories.contains(FailureCategory.STRUCTURAL_DIFFERENCE))
        assertTrue(categories.contains(FailureCategory.EXECUTION_ERROR))
        
        // Verify critical failures identification
        assertTrue(failureAnalysis.criticalFailures.isNotEmpty())
        
        // Verify recommendations
        assertTrue(failureAnalysis.recommendations.isNotEmpty())
    }
    
    @Test
    fun testDetailedTrendAnalysis() {
        // Create current results
        val currentResults = TestSuiteResult(
            results = listOf(
                createSampleTestResult("test1", true, 0.95),
                createSampleTestResult("test2", true, 0.88),
                createSampleTestResult("test3", false, 0.65)
            ),
            errors = emptyList(),
            totalTime = 3000L,
            summary = TestSummary(3, 2, 1, 0.67)
        )
        
        // Create historical results
        val historicalResults = listOf(
            TestSuiteResult(
                results = listOf(
                    createSampleTestResult("test1", true, 0.92),
                    createSampleTestResult("test2", true, 0.85),
                    createSampleTestResult("test3", true, 0.78)
                ),
                errors = emptyList(),
                totalTime = 3200L,
                summary = TestSummary(3, 3, 0, 1.0)
            ),
            TestSuiteResult(
                results = listOf(
                    createSampleTestResult("test1", true, 0.90),
                    createSampleTestResult("test2", false, 0.82),
                    createSampleTestResult("test3", true, 0.75)
                ),
                errors = emptyList(),
                totalTime = 3100L,
                summary = TestSummary(3, 2, 1, 0.67)
            )
        )
        
        val trendAnalysis = ComprehensiveReporting.generateDetailedTrendAnalysis(
            currentResults, historicalResults
        )
        
        // Verify trend analysis
        assertNotNull(trendAnalysis)
        assertEquals(3, trendAnalysis.testTrends.size)
        assertTrue(trendAnalysis.timeSeriesData.isNotEmpty())
        
        // Verify overall metrics
        assertNotNull(trendAnalysis.overallMetrics)
        assertTrue(trendAnalysis.overallMetrics.averageScore > 0)
        
        // Verify stability metrics
        assertNotNull(trendAnalysis.stabilityMetrics)
        assertTrue(trendAnalysis.stabilityMetrics.overallStability >= 0)
        
        // Verify quality metrics
        assertNotNull(trendAnalysis.qualityMetrics)
        assertTrue(trendAnalysis.qualityMetrics.qualityScore >= 0)
        
        // Verify recommendations
        assertTrue(trendAnalysis.recommendations.isNotEmpty())
    }
    
    @Test
    fun testPerformanceAnalysis() {
        val testResults = listOf(
            createSampleTestResult("fast_test", true, 0.95, 50L),
            createSampleTestResult("medium_test", true, 0.88, 150L),
            createSampleTestResult("slow_test", false, 0.65, 500L)
        )
        
        val benchmark = PerformanceBenchmark(
            kotlinAverageTime = 233.0,
            referenceAverageTime = 180.0,
            kotlinMedianTime = 150.0,
            referenceMedianTime = 120.0,
            speedRatio = 1.29
        )
        
        val performanceAnalysis = ComprehensiveReporting.analyzePerformance(testResults, benchmark)
        
        // Verify performance metrics
        assertNotNull(performanceAnalysis.metrics)
        assertTrue(performanceAnalysis.metrics.averageExecutionTime > 0)
        assertTrue(performanceAnalysis.metrics.medianExecutionTime > 0)
        assertTrue(performanceAnalysis.metrics.p95ExecutionTime > 0)
        assertTrue(performanceAnalysis.metrics.p99ExecutionTime > 0)
        
        // Verify benchmark comparison
        assertNotNull(performanceAnalysis.benchmarkComparison)
        assertEquals(1.29, performanceAnalysis.benchmarkComparison!!.kotlinVsReference.speedRatio, 0.01)
        
        // Verify memory analysis
        assertNotNull(performanceAnalysis.memoryAnalysis)
        assertTrue(performanceAnalysis.memoryAnalysis.averageMemoryUsage > 0)
    }
    
    @Test
    fun testVisualComparisonReport() {
        val testResult = createFailedTestResult("visual_test", 5.0, 0.85, 0.75)
        
        val visualReport = ComprehensiveReporting.generateVisualComparisonReport(
            testResult = testResult,
            includeDetailedAnalysis = true
        )
        
        // Verify visual comparison report
        assertNotNull(visualReport)
        assertEquals("visual_test", visualReport.testName)
        assertFalse(visualReport.passed)
        assertTrue(visualReport.overallScore > 0)
        
        // Verify analysis components
        assertNotNull(visualReport.coordinateAnalysis)
        assertNotNull(visualReport.attributeAnalysis)
        assertNotNull(visualReport.visualDifferences)
        assertTrue(visualReport.recommendations.isNotEmpty())
    }
    
    @Test
    fun testDashboardDataGeneration() {
        val suiteResult = TestSuiteResult(
            results = listOf(
                createSampleTestResult("test1", true, 0.95),
                createSampleTestResult("test2", false, 0.65),
                createSampleTestResult("test3", true, 0.88)
            ),
            errors = emptyList(),
            totalTime = 4000L,
            summary = TestSummary(3, 2, 1, 0.67)
        )
        
        val dashboardData = ComprehensiveReporting.generateDashboardData(
            suiteResult = suiteResult,
            historicalResults = emptyList(),
            performanceBenchmark = null
        )
        
        // Verify dashboard data
        assertNotNull(dashboardData)
        assertEquals(suiteResult.summary, dashboardData.summary)
        
        // Verify chart data
        assertNotNull(dashboardData.chartData)
        assertNotNull(dashboardData.chartData.successRateChart)
        assertNotNull(dashboardData.chartData.performanceChart)
        assertNotNull(dashboardData.chartData.categoryBreakdownChart)
        assertNotNull(dashboardData.chartData.deviationHeatmap)
        
        // Verify metadata
        assertNotNull(dashboardData.metadata)
        assertEquals(3, dashboardData.metadata.totalTests)
        assertEquals(4000L, dashboardData.metadata.executionTime)
    }
    
    @Test
    fun testHtmlReportContainsVisualComparisons() {
        val failedResult = createFailedTestResult("failed_visual_test", 8.0, 0.75, 0.60)
        
        val suiteResult = TestSuiteResult(
            results = listOf(failedResult),
            errors = emptyList(),
            totalTime = 2000L,
            summary = TestSummary(1, 0, 1, 0.0)
        )
        
        val htmlReport = ComprehensiveReporting.generateEnhancedHtmlReport(
            suiteResult = suiteResult,
            historicalResults = emptyList(),
            performanceBenchmark = null
        )
        
        // Verify side-by-side visual comparison is included
        assertTrue(htmlReport.content.contains("visual-comparison"))
        assertTrue(htmlReport.content.contains("Kotlin Output"))
        assertTrue(htmlReport.content.contains("Reference Output"))
        assertTrue(htmlReport.content.contains("svg-container"))
        
        // Verify failure analysis section
        assertTrue(htmlReport.content.contains("Failure Analysis"))
        assertTrue(htmlReport.content.contains("failure-categories"))
    }
    
    @Test
    fun testReportGenerationWithEmptyResults() {
        val emptyResult = TestSuiteResult(
            results = emptyList(),
            errors = emptyList(),
            totalTime = 0L,
            summary = TestSummary(0, 0, 0, 0.0)
        )
        
        val htmlReport = ComprehensiveReporting.generateEnhancedHtmlReport(
            suiteResult = emptyResult,
            historicalResults = emptyList(),
            performanceBenchmark = null
        )
        
        // Should handle empty results gracefully
        assertNotNull(htmlReport)
        assertTrue(htmlReport.content.contains("Visual Regression Test Report"))
        assertEquals(0, htmlReport.summary.totalTests)
        assertEquals(0, htmlReport.failureAnalysis.totalFailures)
    }
    
    // Helper methods for creating test data
    
    private fun createSampleTestResult(
        name: String, 
        passed: Boolean, 
        score: Double, 
        executionTime: Long = 100L
    ): TestResult {
        val testCase = TestCase(
            name = name,
            description = "Sample test case",
            dotContent = "digraph test { A -> B; }",
            category = TestCategory.NODE_SHAPES,
            expectedElements = listOf("A", "B")
        )
        
        val comparison = ComparisonResult(
            structuralSimilarity = StructuralSimilarity(score, 2, emptyList(), emptyList()),
            coordinateDeviations = CoordinateDeviations(emptyList(), 1.0, 0.5, true),
            attributeMatches = AttributeMatches(emptyList(), 10, 9, 0.9),
            overallScore = score,
            passed = passed
        )
        
        return TestResult(
            testCase = testCase,
            passed = passed,
            kotlinOutput = "<svg>kotlin output</svg>",
            referenceOutput = "<svg>reference output</svg>",
            comparison = comparison,
            executionTime = executionTime,
            performanceMetrics = PerformanceMetrics(executionTime, 1000, 10.0)
        )
    }
    
    private fun createFailedTestResult(
        name: String,
        maxDeviation: Double,
        structuralSimilarity: Double,
        attributeMatch: Double
    ): TestResult {
        val testCase = TestCase(
            name = name,
            description = "Failed test case",
            dotContent = "digraph test { A -> B; }",
            category = TestCategory.NODE_SHAPES,
            expectedElements = listOf("A", "B")
        )
        
        val deviations = listOf(
            CoordinateDeviation(
                elementId = "element1", 
                kotlinCoordinate = Point(0.0, 0.0),
                referenceCoordinate = Point(maxDeviation, 0.0),
                deviation = maxDeviation
            )
        )
        
        val comparison = ComparisonResult(
            structuralSimilarity = StructuralSimilarity(structuralSimilarity, 2, emptyList(), emptyList()),
            coordinateDeviations = CoordinateDeviations(deviations, maxDeviation, maxDeviation / 2, false),
            attributeMatches = AttributeMatches(emptyList(), 10, (attributeMatch * 10).toInt(), attributeMatch),
            overallScore = (structuralSimilarity + attributeMatch) / 2,
            passed = false
        )
        
        return TestResult(
            testCase = testCase,
            passed = false,
            kotlinOutput = "<svg>kotlin output</svg>",
            referenceOutput = "<svg>reference output</svg>",
            comparison = comparison,
            executionTime = 200L,
            performanceMetrics = PerformanceMetrics(200L, 1200, 6.0)
        )
    }
    
    private fun createExecutionErrorResult(name: String): TestResult {
        val testCase = TestCase(
            name = name,
            description = "Error test case",
            dotContent = "invalid dot content",
            category = TestCategory.NODE_SHAPES,
            expectedElements = emptyList()
        )
        
        return TestResult(
            testCase = testCase,
            passed = false,
            kotlinOutput = "",
            referenceOutput = null,
            comparison = null,
            executionTime = 50L,
            performanceMetrics = null,
            error = "Execution failed: Invalid DOT syntax"
        )
    }
}