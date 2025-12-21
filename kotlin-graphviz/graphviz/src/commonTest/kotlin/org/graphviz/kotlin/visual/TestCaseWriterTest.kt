package org.graphviz.kotlin.visual

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Tests for the TestCaseWriter.
 * **Feature: kotlin-multiplatform-port, Task 14.1: Create comprehensive test case generation**
 */
class TestCaseWriterTest : FunSpec({
    
    test("should write test cases to files") {
        val testCases = listOf(
            TestCase(
                name = "test_box_shape",
                description = "Test box shape",
                dotContent = "digraph test { node1 [shape=box]; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("node1"),
                tags = setOf("shape", "box")
            ),
            TestCase(
                name = "test_dashed_edge",
                description = "Test dashed edge",
                dotContent = "digraph test { node1 -> node2 [style=dashed]; }",
                category = TestCategory.EDGE_STYLES,
                expectedElements = listOf("node1", "node2"),
                tags = setOf("style", "dashed")
            )
        )
        
        val testCaseFiles = TestCaseWriter.writeTestCasesToFiles(testCases, "test-output")
        
        testCaseFiles.size shouldBe 2
        
        val boxShapeFile = testCaseFiles.find { it.testCase.name == "test_box_shape" }!!
        boxShapeFile.filePath shouldBe "test-output/node_shapes/test_box_shape.dot"
        boxShapeFile.content shouldContain "shape=box"
        
        val dashedEdgeFile = testCaseFiles.find { it.testCase.name == "test_dashed_edge" }!!
        dashedEdgeFile.filePath shouldBe "test-output/edge_styles/test_dashed_edge.dot"
        dashedEdgeFile.content shouldContain "style=dashed"
    }
    
    test("should generate test suite manifest") {
        val testCases = listOf(
            TestCase(
                name = "test1",
                description = "First test",
                dotContent = "digraph test1 { a; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("a"),
                tags = setOf("tag1")
            ),
            TestCase(
                name = "test2",
                description = "Second test",
                dotContent = "digraph test2 { b; }",
                category = TestCategory.EDGE_STYLES,
                expectedElements = listOf("b"),
                tags = setOf("tag2")
            ),
            TestCase(
                name = "test3",
                description = "Third test",
                dotContent = "digraph test3 { c; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("c"),
                tags = setOf("tag3")
            )
        )
        
        val manifest = TestCaseWriter.generateTestSuiteManifest(testCases)
        
        manifest shouldContain "Visual Regression Test Suite Manifest"
        manifest shouldContain "Total test cases: 3"
        manifest shouldContain "NODE_SHAPES: 2 tests"
        manifest shouldContain "EDGE_STYLES: 1 tests"
        manifest shouldContain "**test1**: First test"
        manifest shouldContain "**test2**: Second test"
        manifest shouldContain "**test3**: Third test"
        manifest shouldContain "Tags: tag1"
        manifest shouldContain "Expected elements: a"
    }
    
    test("should generate test execution script") {
        val testCaseFiles = listOf(
            TestCaseFile(
                testCase = TestCase(
                    name = "test1",
                    description = "Test 1",
                    dotContent = "digraph test1 { a; }",
                    category = TestCategory.NODE_SHAPES,
                    expectedElements = listOf("a")
                ),
                filePath = "test-cases/node_shapes/test1.dot",
                content = "digraph test1 { a; }"
            ),
            TestCaseFile(
                testCase = TestCase(
                    name = "test2",
                    description = "Test 2",
                    dotContent = "digraph test2 { b; }",
                    category = TestCategory.EDGE_STYLES,
                    expectedElements = listOf("b")
                ),
                filePath = "test-cases/edge_styles/test2.dot",
                content = "digraph test2 { b; }"
            )
        )
        
        val script = TestCaseWriter.generateTestExecutionScript(testCaseFiles)
        
        script shouldContain "#!/bin/bash"
        script shouldContain "Visual Regression Test Execution Script"
        script shouldContain "KOTLIN_OUTPUT_DIR=\"kotlin-outputs\""
        script shouldContain "REFERENCE_OUTPUT_DIR=\"reference-outputs\""
        script shouldContain "COMPARISON_DIR=\"comparisons\""
        script shouldContain "Processing test: test1"
        script shouldContain "Processing test: test2"
        script shouldContain "kotlin-graphviz-cli"
        script shouldContain "dot -Tsvg"
        script shouldContain "visual-compare"
    }
    
    test("should generate test report template") {
        val reportTemplate = TestCaseWriter.generateTestReportTemplate()
        
        reportTemplate shouldContain "Visual Regression Test Report"
        reportTemplate shouldContain "Executive Summary"
        reportTemplate shouldContain "Test Results by Category"
        reportTemplate shouldContain "Node Shapes"
        reportTemplate shouldContain "Edge Styles"
        reportTemplate shouldContain "Arrowheads"
        reportTemplate shouldContain "Text Rendering"
        reportTemplate shouldContain "Complex Layouts"
        reportTemplate shouldContain "Pathological Cases"
        reportTemplate shouldContain "Detailed Results"
        reportTemplate shouldContain "Performance Metrics"
        reportTemplate shouldContain "Visual Differences"
        reportTemplate shouldContain "Recommendations"
        reportTemplate shouldContain "[TO_BE_FILLED]"
    }
    
    test("should organize files by category") {
        val testCases = TestCategory.values().map { category ->
            TestCase(
                name = "test_${category.name.lowercase()}",
                description = "Test for $category",
                dotContent = "digraph test { a; }",
                category = category,
                expectedElements = listOf("a")
            )
        }
        
        val testCaseFiles = TestCaseWriter.writeTestCasesToFiles(testCases, "output")
        
        testCaseFiles.shouldNotBeEmpty()
        
        // Verify each category gets its own directory
        val directories = testCaseFiles.map { it.filePath.substringBeforeLast("/") }
        directories.shouldContain("output/node_shapes")
        directories.shouldContain("output/edge_styles")
        directories.shouldContain("output/arrowheads")
        directories.shouldContain("output/text_rendering")
        directories.shouldContain("output/complex_layouts")
        directories.shouldContain("output/pathological")
    }
})