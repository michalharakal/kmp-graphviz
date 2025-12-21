package org.graphviz.kotlin.visual

/**
 * Provides CI/CD integration for visual regression testing.
 * Handles test execution in CI environments and result reporting.
 */
object CiCdIntegration {
    
    /**
     * Runs visual regression tests in CI environment.
     */
    fun runInCi(
        testCases: List<TestCase>,
        kotlinRenderer: (String) -> String,
        ciConfig: CiConfig = CiConfig.DEFAULT
    ): CiTestResult {
        val startTime = System.currentTimeMillis()
        
        // Check if we're in a CI environment
        val isCI = detectCiEnvironment()
        
        // Run the test suite
        val suiteResult = VisualTestAutomation.runTestSuite(testCases, kotlinRenderer, ciConfig.testConfig)
        
        // Generate reports
        val reports = generateReports(suiteResult, ciConfig)
        
        // Check for regressions if baseline exists
        val regressionReport = if (ciConfig.baselineResultsPath != null) {
            checkRegressions(suiteResult, ciConfig.baselineResultsPath)
        } else null
        
        val endTime = System.currentTimeMillis()
        
        return CiTestResult(
            suiteResult = suiteResult,
            reports = reports,
            regressionReport = regressionReport,
            isCI = isCI,
            totalTime = endTime - startTime,
            exitCode = if (suiteResult.summary.failedTests == 0 && regressionReport?.hasRegressions != true) 0 else 1
        )
    }
    
    /**
     * Generates GitHub Actions workflow for visual regression testing.
     */
    fun generateGitHubActionsWorkflow(): String {
        return """
            name: Visual Regression Tests
            
            on:
              push:
                branches: [ main, develop ]
              pull_request:
                branches: [ main ]
            
            jobs:
              visual-regression:
                runs-on: ubuntu-latest
                
                steps:
                - uses: actions/checkout@v3
                
                - name: Set up JDK 11
                  uses: actions/setup-java@v3
                  with:
                    java-version: '11'
                    distribution: 'temurin'
                
                - name: Install Graphviz
                  run: |
                    sudo apt-get update
                    sudo apt-get install -y graphviz
                
                - name: Cache Gradle packages
                  uses: actions/cache@v3
                  with:
                    path: |
                      ~/.gradle/caches
                      ~/.gradle/wrapper
                    key: ${'$'}{{ runner.os }}-gradle-${'$'}{{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
                    restore-keys: |
                      ${'$'}{{ runner.os }}-gradle-
                
                - name: Run visual regression tests
                  run: ./gradlew visualRegressionTest
                
                - name: Upload test reports
                  uses: actions/upload-artifact@v3
                  if: always()
                  with:
                    name: visual-regression-reports
                    path: |
                      build/reports/visual-regression/
                      build/test-results/visual-regression/
                
                - name: Comment PR with results
                  if: github.event_name == 'pull_request'
                  uses: actions/github-script@v6
                  with:
                    script: |
                      const fs = require('fs');
                      const path = 'build/reports/visual-regression/summary.json';
                      if (fs.existsSync(path)) {
                        const summary = JSON.parse(fs.readFileSync(path, 'utf8'));
                        const comment = `## Visual Regression Test Results
                        
                        - **Total Tests**: ${'$'}{summary.totalTests}
                        - **Passed**: ${'$'}{summary.passedTests}
                        - **Failed**: ${'$'}{summary.failedTests}
                        - **Success Rate**: ${'$'}{(summary.successRate * 100).toFixed(2)}%
                        
                        ${'$'}{summary.failedTests > 0 ? '❌ Some visual regression tests failed!' : '✅ All visual regression tests passed!'}`;
                        
                        github.rest.issues.createComment({
                          issue_number: context.issue.number,
                          owner: context.repo.owner,
                          repo: context.repo.repo,
                          body: comment
                        });
                      }
        """.trimIndent()
    }
    
