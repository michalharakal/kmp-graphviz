package org.graphviz.kotlin.visual

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Tests for the TestCaseGenerator.
 * **Feature: kotlin-multiplatform-port, Task 14.1: Create comprehensive test case generation**
 */
class TestCaseGeneratorTest : FunSpec({
    
    test("should generate node shape test cases") {
        val testCases = TestCaseGenerator.generateNodeShapeTests()
        
        testCases.shouldNotBeEmpty()
        testCases.forEach { testCase ->
            testCase.category shouldBe TestCategory.NODE_SHAPES
            testCase.name shouldContain "node_shape_"
            testCase.dotContent shouldContain "shape="
            testCase.expectedElements shouldContain "node1"
        }
        
        // Verify some specific shapes are included
        val shapeNames = testCases.map { it.name }
        shapeNames shouldContain "node_shape_box"
        shapeNames shouldContain "node_shape_circle"
        shapeNames shouldContain "node_shape_diamond"
    }
    
    test("should generate edge style test cases") {
        val testCases = TestCaseGenerator.generateEdgeStyleTests()
        
        testCases.shouldNotBeEmpty()
        testCases.forEach { testCase ->
            testCase.category shouldBe TestCategory.EDGE_STYLES
            testCase.name shouldContain "edge_style_"
            testCase.dotContent shouldContain "style="
            testCase.expectedElements shouldContain "node1"
            testCase.expectedElements shouldContain "node2"
        }
        
        // Verify specific styles are included
        val styleNames = testCases.map { it.name }
        styleNames shouldContain "edge_style_solid"
        styleNames shouldContain "edge_style_dashed"
        styleNames shouldContain "edge_style_dotted"
    }
    
    test("should generate arrowhead test cases") {
        val testCases = TestCaseGenerator.generateArrowheadTests()
        
        testCases.shouldNotBeEmpty()
        testCases.forEach { testCase ->
            testCase.category shouldBe TestCategory.ARROWHEADS
            testCase.dotContent shouldContain "node1 -> node2"
            testCase.expectedElements shouldContain "node1"
            testCase.expectedElements shouldContain "node2"
        }
        
        // Verify both arrowhead and arrowtail tests are generated
        val arrowheadTests = testCases.filter { it.name.startsWith("arrowhead_") }
        val arrowtailTests = testCases.filter { it.name.startsWith("arrowtail_") }
        
        arrowheadTests.shouldNotBeEmpty()
        arrowtailTests.shouldNotBeEmpty()
        
        // Verify specific arrowheads are included
        val arrowheadNames = arrowheadTests.map { it.name }
        arrowheadNames shouldContain "arrowhead_normal"
        arrowheadNames shouldContain "arrowhead_diamond"
        arrowheadNames shouldContain "arrowhead_dot"
    }
    
    test("should generate text rendering test cases") {
        val testCases = TestCaseGenerator.generateTextRenderingTests()
        
        testCases.shouldNotBeEmpty()
        testCases.forEach { testCase ->
            testCase.category shouldBe TestCategory.TEXT_RENDERING
            testCase.name shouldContain "text_"
            testCase.dotContent shouldContain "label="
        }
        
        // Verify specific text tests are included
        val textNames = testCases.map { it.name }
        textNames shouldContain "text_simple_labels"
        textNames shouldContain "text_multiline_labels"
        textNames shouldContain "text_html_labels"
    }
    
    test("should generate complex layout test cases") {
        val testCases = TestCaseGenerator.generateComplexLayoutTests()
        
        testCases.shouldNotBeEmpty()
        testCases.forEach { testCase ->
            testCase.category shouldBe TestCategory.COMPLEX_LAYOUTS
            testCase.name shouldContain "layout_"
            testCase.expectedElements.shouldNotBeEmpty()
        }
        
        // Verify specific layout tests are included
        val layoutNames = testCases.map { it.name }
        layoutNames shouldContain "layout_simple_hierarchy"
        layoutNames shouldContain "layout_clusters"
        layoutNames shouldContain "layout_subgraphs"
    }
    
    test("should generate pathological test cases") {
        val testCases = TestCaseGenerator.generatePathologicalTests()
        
        testCases.shouldNotBeEmpty()
        testCases.forEach { testCase ->
            testCase.category shouldBe TestCategory.PATHOLOGICAL
            testCase.name shouldContain "pathological_"
        }
        
        // Verify specific pathological tests are included
        val pathologicalNames = testCases.map { it.name }
        pathologicalNames shouldContain "pathological_empty_graph"
        pathologicalNames shouldContain "pathological_single_node"
        pathologicalNames shouldContain "pathological_large_graph"
        
        // Verify empty graph has no expected elements
        val emptyGraphTest = testCases.find { it.name == "pathological_empty_graph" }
        emptyGraphTest shouldNotBe null
        emptyGraphTest!!.expectedElements shouldBe emptyList()
        
        // Verify large graph has many expected elements
        val largeGraphTest = testCases.find { it.name == "pathological_large_graph" }
        largeGraphTest shouldNotBe null
        largeGraphTest!!.expectedElements.size shouldBe 100
    }
    
    test("should generate all test cases") {
        val allTestCases = TestCaseGenerator.generateAllTestCases()
        
        allTestCases.shouldNotBeEmpty()
        
        // Verify all categories are represented
        val categories = allTestCases.map { it.category }.toSet()
        categories shouldContain TestCategory.NODE_SHAPES
        categories shouldContain TestCategory.EDGE_STYLES
        categories shouldContain TestCategory.ARROWHEADS
        categories shouldContain TestCategory.TEXT_RENDERING
        categories shouldContain TestCategory.COMPLEX_LAYOUTS
        categories shouldContain TestCategory.PATHOLOGICAL
        
        // Verify total count is reasonable (should be hundreds of tests)
        allTestCases.size shouldBe (
            TestCaseGenerator.generateNodeShapeTests().size +
            TestCaseGenerator.generateEdgeStyleTests().size +
            TestCaseGenerator.generateArrowheadTests().size +
            TestCaseGenerator.generateTextRenderingTests().size +
            TestCaseGenerator.generateComplexLayoutTests().size +
            TestCaseGenerator.generatePathologicalTests().size
        )
    }
    
    test("should generate valid DOT content") {
        val testCases = TestCaseGenerator.generateAllTestCases()
        
        testCases.forEach { testCase ->
            // Basic DOT syntax validation
            testCase.dotContent shouldContain "digraph"
            testCase.dotContent shouldContain "{"
            testCase.dotContent shouldContain "}"
            
            // Verify DOT content is not empty
            testCase.dotContent.trim().shouldNotBe("")
        }
    }
    
    test("should include appropriate tags") {
        val nodeShapeTests = TestCaseGenerator.generateNodeShapeTests()
        nodeShapeTests.forEach { testCase ->
            testCase.tags shouldContain "shape"
        }
        
        val edgeStyleTests = TestCaseGenerator.generateEdgeStyleTests()
        edgeStyleTests.forEach { testCase ->
            testCase.tags shouldContain "style"
        }
        
        val arrowheadTests = TestCaseGenerator.generateArrowheadTests()
        arrowheadTests.forEach { testCase ->
            testCase.tags.any { it == "arrowhead" || it == "arrowtail" } shouldBe true
        }
    }
})