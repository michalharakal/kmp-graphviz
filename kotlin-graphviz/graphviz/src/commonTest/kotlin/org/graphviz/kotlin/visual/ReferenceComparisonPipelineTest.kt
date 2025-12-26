package org.graphviz.kotlin.visual

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.string.shouldContain

/**
 * Integration tests for the complete reference comparison pipeline.
 * Tests the full workflow from DOT input to detailed comparison reporting.
 * **Feature: kotlin-multiplatform-port, Task 14.2: Build reference comparison pipeline**
 */
class ReferenceComparisonPipelineTest : FunSpec({
    
    test("should execute complete pipeline for single test case") {
        // Step 1: Generate test case
        val testCase = TestCase(
            name = "pipeline_test_node_shapes",
            description = "Test various node shapes in pipeline",
            dotContent = """
                digraph test {
                    A [shape=box, color=red, style=filled];
                    B [shape=circle, color=blue, style=filled];
                    C [shape=diamond, color=green, style=filled];
                    A -> B [style=dashed, color=purple];
                    B -> C [style=dotted, color=orange];
                }
            """.trimIndent(),
            category = TestCategory.NODE_SHAPES,
            expectedElements = listOf("A", "B", "C"),
            tags = setOf("shapes", "colors", "styles")
        )
        
        // Step 2: Execute original Graphviz to generate reference
        val referenceResult = ReferenceComparison.generateReferenceOutput(
            dotContent = testCase.dotContent,
            outputFormat = "svg",
            engine = "dot"
        )
        
        referenceResult shouldBe ReferenceExecutionResult.Success::class
        val referenceOutput = (referenceResult as ReferenceExecutionResult.Success).output
        referenceOutput.shouldNotBe("")
        referenceOutput shouldContain "<?xml"
        referenceOutput shouldContain "<svg"
        
        // Step 3: Simulate Kotlin implementation output (would be real in production)
        val kotlinOutput = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <svg xmlns="http://www.w3.org/2000/svg" width="400" height="200" viewBox="0 0 400 200">
                <g id="node-A" transform="translate(100,50)">
                    <rect x="-30" y="-20" width="60" height="40" fill="red" stroke="black"/>
                    <text x="0" y="0" text-anchor="middle" fill="black">A</text>
                </g>
                <g id="node-B" transform="translate(200,50)">
                    <ellipse cx="0" cy="0" rx="30" ry="20" fill="blue" stroke="black"/>
                    <text x="0" y="0" text-anchor="middle" fill="white">B</text>
                </g>
                <g id="node-C" transform="translate(300,50)">
                    <polygon points="-25,0 0,-20 25,0 0,20" fill="green" stroke="black"/>
                    <text x="0" y="0" text-anchor="middle" fill="white">C</text>
                </g>
                <g id="edge-A-B">
                    <path d="M130,50 L170,50" stroke="purple" stroke-dasharray="5,5" fill="none"/>
                </g>
                <g id="edge-B-C">
                    <path d="M230,50 L270,50" stroke="orange" stroke-dasharray="2,2" fill="none"/>
                </g>
            </svg>
        """.trimIndent()
        
        // Step 4: Parse both outputs with enhanced SVG parser
        val kotlinParsed = SvgParser.parse(kotlinOutput)
        val referenceParsed = SvgParser.parse(referenceOutput)
        
        kotlinParsed.elements.shouldNotBeEmpty()
        kotlinParsed.coordinates.shouldNotBeEmpty()
        kotlinParsed.attributes.shouldNotBeEmpty()
        
        referenceParsed.elements.shouldNotBeEmpty()
        
        // Step 5: Perform detailed comparison
        val tolerances = ComparisonTolerances(
            maxCoordinateDeviation = 2.0,
            numericTolerance = 0.1,
            minimumOverallScore = 0.7
        )
        
        val comparison = ReferenceComparison.compareOutputs(kotlinOutput, referenceOutput, tolerances)
        
        // Step 6: Validate comparison results
        comparison.structuralSimilarity.similarity shouldBeGreaterThan 0.0
        comparison.coordinateDeviations.deviations.shouldNotBeEmpty()
        comparison.attributeMatches.matches.shouldNotBeEmpty()
        comparison.overallScore shouldBeGreaterThan 0.0
        
        // Step 7: Generate detailed report
        val report = ReferenceComparison.generateComparisonReport(testCase, kotlinOutput, referenceOutput, comparison)
        
        report.testCase shouldBe testCase
        report.kotlinOutputSize shouldBe kotlinOutput.length
        report.referenceOutputSize shouldBe referenceOutput.length
        report.comparisonResult shouldBe comparison
        report.recommendations.shouldNotBeEmpty()
    }
    
    test("should execute pipeline for batch of test cases") {
        // Step 1: Generate comprehensive test suite
        val testCases = listOf(
            TestCase(
                name = "batch_simple_node",
                description = "Simple single node",
                dotContent = "digraph test { A; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("A")
            ),
            TestCase(
                name = "batch_simple_edge",
                description = "Simple edge connection",
                dotContent = "digraph test { A -> B; }",
                category = TestCategory.EDGE_STYLES,
                expectedElements = listOf("A", "B")
            ),
            TestCase(
                name = "batch_styled_nodes",
                description = "Nodes with various styles",
                dotContent = """
                    digraph test {
                        A [shape=box, color=red];
                        B [shape=circle, color=blue];
                        C [shape=diamond, color=green];
                    }
                """.trimIndent(),
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("A", "B", "C")
            )
        )
        
        // Step 2: Simulate Kotlin implementation outputs
        val kotlinOutputs = mapOf(
            "batch_simple_node" to """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
                    <g id="node-A" transform="translate(100,50)">
                        <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                        <text x="0" y="0" text-anchor="middle">A</text>
                    </g>
                </svg>
            """.trimIndent(),
            "batch_simple_edge" to """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="300" height="100" viewBox="0 0 300 100">
                    <g id="node-A" transform="translate(75,50)">
                        <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                        <text x="0" y="0" text-anchor="middle">A</text>
                    </g>
                    <g id="node-B" transform="translate(225,50)">
                        <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                        <text x="0" y="0" text-anchor="middle">B</text>
                    </g>
                    <g id="edge-A-B">
                        <path d="M105,50 L195,50" stroke="black" fill="none"/>
                    </g>
                </svg>
            """.trimIndent(),
            "batch_styled_nodes" to """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="400" height="100" viewBox="0 0 400 100">
                    <g id="node-A" transform="translate(100,50)">
                        <rect x="-25" y="-15" width="50" height="30" fill="red" stroke="black"/>
                        <text x="0" y="0" text-anchor="middle">A</text>
                    </g>
                    <g id="node-B" transform="translate(200,50)">
                        <ellipse cx="0" cy="0" rx="25" ry="15" fill="blue" stroke="black"/>
                        <text x="0" y="0" text-anchor="middle">B</text>
                    </g>
                    <g id="node-C" transform="translate(300,50)">
                        <polygon points="-20,0 0,-15 20,0 0,15" fill="green" stroke="black"/>
                        <text x="0" y="0" text-anchor="middle">C</text>
                    </g>
                </svg>
            """.trimIndent()
        )
        
        // Step 3: Execute batch comparison pipeline
        val batchResult = ReferenceComparison.batchCompare(
            testCases = testCases,
            kotlinOutputs = kotlinOutputs,
            tolerances = ComparisonTolerances.LENIENT,
            engine = "dot",
            outputFormat = "svg"
        )
        
        // Step 4: Validate batch results
        batchResult.reports.size shouldBe 3
        batchResult.executionMetrics.size shouldBe 3
        batchResult.errors.size shouldBe 0
        
        // Step 5: Validate summary metrics
        val summary = batchResult.summary
        summary.totalTests shouldBe 3
        summary.totalBatchTime shouldBeGreaterThan 0
        summary.averageReferenceExecutionTime shouldBeGreaterThanOrEqual 0.0
        summary.averageComparisonTime shouldBeGreaterThanOrEqual 0.0
        summary.totalInputSize shouldBeGreaterThan 0
        summary.totalKotlinOutputSize shouldBeGreaterThan 0
        summary.totalReferenceOutputSize shouldBeGreaterThanOrEqual 0
        
        // Step 6: Validate individual execution metrics
        batchResult.executionMetrics.forEach { metric ->
            metric.testName.shouldNotBe("")
            metric.inputSize shouldBeGreaterThan 0
            metric.kotlinOutputSize shouldBeGreaterThan 0
            metric.referenceExecutionTime shouldBeGreaterThanOrEqual 0
            metric.comparisonTime shouldBeGreaterThanOrEqual 0
        }
        
        // Step 7: Validate individual reports
        batchResult.reports.forEach { report ->
            report.testCase.name.shouldNotBe("")
            report.kotlinOutputSize shouldBeGreaterThan 0
            report.referenceOutputSize shouldBeGreaterThanOrEqual 0
            report.comparisonResult.overallScore shouldBeGreaterThanOrEqual 0.0
            report.recommendations.shouldNotBeEmpty()
        }
    }
    
    test("should handle coordinate deviation analysis with configurable tolerances") {
        val testCase = TestCase(
            name = "coordinate_precision_test",
            description = "Test coordinate precision analysis",
            dotContent = """
                digraph test {
                    A [pos="50,50!"];
                    B [pos="150,50!"];
                    A -> B;
                }
            """.trimIndent(),
            category = TestCategory.COMPLEX_LAYOUTS,
            expectedElements = listOf("A", "B")
        )
        
        // Kotlin output with slight coordinate differences
        val kotlinOutput = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
                <g id="node-A" transform="translate(50.5,50.2)">
                    <ellipse cx="0" cy="0" rx="20" ry="15" fill="white" stroke="black"/>
                    <text x="0" y="0">A</text>
                </g>
                <g id="node-B" transform="translate(149.8,49.9)">
                    <ellipse cx="0" cy="0" rx="20" ry="15" fill="white" stroke="black"/>
                    <text x="0" y="0">B</text>
                </g>
                <g id="edge-A-B">
                    <path d="M70.5,50.2 L129.8,49.9" stroke="black"/>
                </g>
            </svg>
        """.trimIndent()
        
        // Reference output with exact coordinates
        val referenceOutput = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
                <g id="node-A" transform="translate(50,50)">
                    <ellipse cx="0" cy="0" rx="20" ry="15" fill="white" stroke="black"/>
                    <text x="0" y="0">A</text>
                </g>
                <g id="node-B" transform="translate(150,50)">
                    <ellipse cx="0" cy="0" rx="20" ry="15" fill="white" stroke="black"/>
                    <text x="0" y="0">B</text>
                </g>
                <g id="edge-A-B">
                    <path d="M70,50 L130,50" stroke="black"/>
                </g>
            </svg>
        """.trimIndent()
        
        // Test with strict tolerances (should fail)
        val strictTolerances = ComparisonTolerances(
            maxCoordinateDeviation = 0.1,
            numericTolerance = 0.01,
            minimumOverallScore = 0.95
        )
        
        val strictComparison = ReferenceComparison.compareOutputs(kotlinOutput, referenceOutput, strictTolerances)
        strictComparison.coordinateDeviations.withinTolerance shouldBe false
        strictComparison.coordinateDeviations.maxDeviation shouldBeGreaterThan 0.1
        strictComparison.passed shouldBe false
        
        // Test with lenient tolerances (should pass)
        val lenientTolerances = ComparisonTolerances(
            maxCoordinateDeviation = 1.0,
            numericTolerance = 0.5,
            minimumOverallScore = 0.6
        )
        
        val lenientComparison = ReferenceComparison.compareOutputs(kotlinOutput, referenceOutput, lenientTolerances)
        lenientComparison.coordinateDeviations.withinTolerance shouldBe true
        lenientComparison.passed shouldBe true
        
        // Validate detailed deviation analysis
        strictComparison.coordinateDeviations.deviations.shouldNotBeEmpty()
        strictComparison.coordinateDeviations.deviations.forEach { deviation ->
            deviation.elementId.shouldNotBe("")
            deviation.deviation shouldBeGreaterThan 0.0
            deviation.kotlinCoordinate shouldNotBe deviation.referenceCoordinate
        }
    }
    
    test("should perform attribute matching validation with detailed reporting") {
        val kotlinOutput = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(100,50)">
                    <rect x="-25" y="-15" width="50" height="30" fill="red" stroke="blue" stroke-width="2"/>
                    <text x="0" y="0" font-family="Arial" font-size="12" fill="white">A</text>
                </g>
            </svg>
        """.trimIndent()
        
        val referenceOutput = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(100,50)">
                    <rect x="-25" y="-15" width="50" height="30" fill="red" stroke="black" stroke-width="1"/>
                    <text x="0" y="0" font-family="Arial" font-size="12" fill="black">A</text>
                </g>
            </svg>
        """.trimIndent()
        
        val comparison = ReferenceComparison.compareOutputs(kotlinOutput, referenceOutput, ComparisonTolerances.DEFAULT)
        
        // Should detect attribute differences
        comparison.attributeMatches.matchPercentage shouldBeLessThan 1.0
        comparison.attributeMatches.matches.shouldNotBeEmpty()
        
        // Find specific attribute mismatches
        val strokeMismatch = comparison.attributeMatches.matches.find { 
            it.attributeName == "stroke" && !it.matches 
        }
        strokeMismatch shouldNotBe null
        strokeMismatch!!.kotlinValue shouldBe "blue"
        strokeMismatch.referenceValue shouldBe "black"
        
        val fillMismatch = comparison.attributeMatches.matches.find { 
            it.attributeName == "fill" && it.elementId.contains("text") && !it.matches 
        }
        fillMismatch shouldNotBe null
        fillMismatch!!.kotlinValue shouldBe "white"
        fillMismatch.referenceValue shouldBe "black"
        
        // Should have detailed comparison with enhanced analysis
        comparison.detailedComparison shouldNotBe null
        val detailed = comparison.detailedComparison!!
        detailed.attributes.matches.shouldNotBeEmpty()
        detailed.overallScore shouldBeGreaterThan 0.0
    }
})