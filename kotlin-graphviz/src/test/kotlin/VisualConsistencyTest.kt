#!/usr/bin/env kotlin

@file:DependsOn("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")

import org.graphviz.kotlin.parser.*
import org.graphviz.kotlin.layout.*
import org.graphviz.kotlin.renderer.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.abs

/**
 * Visual consistency testing framework to compare Kotlin Graphviz output
 * with original C Graphviz reference outputs.
 */
class VisualConsistencyTest {
    
    data class ComparisonResult(
        val testName: String,
        val structuralSimilarity: Double,
        val coordinateDeviation: Double,
        val attributeMatches: Int,
        val attributeMismatches: Int,
        val passed: Boolean,
        val differences: List<String>
    )
    
    data class SvgElement(
        val tag: String,
        val attributes: Map<String, String>,
        val x: Double? = null,
        val y: Double? = null,
        val width: Double? = null,
        val height: Double? = null
    )
    
    companion object {
        const val COORDINATE_TOLERANCE = 1.0 // pixels
        const val SIMILARITY_THRESHOLD = 0.95
        const val VISUAL_SIMILARITY_THRESHOLD = 0.98
    }
    
    /**
     * Run comprehensive visual consistency tests.
     */
    fun runConsistencyTests(): List<ComparisonResult> {
        val results = mutableListOf<ComparisonResult>()
        
        // Test basic shapes
        results.addAll(testBasicShapes())
        
        // Test edge styles
        results.addAll(testEdgeStyles())
        
        // Test arrowheads
        results.addAll(testArrowheads())
        
        // Test text rendering
        results.addAll(testTextRendering())
        
        // Test complex layouts
        results.addAll(testComplexLayouts())
        
        return results
    }
    
    /**
     * Test basic node shapes consistency.
     */
    private fun testBasicShapes(): List<ComparisonResult> {
        val shapes = listOf("box", "ellipse", "circle", "diamond", "triangle", "hexagon", "octagon")
        val results = mutableListOf<ComparisonResult>()
        
        for (shape in shapes) {
            val dotContent = """
                digraph test_$shape {
                    A [shape=$shape, label="Test $shape"];
                }
            """.trimIndent()
            
            val result = compareWithReference("basic_shape_$shape", dotContent)
            results.add(result)
        }
        
        return results
    }
    
    /**
     * Test edge styles consistency.
     */
    private fun testEdgeStyles(): List<ComparisonResult> {
        val styles = listOf("solid", "dashed", "dotted", "bold")
        val results = mutableListOf<ComparisonResult>()
        
        for (style in styles) {
            val dotContent = """
                digraph test_edge_$style {
                    A -> B [style=$style];
                }
            """.trimIndent()
            
            val result = compareWithReference("edge_style_$style", dotContent)
            results.add(result)
        }
        
        return results
    }
    
    /**
     * Test arrowhead types consistency.
     */
    private fun testArrowheads(): List<ComparisonResult> {
        val arrowheads = listOf("normal", "inv", "dot", "diamond", "box", "vee", "tee", "crow")
        val results = mutableListOf<ComparisonResult>()
        
        for (arrowhead in arrowheads) {
            val dotContent = """
                digraph test_arrow_$arrowhead {
                    A -> B [arrowhead=$arrowhead];
                }
            """.trimIndent()
            
            val result = compareWithReference("arrowhead_$arrowhead", dotContent)
            results.add(result)
        }
        
        return results
    }
    
    /**
     * Test text rendering consistency.
     */
    private fun testTextRendering(): List<ComparisonResult> {
        val results = mutableListOf<ComparisonResult>()
        
        // Test basic labels
        val basicLabelTest = """
            digraph test_labels {
                A [label="Node A"];
                B [label="Node B"];
                A -> B [label="Edge Label"];
            }
        """.trimIndent()
        
        results.add(compareWithReference("text_basic_labels", basicLabelTest))
        
        // Test font attributes
        val fontTest = """
            digraph test_fonts {
                A [label="Arial Text", fontname="Arial", fontsize=14];
                B [label="Times Text", fontname="Times", fontsize=16];
                A -> B [label="Edge Text", fontname="Helvetica", fontsize=12];
            }
        """.trimIndent()
        
        results.add(compareWithReference("text_fonts", fontTest))
        
        return results
    }
    
    /**
     * Test complex layout consistency.
     */
    private fun testComplexLayouts(): List<ComparisonResult> {
        val results = mutableListOf<ComparisonResult>()
        
        // Test hierarchical layout
        val hierarchicalTest = """
            digraph test_hierarchical {
                A -> B;
                A -> C;
                B -> D;
                B -> E;
                C -> F;
                D -> G;
                E -> G;
                F -> G;
            }
        """.trimIndent()
        
        results.add(compareWithReference("layout_hierarchical", hierarchicalTest))
        
        // Test subgraphs
        val subgraphTest = """
            digraph test_subgraphs {
                subgraph cluster_0 {
                    label="Cluster 0";
                    A -> B;
                }
                subgraph cluster_1 {
                    label="Cluster 1";
                    C -> D;
                }
                A -> C;
                B -> D;
            }
        """.trimIndent()
        
        results.add(compareWithReference("layout_subgraphs", subgraphTest))
        
        return results
    }
    
