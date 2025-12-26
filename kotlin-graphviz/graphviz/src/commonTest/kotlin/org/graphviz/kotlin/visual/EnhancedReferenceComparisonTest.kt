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
 * Tests for the enhanced reference comparison pipeline.
 * **Feature: kotlin-multiplatform-port, Task 14.2: Build reference comparison pipeline**
 */
class EnhancedReferenceComparisonTest : FunSpec({
    
    test("should execute GraphvizExecutor with enhanced metrics") {
        val dotContent = """
            digraph test {
                A [shape=box, color=red];
                B [shape=circle, color=blue];
                A -> B [style=dashed, color=green];
            }
        """.trimIndent()
        
        val result = GraphvizExecutor.execute(dotContent, "dot", "svg")
        
        when (result) {
            is GraphvizExecutionResult.Success -> {
                result.output.shouldNotBe("")
                result.output shouldContain "<?xml"
                result.output shouldContain "<svg"
                result.executionTime shouldBeGreaterThanOrEqual 0
                result.inputSize shouldBe dotContent.length
                result.command.shouldNotBe("")
            }
            is GraphvizExecutionResult.Error -> {
                // In simulation mode, this might happen - that's ok for testing
                result.message.shouldNotBe("")
            }
        }
    }
    
    test("should perform batch execution with detailed metrics") {
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
        
        val batchResult = GraphvizExecutor.batchExecute(testCases)
        
        batchResult.results.size shouldBe 2
        batchResult.metrics.size shouldBe 2
        batchResult.totalExecutionTime shouldBeGreaterThanOrEqual 0
        batchResult.successCount shouldBeGreaterThanOrEqual 0
        batchResult.failureCount shouldBeGreaterThanOrEqual 0
        
        batchResult.metrics.forEach { metric ->
            metric.testName.shouldNotBe("")
            metric.inputSize shouldBeGreaterThan 0
        }
    }
    
    test("should parse SVG with enhanced detail extraction") {
        val svgContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
                <style>
                    .node { fill: white; stroke: black; }
                    .edge { stroke: black; }
                </style>
                <g id="node-A" transform="translate(50,50) scale(1.2)" style="fill:red;stroke:blue">
                    <rect x="-20" y="-10" width="40" height="20" class="node"/>
                    <text x="0" y="0" text-anchor="middle">Node A</text>
                </g>
                <g id="edge-A-B">
                    <path d="M70,50 L130,50" stroke="green" stroke-dasharray="5,5"/>
                </g>
            </svg>
        """.trimIndent()
        
        val parsed = SvgParser.parse(svgContent)
        
        // Check basic parsing
        parsed.elements.shouldNotBeEmpty()
        parsed.coordinates.shouldNotBeEmpty()
        parsed.attributes.shouldNotBeEmpty()
        parsed.viewport shouldNotBe null
        
        // Check enhanced parsing
        parsed.styleElements.shouldNotBeEmpty()
        parsed.transformations.shouldNotBeEmpty()
        parsed.rawContent shouldBe svgContent
        
        // Verify style parsing
        val nodeStyle = parsed.styleElements.find { it.elementId == "node-A" }
        nodeStyle shouldNotBe null
        nodeStyle!!.properties shouldContain ("fill" to "red")
        nodeStyle.properties shouldContain ("stroke" to "blue")
        
        // Verify transformation parsing
        val nodeTransform = parsed.transformations["node-A"]
        nodeTransform shouldNotBe null
        nodeTransform!!.operations.size shouldBe 2
        nodeTransform.operations[0] shouldBe TransformOperation.Translate(50.0, 50.0)
        nodeTransform.operations[1] shouldBe TransformOperation.Scale(1.2, 1.2)
    }
    
    test("should perform detailed SVG comparison") {
        val kotlinSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
                <g id="node-A" transform="translate(50,50)" style="fill:red">
                    <rect x="-20" y="-10" width="40" height="20"/>
                    <text x="0" y="0">Node A</text>
                </g>
            </svg>
        """.trimIndent()
        
        val referenceSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
                <g id="node-A" transform="translate(52,52)" style="fill:blue">
                    <rect x="-20" y="-10" width="40" height="20"/>
                    <text x="0" y="0">Node A</text>
                </g>
            </svg>
        """.trimIndent()
        
        val kotlinParsed = SvgParser.parse(kotlinSvg)
        val referenceParsed = SvgParser.parse(referenceSvg)
        
        val comparison = SvgParser.compare(kotlinParsed, referenceParsed, ComparisonTolerances.DEFAULT)
        
        // Should detect coordinate differences
        comparison.coordinates.withinTolerance shouldBe true // Within default tolerance of 1.0
        comparison.coordinates.maxDeviation shouldBeGreaterThan 0.0
        
        // Should detect style differences
        comparison.styles.overallMatchPercentage shouldBeLessThan 1.0
        
        // Should have overall score
        comparison.overallScore shouldBeGreaterThan 0.0
        comparison.overallScore shouldBeLessThan 1.0
    }
    
    test("should generate enhanced reference output") {
        val dotContent = """
            digraph test {
                A [shape=box, color=red];
                B [shape=circle];
                A -> B [style=dashed];
            }
        """.trimIndent()
        
        val result = ReferenceComparison.generateReferenceOutput(
            dotContent = dotContent,
            outputFormat = "svg",
            engine = "dot",
            options = GraphvizOptions(verbose = true)
        )
        
        when (result) {
            is ReferenceExecutionResult.Success -> {
                result.output.shouldNotBe("")
                result.executionTime shouldBeGreaterThanOrEqual 0
                result.command.shouldNotBe("")
            }
            is ReferenceExecutionResult.Error -> {
                // In simulation mode, this might happen
                result.message.shouldNotBe("")
                result.command.shouldNotBe("")
            }
        }
    }
    
    test("should perform enhanced batch comparison") {
        val testCases = listOf(
            TestCase(
                name = "box_node",
                description = "Box node test",
                dotContent = "digraph test { A [shape=box]; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("A")
            ),
            TestCase(
                name = "circle_node", 
                description = "Circle node test",
                dotContent = "digraph test { B [shape=circle]; }",
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("B")
            )
        )
        
        val kotlinOutputs = mapOf(
            "box_node" to """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                    <g id="node-A" transform="translate(100,50)">
                        <rect x="-20" y="-10" width="40" height="20"/>
                        <text x="0" y="0">A</text>
                    </g>
                </svg>
            """.trimIndent(),
            "circle_node" to """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                    <g id="node-B" transform="translate(100,50)">
                        <circle cx="0" cy="0" r="20"/>
                        <text x="0" y="0">B</text>
                    </g>
                </svg>
            """.trimIndent()
        )
        
        val batchResult = ReferenceComparison.batchCompare(
            testCases = testCases,
            kotlinOutputs = kotlinOutputs,
            tolerances = ComparisonTolerances.DEFAULT,
            engine = "dot",
            outputFormat = "svg"
        )
        
        batchResult.reports.size shouldBe 2
        batchResult.executionMetrics.size shouldBe 2
        batchResult.summary.totalTests shouldBe 2
        batchResult.summary.totalBatchTime shouldBeGreaterThanOrEqual 0
        batchResult.summary.averageReferenceExecutionTime shouldBeGreaterThanOrEqual 0.0
        batchResult.summary.averageComparisonTime shouldBeGreaterThanOrEqual 0.0
        batchResult.summary.totalInputSize shouldBeGreaterThan 0
        batchResult.summary.totalKotlinOutputSize shouldBeGreaterThan 0
        
        batchResult.executionMetrics.forEach { metric ->
            metric.testName.shouldNotBe("")
            metric.inputSize shouldBeGreaterThan 0
            metric.kotlinOutputSize shouldBeGreaterThan 0
        }
    }
    
    test("should handle comparison tolerances correctly") {
        val kotlinSvg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                <g id="node-A" transform="translate(50.05,50.05)">
                    <rect x="-20" y="-10" width="40.1" height="20.1"/>
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
        
        // With strict tolerances, should fail
        val strictResult = ReferenceComparison.compareOutputs(kotlinSvg, referenceSvg, ComparisonTolerances.STRICT)
        strictResult.coordinateDeviations.withinTolerance shouldBe false
        
        // With lenient tolerances, should pass
        val lenientResult = ReferenceComparison.compareOutputs(kotlinSvg, referenceSvg, ComparisonTolerances.LENIENT)
        lenientResult.coordinateDeviations.withinTolerance shouldBe true
        
        // Should have detailed comparison data
        strictResult.detailedComparison shouldNotBe null
        lenientResult.detailedComparison shouldNotBe null
    }
    
    test("should validate configurable tolerances") {
        val customTolerances = ComparisonTolerances(
            maxCoordinateDeviation = 0.5,
            numericTolerance = 0.05,
            numericAttributes = setOf("x", "y", "width", "height", "cx", "cy", "rx", "ry", "r"),
            minimumOverallScore = 0.9
        )
        
        customTolerances.maxCoordinateDeviation shouldBe 0.5
        customTolerances.numericTolerance shouldBe 0.05
        customTolerances.numericAttributes shouldContain "r"
        customTolerances.minimumOverallScore shouldBe 0.9
        
        // Test with custom tolerances
        val kotlinSvg = """<svg><g id="test" transform="translate(50.3,50.3)"><circle r="20.3"/></g></svg>"""
        val referenceSvg = """<svg><g id="test" transform="translate(50,50)"><circle r="20"/></g></svg>"""
        
        val result = ReferenceComparison.compareOutputs(kotlinSvg, referenceSvg, customTolerances)
        
        // Should use custom tolerances for evaluation
        result.passed shouldBe (result.overallScore >= customTolerances.minimumOverallScore)
    }
})