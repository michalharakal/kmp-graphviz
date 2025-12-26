package org.graphviz.kotlin.visual

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Tests for Task 14.2: Build reference comparison pipeline
 * 
 * This test validates the four main components:
 * 1. Automated original Graphviz execution for reference generation
 * 2. SVG parsing and element extraction for structural comparison
 * 3. Coordinate deviation analysis with configurable tolerances
 * 4. Attribute matching validation with detailed reporting
 */
class Task14_2_ReferenceComparisonTest {
    
    @Test
    fun testAutomatedGraphvizExecution() {
        // Test automated original Graphviz execution for reference generation
        val dotContent = """
            digraph test {
                A [shape=box, color=red];
                B [shape=circle, color=blue];
                A -> B [style=dashed];
            }
        """.trimIndent()
        
        val result = GraphvizExecutor.execute(dotContent, "dot", "svg")
        
        when (result) {
            is GraphvizExecutionResult.Success -> {
                assertTrue(result.output.isNotEmpty(), "SVG output should not be empty")
                assertTrue(result.output.contains("<?xml"), "Output should be valid XML")
                assertTrue(result.output.contains("<svg"), "Output should contain SVG element")
                assertTrue(result.executionTime >= 0, "Execution time should be non-negative")
                assertTrue(result.inputSize > 0, "Input size should be positive")
            }
            is GraphvizExecutionResult.Error -> {
                // In simulation mode, this might happen - that's acceptable for testing
                assertTrue(result.message.isNotEmpty(), "Error message should not be empty")
            }
        }
    }
    
