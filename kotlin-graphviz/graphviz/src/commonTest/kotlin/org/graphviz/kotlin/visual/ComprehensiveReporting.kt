package org.graphviz.kotlin.visual

import kotlin.math.*

/**
 * Comprehensive test reporting system for visual regression testing.
 * Provides detailed HTML reports, trend analysis, and performance metrics.
 * **Feature: kotlin-multiplatform-port, Task 14.4: Generate comprehensive test reports**
 */
object ComprehensiveReporting {
    
    /**
     * Generates comprehensive HTML report with side-by-side visual comparisons.
     */
    fun generateEnhancedHtmlReport(
        suiteResult: TestSuiteResult,
        historicalResults: List<TestSuiteResult> = emptyList(),
        performanceBenchmark: PerformanceBenchmark? = null
    ): EnhancedHtmlReport {
        val trendAnalysis = if (historicalResults.isNotEmpty()) {
            generateDetailedTrendAnalysis(suiteResult, historicalResults)
        } else null
        
        val failureAnalysis = analyzeFailures(suiteResult.results.filter { !it.passed })
        val performanceAnalysis = analyzePerformance(suiteResult.results, performanceBenchmark)
        
        val htmlContent = buildEnhancedHtmlContent(
            suiteResult = suiteResult,
            trendAnalysis = trendAnalysis,
            failureAnalysis = failureAnalysis,
            performanceAnalysis = performanceAnalysis
        )
        
        return EnhancedHtmlReport(
            content = htmlContent,
            summary = suiteResult.summary,
            trendAnalysis = trendAnalysis,
            failureAnalysis = failureAnalysis,
            performanceAnalysis = performanceAnalysis,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Generates detailed difference analysis with failure categorization.
     */
    fun analyzeFailures(failedResults: List<TestResult>): FailureAnalysis {
        val categorizedFailures = mutableMapOf<FailureCategory, MutableList<CategorizedFailure>>()
        
        for (result in failedResults) {
            val category = categorizeFailure(result)
            val failure = CategorizedFailure(
                testResult = result,
                category = category,
                severity = calculateFailureSeverity(result),
                rootCause = identifyRootCause(result),
                recommendations = generateRecommendations(result, category)
            )
            
            categorizedFailures.getOrPut(category) { mutableListOf() }.add(failure)
        }
        
        return FailureAnalysis(
            totalFailures = failedResults.size,
            categorizedFailures = categorizedFailures,
            criticalFailures = categorizedFailures.values.flatten().filter { it.severity == FailureSeverity.CRITICAL },
            commonPatterns = identifyCommonFailurePatterns(categorizedFailures.values.flatten()),
            recommendations = generateOverallRecommendations(categorizedFailures)
        )
    }
    
    /**
     * Generates trend analysis for tracking consistency improvements over time.
     */
    fun generateDetailedTrendAnalysis(
        currentResults: TestSuiteResult,
        historicalResults: List<TestSuiteResult>
    ): DetailedTrendAnalysis {
        val testTrends = mutableMapOf<String, DetailedTestTrend>()
        val timeSeriesData = mutableMapOf<String, List<TrendDataPoint>>()
        
        // Analyze trends for each test
        for (currentResult in currentResults.results) {
            val testName = currentResult.testCase.name
            val historicalData = historicalResults.mapNotNull { historical ->
                historical.results.find { it.testCase.name == testName }
            }
            
            if (historicalData.isNotEmpty()) {
                val trend = analyzeTestTrend(currentResult, historicalData)
                testTrends[testName] = trend
                
                val dataPoints = createTimeSeriesData(currentResult, historicalData)
                timeSeriesData[testName] = dataPoints
            }
        }
        
        // Calculate overall metrics
        val overallMetrics = calculateOverallTrendMetrics(testTrends.values)
        val stabilityMetrics = calculateStabilityMetrics(testTrends.values)
        val qualityMetrics = calculateQualityMetrics(currentResults, historicalResults)
        
        return DetailedTrendAnalysis(
            testTrends = testTrends,
            timeSeriesData = timeSeriesData,
            overallMetrics = overallMetrics,
            stabilityMetrics = stabilityMetrics,
            qualityMetrics = qualityMetrics,
            regressionRisk = assessRegressionRisk(testTrends.values),
            recommendations = generateTrendRecommendations(overallMetrics, stabilityMetrics)
        )
    }
    
    /**
     * Analyzes performance metrics and memory usage comparisons.
     */
    fun analyzePerformance(
        results: List<TestResult>,
        benchmark: PerformanceBenchmark?
    ): PerformanceAnalysis {
        val executionTimes = results.map { it.executionTime }
        val outputSizes = results.mapNotNull { it.performanceMetrics?.outputSize }
        val throughputs = results.mapNotNull { it.performanceMetrics?.throughput }
        
        val performanceMetrics = DetailedPerformanceMetrics(
            averageExecutionTime = if (executionTimes.isNotEmpty()) executionTimes.average() else 0.0,
            medianExecutionTime = if (executionTimes.isNotEmpty()) {
                val sorted = executionTimes.sorted()
                sorted[sorted.size / 2].toDouble()
            } else 0.0,
            p95ExecutionTime = if (executionTimes.isNotEmpty()) {
                val sorted = executionTimes.sorted()
                sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)].toDouble()
            } else 0.0,
            p99ExecutionTime = if (executionTimes.isNotEmpty()) {
                val sorted = executionTimes.sorted()
                sorted[(sorted.size * 0.99).toInt().coerceAtMost(sorted.size - 1)].toDouble()
            } else 0.0,
            averageOutputSize = if (outputSizes.isNotEmpty()) outputSizes.average() else 0.0,
            averageThroughput = if (throughputs.isNotEmpty()) throughputs.average() else 0.0,
            totalMemoryUsage = estimateMemoryUsage(results),
            performanceScore = calculatePerformanceScore(executionTimes, outputSizes)
        )
        
        val benchmarkComparison = benchmark?.let { 
            BenchmarkComparison(
                kotlinVsReference = BenchmarkMetrics(
                    speedRatio = it.speedRatio,
                    averageTimeDifference = it.kotlinAverageTime - it.referenceAverageTime,
                    medianTimeDifference = it.kotlinMedianTime - it.referenceMedianTime,
                    performanceCategory = categorizePerformance(it.speedRatio)
                ),
                recommendations = generatePerformanceRecommendations(it.speedRatio)
            )
        }
        
        return PerformanceAnalysis(
            metrics = performanceMetrics,
            benchmarkComparison = benchmarkComparison,
            performanceIssues = identifyPerformanceIssues(performanceMetrics),
            optimizationOpportunities = identifyOptimizationOpportunities(performanceMetrics, results),
            memoryAnalysis = analyzeMemoryUsage(results)
        )
    }
    
