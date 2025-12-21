package org.graphviz.kotlin.integration

import org.graphviz.kotlin.model.Point

/**
 * Utility functions for visual validation in integration tests.
 */
object VisualValidationUtils {
    
    /**
     * Validates that SVG content contains required structural elements.
     */
    fun validateSvgStructure(svg: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Check XML declaration
        if (!svg.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) {
            errors.add("Missing XML declaration")
        }
        
        // Check DOCTYPE
        if (!svg.contains("<!DOCTYPE svg")) {
            errors.add("Missing DOCTYPE declaration")
        }
        
        // Check SVG namespace
        if (!svg.contains("xmlns=\"http://www.w3.org/2000/svg\"")) {
            errors.add("Missing SVG namespace")
        }
        
        // Check SVG root element
        if (!svg.contains("<svg") || !svg.contains("</svg>")) {
            errors.add("Missing SVG root element")
        }
        
        // Check dimensions
        if (!svg.contains("width=") || !svg.contains("height=")) {
            errors.add("Missing SVG dimensions")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    /**
     * Validates that SVG contains expected node elements.
     */
    fun validateNodesPresent(svg: String, expectedNodeIds: List<String>): ValidationResult {
        val errors = mutableListOf<String>()
        
        for (nodeId in expectedNodeIds) {
            if (!svg.contains("id=\"node-$nodeId\"")) {
                errors.add("Missing node: $nodeId")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    /**
     * Validates that SVG contains expected edge elements.
     */
    fun validateEdgesPresent(svg: String, expectedEdges: List<Pair<String, String>>): ValidationResult {
        val errors = mutableListOf<String>()
        
        for ((source, target) in expectedEdges) {
            if (!svg.contains("id=\"edge-$source-$target\"")) {
                errors.add("Missing edge: $source -> $target")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    /**
     * Validates that text labels are present in SVG.
     */
    fun validateLabelsPresent(svg: String, expectedLabels: List<String>): ValidationResult {
        val errors = mutableListOf<String>()
        
        for (label in expectedLabels) {
            if (!svg.contains(label)) {
                errors.add("Missing label: $label")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    /**
     * Validates hierarchical positioning of nodes.
     */
    fun validateHierarchicalLayout(
        nodes: Map<String, Point>,
        expectedLevels: Map<String, Int>
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Group nodes by expected level
        val levelGroups = expectedLevels.entries.groupBy { it.value }
        
        for ((level, nodeEntries) in levelGroups) {
            val nodeIds = nodeEntries.map { it.key }
            val yCoordinates = nodeIds.mapNotNull { nodes[it]?.y }
            
            if (yCoordinates.size != nodeIds.size) {
                errors.add("Missing positions for level $level nodes")
                continue
            }
            
            // Check that nodes at the same level have similar Y coordinates
            val minY = yCoordinates.minOrNull()!!
            val maxY = yCoordinates.maxOrNull()!!
            val tolerance = 5.0 // Allow small variations
            
            if (maxY - minY > tolerance) {
                errors.add("Level $level nodes not aligned: Y range ${minY}-${maxY}")
            }
        }
        
        // Check that levels are properly ordered
        val levelYCoordinates = levelGroups.mapValues { (_, nodeEntries) ->
            val nodeIds = nodeEntries.map { it.key }
            nodeIds.mapNotNull { nodes[it]?.y }.average()
        }
        
        val sortedLevels = levelYCoordinates.toList().sortedBy { it.second }
        val expectedOrder = levelYCoordinates.keys.sorted()
        val actualOrder = sortedLevels.map { it.first }
        
        if (actualOrder != expectedOrder) {
            errors.add("Level ordering incorrect: expected $expectedOrder, got $actualOrder")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    /**
     * Validates that coordinate values are within reasonable bounds.
     */
    fun validateCoordinateBounds(
        nodes: Map<String, Point>,
        minX: Double = -1000.0,
        maxX: Double = 1000.0,
        minY: Double = -1000.0,
        maxY: Double = 1000.0
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        for ((nodeId, point) in nodes) {
            if (point.x < minX || point.x > maxX) {
                errors.add("Node $nodeId X coordinate ${point.x} out of bounds [$minX, $maxX]")
            }
            if (point.y < minY || point.y > maxY) {
                errors.add("Node $nodeId Y coordinate ${point.y} out of bounds [$minY, $maxY]")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    /**
     * Validates that SVG contains proper styling attributes.
     */
    fun validateStyling(svg: String, expectedStyles: List<String>): ValidationResult {
        val errors = mutableListOf<String>()
        
        for (style in expectedStyles) {
            if (!svg.contains(style)) {
                errors.add("Missing style: $style")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    /**
     * Extracts coordinate information from SVG for validation.
     */
    fun extractCoordinatesFromSvg(svg: String): Map<String, Point> {
        val coordinates = mutableMapOf<String, Point>()
        
        // This is a simplified extraction - in a real implementation,
        // you would parse the SVG XML properly
        val nodePattern = Regex("""id="node-(\w+)"[^>]*transform="translate\(([^,]+),([^)]+)\)"""")
        
        nodePattern.findAll(svg).forEach { match ->
            val nodeId = match.groupValues[1]
            val x = match.groupValues[2].toDoubleOrNull() ?: 0.0
            val y = match.groupValues[3].toDoubleOrNull() ?: 0.0
            coordinates[nodeId] = Point(x, y)
        }
        
        return coordinates
    }
    
    /**
     * Compares two SVG outputs for structural similarity.
     */
    fun compareSvgStructure(svg1: String, svg2: String): Double {
        // Simplified structural comparison
        val elements1 = extractSvgElements(svg1)
        val elements2 = extractSvgElements(svg2)
        
        val commonElements = elements1.intersect(elements2).size
        val totalElements = elements1.union(elements2).size
        
        return if (totalElements == 0) 1.0 else commonElements.toDouble() / totalElements
    }
    
    private fun extractSvgElements(svg: String): Set<String> {
        val elements = mutableSetOf<String>()
        
        // Extract element types and IDs
        val elementPattern = Regex("""<(\w+)[^>]*id="([^"]+)"""")
        elementPattern.findAll(svg).forEach { match ->
            elements.add("${match.groupValues[1]}:${match.groupValues[2]}")
        }
        
        return elements
    }
}

/**
 * Result of a validation operation.
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errors: List<String>) : ValidationResult()
    
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    
    fun getErrorsOrEmpty(): List<String> = when (this) {
        is Success -> emptyList()
        is Failure -> errors
    }
}