    /**
     * Generates GitLab CI configuration for visual regression testing.
     */
    fun generateGitLabCiConfig(): String {
        return """
            stages:
              - test
              - report
            
            variables:
              GRADLE_OPTS: "-Dorg.gradle.daemon=false"
              GRADLE_USER_HOME: "${'$'}CI_PROJECT_DIR/.gradle"
            
            cache:
              paths:
                - .gradle/wrapper
                - .gradle/caches
            
            visual-regression-test:
              stage: test
              image: openjdk:11-jdk
              before_script:
                - apt-get update -qq && apt-get install -y -qq graphviz
                - chmod +x ./gradlew
              script:
                - ./gradlew visualRegressionTest
              artifacts:
                when: always
                reports:
                  junit: build/test-results/visual-regression/TEST-*.xml
                paths:
                  - build/reports/visual-regression/
                expire_in: 1 week
              only:
                - main
                - develop
                - merge_requests
            
            visual-regression-report:
              stage: report
              image: alpine:latest
              dependencies:
                - visual-regression-test
              script:
                - echo "Visual regression test results:"
                - cat build/reports/visual-regression/summary.json
              only:
                - main
                - develop
                - merge_requests
        """.trimIndent()
    }
    
    /**
     * Generates Jenkins pipeline for visual regression testing.
     */
    fun generateJenkinsPipeline(): String {
        return """
            pipeline {
                agent any
                
                tools {
                    jdk 'JDK-11'
                }
                
                stages {
                    stage('Checkout') {
                        steps {
                            checkout scm
                        }
                    }
                    
                    stage('Install Dependencies') {
                        steps {
                            sh '''
                                # Install Graphviz
                                if command -v apt-get >/dev/null 2>&1; then
                                    sudo apt-get update
                                    sudo apt-get install -y graphviz
                                elif command -v yum >/dev/null 2>&1; then
                                    sudo yum install -y graphviz
                                elif command -v brew >/dev/null 2>&1; then
                                    brew install graphviz
                                fi
                            '''
                        }
                    }
                    
                    stage('Run Visual Regression Tests') {
                        steps {
                            sh './gradlew visualRegressionTest'
                        }
                        post {
                            always {
                                publishTestResults testResultsPattern: 'build/test-results/visual-regression/TEST-*.xml'
                                publishHTML([
                                    allowMissing: false,
                                    alwaysLinkToLastBuild: true,
                                    keepAll: true,
                                    reportDir: 'build/reports/visual-regression',
                                    reportFiles: 'index.html',
                                    reportName: 'Visual Regression Report'
                                ])
                            }
                        }
                    }
                }
                
                post {
                    always {
                        archiveArtifacts artifacts: 'build/reports/visual-regression/**', fingerprint: true
                    }
                    failure {
                        emailext (
                            subject: "Visual Regression Tests Failed: ${'$'}{env.JOB_NAME} - ${'$'}{env.BUILD_NUMBER}",
                            body: "Visual regression tests failed. Check the build at ${'$'}{env.BUILD_URL}",
                            to: "${'$'}{env.CHANGE_AUTHOR_EMAIL}"
                        )
                    }
                }
            }
        """.trimIndent()
    }
    
    /**
     * Generates Gradle task for visual regression testing.
     */
    fun generateGradleTask(): String {
        return """
            task visualRegressionTest(type: Test) {
                description = 'Runs visual regression tests'
                group = 'verification'
                
                useJUnitPlatform {
                    includeTags 'visual-regression'
                }
                
                testClassesDirs = sourceSets.test.output.classesDirs
                classpath = sourceSets.test.runtimeClasspath
                
                // Configure test execution
                maxParallelForks = 1 // Visual tests should run sequentially
                forkEvery = 0 // Don't fork for each test
                
                // Set system properties for test configuration
                systemProperty 'visual.regression.enabled', 'true'
                systemProperty 'visual.regression.tolerance', project.findProperty('visualTolerance') ?: 'default'
                systemProperty 'visual.regression.baseline', project.findProperty('visualBaseline') ?: ''
                
                // Configure reports
                reports {
                    html.destination = file("${'$'}buildDir/reports/visual-regression")
                    junitXml.destination = file("${'$'}buildDir/test-results/visual-regression")
                }
                
                // Generate summary report
                doLast {
                    def summaryFile = file("${'$'}buildDir/reports/visual-regression/summary.json")
                    def testResults = [
                        totalTests: 0,
                        passedTests: 0,
                        failedTests: 0,
                        successRate: 0.0
                    ]
                    
                    // Parse test results and generate summary
                    fileTree("${'$'}buildDir/test-results/visual-regression").include("**/*.xml").each { file ->
                        def testsuite = new XmlSlurper().parse(file)
                        testResults.totalTests += testsuite.@tests.toInteger()
                        testResults.failedTests += testsuite.@failures.toInteger() + testsuite.@errors.toInteger()
                    }
                    
                    testResults.passedTests = testResults.totalTests - testResults.failedTests
                    testResults.successRate = testResults.totalTests > 0 ? 
                        testResults.passedTests / testResults.totalTests : 0.0
                    
                    summaryFile.text = groovy.json.JsonBuilder(testResults).toPrettyString()
                }
            }
            
            // Add visual regression test to check task
            check.dependsOn visualRegressionTest
        """.trimIndent()
    }
    