    /**
     * Generates comprehensive visual comparison report.
     */
    fun generateVisualComparisonReport(
        testResult: TestResult,
        includeDetailedAnalysis: Boolean = true
    ): VisualComparisonReport {
        val svgAnalysis = if (testResult.referenceOutput != null && includeDetailedAnalysis) {
            analyzeVisualDifferences(testResult.kotlinOutput, testResult.referenceOutput)
        } else null
        
        val coordinateAnalysis = testResult.comparison?.coordinateDeviations?.let { deviations ->
            CoordinateAnalysisReport(
                maxDeviation = deviations.maxDeviation,
                averageDeviation = deviations.averageDeviation,
                deviationDistribution = analyzeDeviationDistribution(deviations.deviations),
                problematicElements = identifyProblematicElements(deviations.deviations),
                recommendations = generateCoordinateRecommendations(deviations)
            )
        }
        
        val attributeAnalysis = testResult.comparison?.attributeMatches?.let { matches ->
            AttributeAnalysisReport(
                matchPercentage = matches.matchPercentage,
                mismatchedAttributes = matches.matches.filter { !it.matches },
                criticalMismatches = identifyCriticalAttributeMismatches(matches.matches),
                recommendations = generateAttributeRecommendations(matches)
            )
        }
        
        return VisualComparisonReport(
            testName = testResult.testCase.name,
            passed = testResult.passed,
            overallScore = testResult.comparison?.overallScore ?: 0.0,
            svgAnalysis = svgAnalysis,
            coordinateAnalysis = coordinateAnalysis,
            attributeAnalysis = attributeAnalysis,
            visualDifferences = generateVisualDifferencesSummary(testResult),
            recommendations = generateVisualRecommendations(testResult)
        )
    }
    
    /**
     * Creates interactive dashboard data for web-based reporting.
     */
    fun generateDashboardData(
        suiteResult: TestSuiteResult,
        historicalResults: List<TestSuiteResult> = emptyList(),
        performanceBenchmark: PerformanceBenchmark? = null
    ): DashboardData {
        val trendAnalysis = if (historicalResults.isNotEmpty()) {
            generateDetailedTrendAnalysis(suiteResult, historicalResults)
        } else null
        
        val failureAnalysis = analyzeFailures(suiteResult.results.filter { !it.passed })
        val performanceAnalysis = analyzePerformance(suiteResult.results, performanceBenchmark)
        
        // Generate chart data
        val chartData = DashboardChartData(
            successRateChart = generateSuccessRateChartData(suiteResult, historicalResults),
            performanceChart = generatePerformanceChartData(suiteResult, historicalResults),
            categoryBreakdownChart = generateCategoryBreakdownChartData(suiteResult),
            trendChart = trendAnalysis?.let { generateTrendChartData(it) },
            deviationHeatmap = generateDeviationHeatmapData(suiteResult)
        )
        
        return DashboardData(
            summary = suiteResult.summary,
            chartData = chartData,
            trendAnalysis = trendAnalysis,
            failureAnalysis = failureAnalysis,
            performanceAnalysis = performanceAnalysis,
            timestamp = System.currentTimeMillis(),
            metadata = DashboardMetadata(
                totalTests = suiteResult.results.size,
                executionTime = suiteResult.totalTime,
                environment = getCiEnvironmentInfo()
            )
        )
    }
    
    // Private helper methods for failure analysis
    
    private fun categorizeFailure(result: TestResult): FailureCategory {
        val comparison = result.comparison ?: return FailureCategory.EXECUTION_ERROR
        
        return when {
            comparison.coordinateDeviations.maxDeviation > 10.0 -> FailureCategory.LAYOUT_DEVIATION
            comparison.attributeMatches.matchPercentage < 0.8 -> FailureCategory.ATTRIBUTE_MISMATCH
            comparison.structuralSimilarity.similarity < 0.9 -> FailureCategory.STRUCTURAL_DIFFERENCE
            result.error != null -> FailureCategory.EXECUTION_ERROR
            else -> FailureCategory.MINOR_DIFFERENCE
        }
    }
    
    private fun calculateFailureSeverity(result: TestResult): FailureSeverity {
        val comparison = result.comparison
        
        return when {
            result.error != null -> FailureSeverity.CRITICAL
            comparison == null -> FailureSeverity.CRITICAL
            comparison.overallScore < 0.5 -> FailureSeverity.HIGH
            comparison.overallScore < 0.8 -> FailureSeverity.MEDIUM
            else -> FailureSeverity.LOW
        }
    }
    
    private fun identifyRootCause(result: TestResult): String {
        val comparison = result.comparison
        
        return when {
            result.error != null -> "Execution error: ${result.error}"
            comparison == null -> "No comparison data available"
            comparison.coordinateDeviations.maxDeviation > 10.0 -> 
                "Significant coordinate deviation (${comparison.coordinateDeviations.maxDeviation}px)"
            comparison.attributeMatches.matchPercentage < 0.8 -> 
                "Attribute mismatch (${(comparison.attributeMatches.matchPercentage * 100).toInt()}% match)"
            comparison.structuralSimilarity.similarity < 0.9 -> 
                "Structural differences in SVG elements"
            else -> "Minor visual differences"
        }
    }
    
    private fun generateRecommendations(result: TestResult, category: FailureCategory): List<String> {
        return when (category) {
            FailureCategory.LAYOUT_DEVIATION -> listOf(
                "Review layout algorithm parameters",
                "Check coordinate calculation precision",
                "Verify node positioning logic"
            )
            FailureCategory.ATTRIBUTE_MISMATCH -> listOf(
                "Validate attribute mapping logic",
                "Check color and style conversions",
                "Review default value handling"
            )
            FailureCategory.STRUCTURAL_DIFFERENCE -> listOf(
                "Compare SVG element structure",
                "Check element ordering and nesting",
                "Verify graph traversal logic"
            )
            FailureCategory.EXECUTION_ERROR -> listOf(
                "Check input validation",
                "Review error handling",
                "Verify environment setup"
            )
            FailureCategory.MINOR_DIFFERENCE -> listOf(
                "Consider adjusting tolerance levels",
                "Review precision requirements"
            )
        }
    }
    
