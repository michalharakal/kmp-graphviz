package org.graphviz.kotlin.visual

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Comprehensive tests for test case generation covering all requirements.
 * **Feature: kotlin-multiplatform-port, Task 14.1: Create comprehensive test case generation**
 */
class ComprehensiveTestCaseGenerationTest : FunSpec({
    
    test("should generate comprehensive node shape test coverage") {
        val testCases = TestCaseGenerator.generateNodeShapeTests()
        
        testCases.shouldNotBeEmpty()
        testCases.size shouldBeGreaterThan 50 // Should have many node shapes
        
        // Verify basic shapes are covered
        val shapeNames = testCases.map { it.name }
        shapeNames shouldContain "node_shape_box"
        shapeNames shouldContain "node_shape_circle"
        shapeNames shouldContain "node_shape_diamond"
        shapeNames shouldContain "node_shape_ellipse"
        shapeNames shouldContain "node_shape_triangle"
        
        // Verify biological shapes are covered
        shapeNames shouldContain "node_shape_promoter"
        shapeNames shouldContain "node_shape_cds"
        shapeNames shouldContain "node_shape_terminator"
        
        // Verify record shapes are covered
        shapeNames shouldContain "node_shape_record"
        shapeNames shouldContain "node_shape_Mrecord"
        
        // Verify all test cases have proper structure
        testCases.forEach { testCase ->
            testCase.category shouldBe TestCategory.NODE_SHAPES
            testCase.dotContent shouldContain "shape="
            testCase.expectedElements shouldContain "node1"
            testCase.tags shouldContain "shape"
        }
    }
    
    test("should generate comprehensive edge style test coverage") {
        val testCases = TestCaseGenerator.generateEdgeStyleTests()
        
        testCases.shouldNotBeEmpty()
        testCases.size shouldBeGreaterThan 15 // Should include styles, colors, and penwidths
        
        // Verify basic styles
        val styleTests = testCases.filter { it.name.startsWith("edge_style_") }
        styleTests.map { it.name } shouldContain "edge_style_solid"
        styleTests.map { it.name } shouldContain "edge_style_dashed"
        styleTests.map { it.name } shouldContain "edge_style_dotted"
        
        // Verify color tests
        val colorTests = testCases.filter { it.name.startsWith("edge_color_") }
        colorTests.shouldNotBeEmpty()
        colorTests.map { it.name } shouldContain "edge_color_red"
        colorTests.map { it.name } shouldContain "edge_color_blue"
        
        // Verify penwidth tests
        val penwidthTests = testCases.filter { it.name.startsWith("edge_penwidth_") }
        penwidthTests.shouldNotBeEmpty()
        penwidthTests.map { it.name } shouldContain "edge_penwidth_1"
        penwidthTests.map { it.name } shouldContain "edge_penwidth_5"
        
        // Verify all test cases have proper structure
        testCases.forEach { testCase ->
            testCase.category shouldBe TestCategory.EDGE_STYLES
            testCase.expectedElements shouldContain "node1"
            testCase.expectedElements shouldContain "node2"
        }
    }
    
    test("should generate comprehensive arrowhead test coverage") {
        val testCases = TestCaseGenerator.generateArrowheadTests()
        
        testCases.shouldNotBeEmpty()
        testCases.size shouldBeGreaterThan 40 // Should include arrowheads, arrowtails, sizes, and combinations
        
        // Verify arrowhead tests
        val arrowheadTests = testCases.filter { it.name.startsWith("arrowhead_") }
        arrowheadTests.shouldNotBeEmpty()
        arrowheadTests.map { it.name } shouldContain "arrowhead_normal"
        arrowheadTests.map { it.name } shouldContain "arrowhead_diamond"
        arrowheadTests.map { it.name } shouldContain "arrowhead_dot"
        
        // Verify arrowtail tests
        val arrowtailTests = testCases.filter { it.name.startsWith("arrowtail_") }
        arrowtailTests.shouldNotBeEmpty()
        arrowtailTests.map { it.name } shouldContain "arrowtail_normal"
        arrowtailTests.map { it.name } shouldContain "arrowtail_diamond"
        
        // Verify arrowsize tests
        val arrowsizeTests = testCases.filter { it.name.startsWith("arrowsize_") }
        arrowsizeTests.shouldNotBeEmpty()
        arrowsizeTests.map { it.name } shouldContain "arrowsize_1_0"
        arrowsizeTests.map { it.name } shouldContain "arrowsize_2_0"
        
        // Verify combined arrow tests
        val combinedTests = testCases.filter { it.name.startsWith("combined_") }
        combinedTests.shouldNotBeEmpty()
        combinedTests.map { it.name } shouldContain "combined_arrows_both_ends"
        
        // Verify all test cases have proper structure
        testCases.forEach { testCase ->
            testCase.category shouldBe TestCategory.ARROWHEADS
            testCase.dotContent shouldContain "node1 -> node2"
        }
    }
    
    test("should generate comprehensive text rendering test coverage") {
        val testCases = TestCaseGenerator.generateTextRenderingTests()
        
        testCases.shouldNotBeEmpty()
        testCases.size shouldBeGreaterThan 20 // Should include various text variations
        
        // Verify basic text tests
        val basicTests = testCases.filter { it.name.startsWith("text_") && !it.name.contains("fontsize") && !it.name.contains("fontfamily") && !it.name.contains("color") }
        basicTests.map { it.name } shouldContain "text_simple_labels"
        basicTests.map { it.name } shouldContain "text_multiline_labels"
        basicTests.map { it.name } shouldContain "text_html_labels"
        basicTests.map { it.name } shouldContain "text_unicode_characters"
        
        // Verify font size tests
        val fontsizeTests = testCases.filter { it.name.contains("fontsize") }
        fontsizeTests.shouldNotBeEmpty()
        fontsizeTests.map { it.name } shouldContain "text_fontsize_12"
        fontsizeTests.map { it.name } shouldContain "text_fontsize_24"
        
        // Verify font family tests
        val fontfamilyTests = testCases.filter { it.name.contains("fontfamily") }
        fontfamilyTests.shouldNotBeEmpty()
        fontfamilyTests.map { it.name } shouldContain "text_fontfamily_arial"
        fontfamilyTests.map { it.name } shouldContain "text_fontfamily_times"
        
        // Verify text color tests
        val colorTests = testCases.filter { it.name.contains("color") }
        colorTests.shouldNotBeEmpty()
        colorTests.map { it.name } shouldContain "text_color_red"
        colorTests.map { it.name } shouldContain "text_color_blue"
        
        // Verify all test cases have proper structure
        testCases.forEach { testCase ->
            testCase.category shouldBe TestCategory.TEXT_RENDERING
            testCase.dotContent shouldContain "label="
        }
    }
    
    test("should generate comprehensive complex layout test coverage") {
        val testCases = TestCaseGenerator.generateComplexLayoutTests()
        
        testCases.shouldNotBeEmpty()
        testCases.size shouldBeGreaterThan 10 // Should include various complex layouts
        
        // Verify basic layout tests
        val layoutNames = testCases.map { it.name }
        layoutNames shouldContain "layout_simple_hierarchy"
        layoutNames shouldContain "layout_clusters"
        layoutNames shouldContain "layout_subgraphs"
        layoutNames shouldContain "layout_cycles"
        
        // Verify advanced layout tests
        layoutNames shouldContain "layout_nested_clusters"
        layoutNames shouldContain "layout_cross_cluster_edges"
        layoutNames shouldContain "layout_rank_constraints"
        layoutNames shouldContain "layout_wide_graph"
        layoutNames shouldContain "layout_deep_hierarchy"
        
        // Verify special layout tests
        layoutNames shouldContain "layout_undirected_graph"
        layoutNames shouldContain "layout_mixed_graph_types"
        
        // Verify all test cases have proper structure
        testCases.forEach { testCase ->
            testCase.category shouldBe TestCategory.COMPLEX_LAYOUTS
            testCase.expectedElements.shouldNotBeEmpty()
        }
    }
    
    test("should generate comprehensive pathological test coverage") {
        val testCases = TestCaseGenerator.generatePathologicalTests()
        
        testCases.shouldNotBeEmpty()
        testCases.size shouldBeGreaterThan 15 // Should include many edge cases
        
        // Verify basic pathological tests
        val pathologicalNames = testCases.map { it.name }
        pathologicalNames shouldContain "pathological_empty_graph"
        pathologicalNames shouldContain "pathological_single_node"
        pathologicalNames shouldContain "pathological_disconnected"
        pathologicalNames shouldContain "pathological_self_loops"
        pathologicalNames shouldContain "pathological_multiple_edges"
        
        // Verify large graph tests
        pathologicalNames shouldContain "pathological_large_graph"
        pathologicalNames shouldContain "pathological_very_large_graph"
        pathologicalNames shouldContain "pathological_dense_graph"
        
        // Verify special structure tests
        pathologicalNames shouldContain "pathological_long_chain"
        pathologicalNames shouldContain "pathological_star_graph"
        pathologicalNames shouldContain "pathological_complete_graph"
        pathologicalNames shouldContain "pathological_bipartite_graph"
        
        // Verify edge case tests
        pathologicalNames shouldContain "pathological_many_self_loops"
        pathologicalNames shouldContain "pathological_parallel_edges"
        pathologicalNames shouldContain "pathological_deeply_nested_clusters"
        pathologicalNames shouldContain "pathological_many_disconnected_components"
        
        // Verify empty graph has no expected elements
        val emptyGraphTest = testCases.find { it.name == "pathological_empty_graph" }!!
        emptyGraphTest.expectedElements shouldBe emptyList()
        
        // Verify large graphs have many expected elements
        val largeGraphTest = testCases.find { it.name == "pathological_large_graph" }!!
        largeGraphTest.expectedElements.size shouldBe 100
        
        val veryLargeGraphTest = testCases.find { it.name == "pathological_very_large_graph" }!!
        veryLargeGraphTest.expectedElements.size shouldBe 500
        
        // Verify all test cases have proper structure
        testCases.forEach { testCase ->
            testCase.category shouldBe TestCategory.PATHOLOGICAL
        }
    }
    
    test("should generate comprehensive test suite with all categories") {
        val allTestCases = TestCaseGenerator.generateAllTestCases()
        
        allTestCases.shouldNotBeEmpty()
        allTestCases.size shouldBeGreaterThan 200 // Should be a comprehensive suite
        
        // Verify all categories are represented
        val categories = allTestCases.map { it.category }.toSet()
        categories shouldContain TestCategory.NODE_SHAPES
        categories shouldContain TestCategory.EDGE_STYLES
        categories shouldContain TestCategory.ARROWHEADS
        categories shouldContain TestCategory.TEXT_RENDERING
        categories shouldContain TestCategory.COMPLEX_LAYOUTS
        categories shouldContain TestCategory.PATHOLOGICAL
        
        // Verify reasonable distribution across categories
        val nodeShapeCount = allTestCases.count { it.category == TestCategory.NODE_SHAPES }
        val edgeStyleCount = allTestCases.count { it.category == TestCategory.EDGE_STYLES }
        val arrowheadCount = allTestCases.count { it.category == TestCategory.ARROWHEADS }
        val textRenderingCount = allTestCases.count { it.category == TestCategory.TEXT_RENDERING }
        val complexLayoutCount = allTestCases.count { it.category == TestCategory.COMPLEX_LAYOUTS }
        val pathologicalCount = allTestCases.count { it.category == TestCategory.PATHOLOGICAL }
        
        nodeShapeCount shouldBeGreaterThan 50
        edgeStyleCount shouldBeGreaterThan 15
        arrowheadCount shouldBeGreaterThan 40
        textRenderingCount shouldBeGreaterThan 20
        complexLayoutCount shouldBeGreaterThan 10
        pathologicalCount shouldBeGreaterThan 15
        
        // Verify all test cases have valid DOT content
        allTestCases.forEach { testCase ->
            testCase.dotContent shouldContain "{"
            testCase.dotContent shouldContain "}"
            testCase.dotContent.trim().shouldNotBe("")
        }
    }
    
    test("should generate test cases suitable for visual regression testing") {
        val allTestCases = TestCaseGenerator.generateAllTestCases()
        
        // Verify test cases have appropriate metadata for visual testing
        allTestCases.forEach { testCase ->
            // Each test case should have a unique name
            testCase.name.shouldNotBe("")
            
            // Each test case should have a description
            testCase.description.shouldNotBe("")
            
            // Each test case should have tags for categorization
            testCase.tags.shouldNotBeEmpty()
            
            // Each test case should specify expected elements for validation
            // (except for empty graph which legitimately has no elements)
            if (testCase.name != "pathological_empty_graph") {
                testCase.expectedElements.shouldNotBeEmpty()
            }
        }
        
        // Verify test cases cover edge cases that are important for visual consistency
        val testNames = allTestCases.map { it.name }
        
        // Should test various node shapes that might render differently
        testNames shouldContain "node_shape_record"
        testNames shouldContain "node_shape_cylinder"
        testNames shouldContain "node_shape_box3d"
        
        // Should test various arrow types that are visually distinct
        testNames shouldContain "arrowhead_diamond"
        testNames shouldContain "arrowhead_crow"
        testNames shouldContain "arrowhead_tee"
        
        // Should test text rendering edge cases
        testNames shouldContain "text_multiline_labels"
        testNames shouldContain "text_html_labels"
        testNames shouldContain "text_unicode_characters"
        
        // Should test complex layouts that stress the layout engine
        testNames shouldContain "layout_nested_clusters"
        testNames shouldContain "layout_cycles"
        testNames shouldContain "pathological_dense_graph"
    }
})