    /**
     * Compare Kotlin output with reference Graphviz output.
     */
    private fun compareWithReference(testName: String, dotContent: String): ComparisonResult {
        try {
            // Generate reference SVG using original Graphviz
            val referenceSvg = generateReferenceSvg(testName, dotContent)
            
            // Generate Kotlin SVG
            val kotlinSvg = generateKotlinSvg(dotContent)
            
            // Parse both SVGs
            val referenceElements = parseSvgElements(referenceSvg)
            val kotlinElements = parseSvgElements(kotlinSvg)
            
            // Compare structure and content
            val structuralSimilarity = calculateStructuralSimilarity(referenceElements, kotlinElements)
            val coordinateDeviation = calculateCoordinateDeviation(referenceElements, kotlinElements)
            val (attributeMatches, attributeMismatches) = compareAttributes(referenceElements, kotlinElements)
            val differences = findDifferences(referenceElements, kotlinElements)
            
            val passed = structuralSimilarity >= SIMILARITY_THRESHOLD && 
                        coordinateDeviation <= COORDINATE_TOLERANCE
            
            return ComparisonResult(
                testName = testName,
                structuralSimilarity = structuralSimilarity,
                coordinateDeviation = coordinateDeviation,
                attributeMatches = attributeMatches,
                attributeMismatches = attributeMismatches,
                passed = passed,
                differences = differences
            )
            
        } catch (e: Exception) {
            return ComparisonResult(
                testName = testName,
                structuralSimilarity = 0.0,
                coordinateDeviation = Double.MAX_VALUE,
                attributeMatches = 0,
                attributeMismatches = 1,
                passed = false,
                differences = listOf("Error: ${e.message}")
            )
        }
    }
    