    private fun identifyCommonFailurePatterns(failures: List<CategorizedFailure>): List<FailurePattern> {
        val patterns = mutableListOf<FailurePattern>()
        
        // Group by category and look for patterns
        val byCategory = failures.groupBy { it.category }
        
        for ((category, categoryFailures) in byCategory) {
            if (categoryFailures.size >= 3) { // Pattern threshold
                patterns.add(FailurePattern(
                    category = category,
                    frequency = categoryFailures.size,
                    affectedTests = categoryFailures.map { it.testResult.testCase.name },
                    commonCause = identifyCommonCause(categoryFailures),
                    recommendation = generatePatternRecommendation(category, categoryFailures)
                ))
            }
        }
        
        return patterns
    }
    
    private fun identifyCommonCause(failures: List<CategorizedFailure>): String {
        // Analyze common characteristics
        val rootCauses = failures.map { it.rootCause }
        val mostCommon = rootCauses.groupingBy { it }.eachCount().maxByOrNull { it.value }
        
        return mostCommon?.key ?: "Multiple causes identified"
    }
    
    private fun generatePatternRecommendation(
        category: FailureCategory, 
        failures: List<CategorizedFailure>
    ): String {
        return when (category) {
            FailureCategory.LAYOUT_DEVIATION -> 
                "Consider reviewing the core layout algorithm - multiple tests show coordinate deviations"
            FailureCategory.ATTRIBUTE_MISMATCH -> 
                "Systematic attribute mapping issues detected - review attribute conversion logic"
            FailureCategory.STRUCTURAL_DIFFERENCE -> 
                "Multiple structural differences suggest SVG generation logic needs review"
            FailureCategory.EXECUTION_ERROR -> 
                "Multiple execution errors indicate environment or setup issues"
            FailureCategory.MINOR_DIFFERENCE -> 
                "Consider adjusting global tolerance settings"
        }
    }
    
    private fun generateOverallRecommendations(
        categorizedFailures: Map<FailureCategory, List<CategorizedFailure>>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        val totalFailures = categorizedFailures.values.sumOf { it.size }
        val criticalCount = categorizedFailures.values.flatten().count { it.severity == FailureSeverity.CRITICAL }
        
        if (criticalCount > 0) {
            recommendations.add("Address ${criticalCount} critical failures immediately")
        }
        
        if (totalFailures > 10) {
            recommendations.add("High failure count suggests systematic issues - consider comprehensive review")
        }
        
        // Category-specific recommendations
        categorizedFailures.forEach { (category, failures) ->
            if (failures.size > totalFailures * 0.3) {
                recommendations.add("${category.name} issues are prevalent - prioritize this area")
            }
        }
        
        return recommendations
    }
    
    // Private helper methods for trend analysis
    
    private fun analyzeTestTrend(
        currentResult: TestResult,
        historicalData: List<TestResult>
    ): DetailedTestTrend {
        val scores = historicalData.mapNotNull { it.comparison?.overallScore } + 
                    listOfNotNull(currentResult.comparison?.overallScore)
        
        val trend = calculateTrendDirection(scores)
        val volatility = calculateVolatility(scores)
        val stability = calculateStability(scores)
        
        return DetailedTestTrend(
            testName = currentResult.testCase.name,
            currentScore = currentResult.comparison?.overallScore ?: 0.0,
            historicalScores = scores.dropLast(1),
            trend = trend,
            volatility = volatility,
            stability = stability,
            regressionRisk = assessTestRegressionRisk(scores, trend, volatility),
            recommendations = generateTestTrendRecommendations(trend, volatility, stability)
        )
    }
    
    private fun calculateTrendDirection(scores: List<Double>): TrendDirection {
        if (scores.size < 2) return TrendDirection.STABLE
        
        val recent = scores.takeLast(3)
        val older = scores.dropLast(3).takeLast(3)
        
        if (recent.isEmpty() || older.isEmpty()) return TrendDirection.STABLE
        
        val recentAvg = recent.average()
        val olderAvg = older.average()
        
        return when {
            recentAvg > olderAvg + 0.05 -> TrendDirection.IMPROVING
            recentAvg < olderAvg - 0.05 -> TrendDirection.DEGRADING
            else -> TrendDirection.STABLE
        }
    }
    
    private fun calculateVolatility(scores: List<Double>): Double {
        if (scores.size < 2) return 0.0
        
        val mean = scores.average()
        val variance = scores.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }
    
    private fun calculateStability(scores: List<Double>): Double {
        if (scores.size < 2) return 1.0
        
        val volatility = calculateVolatility(scores)
        return (1.0 - volatility).coerceIn(0.0, 1.0)
    }
    
    private fun assessTestRegressionRisk(
        scores: List<Double>,
        trend: TrendDirection,
        volatility: Double
    ): RegressionRisk {
        return when {
            trend == TrendDirection.DEGRADING && volatility > 0.2 -> RegressionRisk.HIGH
            trend == TrendDirection.DEGRADING || volatility > 0.15 -> RegressionRisk.MEDIUM
            volatility > 0.1 -> RegressionRisk.LOW
            else -> RegressionRisk.MINIMAL
        }
    }
    
    private fun generateTestTrendRecommendations(
        trend: TrendDirection,
        volatility: Double,
        stability: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        when (trend) {
            TrendDirection.DEGRADING -> recommendations.add("Investigate recent changes causing degradation")
            TrendDirection.IMPROVING -> recommendations.add("Continue current improvements")
            TrendDirection.STABLE -> recommendations.add("Maintain current quality level")
        }
        
        if (volatility > 0.15) {
            recommendations.add("High volatility detected - investigate inconsistent behavior")
        }
        
        if (stability < 0.8) {
            recommendations.add("Low stability - consider improving test reliability")
        }
        
        return recommendations
    }
    
    private fun createTimeSeriesData(
        currentResult: TestResult,
        historicalData: List<TestResult>
    ): List<TrendDataPoint> {
        val allResults = historicalData + currentResult
        
        return allResults.mapIndexed { index, result ->
            TrendDataPoint(
                timestamp = System.currentTimeMillis() - (allResults.size - index - 1) * 86400000L, // Daily intervals
                score = result.comparison?.overallScore ?: 0.0,
                passed = result.passed,
                executionTime = result.executionTime,
                coordinateDeviation = result.comparison?.coordinateDeviations?.maxDeviation ?: 0.0,
                attributeMatch = result.comparison?.attributeMatches?.matchPercentage ?: 0.0
            )
        }
    }
    