    // Private helper methods
    
    private fun detectCiEnvironment(): Boolean {
        val ciEnvVars = listOf(
            "CI", "CONTINUOUS_INTEGRATION", "BUILD_NUMBER", "JENKINS_URL",
            "GITHUB_ACTIONS", "GITLAB_CI", "TRAVIS", "CIRCLECI", "BUILDKITE"
        )
        
        return ciEnvVars.any { System.getenv(it) != null }
    }
    
    private fun generateReports(suiteResult: TestSuiteResult, config: CiConfig): CiReports {
        val htmlReport = if (config.generateHtmlReport) {
            VisualTestAutomation.generateHtmlReport(suiteResult)
        } else null
        
        val jsonReport = if (config.generateJsonReport) {
            VisualTestAutomation.generateJsonReport(suiteResult)
        } else null
        
        val junitReport = if (config.generateJunitReport) {
            generateJunitReport(suiteResult)
        } else null
        
        return CiReports(
            htmlReport = htmlReport,
            jsonReport = jsonReport,
            junitReport = junitReport
        )
    }
    
    private fun checkRegressions(suiteResult: TestSuiteResult, baselinePath: String): RegressionReport? {
        // In a real implementation, this would load baseline results from file
        // For now, return null to indicate no baseline available
        return null
    }
    
    private fun generateJunitReport(suiteResult: TestSuiteResult): String {
        val xml = StringBuilder()
        xml.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        xml.appendLine("<testsuite name=\"VisualRegressionTests\" tests=\"${suiteResult.summary.totalTests}\" failures=\"${suiteResult.summary.failedTests}\" time=\"${suiteResult.totalTime / 1000.0}\">")
        
        for (result in suiteResult.results) {
            xml.appendLine("  <testcase name=\"${result.testCase.name}\" classname=\"VisualRegressionTest\" time=\"${result.executionTime / 1000.0}\">")
            
            if (!result.passed) {
                xml.appendLine("    <failure message=\"Visual regression test failed\">")
                xml.appendLine("      Test: ${result.testCase.name}")
                xml.appendLine("      Description: ${result.testCase.description}")
                if (result.comparison != null) {
                    xml.appendLine("      Structural Similarity: ${result.comparison.structuralSimilarity.similarity}")
                    xml.appendLine("      Max Coordinate Deviation: ${result.comparison.coordinateDeviations.maxDeviation}")
                    xml.appendLine("      Attribute Match: ${result.comparison.attributeMatches.matchPercentage}")
                }
                if (result.error != null) {
                    xml.appendLine("      Error: ${result.error}")
                }
                xml.appendLine("    </failure>")
            }
            
            xml.appendLine("  </testcase>")
        }
        
        for (error in suiteResult.errors) {
            xml.appendLine("  <testcase name=\"${error.testCase.name}\" classname=\"VisualRegressionTest\">")
            xml.appendLine("    <error message=\"Test execution error\">")
            xml.appendLine("      ${error.message}")
            xml.appendLine("    </error>")
            xml.appendLine("  </testcase>")
        }
        
        xml.appendLine("</testsuite>")
        return xml.toString()
    }
}

// Data classes for CI/CD integration

data class CiConfig(
    val testConfig: TestSuiteConfig = TestSuiteConfig.DEFAULT,
    val generateHtmlReport: Boolean = true,
    val generateJsonReport: Boolean = true,
    val generateJunitReport: Boolean = true,
    val baselineResultsPath: String? = null,
    val failOnRegression: Boolean = true
) {
    companion object {
        val DEFAULT = CiConfig()
    }
}

data class CiTestResult(
    val suiteResult: TestSuiteResult,
    val reports: CiReports,
    val regressionReport: RegressionReport?,
    val isCI: Boolean,
    val totalTime: Long,
    val exitCode: Int
)

data class CiReports(
    val htmlReport: String?,
    val jsonReport: String?,
    val junitReport: String?
)