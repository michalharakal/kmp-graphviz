package org.graphviz.kotlin.visual

/**
 * Automates visual regression testing with CI/CD integration.
 * Provides test result reporting, performance benchmarking, and regression prevention.
 */
object VisualTestAutomation {
    
    /**
     * Runs the complete visual regression test suite.
     */
    fun runTestSuite(
        testCases: List<TestCase>,
        kotlinRenderer: (String) -> String,
        config: TestSuiteConfig = TestSuiteConfig.DEFAULT
    ): TestSuiteResult {
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<TestResult>()
        val errors = mutableListOf<TestError>()
        
        for (testCase in testCases) {
            try {
                val testResult = runSingleTest(testCase, kotlinRenderer, config)
                results.add(testResult)
            } catch (e: Exception) {
                errors.add(TestError(
                    testCase = testCase,
                    message = e.message ?: "Unknown error",
                    stackTrace = e.stackTraceToString()
                ))
            }
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        return TestSuiteResult(
            results = results,
            errors = errors,
            totalTime = totalTime,
            summary = generateSummary(results, errors)
        )
    }
    
    /**
     * Runs a single visual regression test.
     */
    fun runSingleTest(
        testCase: TestCase,
        kotlinRenderer: (String) -> String,
        config: TestSuiteConfig
    ): TestResult {
        val startTime = System.currentTimeMillis()
        
        // Generate Kotlin output
        val kotlinOutput = kotlinRenderer(testCase.dotContent)
        
        // Generate reference output
        val referenceResult = ReferenceComparison.generateReferenceOutput(testCase.dotContent)
        
        val endTime = System.currentTimeMillis()
        val executionTime = endTime - startTime
        
        return when (referenceResult) {
            is ReferenceExecutionResult.Success -> {
                // Compare outputs
                val comparison = ReferenceComparison.compareOutputs(
                    kotlinOutput,
                    referenceResult.output,
                    config.tolerances
                )
                
                TestResult(
                    testCase = testCase,
                    passed = comparison.passed,
                    kotlinOutput = kotlinOutput,
                    referenceOutput = referenceResult.output,
                    comparison = comparison,
                    executionTime = executionTime,
                    performanceMetrics = calculatePerformanceMetrics(executionTime, kotlinOutput.length)
                )
            }
            is ReferenceExecutionResult.Error -> {
                TestResult(
                    testCase = testCase,
                    passed = false,
                    kotlinOutput = kotlinOutput,
                    referenceOutput = null,
                    comparison = null,
                    executionTime = executionTime,
                    performanceMetrics = null,
                    error = referenceResult.message
                )
            }
        }
    }
    
    /**
     * Generates HTML report for test results.
     */
    fun generateHtmlReport(suiteResult: TestSuiteResult): String {
        val html = StringBuilder()
        
        html.appendLine("<!DOCTYPE html>")
        html.appendLine("<html>")
        html.appendLine("<head>")
        html.appendLine("    <title>Visual Regression Test Report</title>")
        html.appendLine("    <style>")
        html.appendLine(getReportCss())
        html.appendLine("    </style>")
        html.appendLine("</head>")
        html.appendLine("<body>")
        html.appendLine("    <h1>Visual Regression Test Report</h1>")
        
        // Summary section
        html.appendLine("    <div class='summary'>")
        html.appendLine("        <h2>Summary</h2>")
        html.appendLine("        <p>Total Tests: ${suiteResult.summary.totalTests}</p>")
        html.appendLine("        <p>Passed: ${suiteResult.summary.passedTests}</p>")
        html.appendLine("        <p>Failed: ${suiteResult.summary.failedTests}</p>")
        html.appendLine("        <p>Success Rate: ${String.format("%.2f", suiteResult.summary.successRate * 100)}%</p>")
        html.appendLine("        <p>Total Time: ${suiteResult.totalTime}ms</p>")
        html.appendLine("    </div>")
        
        // Results by category
        html.appendLine("    <div class='results'>")
        html.appendLine("        <h2>Results by Category</h2>")
        
        val resultsByCategory = suiteResult.results.groupBy { it.testCase.category }
        for ((category, results) in resultsByCategory) {
            val passed = results.count { it.passed }
            val total = results.size
            
            html.appendLine("        <div class='category'>")
            html.appendLine("            <h3>${category.name}</h3>")
            html.appendLine("            <p>Passed: $passed / $total</p>")
            html.appendLine("        </div>")
        }
        
        html.appendLine("    </div>")
        
        // Detailed results
        html.appendLine("    <div class='detailed-results'>")
        html.appendLine("        <h2>Detailed Results</h2>")
        
        for (result in suiteResult.results) {
            val statusClass = if (result.passed) "passed" else "failed"
            
            html.appendLine("        <div class='test-result $statusClass'>")
            html.appendLine("            <h3>${result.testCase.name}</h3>")
            html.appendLine("            <p>Status: ${if (result.passed) "PASSED" else "FAILED"}</p>")
            html.appendLine("            <p>Execution Time: ${result.executionTime}ms</p>")
            
            if (result.comparison != null) {
                html.appendLine("            <p>Structural Similarity: ${String.format("%.2f", result.comparison.structuralSimilarity.similarity * 100)}%</p>")
                html.appendLine("            <p>Max Coordinate Deviation: ${String.format("%.2f", result.comparison.coordinateDeviations.maxDeviation)}</p>")
                html.appendLine("            <p>Attribute Match: ${String.format("%.2f", result.comparison.attributeMatches.matchPercentage * 100)}%</p>")
            }
            
            if (result.error != null) {
                html.appendLine("            <p class='error'>Error: ${result.error}</p>")
            }
            
            html.appendLine("        </div>")
        }
        
        html.appendLine("    </div>")
        
        html.appendLine("</body>")
        html.appendLine("</html>")
        
        return html.toString()
    }
    
    /**
     * Generates JSON report for programmatic consumption.
     */
    fun generateJsonReport(suiteResult: TestSuiteResult): String {
        // Simplified JSON generation
        val json = StringBuilder()
        json.appendLine("{")
        json.appendLine("  \"summary\": {")
        json.appendLine("    \"totalTests\": ${suiteResult.summary.totalTests},")
        json.appendLine("    \"passedTests\": ${suiteResult.summary.passedTests},")
        json.appendLine("    \"failedTests\": ${suiteResult.summary.failedTests},")
        json.appendLine("    \"successRate\": ${suiteResult.summary.successRate},")
        json.appendLine("    \"totalTime\": ${suiteResult.totalTime}")
        json.appendLine("  },")
        json.appendLine("  \"results\": [")
        
        suiteResult.results.forEachIndexed { index, result ->
            json.appendLine("    {")
            json.appendLine("      \"testName\": \"${result.testCase.name}\",")
            json.appendLine("      \"passed\": ${result.passed},")
            json.appendLine("      \"executionTime\": ${result.executionTime}")
            json.append("    }")
            if (index < suiteResult.results.size - 1) json.appendLine(",")
            else json.appendLine()
        }
        
        json.appendLine("  ]")
        json.appendLine("}")
        
        return json.toString()
    }
    
    /**
     * Benchmarks performance against reference Graphviz.
     */
    fun benchmarkPerformance(
        testCases: List<TestCase>,
        kotlinRenderer: (String) -> String
    ): PerformanceBenchmark {
        val kotlinTimes = mutableListOf<Long>()
        val referenceTimes = mutableListOf<Long>()
        
        for (testCase in testCases) {
            // Benchmark Kotlin implementation
            val kotlinStart = System.currentTimeMillis()
            kotlinRenderer(testCase.dotContent)
            val kotlinEnd = System.currentTimeMillis()
            kotlinTimes.add(kotlinEnd - kotlinStart)
            
            // Benchmark reference Graphviz
            val referenceStart = System.currentTimeMillis()
            ReferenceComparison.generateReferenceOutput(testCase.dotContent)
            val referenceEnd = System.currentTimeMillis()
            referenceTimes.add(referenceEnd - referenceStart)
        }
        
        return PerformanceBenchmark(
            kotlinAverageTime = kotlinTimes.average(),
            referenceAverageTime = referenceTimes.average(),
            kotlinMedianTime = kotlinTimes.sorted()[kotlinTimes.size / 2].toDouble(),
            referenceMedianTime = referenceTimes.sorted()[referenceTimes.size / 2].toDouble(),
            speedRatio = kotlinTimes.average() / referenceTimes.average()
        )
    }
    
    /**
     * Checks for regressions by comparing with previous test results.
     */
    fun checkForRegressions(
        currentResults: TestSuiteResult,
        previousResults: TestSuiteResult
    ): RegressionReport {
        val regressions = mutableListOf<Regression>()
        val improvements = mutableListOf<Improvement>()
        
        for (currentResult in currentResults.results) {
            val previousResult = previousResults.results.find { it.testCase.name == currentResult.testCase.name }
            
            if (previousResult != null) {
                // Check for regression (previously passed, now failed)
                if (previousResult.passed && !currentResult.passed) {
                    regressions.add(Regression(
                        testName = currentResult.testCase.name,
                        previousScore = previousResult.comparison?.overallScore ?: 0.0,
                        currentScore = currentResult.comparison?.overallScore ?: 0.0
                    ))
                }
                
                // Check for improvement (previously failed, now passed)
                if (!previousResult.passed && currentResult.passed) {
                    improvements.add(Improvement(
                        testName = currentResult.testCase.name,
                        previousScore = previousResult.comparison?.overallScore ?: 0.0,
                        currentScore = currentResult.comparison?.overallScore ?: 1.0
                    ))
                }
            }
        }
        
        return RegressionReport(
            regressions = regressions,
            improvements = improvements,
            hasRegressions = regressions.isNotEmpty()
        )
    }
    
    // Private helper methods
    
    private fun generateSummary(results: List<TestResult>, errors: List<TestError>): TestSummary {
        val totalTests = results.size + errors.size
        val passedTests = results.count { it.passed }
        val failedTests = totalTests - passedTests
        val successRate = if (totalTests > 0) passedTests.toDouble() / totalTests else 0.0
        
        return TestSummary(
            totalTests = totalTests,
            passedTests = passedTests,
            failedTests = failedTests,
            successRate = successRate
        )
    }
    
    private fun calculatePerformanceMetrics(executionTime: Long, outputSize: Int): PerformanceMetrics {
        return PerformanceMetrics(
            executionTime = executionTime,
            outputSize = outputSize,
            throughput = if (executionTime > 0) outputSize.toDouble() / executionTime else 0.0
        )
    }
    
    private fun getReportCss(): String {
        return """
            body {
                font-family: Arial, sans-serif;
                margin: 20px;
                background-color: #f5f5f5;
            }
            h1 {
                color: #333;
            }
            .summary {
                background-color: white;
                padding: 20px;
                border-radius: 5px;
                margin-bottom: 20px;
            }
            .results {
                background-color: white;
                padding: 20px;
                border-radius: 5px;
                margin-bottom: 20px;
            }
            .category {
                margin-bottom: 10px;
            }
            .detailed-results {
                background-color: white;
                padding: 20px;
                border-radius: 5px;
            }
            .test-result {
                border: 1px solid #ddd;
                padding: 15px;
                margin-bottom: 10px;
                border-radius: 3px;
            }
            .test-result.passed {
                border-left: 5px solid #4CAF50;
            }
            .test-result.failed {
                border-left: 5px solid #f44336;
            }
            .error {
                color: #f44336;
                font-weight: bold;
            }
        """.trimIndent()
    }
}

// Data classes for test automation

data class TestSuiteConfig(
    val tolerances: ComparisonTolerances = ComparisonTolerances.DEFAULT,
    val generateHtmlReport: Boolean = true,
    val generateJsonReport: Boolean = true,
    val benchmarkPerformance: Boolean = false
) {
    companion object {
        val DEFAULT = TestSuiteConfig()
    }
}

data class TestSuiteResult(
    val results: List<TestResult>,
    val errors: List<TestError>,
    val totalTime: Long,
    val summary: TestSummary
)

data class TestResult(
    val testCase: TestCase,
    val passed: Boolean,
    val kotlinOutput: String,
    val referenceOutput: String?,
    val comparison: ComparisonResult?,
    val executionTime: Long,
    val performanceMetrics: PerformanceMetrics?,
    val error: String? = null
)

data class TestError(
    val testCase: TestCase,
    val message: String,
    val stackTrace: String
)

data class TestSummary(
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val successRate: Double
)

data class PerformanceMetrics(
    val executionTime: Long,
    val outputSize: Int,
    val throughput: Double
)

data class PerformanceBenchmark(
    val kotlinAverageTime: Double,
    val referenceAverageTime: Double,
    val kotlinMedianTime: Double,
    val referenceMedianTime: Double,
    val speedRatio: Double
)

data class RegressionReport(
    val regressions: List<Regression>,
    val improvements: List<Improvement>,
    val hasRegressions: Boolean
)

data class Regression(
    val testName: String,
    val previousScore: Double,
    val currentScore: Double
)

data class Improvement(
    val testName: String,
    val previousScore: Double,
    val currentScore: Double
)