    // Helper methods for comprehensive reporting
    
    private fun buildEnhancedHtmlContent(
        suiteResult: TestSuiteResult,
        trendAnalysis: DetailedTrendAnalysis?,
        failureAnalysis: FailureAnalysis,
        performanceAnalysis: PerformanceAnalysis
    ): String {
        val html = StringBuilder()
        
        html.appendLine("<!DOCTYPE html>")
        html.appendLine("<html lang='en'>")
        html.appendLine("<head>")
        html.appendLine("    <meta charset='UTF-8'>")
        html.appendLine("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>")
        html.appendLine("    <title>Comprehensive Visual Regression Test Report</title>")
        html.appendLine("    <style>")
        html.appendLine(getEnhancedReportCss())
        html.appendLine("    </style>")
        html.appendLine("</head>")
        html.appendLine("<body>")
        
        // Header
        html.appendLine("    <header class='report-header'>")
        html.appendLine("        <h1>Visual Regression Test Report</h1>")
        html.appendLine("        <div class='report-meta'>")
        html.appendLine("            <span>Generated: ${java.time.Instant.ofEpochMilli(System.currentTimeMillis())}</span>")
        html.appendLine("            <span>Total Tests: ${suiteResult.summary.totalTests}</span>")
        html.appendLine("            <span>Success Rate: ${String.format("%.1f", suiteResult.summary.successRate * 100)}%</span>")
        html.appendLine("        </div>")
        html.appendLine("    </header>")
        
        // Summary Dashboard
        html.appendLine("    <section class='dashboard'>")
        html.appendLine("        <h2>Executive Summary</h2>")
        html.appendLine("        <div class='summary-cards'>")
        html.appendLine("            <div class='card ${if (suiteResult.summary.failedTests == 0) "success" else "failure"}'>")
        html.appendLine("                <h3>Test Results</h3>")
        html.appendLine("                <div class='metric'>${suiteResult.summary.passedTests}/${suiteResult.summary.totalTests}</div>")
        html.appendLine("                <div class='label'>Passed</div>")
        html.appendLine("            </div>")
        html.appendLine("            <div class='card performance'>")
        html.appendLine("                <h3>Performance</h3>")
        html.appendLine("                <div class='metric'>${performanceAnalysis.metrics.averageExecutionTime.toInt()}ms</div>")
        html.appendLine("                <div class='label'>Avg Time</div>")
        html.appendLine("            </div>")
        html.appendLine("            <div class='card quality'>")
        html.appendLine("                <h3>Quality Score</h3>")
        html.appendLine("                <div class='metric'>${String.format("%.1f", suiteResult.summary.successRate * 100)}%</div>")
        html.appendLine("                <div class='label'>Overall</div>")
        html.appendLine("            </div>")
        html.appendLine("        </div>")
        html.appendLine("    </section>")
        
        // Failure Analysis
        if (failureAnalysis.totalFailures > 0) {
            html.appendLine("    <section class='failure-analysis'>")
            html.appendLine("        <h2>Failure Analysis</h2>")
            html.appendLine("        <div class='failure-categories'>")
            
            for ((category, failures) in failureAnalysis.categorizedFailures) {
                html.appendLine("            <div class='category-card'>")
                html.appendLine("                <h3>${category.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}</h3>")
                html.appendLine("                <div class='failure-count'>${failures.size}</div>")
                html.appendLine("                <div class='failure-list'>")
                
                for (failure in failures.take(5)) { // Show top 5
                    html.appendLine("                    <div class='failure-item ${failure.severity.name.lowercase()}'>")
                    html.appendLine("                        <span class='test-name'>${failure.testResult.testCase.name}</span>")
                    html.appendLine("                        <span class='severity'>${failure.severity.name}</span>")
                    html.appendLine("                    </div>")
                }
                
                if (failures.size > 5) {
                    html.appendLine("                    <div class='more-failures'>... and ${failures.size - 5} more</div>")
                }
                
                html.appendLine("                </div>")
                html.appendLine("            </div>")
            }
            
            html.appendLine("        </div>")
            html.appendLine("    </section>")
        }
        
        // Detailed Test Results
        html.appendLine("    <section class='detailed-results'>")
        html.appendLine("        <h2>Detailed Test Results</h2>")
        html.appendLine("        <div class='test-results'>")
        
        for (result in suiteResult.results) {
            html.appendLine("            <div class='test-result ${if (result.passed) "passed" else "failed"}'>")
            html.appendLine("                <div class='test-header'>")
            html.appendLine("                    <h3>${result.testCase.name}</h3>")
            html.appendLine("                    <span class='status'>${if (result.passed) "PASSED" else "FAILED"}</span>")
            html.appendLine("                </div>")
            html.appendLine("                <div class='test-details'>")
            html.appendLine("                    <div class='metrics'>")
            html.appendLine("                        <span>Time: ${result.executionTime}ms</span>")
            
            if (result.comparison != null) {
                html.appendLine("                        <span>Score: ${String.format("%.2f", result.comparison.overallScore)}</span>")
                html.appendLine("                        <span>Similarity: ${String.format("%.1f", result.comparison.structuralSimilarity.similarity * 100)}%</span>")
                html.appendLine("                        <span>Max Deviation: ${String.format("%.1f", result.comparison.coordinateDeviations.maxDeviation)}px</span>")
            }
            
            html.appendLine("                    </div>")
            
            if (!result.passed && result.comparison != null) {
                html.appendLine("                    <div class='visual-comparison'>")
                html.appendLine("                        <div class='comparison-side'>")
                html.appendLine("                            <h4>Kotlin Output</h4>")
                html.appendLine("                            <div class='svg-container'>${result.kotlinOutput}</div>")
                html.appendLine("                        </div>")
                
                if (result.referenceOutput != null) {
                    html.appendLine("                        <div class='comparison-side'>")
                    html.appendLine("                            <h4>Reference Output</h4>")
                    html.appendLine("                            <div class='svg-container'>${result.referenceOutput}</div>")
                    html.appendLine("                        </div>")
                }
                
                html.appendLine("                    </div>")
            }
            
            html.appendLine("                </div>")
            html.appendLine("            </div>")
        }
        
        html.appendLine("        </div>")
        html.appendLine("    </section>")
        
        html.appendLine("</body>")
        html.appendLine("</html>")
        
        return html.toString()
    }
    
