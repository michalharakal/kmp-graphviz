package org.graphviz.kotlin.visual

/**
 * Demo script showing how to use the test case generation system.
 * This demonstrates the comprehensive test case generation for visual regression testing.
 */
fun main() {
    println("=== Visual Regression Test Case Generation Demo ===\n")
    
    // Generate all test cases
    val allTestCases = TestCaseGenerator.generateAllTestCases()
    
    println("Total test cases generated: ${allTestCases.size}\n")
    
    // Show breakdown by category
    val categoryCounts = allTestCases.groupBy { it.category }.mapValues { it.value.size }
    println("Test cases by category:")
    for ((category, count) in categoryCounts) {
        println("  - ${category.name}: $count tests")
    }
    println()
    
    // Show sample test cases from each category
    println("Sample test cases:")
    for (category in TestCategory.values()) {
        val sampleTest = allTestCases.find { it.category == category }
        if (sampleTest != null) {
            println("\n${category.name}:")
            println("  Name: ${sampleTest.name}")
            println("  Description: ${sampleTest.description}")
            println("  Tags: ${sampleTest.tags.joinToString(", ")}")
            println("  Expected elements: ${sampleTest.expectedElements.joinToString(", ")}")
            println("  DOT content preview:")
            println(sampleTest.dotContent.lines().take(5).joinToString("\n") { "    $it" })
        }
    }
    
    // Generate test case files
    println("\n\n=== Generating Test Case Files ===\n")
    val testCaseFiles = TestCaseWriter.writeTestCasesToFiles(allTestCases, "visual-regression-tests")
    println("Generated ${testCaseFiles.size} test case files")
    
    // Show file organization
    val filesByCategory = testCaseFiles.groupBy { it.testCase.category }
    println("\nFiles by category:")
    for ((category, files) in filesByCategory) {
        println("  ${category.name}: ${files.size} files in ${files.first().filePath.substringBeforeLast("/")}")
    }
    
    // Generate manifest
    println("\n\n=== Generating Test Suite Manifest ===\n")
    val manifest = TestCaseWriter.generateTestSuiteManifest(allTestCases)
    println("Manifest preview (first 20 lines):")
    println(manifest.lines().take(20).joinToString("\n"))
    
    // Generate execution script
    println("\n\n=== Generating Test Execution Script ===\n")
    val script = TestCaseWriter.generateTestExecutionScript(testCaseFiles.take(3)) // Just first 3 for demo
    println("Script preview (first 30 lines):")
    println(script.lines().take(30).joinToString("\n"))
    
    println("\n\n=== Demo Complete ===")
    println("The test case generation system is ready for visual regression testing!")
}
