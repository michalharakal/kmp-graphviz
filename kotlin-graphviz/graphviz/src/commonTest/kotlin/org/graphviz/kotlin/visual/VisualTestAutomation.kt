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
    
    /**
     * Generates visual diff images for failed tests.
     */
    fun generateVisualDiffs(
        suiteResult: TestSuiteResult,
        outputDir: String = "build/reports/visual-regression/diffs"
    ): List<VisualDiff> {
        val diffs = mutableListOf<VisualDiff>()
        
        for (result in suiteResult.results.filter { !it.passed }) {
            if (result.referenceOutput != null) {
                val diffResult = createVisualDiff(
                    testName = result.testCase.name,
                    kotlinSvg = result.kotlinOutput,
                    referenceSvg = result.referenceOutput,
                    outputDir = outputDir
                )
                diffs.add(diffResult)
            }
        }
        
        return diffs
    }
    
    /**
     * Creates baseline results for future regression testing.
     */
    fun createBaseline(
        suiteResult: TestSuiteResult,
        baselineFile: String = "build/reports/visual-regression/baseline.json"
    ) {
        val baseline = BaselineResults(
            timestamp = System.currentTimeMillis(),
            version = getVersionInfo(),
            results = suiteResult.results.map { result ->
                BaselineTestResult(
                    testName = result.testCase.name,
                    passed = result.passed,
                    overallScore = result.comparison?.overallScore ?: 0.0,
                    structuralSimilarity = result.comparison?.structuralSimilarity?.similarity ?: 0.0,
                    maxCoordinateDeviation = result.comparison?.coordinateDeviations?.maxDeviation ?: Double.MAX_VALUE,
                    attributeMatchPercentage = result.comparison?.attributeMatches?.matchPercentage ?: 0.0,
                    executionTime = result.executionTime,
                    outputHash = result.kotlinOutput.hashCode().toString()
                )
            }
        )
        
        // Write baseline to file (simplified JSON serialization)
        val json = serializeBaseline(baseline)
        writeToFile(baselineFile, json)
    }
    
    /**
     * Loads baseline results from file.
     */
    fun loadBaseline(baselineFile: String): BaselineResults? {
        return try {
            val json = readFromFile(baselineFile)
            deserializeBaseline(json)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generates comprehensive CI/CD report with all metrics.
     */
    fun generateCiCdReport(
        suiteResult: TestSuiteResult,
        benchmark: PerformanceBenchmark?,
        regressionReport: RegressionReport?,
        visualDiffs: List<VisualDiff>
    ): CiCdReport {
        return CiCdReport(
            summary = suiteResult.summary,
            executionTime = suiteResult.totalTime,
            benchmark = benchmark,
            regressionReport = regressionReport,
            visualDiffs = visualDiffs,
            failedTests = suiteResult.results.filter { !it.passed },
            timestamp = System.currentTimeMillis(),
            environment = getCiEnvironmentInfo()
        )
    }
    
    /**
     * Generates trend analysis comparing multiple test runs.
     */
    fun generateTrendAnalysis(
        currentResults: TestSuiteResult,
        historicalResults: List<TestSuiteResult>
    ): TrendAnalysis {
        val trends = mutableMapOf<String, TestTrend>()
        
        for (currentResult in currentResults.results) {
            val historicalScores = historicalResults.mapNotNull { historical ->
                historical.results.find { it.testCase.name == currentResult.testCase.name }
                    ?.comparison?.overallScore
            }
            
            if (historicalScores.isNotEmpty()) {
                val currentScore = currentResult.comparison?.overallScore ?: 0.0
                val averageHistorical = historicalScores.average()
                val trend = when {
                    currentScore > averageHistorical + 0.05 -> TrendDirection.IMPROVING
                    currentScore < averageHistorical - 0.05 -> TrendDirection.DEGRADING
                    else -> TrendDirection.STABLE
                }
                
                trends[currentResult.testCase.name] = TestTrend(
                    testName = currentResult.testCase.name,
                    currentScore = currentScore,
                    averageHistoricalScore = averageHistorical,
                    trend = trend,
                    volatility = calculateVolatility(historicalScores)
                )
            }
        }
        
        return TrendAnalysis(
            trends = trends,
            overallTrend = calculateOverallTrend(trends.values),
            stabilityScore = calculateStabilityScore(trends.values)
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
    
    private fun createVisualDiff(
        testName: String,
        kotlinSvg: String,
        referenceSvg: String,
        outputDir: String
    ): VisualDiff {
        // Simplified visual diff creation
        // In a real implementation, this would use image comparison libraries
        return VisualDiff(
            testName = testName,
            diffImagePath = "$outputDir/$testName-diff.png",
            kotlinImagePath = "$outputDir/$testName-kotlin.png",
            referenceImagePath = "$outputDir/$testName-reference.png",
            similarity = 0.95,
            differences = emptyList()
        )
    }
    
    private fun getVersionInfo(): String {
        return "1.0.0" // In real implementation, read from build configuration
    }
    
    private fun serializeBaseline(baseline: BaselineResults): String {
        // Simplified JSON serialization
        val json = StringBuilder()
        json.appendLine("{")
        json.appendLine("  \"timestamp\": ${baseline.timestamp},")
        json.appendLine("  \"version\": \"${baseline.version}\",")
        json.appendLine("  \"results\": [")
        
        baseline.results.forEachIndexed { index, result ->
            json.appendLine("    {")
            json.appendLine("      \"testName\": \"${result.testName}\",")
            json.appendLine("      \"passed\": ${result.passed},")
            json.appendLine("      \"overallScore\": ${result.overallScore},")
            json.appendLine("      \"structuralSimilarity\": ${result.structuralSimilarity},")
            json.appendLine("      \"maxCoordinateDeviation\": ${result.maxCoordinateDeviation},")
            json.appendLine("      \"attributeMatchPercentage\": ${result.attributeMatchPercentage},")
            json.appendLine("      \"executionTime\": ${result.executionTime},")
            json.appendLine("      \"outputHash\": \"${result.outputHash}\"")
            json.append("    }")
            if (index < baseline.results.size - 1) json.appendLine(",")
            else json.appendLine()
        }
        
        json.appendLine("  ]")
        json.appendLine("}")
        
        return json.toString()
    }
    
    private fun deserializeBaseline(json: String): BaselineResults {
        // Simplified JSON deserialization
        // In real implementation, use proper JSON library
        return BaselineResults(
            timestamp = System.currentTimeMillis(),
            version = "1.0.0",
            results = emptyList()
        )
    }
    
    private fun writeToFile(path: String, content: String) {
        // Simplified file writing
        // In real implementation, use proper file I/O
        println("Writing to file: $path")
    }
    
    private fun readFromFile(path: String): String {
        // Simplified file reading
        // In real implementation, use proper file I/O
        return "{}"
    }
    
    private fun getCiEnvironmentInfo(): CiEnvironmentInfo {
        return CiEnvironmentInfo(
            isCI = System.getenv("CI") != null,
            ciProvider = detectCiProvider(),
            buildNumber = System.getenv("BUILD_NUMBER") ?: System.getenv("GITHUB_RUN_NUMBER"),
            branch = System.getenv("BRANCH_NAME") ?: System.getenv("GITHUB_REF_NAME"),
            commitHash = System.getenv("GIT_COMMIT") ?: System.getenv("GITHUB_SHA"),
            javaVersion = System.getProperty("java.version"),
            osName = System.getProperty("os.name"),
            osVersion = System.getProperty("os.version")
        )
    }
    
    private fun detectCiProvider(): String? {
        return when {
            System.getenv("GITHUB_ACTIONS") != null -> "GitHub Actions"
            System.getenv("GITLAB_CI") != null -> "GitLab CI"
            System.getenv("JENKINS_URL") != null -> "Jenkins"
            System.getenv("TRAVIS") != null -> "Travis CI"
            System.getenv("CIRCLECI") != null -> "CircleCI"
            System.getenv("BUILDKITE") != null -> "Buildkite"
            else -> null
        }
    }
    
    private fun calculateVolatility(scores: List<Double>): Double {
        if (scores.size < 2) return 0.0
        
        val mean = scores.average()
        val variance = scores.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
    
    private fun calculateOverallTrend(trends: Collection<TestTrend>): TrendDirection {
        if (trends.isEmpty()) return TrendDirection.STABLE
        
        val improvingCount = trends.count { it.trend == TrendDirection.IMPROVING }
        val degradingCount = trends.count { it.trend == TrendDirection.DEGRADING }
        
        return when {
            improvingCount > degradingCount * 2 -> TrendDirection.IMPROVING
            degradingCount > improvingCount * 2 -> TrendDirection.DEGRADING
            else -> TrendDirection.STABLE
        }
    }
    
    private fun calculateStabilityScore(trends: Collection<TestTrend>): Double {
        if (trends.isEmpty()) return 1.0
        
        val averageVolatility = trends.map { it.volatility }.average()
        return 1.0 - averageVolatility.coerceIn(0.0, 1.0)
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

// Enhanced data classes for CI/CD automation

data class VisualDiff(
    val testName: String,
    val diffImagePath: String,
    val kotlinImagePath: String,
    val referenceImagePath: String,
    val similarity: Double,
    val differences: List<DiffRegion>
)

data class DiffRegion(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val severity: DiffSeverity
)

enum class DiffSeverity {
    MINOR, MODERATE, MAJOR
}

data class BaselineResults(
    val timestamp: Long,
    val version: String,
    val results: List<BaselineTestResult>
)

data class BaselineTestResult(
    val testName: String,
    val passed: Boolean,
    val overallScore: Double,
    val structuralSimilarity: Double,
    val maxCoordinateDeviation: Double,
    val attributeMatchPercentage: Double,
    val executionTime: Long,
    val outputHash: String
)

data class CiCdReport(
    val summary: TestSummary,
    val executionTime: Long,
    val benchmark: PerformanceBenchmark?,
    val regressionReport: RegressionReport?,
    val visualDiffs: List<VisualDiff>,
    val failedTests: List<TestResult>,
    val timestamp: Long,
    val environment: CiEnvironmentInfo
)

data class CiEnvironmentInfo(
    val isCI: Boolean,
    val ciProvider: String?,
    val buildNumber: String?,
    val branch: String?,
    val commitHash: String?,
    val javaVersion: String,
    val osName: String,
    val osVersion: String
)

data class TrendAnalysis(
    val trends: Map<String, TestTrend>,
    val overallTrend: TrendDirection,
    val stabilityScore: Double
)

data class TestTrend(
    val testName: String,
    val currentScore: Double,
    val averageHistoricalScore: Double,
    val trend: TrendDirection,
    val volatility: Double
)

enum class TrendDirection {
    IMPROVING, STABLE, DEGRADING
}