    /**
     * Generate reference SVG using original Graphviz dot command.
     */
    private fun generateReferenceSvg(testName: String, dotContent: String): String {
        val tempDir = Files.createTempDirectory("graphviz_test")
        val dotFile = tempDir.resolve("$testName.dot")
        val svgFile = tempDir.resolve("$testName.svg")
        
        try {
            Files.write(dotFile, dotContent.toByteArray())
            
            val process = ProcessBuilder("dot", "-Tsvg", dotFile.toString(), "-o", svgFile.toString())
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val error = process.inputStream.bufferedReader().readText()
                throw RuntimeException("Graphviz dot command failed: $error")
            }
            
            return Files.readString(svgFile)
            
        } finally {
            // Clean up temp files
            Files.deleteIfExists(dotFile)
            Files.deleteIfExists(svgFile)
            Files.deleteIfExists(tempDir)
        }
    }
    
    /**
     * Generate SVG using Kotlin implementation.
     */
    private fun generateKotlinSvg(dotContent: String): String {
        val parser = DotParser()
        val graph = parser.parse(dotContent)
        
        // Apply layout
        val layoutEngine = DotLayoutEngine()
        val layoutResult = layoutEngine.layout(graph)
        
        // Render to SVG
        val renderer = DefaultSvgRenderer()
        return renderer.render(layoutResult.graph, RenderOptions.default())
    }
    
    /**
     * Parse SVG content into structured elements.
     */
    private fun parseSvgElements(svgContent: String): List<SvgElement> {
        val elements = mutableListOf<SvgElement>()
        
        // Simple regex-based parsing (for testing purposes)
        // In production, use a proper XML parser
        val elementRegex = """<(\w+)([^>]*)>""".toRegex()
        
        elementRegex.findAll(svgContent).forEach { match ->
            val tag = match.groupValues[1]
            val attributesStr = match.groupValues[2]
            
            val attributes = parseAttributes(attributesStr)
            val x = attributes["x"]?.toDoubleOrNull() ?: attributes["cx"]?.toDoubleOrNull()
            val y = attributes["y"]?.toDoubleOrNull() ?: attributes["cy"]?.toDoubleOrNull()
            val width = attributes["width"]?.toDoubleOrNull() ?: attributes["rx"]?.let { it.toDouble() * 2 }
            val height = attributes["height"]?.toDoubleOrNull() ?: attributes["ry"]?.let { it.toDouble() * 2 }
            
            elements.add(SvgElement(tag, attributes, x, y, width, height))
        }
        
        return elements
    }
    
    /**
     * Parse attribute string into map.
     */
    private fun parseAttributes(attributesStr: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        val attrRegex = """(\w+)="([^"]*)"""".toRegex()
        
        attrRegex.findAll(attributesStr).forEach { match ->
            attributes[match.groupValues[1]] = match.groupValues[2]
        }
        
        return attributes
    }
    
    /**
     * Calculate structural similarity between element lists.
     */
    private fun calculateStructuralSimilarity(reference: List<SvgElement>, kotlin: List<SvgElement>): Double {
        val refTags = reference.map { it.tag }.sorted()
        val kotlinTags = kotlin.map { it.tag }.sorted()
        
        val intersection = refTags.intersect(kotlinTags.toSet()).size
        val union = refTags.union(kotlinTags.toSet()).size
        
        return if (union == 0) 1.0 else intersection.toDouble() / union.toDouble()
    }
    
    /**
     * Calculate average coordinate deviation.
     */
    private fun calculateCoordinateDeviation(reference: List<SvgElement>, kotlin: List<SvgElement>): Double {
        val deviations = mutableListOf<Double>()
        
        // Match elements by tag and find coordinate differences
        for (refElement in reference) {
            val matchingKotlin = kotlin.find { it.tag == refElement.tag }
            if (matchingKotlin != null) {
                refElement.x?.let { refX ->
                    matchingKotlin.x?.let { kotlinX ->
                        deviations.add(abs(refX - kotlinX))
                    }
                }
                refElement.y?.let { refY ->
                    matchingKotlin.y?.let { kotlinY ->
                        deviations.add(abs(refY - kotlinY))
                    }
                }
            }
        }
        
        return if (deviations.isEmpty()) 0.0 else deviations.average()
    }
    
    /**
     * Compare attributes between reference and Kotlin elements.
     */
    private fun compareAttributes(reference: List<SvgElement>, kotlin: List<SvgElement>): Pair<Int, Int> {
        var matches = 0
        var mismatches = 0
        
        for (refElement in reference) {
            val matchingKotlin = kotlin.find { it.tag == refElement.tag }
            if (matchingKotlin != null) {
                for ((key, refValue) in refElement.attributes) {
                    val kotlinValue = matchingKotlin.attributes[key]
                    if (kotlinValue == refValue) {
                        matches++
                    } else {
                        mismatches++
                    }
                }
            }
        }
        
        return Pair(matches, mismatches)
    }
    
    /**
     * Find specific differences between reference and Kotlin output.
     */
    private fun findDifferences(reference: List<SvgElement>, kotlin: List<SvgElement>): List<String> {
        val differences = mutableListOf<String>()
        
        // Check for missing elements
        val refTags = reference.map { it.tag }.toSet()
        val kotlinTags = kotlin.map { it.tag }.toSet()
        
        (refTags - kotlinTags).forEach { tag ->
            differences.add("Missing element: $tag")
        }
        
        (kotlinTags - refTags).forEach { tag ->
            differences.add("Extra element: $tag")
        }
        
        // Check for attribute differences
        for (refElement in reference) {
            val matchingKotlin = kotlin.find { it.tag == refElement.tag }
            if (matchingKotlin != null) {
                for ((key, refValue) in refElement.attributes) {
                    val kotlinValue = matchingKotlin.attributes[key]
                    if (kotlinValue != refValue) {
                        differences.add("${refElement.tag}.$key: expected '$refValue', got '$kotlinValue'")
                    }
                }
            }
        }
        
        return differences
    }
    
    /**
     * Print test results summary.
     */
    fun printResults(results: List<ComparisonResult>) {
        println("Visual Consistency Test Results")
        println("=" * 50)
        
        val passed = results.count { it.passed }
        val total = results.size
        
        println("Overall: $passed/$total tests passed (${(passed * 100.0 / total).toInt()}%)")
        println()
        
        for (result in results) {
            val status = if (result.passed) "✓ PASS" else "✗ FAIL"
            println("$status ${result.testName}")
            println("  Structural similarity: ${(result.structuralSimilarity * 100).toInt()}%")
            println("  Coordinate deviation: ${result.coordinateDeviation.toInt()}px")
            println("  Attribute matches: ${result.attributeMatches}/${result.attributeMatches + result.attributeMismatches}")
            
            if (result.differences.isNotEmpty()) {
                println("  Differences:")
                result.differences.take(3).forEach { diff ->
                    println("    - $diff")
                }
                if (result.differences.size > 3) {
                    println("    ... and ${result.differences.size - 3} more")
                }
            }
            println()
        }
    }
}

/**
 * Main function to run visual consistency tests.
 */
fun main() {
    val tester = VisualConsistencyTest()
    val results = tester.runConsistencyTests()
    tester.printResults(results)
    
    val failedTests = results.filter { !it.passed }
    if (failedTests.isNotEmpty()) {
        println("Failed tests require attention:")
        failedTests.forEach { result ->
            println("- ${result.testName}: ${result.differences.firstOrNull() ?: "Unknown issue"}")
        }
        kotlin.system.exitProcess(1)
    } else {
        println("All visual consistency tests passed!")
    }
}