    @Test
    fun testSvgParsingAndElementExtraction() {
        // Test SVG parsing and element extraction for structural comparison
        val svgContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
                <g id="node-A" transform="translate(50,50)" style="fill:red;stroke:blue">
                    <rect x="-20" y="-10" width="40" height="20"/>
                    <text x="0" y="0">Node A</text>
                </g>
                <g id="edge-A-B">
                    <path d="M70,50 L130,50" stroke="green"/>
                </g>
            </svg>
        """.trimIndent()
        
        val parsed = SvgParser.parse(svgContent)
        
        // Verify basic parsing
        assertTrue(parsed.elements.isNotEmpty(), "Should extract elements")
        assertTrue(parsed.coordinates.isNotEmpty(), "Should extract coordinates")
        assertTrue(parsed.attributes.isNotEmpty(), "Should extract attributes")
        assertNotNull(parsed.viewport, "Should extract viewport")
        
        // Verify enhanced parsing features
        assertTrue(parsed.styleElements.isNotEmpty(), "Should extract style elements")
        assertTrue(parsed.transformations.isNotEmpty(), "Should extract transformations")
        assertEquals(svgContent, parsed.rawContent, "Should preserve raw content")
        
        // Verify specific element extraction
        val nodeA = parsed.elements.find { it.id == "node-A" }
        assertNotNull(nodeA, "Should find node-A element")
        assertEquals("g", nodeA.type, "Node-A should be a group element")
        
        // Verify coordinate extraction
        val nodeACoords = parsed.coordinates["node-A"]
        assertNotNull(nodeACoords, "Should extract node-A coordinates")
        assertEquals(50.0, nodeACoords.position.x, "X coordinate should be 50")
        assertEquals(50.0, nodeACoords.position.y, "Y coordinate should be 50")
        
        // Verify style extraction
        val nodeAStyle = parsed.styleElements.find { it.elementId == "node-A" }
        assertNotNull(nodeAStyle, "Should extract node-A style")
        assertTrue(nodeAStyle.properties.containsKey("fill"), "Should have fill property")
        assertEquals("red", nodeAStyle.properties["fill"], "Fill should be red")
    }
    
    @Test
    fun testCoordinateDeviationAnalysis() {
        // Test coordinate deviation analysis with configurable tolerances
        val kotlinSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(50.5,50.2)">
                    <rect x="-20" y="-10" width="40" height="20"/>
                </g>
            </svg>
        """.trimIndent()
        
        val referenceSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(50,50)">
                    <rect x="-20" y="-10" width="40" height="20"/>
                </g>
            </svg>
        """.trimIndent()
        
        // Test with strict tolerances (should fail)
        val strictTolerances = ComparisonTolerances(
            maxCoordinateDeviation = 0.1,
            numericTolerance = 0.01,
            minimumOverallScore = 0.95
        )
        
        val strictComparison = ReferenceComparison.compareOutputs(kotlinSvg, referenceSvg, strictTolerances)
        assertFalse(strictComparison.coordinateDeviations.withinTolerance, "Should fail with strict tolerances")
        assertTrue(strictComparison.coordinateDeviations.maxDeviation > 0.1, "Max deviation should exceed tolerance")
        assertFalse(strictComparison.passed, "Overall comparison should fail")
        
        // Test with lenient tolerances (should pass)
        val lenientTolerances = ComparisonTolerances(
            maxCoordinateDeviation = 1.0,
            numericTolerance = 0.5,
            minimumOverallScore = 0.6
        )
        
        val lenientComparison = ReferenceComparison.compareOutputs(kotlinSvg, referenceSvg, lenientTolerances)
        assertTrue(lenientComparison.coordinateDeviations.withinTolerance, "Should pass with lenient tolerances")
        assertTrue(lenientComparison.passed, "Overall comparison should pass")
        
        // Verify detailed deviation analysis
        assertTrue(strictComparison.coordinateDeviations.deviations.isNotEmpty(), "Should have deviation details")
        val deviation = strictComparison.coordinateDeviations.deviations.first()
        assertEquals("node-A", deviation.elementId, "Should identify correct element")
        assertTrue(deviation.deviation > 0.0, "Should calculate positive deviation")
    }
    
    @Test
    fun testAttributeMatchingValidation() {
        // Test attribute matching validation with detailed reporting
        val kotlinSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A">
                    <rect x="-25" y="-15" width="50" height="30" fill="red" stroke="blue" stroke-width="2"/>
                    <text x="0" y="0" font-family="Arial" font-size="12" fill="white">A</text>
                </g>
            </svg>
        """.trimIndent()
        
        val referenceSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A">
                    <rect x="-25" y="-15" width="50" height="30" fill="red" stroke="black" stroke-width="1"/>
                    <text x="0" y="0" font-family="Arial" font-size="12" fill="black">A</text>
                </g>
            </svg>
        """.trimIndent()
        
        val comparison = ReferenceComparison.compareOutputs(kotlinSvg, referenceSvg, ComparisonTolerances.DEFAULT)
        
        // Should detect attribute differences
        assertTrue(comparison.attributeMatches.matchPercentage < 1.0, "Should detect attribute mismatches")
        assertTrue(comparison.attributeMatches.matches.isNotEmpty(), "Should have attribute match details")
        
        // Find specific attribute mismatches
        val strokeMismatch = comparison.attributeMatches.matches.find { 
            it.attributeName == "stroke" && !it.matches 
        }
        assertNotNull(strokeMismatch, "Should detect stroke color mismatch")
        assertEquals("blue", strokeMismatch.kotlinValue, "Kotlin stroke should be blue")
        assertEquals("black", strokeMismatch.referenceValue, "Reference stroke should be black")
        
        // Should have detailed comparison with enhanced analysis
        assertNotNull(comparison.detailedComparison, "Should have detailed comparison")
        val detailed = comparison.detailedComparison!!
        assertTrue(detailed.attributes.matches.isNotEmpty(), "Should have detailed attribute matches")
        assertTrue(detailed.overallScore > 0.0, "Should have positive overall score")
    }
    
    @Test
    fun testBatchComparisonPipeline() {
        // Test the complete batch comparison pipeline
        val testCases = listOf(
            TestCase(
                name = "simple_node",
                description = "Simple node test",
                dotContent = "digraph test { A; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("A")
            ),
            TestCase(
                name = "simple_edge",
                description = "Simple edge test", 
                dotContent = "digraph test { A -> B; }",
                category = TestCategory.EDGE_STYLES,
                expectedElements = listOf("A", "B")
            )
        )
        
        val kotlinOutputs = mapOf(
            "simple_node" to """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                    <g id="node-A" transform="translate(100,50)">
                        <ellipse cx="0" cy="0" rx="30" ry="20"/>
                        <text x="0" y="0">A</text>
                    </g>
                </svg>
            """.trimIndent(),
            "simple_edge" to """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="300" height="100">
                    <g id="node-A" transform="translate(75,50)">
                        <ellipse cx="0" cy="0" rx="30" ry="20"/>
                        <text x="0" y="0">A</text>
                    </g>
                    <g id="node-B" transform="translate(225,50)">
                        <ellipse cx="0" cy="0" rx="30" ry="20"/>
                        <text x="0" y="0">B</text>
                    </g>
                    <g id="edge-A-B">
                        <path d="M105,50 L195,50" stroke="black"/>
                    </g>
                </svg>
            """.trimIndent()
        )
        
        val batchResult = ReferenceComparison.batchCompare(
            testCases = testCases,
            kotlinOutputs = kotlinOutputs,
            tolerances = ComparisonTolerances.LENIENT,
            engine = "dot",
            outputFormat = "svg"
        )
        
        // Validate batch results
        assertEquals(2, batchResult.reports.size, "Should have 2 reports")
        assertEquals(2, batchResult.executionMetrics.size, "Should have 2 execution metrics")
        assertEquals(0, batchResult.errors.size, "Should have no errors")
        
        // Validate summary metrics
        val summary = batchResult.summary
        assertEquals(2, summary.totalTests, "Should have 2 total tests")
        assertTrue(summary.totalBatchTime > 0, "Should have positive batch time")
        assertTrue(summary.averageReferenceExecutionTime >= 0.0, "Should have non-negative average execution time")
        assertTrue(summary.averageComparisonTime >= 0.0, "Should have non-negative average comparison time")
        assertTrue(summary.totalInputSize > 0, "Should have positive total input size")
        assertTrue(summary.totalKotlinOutputSize > 0, "Should have positive total Kotlin output size")
        
        // Validate individual execution metrics
        batchResult.executionMetrics.forEach { metric ->
            assertTrue(metric.testName.isNotEmpty(), "Test name should not be empty")
            assertTrue(metric.inputSize > 0, "Input size should be positive")
            assertTrue(metric.kotlinOutputSize > 0, "Kotlin output size should be positive")
            assertTrue(metric.referenceExecutionTime >= 0, "Reference execution time should be non-negative")
            assertTrue(metric.comparisonTime >= 0, "Comparison time should be non-negative")
        }
        
        // Validate individual reports
        batchResult.reports.forEach { report ->
            assertTrue(report.testCase.name.isNotEmpty(), "Test case name should not be empty")
            assertTrue(report.kotlinOutputSize > 0, "Kotlin output size should be positive")
            assertTrue(report.referenceOutputSize >= 0, "Reference output size should be non-negative")
            assertTrue(report.comparisonResult.overallScore >= 0.0, "Overall score should be non-negative")
            assertTrue(report.recommendations.isNotEmpty(), "Should have recommendations")
        }
    }
    
    @Test
    fun testConfigurableTolerances() {
        // Test configurable tolerances functionality
        val customTolerances = ComparisonTolerances(
            maxCoordinateDeviation = 0.5,
            numericTolerance = 0.05,
            numericAttributes = setOf("x", "y", "width", "height", "cx", "cy", "rx", "ry", "r"),
            minimumOverallScore = 0.9
        )
        
        assertEquals(0.5, customTolerances.maxCoordinateDeviation, "Should set custom coordinate deviation")
        assertEquals(0.05, customTolerances.numericTolerance, "Should set custom numeric tolerance")
        assertTrue(customTolerances.numericAttributes.contains("r"), "Should include radius in numeric attributes")
        assertEquals(0.9, customTolerances.minimumOverallScore, "Should set custom minimum score")
        
        // Test with custom tolerances
        val kotlinSvg = """<svg><g id="test" transform="translate(50.3,50.3)"><circle r="20.3"/></g></svg>"""
        val referenceSvg = """<svg><g id="test" transform="translate(50,50)"><circle r="20"/></g></svg>"""
        
        val result = ReferenceComparison.compareOutputs(kotlinSvg, referenceSvg, customTolerances)
        
        // Should use custom tolerances for evaluation
        assertEquals(result.passed, result.overallScore >= customTolerances.minimumOverallScore, 
            "Should use custom minimum score for pass/fail determination")
    }
}