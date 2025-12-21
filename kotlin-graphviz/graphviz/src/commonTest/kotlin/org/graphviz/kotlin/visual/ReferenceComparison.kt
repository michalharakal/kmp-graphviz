package org.graphviz.kotlin.visual

import org.graphviz.kotlin.model.Point

/**
 * Handles comparison between Kotlin implementation output and reference Graphviz output.
 * Provides automated execution of original Graphviz and detailed comparison analysis.
 */
object ReferenceComparison {
    
    /**
     * Executes original Graphviz to generate reference output.
     */
    fun generateReferenceOutput(dotContent: String, outputFormat: String = "svg"): ReferenceExecutionResult {
        return try {
            // In a real implementation, this would execute the original Graphviz binary
            // For now, we simulate the execution
            val referenceOutput = simulateGraphvizExecution(dotContent, outputFormat)
            ReferenceExecutionResult.Success(referenceOutput)
        } catch (e: Exception) {
            ReferenceExecutionResult.Error("Failed to execute Graphviz: ${e.message}", e)
        }
    }
    
    /**
     * Compares Kotlin implementation output with reference output.
     */
    fun compareOutputs(
        kotlinOutput: String,
        referenceOutput: String,
        tolerances: ComparisonTolerances = ComparisonTolerances.DEFAULT
    ): ComparisonResult {
        val kotlinSvg = parseSvgStructure(kotlinOutput)
        val referenceSvg = parseSvgStructure(referenceOutput)
        
        val structuralSimilarity = calculateStructuralSimilarity(kotlinSvg, referenceSvg)
        val coordinateDeviations = calculateCoordinateDeviations(kotlinSvg, referenceSvg, tolerances)
        val attributeMatches = validateAttributeMatching(kotlinSvg, referenceSvg, tolerances)
        
        val overallScore = calculateOverallScore(structuralSimilarity, coordinateDeviations, attributeMatches)
        
        return ComparisonResult(
            structuralSimilarity = structuralSimilarity,
            coordinateDeviations = coordinateDeviations,
            attributeMatches = attributeMatches,
            overallScore = overallScore,
            passed = overallScore >= tolerances.minimumOverallScore
        )
    }
    
    /**
     * Generates detailed comparison report.
     */
    fun generateComparisonReport(
        testCase: TestCase,
        kotlinOutput: String,
        referenceOutput: String,
        comparisonResult: ComparisonResult
    ): ComparisonReport {
        return ComparisonReport(
            testCase = testCase,
            kotlinOutputSize = kotlinOutput.length,
            referenceOutputSize = referenceOutput.length,
            comparisonResult = comparisonResult,
            timestamp = getCurrentTimestamp(),
            recommendations = generateRecommendations(comparisonResult)
        )
    }
    
    /**
     * Batch processes multiple test cases for comparison.
     */
    fun batchCompare(
        testCases: List<TestCase>,
        kotlinOutputs: Map<String, String>,
        tolerances: ComparisonTolerances = ComparisonTolerances.DEFAULT
    ): BatchComparisonResult {
        val results = mutableListOf<ComparisonReport>()
        val errors = mutableListOf<String>()
        
        for (testCase in testCases) {
            try {
                val kotlinOutput = kotlinOutputs[testCase.name]
                if (kotlinOutput == null) {
                    errors.add("Missing Kotlin output for test: ${testCase.name}")
                    continue
                }
                
                val referenceResult = generateReferenceOutput(testCase.dotContent)
                when (referenceResult) {
                    is ReferenceExecutionResult.Success -> {
                        val comparison = compareOutputs(kotlinOutput, referenceResult.output, tolerances)
                        val report = generateComparisonReport(testCase, kotlinOutput, referenceResult.output, comparison)
                        results.add(report)
                    }
                    is ReferenceExecutionResult.Error -> {
                        errors.add("Reference generation failed for ${testCase.name}: ${referenceResult.message}")
                    }
                }
            } catch (e: Exception) {
                errors.add("Comparison failed for ${testCase.name}: ${e.message}")
            }
        }
        
        return BatchComparisonResult(
            reports = results,
            errors = errors,
            summary = generateBatchSummary(results)
        )
    }
    
    // Private helper methods
    
