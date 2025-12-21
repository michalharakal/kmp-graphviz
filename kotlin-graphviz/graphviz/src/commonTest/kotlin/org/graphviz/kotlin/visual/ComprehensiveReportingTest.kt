package org.graphviz.kotlin.visual

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Tests for comprehensive test reporting functionality.
 * **Feature: kotlin-multiplatform-port, Task 14.4: Generate comprehensive test reports**
 */
class ComprehensiveReportingTest : FunSpec({
    
    test("should generate detailed HTML report with visual comparisons") {
        val testResult = createSampleTestResult()
        val suiteResult = TestSuiteResult(
            results = listOf(testResult),
            errors = emptyList(),
            totalTime = 1000L,
            summary = TestSummary(1, 1, 0, 1.0)
        )
        
        val htmlReport = VisualTestAutomation.generateHtmlReport(suiteResult)
        
        // Verify HTML structure
        htmlReport shouldContain "<!DOCTYPE html>"
        htmlReport shouldContain "<html>"
        htmlReport shouldContain "<head>"
        htmlReport shouldContain "<body>"
        htmlReport shouldContain "</html>"
        
        // Verify content sections
        htmlReport shouldContain "Visual Regression Test Report"
        htmlReport shouldContain "Summary"
        htmlReport shouldContain "Results by Category"
        htmlReport shouldContain "Detailed Results"
        
        // Verify test data
        htmlReport shouldContain "Total Tests: 1"
        htmlReport shouldContain "Passed: 1"
        htmlReport shouldContain "Failed: 0"
        htmlReport shouldContain "Success Rate: 100.00%"
        
        // Verify CSS styling
        htmlReport shouldContain "<style>"
        htmlReport shouldContain "font-family: Arial"
        htmlReport shouldContain ".test-result.passed"
        htmlReport shouldContain ".test-result.failed"
    }
    
    test("should generate comprehensive JSON report") {
        val testResult = createSampleTestResult()
        val suiteResult = TestSuiteResult(
            results = listOf(testResult),
            errors = emptyList(),
            totalTime = 1000L,
            summary = TestSummary(1, 1, 0, 1.0)
        )
        
        val jsonReport = VisualTestAutomation.generateJsonReport(suiteResult)
        
        // Verify JSON structure
        jsonReport shouldContain "{"
        jsonReport shouldContain "}"
        jsonReport shouldContain "\"summary\":"
        jsonReport shouldContain "\"results\":"
        
        // Verify summary data
        jsonReport shouldContain "\"totalTests\": 1"
        jsonReport shouldContain "\"passedTests\": 1"
        jsonReport shouldContain "\"failedTests\": 0"
        jsonReport shouldContain "\"successRate\": 1.0"
        
        // Verify test result data
        jsonReport shouldContain "\"testName\": \"sample_test\""
        jsonReport shouldContain "\"passed\": true"
        jsonReport shouldContain "\"executionTime\": 100"
    }
    
    test("should generate JUnit XML report") {
        val testResult = createSampleTestResult()
        val suiteResult = TestSuiteResult(
            results = listOf(testResult),
            errors = emptyList(),
            totalTime = 1000L,
            summary = TestSummary(1, 1, 0, 1.0)
        )
        
        val junitReport = CiCdIntegration.generateJunitReport(suiteResult)
        
        // Verify XML structure
        junitReport shouldContain "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        junitReport shouldContain "<testsuite"
        junitReport shouldContain "</testsuite>"
        junitReport shouldContain "<testcase"
        junitReport shouldContain "</testcase>"
        
        // Verify test suite attributes
        junitReport shouldContain "name=\"VisualRegressionTests\""
        junitReport shouldContain "tests=\"1\""
        junitReport shouldContain "failures=\"0\""
        
        // Verify test case attributes
        junitReport shouldContain "name=\"sample_test\""
        junitReport shouldContain "classname=\"VisualRegressionTest\""
        junitReport shouldContain "time=\"0.1\""
    }
    
    test("should generate performance benchmark report") {
        val testCases = listOf(createSampleTestCase())
        val mockRenderer: (String) -> String = { _ -> "<svg>mock</svg>" }
        
        val benchmark = VisualTestAutomation.benchmarkPerformance(testCases, mockRenderer)
        
        benchmark.kotlinAverageTime shouldNotBe null
        benchmark.referenceAverageTime shouldNotBe null
        benchmark.kotlinMedianTime shouldNotBe null
        benchmark.referenceMedianTime shouldNotBe null
        benchmark.speedRatio shouldNotBe null
    }
    
    test("should generate regression analysis report") {
        val currentResult = createSampleTestResult(passed = false, score = 0.7)
        val previousResult = createSampleTestResult(passed = true, score = 0.9)
        
        val currentSuite = TestSuiteResult(
            results = listOf(currentResult),
            errors = emptyList(),
            totalTime = 1000L,
            summary = TestSummary(1, 0, 1, 0.0)
        )
        
        val previousSuite = TestSuiteResult(
            results = listOf(previousResult),
            errors = emptyList(),
            totalTime = 1000L,
            summary = TestSummary(1, 1, 0, 1.0)
        )
        
        val regressionReport = VisualTestAutomation.checkForRegressions(currentSuite, previousSuite)
        
        regressionReport.hasRegressions shouldBe true
        regressionReport.regressions.size shouldBe 1
        regressionReport.regressions.first().testName shouldBe "sample_test"
        regressionReport.regressions.first().previousScore shouldBe 0.9
        regressionReport.regressions.first().currentScore shouldBe 0.7
    }
    
    test("should generate CI/CD integration reports") {
        val testCases = listOf(createSampleTestCase())
        val mockRenderer: (String) -> String = { _ -> "<svg>mock</svg>" }
        
        val ciResult = CiCdIntegration.runInCi(testCases, mockRenderer)
        
        ciResult.suiteResult shouldNotBe null
        ciResult.reports shouldNotBe null
        ciResult.totalTime shouldNotBe null
        ciResult.exitCode shouldBe 0 // Should pass
        
        // Verify reports are generated
        ciResult.reports.htmlReport shouldNotBe null
        ciResult.reports.jsonReport shouldNotBe null
        ciResult.reports.junitReport shouldNotBe null
    }
    
    test("should generate GitHub Actions workflow") {
        val workflow = CiCdIntegration.generateGitHubActionsWorkflow()
        
        workflow shouldContain "name: Visual Regression Tests"
        workflow shouldContain "on:"
        workflow shouldContain "jobs:"
        workflow shouldContain "visual-regression:"
        workflow shouldContain "runs-on: ubuntu-latest"
        workflow shouldContain "uses: actions/checkout@v3"
        workflow shouldContain "Install Graphviz"
        workflow shouldContain "./gradlew visualRegressionTest"
        workflow shouldContain "upload-artifact@v3"
    }
    
    test("should generate GitLab CI configuration") {
        val gitlabConfig = CiCdIntegration.generateGitLabCiConfig()
        
        gitlabConfig shouldContain "stages:"
        gitlabConfig shouldContain "- test"
        gitlabConfig shouldContain "- report"
        gitlabConfig shouldContain "visual-regression-test:"
        gitlabConfig shouldContain "stage: test"
        gitlabConfig shouldContain "image: openjdk:11-jdk"
        gitlabConfig shouldContain "apt-get install -y -qq graphviz"
        gitlabConfig shouldContain "./gradlew visualRegressionTest"
        gitlabConfig shouldContain "artifacts:"
    }
    
    test("should generate Jenkins pipeline") {
        val jenkinsPipeline = CiCdIntegration.generateJenkinsPipeline()
        
        jenkinsPipeline shouldContain "pipeline {"
        jenkinsPipeline shouldContain "agent any"
        jenkinsPipeline shouldContain "tools {"
        jenkinsPipeline shouldContain "jdk 'JDK-11'"
        jenkinsPipeline shouldContain "stages {"
        jenkinsPipeline shouldContain "stage('Checkout')"
        jenkinsPipeline shouldContain "stage('Install Dependencies')"
        jenkinsPipeline shouldContain "stage('Run Visual Regression Tests')"
        jenkinsPipeline shouldContain "./gradlew visualRegressionTest"
        jenkinsPipeline shouldContain "publishTestResults"
        jenkinsPipeline shouldContain "publishHTML"
    }
    
    test("should generate Gradle task configuration") {
        val gradleTask = CiCdIntegration.generateGradleTask()
        
        gradleTask shouldContain "task visualRegressionTest(type: Test)"
        gradleTask shouldContain "description = 'Runs visual regression tests'"
        gradleTask shouldContain "group = 'verification'"
        gradleTask shouldContain "useJUnitPlatform"
        gradleTask shouldContain "includeTags 'visual-regression'"
        gradleTask shouldContain "maxParallelForks = 1"
        gradleTask shouldContain "systemProperty 'visual.regression.enabled', 'true'"
        gradleTask shouldContain "reports {"
        gradleTask shouldContain "check.dependsOn visualRegressionTest"
    }
    
    // Helper methods
    
    private fun createSampleTestCase(): TestCase {
        return TestCase(
            name = "sample_test",
            description = "Sample test case",
            dotContent = "digraph test { A -> B; }",
            category = TestCategory.NODE_SHAPES,
            expectedElements = listOf("A", "B")
        )
    }
    
    private fun createSampleTestResult(passed: Boolean = true, score: Double = 1.0): TestResult {
        return TestResult(
            testCase = createSampleTestCase(),
            passed = passed,
            kotlinOutput = "<svg>kotlin output</svg>",
            referenceOutput = "<svg>reference output</svg>",
            comparison = ComparisonResult(
                structuralSimilarity = StructuralSimilarity(score, 1, emptyList(), emptyList()),
                coordinateDeviations = CoordinateDeviations(emptyList(), 0.0, 0.0, true),
                attributeMatches = AttributeMatches(emptyList(), 0, 0, score),
                overallScore = score,
                passed = passed
            ),
            executionTime = 100L,
            performanceMetrics = PerformanceMetrics(100L, 1000, 10.0)
        )
    }
})