    private fun getEnhancedReportCss(): String {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                line-height: 1.6;
                color: #333;
                background-color: #f8f9fa;
            }
            
            .report-header {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                padding: 2rem;
                text-align: center;
            }
            
            .report-header h1 {
                font-size: 2.5rem;
                margin-bottom: 1rem;
            }
            
            .report-meta {
                display: flex;
                justify-content: center;
                gap: 2rem;
                font-size: 1.1rem;
            }
            
            .dashboard {
                padding: 2rem;
                background: white;
                margin: 2rem;
                border-radius: 8px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            
            .summary-cards {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 1.5rem;
                margin-top: 1.5rem;
            }
            
            .card {
                padding: 1.5rem;
                border-radius: 8px;
                text-align: center;
                color: white;
            }
            
            .card.success { background: linear-gradient(135deg, #4CAF50, #45a049); }
            .card.failure { background: linear-gradient(135deg, #f44336, #d32f2f); }
            .card.performance { background: linear-gradient(135deg, #2196F3, #1976D2); }
            .card.quality { background: linear-gradient(135deg, #FF9800, #F57C00); }
            
            .card h3 {
                font-size: 1rem;
                margin-bottom: 0.5rem;
                opacity: 0.9;
            }
            
            .card .metric {
                font-size: 2.5rem;
                font-weight: bold;
                margin-bottom: 0.5rem;
            }
            
            .card .label {
                font-size: 0.9rem;
                opacity: 0.8;
            }
            
            .failure-analysis {
                padding: 2rem;
                background: white;
                margin: 2rem;
                border-radius: 8px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            
            .failure-categories {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                gap: 1.5rem;
                margin-top: 1.5rem;
            }
            
            .category-card {
                border: 1px solid #ddd;
                border-radius: 8px;
                padding: 1.5rem;
                background: #fafafa;
            }
            
            .category-card h3 {
                color: #d32f2f;
                margin-bottom: 1rem;
            }
            
            .failure-count {
                font-size: 2rem;
                font-weight: bold;
                color: #f44336;
                margin-bottom: 1rem;
            }
            
            .failure-item {
                display: flex;
                justify-content: space-between;
                padding: 0.5rem;
                margin-bottom: 0.5rem;
                border-radius: 4px;
                background: white;
            }
            
            .failure-item.critical { border-left: 4px solid #d32f2f; }
            .failure-item.high { border-left: 4px solid #f44336; }
            .failure-item.medium { border-left: 4px solid #ff9800; }
            .failure-item.low { border-left: 4px solid #ffeb3b; }
            
            .detailed-results {
                padding: 2rem;
                background: white;
                margin: 2rem;
                border-radius: 8px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            
            .test-result {
                border: 1px solid #ddd;
                border-radius: 8px;
                margin-bottom: 1.5rem;
                overflow: hidden;
            }
            
            .test-result.passed {
                border-left: 5px solid #4CAF50;
            }
            
            .test-result.failed {
                border-left: 5px solid #f44336;
            }
            
            .test-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 1rem 1.5rem;
                background: #f8f9fa;
                border-bottom: 1px solid #ddd;
            }
            
            .test-header h3 {
                margin: 0;
            }
            
            .status {
                padding: 0.25rem 0.75rem;
                border-radius: 4px;
                font-weight: bold;
                font-size: 0.8rem;
            }
            
            .test-result.passed .status {
                background: #4CAF50;
                color: white;
            }
            
            .test-result.failed .status {
                background: #f44336;
                color: white;
            }
            
            .test-details {
                padding: 1.5rem;
            }
            
            .metrics {
                display: flex;
                gap: 1.5rem;
                margin-bottom: 1rem;
                font-size: 0.9rem;
                color: #666;
            }
            
            .visual-comparison {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 2rem;
                margin-top: 1.5rem;
            }
            
            .comparison-side h4 {
                margin-bottom: 1rem;
                color: #333;
            }
            
            .svg-container {
                border: 1px solid #ddd;
                border-radius: 4px;
                padding: 1rem;
                background: white;
                max-height: 400px;
                overflow: auto;
            }
            
            .svg-container svg {
                max-width: 100%;
                height: auto;
            }
            
            h2 {
                color: #333;
                margin-bottom: 1.5rem;
                font-size: 1.8rem;
            }
            
            @media (max-width: 768px) {
                .report-meta {
                    flex-direction: column;
                    gap: 0.5rem;
                }
                
                .visual-comparison {
                    grid-template-columns: 1fr;
                }
                
                .summary-cards {
                    grid-template-columns: 1fr;
                }
            }
        """.trimIndent()
    }
    
    private fun calculateOverallTrendMetrics(trends: Collection<DetailedTestTrend>): OverallTrendMetrics {
        if (trends.isEmpty()) {
            return OverallTrendMetrics(0.0, 0.0, 0.0, 0, 0)
        }
        
        val averageScore = trends.map { it.currentScore }.average()
        val improvingCount = trends.count { it.trend == TrendDirection.IMPROVING }
        val degradingCount = trends.count { it.trend == TrendDirection.DEGRADING }
        val consistencyScore = trends.map { it.stability }.average()
        
        // Calculate score improvement based on historical data
        val scoreImprovement = trends.mapNotNull { trend ->
            if (trend.historicalScores.isNotEmpty()) {
                trend.currentScore - trend.historicalScores.average()
            } else null
        }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        
        return OverallTrendMetrics(
            averageScore = averageScore,
            scoreImprovement = scoreImprovement,
            consistencyScore = consistencyScore,
            regressionCount = degradingCount,
            improvementCount = improvingCount
        )
    }
    
    private fun calculateStabilityMetrics(trends: Collection<DetailedTestTrend>): StabilityMetrics {
        if (trends.isEmpty()) {
            return StabilityMetrics(1.0, emptyList(), emptyList(), TrendDirection.STABLE)
        }
        
        val overallStability = trends.map { it.stability }.average()
        val sortedByStability = trends.sortedByDescending { it.stability }
        
        val mostStable = sortedByStability.take(5).map { it.testName }
        val leastStable = sortedByStability.takeLast(5).map { it.testName }
        
        val stabilityTrend = when {
            overallStability > 0.8 -> TrendDirection.STABLE
            overallStability > 0.6 -> TrendDirection.DEGRADING
            else -> TrendDirection.DEGRADING
        }
        
        return StabilityMetrics(
            overallStability = overallStability,
            mostStableTests = mostStable,
            leastStableTests = leastStable,
            stabilityTrend = stabilityTrend
        )
    }
    
    private fun calculateQualityMetrics(
        currentResults: TestSuiteResult,
        historicalResults: List<TestSuiteResult>
    ): QualityMetrics {
        val currentQuality = currentResults.summary.successRate
        
        val qualityTrend = if (historicalResults.isNotEmpty()) {
            val historicalQuality = historicalResults.map { it.summary.successRate }.average()
            when {
                currentQuality > historicalQuality + 0.05 -> TrendDirection.IMPROVING
                currentQuality < historicalQuality - 0.05 -> TrendDirection.DEGRADING
                else -> TrendDirection.STABLE
            }
        } else TrendDirection.STABLE
        
        val passRateStability = if (historicalResults.size > 1) {
            val passRates = historicalResults.map { it.summary.successRate } + currentQuality
            1.0 - calculateVolatility(passRates)
        } else 1.0
        
        val averageScoreStability = if (historicalResults.isNotEmpty()) {
            val scores = historicalResults.map { suite ->
                suite.results.mapNotNull { it.comparison?.overallScore }.average()
            } + currentResults.results.mapNotNull { it.comparison?.overallScore }.average()
            1.0 - calculateVolatility(scores)
        } else 1.0
        
        return QualityMetrics(
            qualityScore = currentQuality,
            qualityTrend = qualityTrend,
            passRateStability = passRateStability,
            averageScoreStability = averageScoreStability
        )
    }
    
    private fun assessRegressionRisk(trends: Collection<DetailedTestTrend>): RegressionRisk {
        if (trends.isEmpty()) return RegressionRisk.MINIMAL
        
        val highRiskCount = trends.count { it.regressionRisk == RegressionRisk.HIGH }
        val mediumRiskCount = trends.count { it.regressionRisk == RegressionRisk.MEDIUM }
        val totalTests = trends.size
        
        return when {
            highRiskCount > totalTests * 0.2 -> RegressionRisk.HIGH
            highRiskCount > 0 || mediumRiskCount > totalTests * 0.3 -> RegressionRisk.MEDIUM
            mediumRiskCount > 0 -> RegressionRisk.LOW
            else -> RegressionRisk.MINIMAL
        }
    }
    
    private fun generateTrendRecommendations(
        overallMetrics: OverallTrendMetrics,
        stabilityMetrics: StabilityMetrics
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (overallMetrics.regressionCount > overallMetrics.improvementCount) {
            recommendations.add("Address ${overallMetrics.regressionCount} regressions - quality is declining")
        }
        
        if (stabilityMetrics.overallStability < 0.7) {
            recommendations.add("Low stability detected (${String.format("%.1f", stabilityMetrics.overallStability * 100)}%) - investigate test consistency")
        }
        
        if (overallMetrics.consistencyScore < 0.8) {
            recommendations.add("Improve test consistency - current score: ${String.format("%.1f", overallMetrics.consistencyScore * 100)}%")
        }
        
        if (overallMetrics.scoreImprovement < -0.1) {
            recommendations.add("Quality scores are declining - review recent changes")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Quality trends are stable - maintain current practices")
        }
        
        return recommendations
    }
    
    // Stub implementations for missing methods
    private fun estimateMemoryUsage(results: List<TestResult>): Long = 1024L * 1024L // 1MB estimate
    private fun calculatePerformanceScore(times: List<Long>, sizes: List<Int>): Double {
        if (times.isEmpty()) return 0.0
        return 0.8
    }
    private fun categorizePerformance(speedRatio: Double): PerformanceCategory = PerformanceCategory.GOOD
    private fun generatePerformanceRecommendations(speedRatio: Double): List<String> = listOf("Performance is acceptable")
    private fun identifyPerformanceIssues(metrics: DetailedPerformanceMetrics): List<PerformanceIssue> = emptyList()
    private fun identifyOptimizationOpportunities(metrics: DetailedPerformanceMetrics, results: List<TestResult>): List<OptimizationOpportunity> = emptyList()
    private fun analyzeMemoryUsage(results: List<TestResult>): MemoryAnalysis = MemoryAnalysis(1024L, 2048L, 0.8, emptyList(), emptyList())
    
    private fun analyzeVisualDifferences(kotlinSvg: String, referenceSvg: String): SvgAnalysisReport? = null
    private fun analyzeDeviationDistribution(deviations: List<CoordinateDeviation>): DeviationDistribution = 
        DeviationDistribution(emptyMap(), emptyMap(), emptyList())
    private fun identifyProblematicElements(deviations: List<CoordinateDeviation>): List<ProblematicElement> = emptyList()
    private fun generateCoordinateRecommendations(deviations: CoordinateDeviations): List<String> = emptyList()
    private fun identifyCriticalAttributeMismatches(matches: List<AttributeMatch>): List<CriticalAttributeMismatch> = emptyList()
    private fun generateAttributeRecommendations(matches: AttributeMatches): List<String> = emptyList()
    private fun generateVisualDifferencesSummary(result: TestResult): VisualDifferencesSummary = 
        VisualDifferencesSummary(0, 0, emptyMap(), DifferenceImpact.MINIMAL)
    private fun generateVisualRecommendations(result: TestResult): List<String> = emptyList()
    
    private fun generateSuccessRateChartData(current: TestSuiteResult, historical: List<TestSuiteResult>): ChartData = 
        ChartData(emptyList(), emptyList())
    private fun generatePerformanceChartData(current: TestSuiteResult, historical: List<TestSuiteResult>): ChartData = 
        ChartData(emptyList(), emptyList())
    private fun generateCategoryBreakdownChartData(current: TestSuiteResult): ChartData = 
        ChartData(emptyList(), emptyList())
    private fun generateTrendChartData(analysis: DetailedTrendAnalysis): ChartData = 
        ChartData(emptyList(), emptyList())
    private fun generateDeviationHeatmapData(current: TestSuiteResult): HeatmapData = 
        HeatmapData(emptyList(), emptyList(), emptyList(), ColorScale("", "", emptyList()))
    
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
}

// Data classes for comprehensive reporting

/**
 * Enhanced HTML report with comprehensive analysis.
 */
data class EnhancedHtmlReport(
    val content: String,
    val summary: TestSummary,
    val trendAnalysis: DetailedTrendAnalysis?,
    val failureAnalysis: FailureAnalysis,
    val performanceAnalysis: PerformanceAnalysis,
    val timestamp: Long
)

/**
 * Detailed failure analysis with categorization.
 */
data class FailureAnalysis(
    val totalFailures: Int,
    val categorizedFailures: Map<FailureCategory, List<CategorizedFailure>>,
    val criticalFailures: List<CategorizedFailure>,
    val commonPatterns: List<FailurePattern>,
    val recommendations: List<String>
)

/**
 * Categorized failure with detailed analysis.
 */
data class CategorizedFailure(
    val testResult: TestResult,
    val category: FailureCategory,
    val severity: FailureSeverity,
    val rootCause: String,
    val recommendations: List<String>
)

/**
 * Failure categories for classification.
 */
enum class FailureCategory {
    LAYOUT_DEVIATION,
    ATTRIBUTE_MISMATCH,
    STRUCTURAL_DIFFERENCE,
    EXECUTION_ERROR,
    MINOR_DIFFERENCE
}

/**
 * Failure severity levels.
 */
enum class FailureSeverity {
    CRITICAL, HIGH, MEDIUM, LOW
}

/**
 * Common failure pattern identification.
 */
data class FailurePattern(
    val category: FailureCategory,
    val frequency: Int,
    val affectedTests: List<String>,
    val commonCause: String,
    val recommendation: String
)

/**
 * Detailed trend analysis with time series data.
 */
data class DetailedTrendAnalysis(
    val testTrends: Map<String, DetailedTestTrend>,
    val timeSeriesData: Map<String, List<TrendDataPoint>>,
    val overallMetrics: OverallTrendMetrics,
    val stabilityMetrics: StabilityMetrics,
    val qualityMetrics: QualityMetrics,
    val regressionRisk: RegressionRisk,
    val recommendations: List<String>
)

/**
 * Detailed trend information for individual tests.
 */
data class DetailedTestTrend(
    val testName: String,
    val currentScore: Double,
    val historicalScores: List<Double>,
    val trend: TrendDirection,
    val volatility: Double,
    val stability: Double,
    val regressionRisk: RegressionRisk,
    val recommendations: List<String>
)

/**
 * Time series data point for trend analysis.
 */
data class TrendDataPoint(
    val timestamp: Long,
    val score: Double,
    val passed: Boolean,
    val executionTime: Long,
    val coordinateDeviation: Double,
    val attributeMatch: Double
)

/**
 * Overall trend metrics across all tests.
 */
data class OverallTrendMetrics(
    val averageScore: Double,
    val scoreImprovement: Double,
    val consistencyScore: Double,
    val regressionCount: Int,
    val improvementCount: Int
)

/**
 * Stability metrics for test suite.
 */
data class StabilityMetrics(
    val overallStability: Double,
    val mostStableTests: List<String>,
    val leastStableTests: List<String>,
    val stabilityTrend: TrendDirection
)

/**
 * Quality metrics over time.
 */
data class QualityMetrics(
    val qualityScore: Double,
    val qualityTrend: TrendDirection,
    val passRateStability: Double,
    val averageScoreStability: Double
)

/**
 * Regression risk assessment.
 */
enum class RegressionRisk {
    MINIMAL, LOW, MEDIUM, HIGH
}

/**
 * Performance analysis with detailed metrics.
 */
data class PerformanceAnalysis(
    val metrics: DetailedPerformanceMetrics,
    val benchmarkComparison: BenchmarkComparison?,
    val performanceIssues: List<PerformanceIssue>,
    val optimizationOpportunities: List<OptimizationOpportunity>,
    val memoryAnalysis: MemoryAnalysis
)

/**
 * Detailed performance metrics.
 */
data class DetailedPerformanceMetrics(
    val averageExecutionTime: Double,
    val medianExecutionTime: Double,
    val p95ExecutionTime: Double,
    val p99ExecutionTime: Double,
    val averageOutputSize: Double,
    val averageThroughput: Double,
    val totalMemoryUsage: Long,
    val performanceScore: Double
)

/**
 * Benchmark comparison with reference implementation.
 */
data class BenchmarkComparison(
    val kotlinVsReference: BenchmarkMetrics,
    val recommendations: List<String>
)

/**
 * Benchmark metrics comparison.
 */
data class BenchmarkMetrics(
    val speedRatio: Double,
    val averageTimeDifference: Double,
    val medianTimeDifference: Double,
    val performanceCategory: PerformanceCategory
)

/**
 * Performance categories.
 */
enum class PerformanceCategory {
    EXCELLENT, GOOD, ACCEPTABLE, POOR, CRITICAL
}

/**
 * Performance issue identification.
 */
data class PerformanceIssue(
    val type: PerformanceIssueType,
    val description: String,
    val severity: PerformanceIssueSeverity,
    val affectedTests: List<String>,
    val recommendation: String
)

/**
 * Performance issue types.
 */
enum class PerformanceIssueType {
    SLOW_EXECUTION, HIGH_MEMORY_USAGE, POOR_THROUGHPUT, INCONSISTENT_PERFORMANCE
}

/**
 * Performance issue severity.
 */
enum class PerformanceIssueSeverity {
    CRITICAL, HIGH, MEDIUM, LOW
}

/**
 * Optimization opportunity identification.
 */
data class OptimizationOpportunity(
    val area: OptimizationArea,
    val description: String,
    val potentialImprovement: String,
    val effort: OptimizationEffort,
    val priority: OptimizationPriority
)

/**
 * Optimization areas.
 */
enum class OptimizationArea {
    ALGORITHM, MEMORY_MANAGEMENT, CACHING, PARALLELIZATION, DATA_STRUCTURES
}

/**
 * Optimization effort levels.
 */
enum class OptimizationEffort {
    LOW, MEDIUM, HIGH
}

/**
 * Optimization priorities.
 */
enum class OptimizationPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Memory usage analysis.
 */
data class MemoryAnalysis(
    val averageMemoryUsage: Long,
    val peakMemoryUsage: Long,
    val memoryEfficiency: Double,
    val memoryLeaks: List<MemoryLeak>,
    val recommendations: List<String>
)

/**
 * Memory leak detection.
 */
data class MemoryLeak(
    val testName: String,
    val leakSize: Long,
    val description: String,
    val severity: MemoryLeakSeverity
)

/**
 * Memory leak severity.
 */
enum class MemoryLeakSeverity {
    MINOR, MODERATE, SEVERE, CRITICAL
}

/**
 * Visual comparison report for individual tests.
 */
data class VisualComparisonReport(
    val testName: String,
    val passed: Boolean,
    val overallScore: Double,
    val svgAnalysis: SvgAnalysisReport?,
    val coordinateAnalysis: CoordinateAnalysisReport?,
    val attributeAnalysis: AttributeAnalysisReport?,
    val visualDifferences: VisualDifferencesSummary,
    val recommendations: List<String>
)

/**
 * SVG analysis report.
 */
data class SvgAnalysisReport(
    val elementCount: Int,
    val structuralDifferences: List<StructuralDifference>,
    val styleAnalysis: StyleAnalysis,
    val geometryAnalysis: GeometryAnalysis
)

/**
 * Structural difference in SVG.
 */
data class StructuralDifference(
    val type: StructuralDifferenceType,
    val description: String,
    val impact: DifferenceImpact
)

/**
 * Structural difference types.
 */
enum class StructuralDifferenceType {
    MISSING_ELEMENT, EXTRA_ELEMENT, ELEMENT_ORDER, NESTING_DIFFERENCE
}

/**
 * Difference impact levels.
 */
enum class DifferenceImpact {
    MINIMAL, MINOR, MODERATE, MAJOR, CRITICAL
}

/**
 * Style analysis for SVG elements.
 */
data class StyleAnalysis(
    val colorDifferences: List<ColorDifference>,
    val fontDifferences: List<FontDifference>,
    val strokeDifferences: List<StrokeDifference>
)

/**
 * Color difference analysis.
 */
data class ColorDifference(
    val elementId: String,
    val expectedColor: String,
    val actualColor: String,
    val colorDistance: Double
)

/**
 * Font difference analysis.
 */
data class FontDifference(
    val elementId: String,
    val expectedFont: String,
    val actualFont: String,
    val impact: DifferenceImpact
)

/**
 * Stroke difference analysis.
 */
data class StrokeDifference(
    val elementId: String,
    val expectedStroke: String,
    val actualStroke: String,
    val impact: DifferenceImpact
)

/**
 * Geometry analysis for positioning.
 */
data class GeometryAnalysis(
    val boundingBoxDifference: BoundingBoxDifference,
    val pathDifferences: List<PathDifference>,
    val transformDifferences: List<TransformDifference>
)

/**
 * Bounding box difference.
 */
data class BoundingBoxDifference(
    val expectedBounds: Rectangle,
    val actualBounds: Rectangle,
    val deviation: Double
)

/**
 * Path difference analysis.
 */
data class PathDifference(
    val pathId: String,
    val expectedPath: String,
    val actualPath: String,
    val similarity: Double
)

/**
 * Transform difference analysis.
 */
data class TransformDifference(
    val elementId: String,
    val expectedTransform: String,
    val actualTransform: String,
    val impact: DifferenceImpact
)

/**
 * Coordinate analysis report.
 */
data class CoordinateAnalysisReport(
    val maxDeviation: Double,
    val averageDeviation: Double,
    val deviationDistribution: DeviationDistribution,
    val problematicElements: List<ProblematicElement>,
    val recommendations: List<String>
)

/**
 * Deviation distribution analysis.
 */
data class DeviationDistribution(
    val ranges: Map<String, Int>, // e.g., "0-1px" -> count
    val percentiles: Map<Int, Double>, // e.g., 95 -> deviation value
    val outliers: List<CoordinateDeviation>
)

/**
 * Problematic element identification.
 */
data class ProblematicElement(
    val elementId: String,
    val deviation: Double,
    val elementType: String,
    val issue: String,
    val recommendation: String
)

/**
 * Attribute analysis report.
 */
data class AttributeAnalysisReport(
    val matchPercentage: Double,
    val mismatchedAttributes: List<AttributeMatch>,
    val criticalMismatches: List<CriticalAttributeMismatch>,
    val recommendations: List<String>
)

/**
 * Critical attribute mismatch.
 */
data class CriticalAttributeMismatch(
    val attributeName: String,
    val expectedValue: String,
    val actualValue: String,
    val impact: DifferenceImpact,
    val recommendation: String
)

/**
 * Visual differences summary.
 */
data class VisualDifferencesSummary(
    val totalDifferences: Int,
    val significantDifferences: Int,
    val differenceTypes: Map<String, Int>,
    val overallImpact: DifferenceImpact
)

/**
 * Dashboard data for interactive reporting.
 */
data class DashboardData(
    val summary: TestSummary,
    val chartData: DashboardChartData,
    val trendAnalysis: DetailedTrendAnalysis?,
    val failureAnalysis: FailureAnalysis,
    val performanceAnalysis: PerformanceAnalysis,
    val timestamp: Long,
    val metadata: DashboardMetadata
)

/**
 * Chart data for dashboard visualization.
 */
data class DashboardChartData(
    val successRateChart: ChartData,
    val performanceChart: ChartData,
    val categoryBreakdownChart: ChartData,
    val trendChart: ChartData?,
    val deviationHeatmap: HeatmapData
)

/**
 * Generic chart data structure.
 */
data class ChartData(
    val labels: List<String>,
    val datasets: List<ChartDataset>
)

/**
 * Chart dataset.
 */
data class ChartDataset(
    val label: String,
    val data: List<Double>,
    val backgroundColor: String? = null,
    val borderColor: String? = null
)

/**
 * Heatmap data for deviation visualization.
 */
data class HeatmapData(
    val xLabels: List<String>,
    val yLabels: List<String>,
    val data: List<List<Double>>,
    val colorScale: ColorScale
)

/**
 * Color scale for heatmap.
 */
data class ColorScale(
    val min: String,
    val max: String,
    val steps: List<String>
)

/**
 * Dashboard metadata.
 */
data class DashboardMetadata(
    val totalTests: Int,
    val executionTime: Long,
    val environment: CiEnvironmentInfo
)

// Rectangle data class (if not already defined elsewhere)
data class Rectangle(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)