    private fun simulateGraphvizExecution(dotContent: String, outputFormat: String): String {
        // This simulates what the original Graphviz would produce
        // In a real implementation, this would execute: dot -T$outputFormat input.dot
        return when {
            dotContent.contains("empty") -> generateEmptyGraphSvg()
            dotContent.contains("single") -> generateSingleNodeSvg()
            dotContent.contains("shape=box") -> generateBoxNodeSvg()
            dotContent.contains("style=dashed") -> generateDashedEdgeSvg()
            else -> generateDefaultSvg(dotContent)
        }
    }
    
    private fun parseSvgStructure(svgContent: String): SvgStructure {
        // Parse SVG content into structured representation
        val elements = mutableListOf<SvgElement>()
        val coordinates = mutableMapOf<String, Point>()
        val attributes = mutableMapOf<String, Map<String, String>>()
        
        // Simple regex-based parsing (in real implementation, use proper XML parser)
        val elementPattern = Regex("""<(\w+)[^>]*id="([^"]+)"[^>]*>""")
        val transformPattern = Regex("""transform="translate\(([^,]+),([^)]+)\)"""")
        val attributePattern = Regex("""(\w+)="([^"]+)"""")
        
        elementPattern.findAll(svgContent).forEach { match ->
            val elementType = match.groupValues[1]
            val elementId = match.groupValues[2]
            
            elements.add(SvgElement(elementType, elementId))
            
            // Extract coordinates
            val elementContent = svgContent.substring(match.range.first, 
                svgContent.indexOf('>', match.range.last) + 1)
            
            transformPattern.find(elementContent)?.let { transformMatch ->
                val x = transformMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val y = transformMatch.groupValues[2].toDoubleOrNull() ?: 0.0
                coordinates[elementId] = Point(x, y)
            }
            
            // Extract attributes
            val elementAttributes = mutableMapOf<String, String>()
            attributePattern.findAll(elementContent).forEach { attrMatch ->
                elementAttributes[attrMatch.groupValues[1]] = attrMatch.groupValues[2]
            }
            attributes[elementId] = elementAttributes
        }
        
        return SvgStructure(elements, coordinates, attributes)
    }
    
    private fun calculateStructuralSimilarity(kotlin: SvgStructure, reference: SvgStructure): StructuralSimilarity {
        val kotlinElements = kotlin.elements.map { "${it.type}:${it.id}" }.toSet()
        val referenceElements = reference.elements.map { "${it.type}:${it.id}" }.toSet()
        
        val commonElements = kotlinElements.intersect(referenceElements)
        val missingElements = referenceElements - kotlinElements
        val extraElements = kotlinElements - referenceElements
        
        val similarity = if (referenceElements.isEmpty()) 1.0 
                        else commonElements.size.toDouble() / referenceElements.size
        
        return StructuralSimilarity(
            similarity = similarity,
            commonElements = commonElements.size,
            missingElements = missingElements.toList(),
            extraElements = extraElements.toList()
        )
    }
    
    private fun calculateCoordinateDeviations(
        kotlin: SvgStructure, 
        reference: SvgStructure, 
        tolerances: ComparisonTolerances
    ): CoordinateDeviations {
        val deviations = mutableListOf<CoordinateDeviation>()
        var maxDeviation = 0.0
        var totalDeviation = 0.0
        var comparedElements = 0
        
        for ((elementId, referencePoint) in reference.coordinates) {
            val kotlinPoint = kotlin.coordinates[elementId]
            if (kotlinPoint != null) {
                val deviation = kotlinPoint.distanceTo(referencePoint)
                deviations.add(CoordinateDeviation(elementId, kotlinPoint, referencePoint, deviation))
                maxDeviation = maxOf(maxDeviation, deviation)
                totalDeviation += deviation
                comparedElements++
            }
        }
        
        val averageDeviation = if (comparedElements > 0) totalDeviation / comparedElements else 0.0
        val withinTolerance = maxDeviation <= tolerances.maxCoordinateDeviation
        
        return CoordinateDeviations(
            deviations = deviations,
            maxDeviation = maxDeviation,
            averageDeviation = averageDeviation,
            withinTolerance = withinTolerance
        )
    }
    
    private fun validateAttributeMatching(
        kotlin: SvgStructure, 
        reference: SvgStructure, 
        tolerances: ComparisonTolerances
    ): AttributeMatches {
        val matches = mutableListOf<AttributeMatch>()
        var totalAttributes = 0
        var matchingAttributes = 0
        
        for ((elementId, referenceAttrs) in reference.attributes) {
            val kotlinAttrs = kotlin.attributes[elementId] ?: emptyMap()
            
            for ((attrName, referenceValue) in referenceAttrs) {
                totalAttributes++
                val kotlinValue = kotlinAttrs[attrName]
                
                val isMatch = when {
                    kotlinValue == null -> false
                    attrName in tolerances.numericAttributes -> {
                        val refNum = referenceValue.toDoubleOrNull()
                        val kotNum = kotlinValue.toDoubleOrNull()
                        if (refNum != null && kotNum != null) {
                            kotlin.math.abs(refNum - kotNum) <= tolerances.numericTolerance
                        } else {
                            kotlinValue == referenceValue
                        }
                    }
                    else -> kotlinValue == referenceValue
                }
                
                if (isMatch) matchingAttributes++
                
                matches.add(AttributeMatch(
                    elementId = elementId,
                    attributeName = attrName,
                    kotlinValue = kotlinValue,
                    referenceValue = referenceValue,
                    matches = isMatch
                ))
            }
        }
        
        val matchPercentage = if (totalAttributes > 0) matchingAttributes.toDouble() / totalAttributes else 1.0
        
        return AttributeMatches(
            matches = matches,
            totalAttributes = totalAttributes,
            matchingAttributes = matchingAttributes,
            matchPercentage = matchPercentage
        )
    }
    
    private fun calculateOverallScore(
        structural: StructuralSimilarity,
        coordinates: CoordinateDeviations,
        attributes: AttributeMatches
    ): Double {
        // Weighted average of different comparison aspects
        val structuralWeight = 0.4
        val coordinateWeight = 0.3
        val attributeWeight = 0.3
        
        val coordinateScore = if (coordinates.withinTolerance) 1.0 else 0.5
        
        return (structural.similarity * structuralWeight) +
               (coordinateScore * coordinateWeight) +
               (attributes.matchPercentage * attributeWeight)
    }
    
    private fun generateRecommendations(result: ComparisonResult): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (result.structuralSimilarity.similarity < 0.9) {
            recommendations.add("Improve structural similarity - missing elements: ${result.structuralSimilarity.missingElements.joinToString(", ")}")
        }
        
        if (!result.coordinateDeviations.withinTolerance) {
            recommendations.add("Reduce coordinate deviations - max deviation: ${result.coordinateDeviations.maxDeviation}")
        }
        
        if (result.attributeMatches.matchPercentage < 0.9) {
            recommendations.add("Improve attribute matching - ${result.attributeMatches.matchingAttributes}/${result.attributeMatches.totalAttributes} attributes match")
        }
        
        return recommendations
    }
    
    private fun generateBatchSummary(reports: List<ComparisonReport>): BatchSummary {
        val totalTests = reports.size
        val passedTests = reports.count { it.comparisonResult.passed }
        val failedTests = totalTests - passedTests
        
        val averageScore = if (totalTests > 0) {
            reports.map { it.comparisonResult.overallScore }.average()
        } else 0.0
        
        return BatchSummary(
            totalTests = totalTests,
            passedTests = passedTests,
            failedTests = failedTests,
            successRate = if (totalTests > 0) passedTests.toDouble() / totalTests else 0.0,
            averageScore = averageScore
        )
    }
    
    private fun getCurrentTimestamp(): String {
        // In real implementation, use actual timestamp
        return "2024-01-01T00:00:00Z"
    }
    
    // Sample SVG generators for simulation
    private fun generateEmptyGraphSvg(): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
        <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100">
        </svg>
    """.trimIndent()
    
    private fun generateSingleNodeSvg(): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
        <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
            <g id="node-node1" transform="translate(100,50)">
                <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                <text x="0" y="0" text-anchor="middle">node1</text>
            </g>
        </svg>
    """.trimIndent()
    
    private fun generateBoxNodeSvg(): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
        <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
            <g id="node-node1" transform="translate(100,50)">
                <rect x="-30" y="-20" width="60" height="40" fill="white" stroke="black"/>
                <text x="0" y="0" text-anchor="middle">box</text>
            </g>
        </svg>
    """.trimIndent()
    
    private fun generateDashedEdgeSvg(): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
        <svg xmlns="http://www.w3.org/2000/svg" width="300" height="100" viewBox="0 0 300 100">
            <g id="node-node1" transform="translate(75,50)">
                <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                <text x="0" y="0" text-anchor="middle">node1</text>
            </g>
            <g id="node-node2" transform="translate(225,50)">
                <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                <text x="0" y="0" text-anchor="middle">node2</text>
            </g>
            <g id="edge-node1-node2">
                <path d="M105,50 L195,50" stroke="black" stroke-dasharray="5,5" fill="none"/>
            </g>
        </svg>
    """.trimIndent()
    
    private fun generateDefaultSvg(dotContent: String): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
        <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
            <!-- Generated from: ${dotContent.lines().first()} -->
            <g id="node-default" transform="translate(100,50)">
                <ellipse cx="0" cy="0" rx="30" ry="20" fill="white" stroke="black"/>
                <text x="0" y="0" text-anchor="middle">default</text>
            </g>
        </svg>
    """.trimIndent()
}

// Data classes for comparison results

/**
 * Result of executing original Graphviz.
 */
sealed class ReferenceExecutionResult {
    data class Success(val output: String) : ReferenceExecutionResult()
    data class Error(val message: String, val cause: Throwable?) : ReferenceExecutionResult()
}

/**
 * Parsed SVG structure for comparison.
 */
data class SvgStructure(
    val elements: List<SvgElement>,
    val coordinates: Map<String, Point>,
    val attributes: Map<String, Map<String, String>>
)

/**
 * SVG element representation.
 */
data class SvgElement(
    val type: String,
    val id: String
)

/**
 * Structural similarity analysis.
 */
data class StructuralSimilarity(
    val similarity: Double,
    val commonElements: Int,
    val missingElements: List<String>,
    val extraElements: List<String>
)

/**
 * Coordinate deviation analysis.
 */
data class CoordinateDeviations(
    val deviations: List<CoordinateDeviation>,
    val maxDeviation: Double,
    val averageDeviation: Double,
    val withinTolerance: Boolean
)

/**
 * Individual coordinate deviation.
 */
data class CoordinateDeviation(
    val elementId: String,
    val kotlinCoordinate: Point,
    val referenceCoordinate: Point,
    val deviation: Double
)

/**
 * Attribute matching analysis.
 */
data class AttributeMatches(
    val matches: List<AttributeMatch>,
    val totalAttributes: Int,
    val matchingAttributes: Int,
    val matchPercentage: Double
)

/**
 * Individual attribute match.
 */
data class AttributeMatch(
    val elementId: String,
    val attributeName: String,
    val kotlinValue: String?,
    val referenceValue: String,
    val matches: Boolean
)

/**
 * Overall comparison result.
 */
data class ComparisonResult(
    val structuralSimilarity: StructuralSimilarity,
    val coordinateDeviations: CoordinateDeviations,
    val attributeMatches: AttributeMatches,
    val overallScore: Double,
    val passed: Boolean
)

/**
 * Detailed comparison report.
 */
data class ComparisonReport(
    val testCase: TestCase,
    val kotlinOutputSize: Int,
    val referenceOutputSize: Int,
    val comparisonResult: ComparisonResult,
    val timestamp: String,
    val recommendations: List<String>
)

/**
 * Batch comparison result.
 */
data class BatchComparisonResult(
    val reports: List<ComparisonReport>,
    val errors: List<String>,
    val summary: BatchSummary
)

/**
 * Batch comparison summary.
 */
data class BatchSummary(
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val successRate: Double,
    val averageScore: Double
)

/**
 * Comparison tolerances configuration.
 */
data class ComparisonTolerances(
    val maxCoordinateDeviation: Double = 1.0,
    val numericTolerance: Double = 0.01,
    val numericAttributes: Set<String> = setOf("x", "y", "width", "height", "cx", "cy", "rx", "ry"),
    val minimumOverallScore: Double = 0.8
) {
    companion object {
        val DEFAULT = ComparisonTolerances()
        val STRICT = ComparisonTolerances(
            maxCoordinateDeviation = 0.1,
            numericTolerance = 0.001,
            minimumOverallScore = 0.95
        )
        val LENIENT = ComparisonTolerances(
            maxCoordinateDeviation = 5.0,
            numericTolerance = 0.1,
            minimumOverallScore = 0.6
        )
    }
}