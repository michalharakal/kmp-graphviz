package org.graphviz.kotlin.validation

import org.graphviz.kotlin.model.*

/**
 * Simple input validation system that validates user inputs with helpful error messages.
 * Uses the existing ValidationResult from the attribute system.
 */
class SimpleInputValidator {
    
    /**
     * Validate a DOT identifier string.
     */
    fun validateIdentifier(identifier: String, context: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        try {
            // Check for empty identifier
            if (identifier.isBlank()) {
                errors.add("Identifier cannot be empty or blank in $context")
                return ValidationResult.Invalid(errors)
            }
            
            // Check for valid characters
            if (!isValidIdentifier(identifier)) {
                errors.add("Identifier '$identifier' contains invalid characters in $context. " +
                        "Identifiers must start with a letter or underscore and contain only " +
                        "letters, digits, and underscores, or be properly quoted.")
            }
            
            // Check for reserved keywords
            if (isReservedKeyword(identifier)) {
                errors.add("Identifier '$identifier' is a reserved keyword and must be quoted in $context")
            }
            
        } catch (e: Exception) {
            errors.add("Validation failed for identifier '$identifier' in $context: ${e.message}")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Validate coordinate values.
     */
    fun validateCoordinate(value: Double, context: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        try {
            if (!value.isFinite()) {
                errors.add("Coordinate value must be finite in $context, got: $value")
            }
            
            // Check for reasonable bounds (prevent overflow in calculations)
            if (value < -1e6 || value > 1e6) {
                errors.add("Coordinate value $value is outside reasonable bounds (-1e6 to 1e6) in $context")
            }
        } catch (e: Exception) {
            errors.add("Validation failed for coordinate in $context: ${e.message}")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Validate a node structure.
     */
    fun validateNode(node: Node): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate node ID
        val idResult = validateIdentifier(node.id, "node")
        if (!idResult.isValid) {
            errors.addAll((idResult as ValidationResult.Invalid).errors)
        }
        
        // Validate node position if present
        node.position?.let { position ->
            val xResult = validateCoordinate(position.x, "node ${node.id} x-coordinate")
            if (!xResult.isValid) {
                errors.addAll((xResult as ValidationResult.Invalid).errors)
            }
            
            val yResult = validateCoordinate(position.y, "node ${node.id} y-coordinate")
            if (!yResult.isValid) {
                errors.addAll((yResult as ValidationResult.Invalid).errors)
            }
        }
        
        // Validate node attributes
        val attrResult = node.attributes.validate()
        if (!attrResult.isValid) {
            errors.addAll((attrResult as ValidationResult.Invalid).errors.map { 
                "Node ${node.id}: $it" 
            })
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Validate an edge structure.
     */
    fun validateEdge(edge: Edge, availableNodes: Set<Node>): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate edge references
        if (!availableNodes.contains(edge.source)) {
            errors.add("Edge source node '${edge.source.id}' not found in graph")
        }
        
        if (!availableNodes.contains(edge.target)) {
            errors.add("Edge target node '${edge.target.id}' not found in graph")
        }
        
        // Validate control points
        edge.controlPoints.forEachIndexed { index, point ->
            val xResult = validateCoordinate(point.x, "edge ${edge.source.id}->${edge.target.id} control point $index x")
            if (!xResult.isValid) {
                errors.addAll((xResult as ValidationResult.Invalid).errors)
            }
            
            val yResult = validateCoordinate(point.y, "edge ${edge.source.id}->${edge.target.id} control point $index y")
            if (!yResult.isValid) {
                errors.addAll((yResult as ValidationResult.Invalid).errors)
            }
        }
        
        // Validate edge attributes
        val attrResult = edge.attributes.validate()
        if (!attrResult.isValid) {
            errors.addAll((attrResult as ValidationResult.Invalid).errors.map { 
                "Edge ${edge.source.id}->${edge.target.id}: $it" 
            })
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Validate a complete graph structure.
     */
    fun validateGraph(graph: Graph): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate graph ID
        val idResult = validateIdentifier(graph.id, "graph")
        if (!idResult.isValid) {
            errors.addAll((idResult as ValidationResult.Invalid).errors)
        }
        
        // Validate all nodes
        for (node in graph.getAllNodes()) {
            val nodeResult = validateNode(node)
            if (!nodeResult.isValid) {
                errors.addAll((nodeResult as ValidationResult.Invalid).errors)
            }
        }
        
        // Validate all edges
        val allNodes = graph.getAllNodes()
        for (edge in graph.getAllEdges()) {
            val edgeResult = validateEdge(edge, allNodes)
            if (!edgeResult.isValid) {
                errors.addAll((edgeResult as ValidationResult.Invalid).errors)
            }
        }
        
        // Validate graph attributes
        val attrResult = graph.attributes.validate()
        if (!attrResult.isValid) {
            errors.addAll((attrResult as ValidationResult.Invalid).errors.map { 
                "Graph ${graph.id}: $it" 
            })
        }
        
        // Check for reasonable graph size limits
        val totalNodes = allNodes.size
        val totalEdges = graph.getAllEdges().size
        
        if (totalNodes > 10000) {
            errors.add("Graph has $totalNodes nodes, which exceeds recommended limit of 10000")
        }
        
        if (totalEdges > 50000) {
            errors.add("Graph has $totalEdges edges, which exceeds recommended limit of 50000")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    // Private helper methods
    
    private fun isValidIdentifier(identifier: String): Boolean {
        // Check if it's a quoted string
        if (identifier.startsWith('"') && identifier.endsWith('"') && identifier.length >= 2) {
            return true
        }
        
        // Check if it's a valid unquoted identifier
        if (identifier.isEmpty()) return false
        
        // Must start with letter or underscore
        if (!identifier[0].isLetter() && identifier[0] != '_') {
            return false
        }
        
        // Rest must be letters, digits, or underscores
        return identifier.all { it.isLetterOrDigit() || it == '_' }
    }
    
    private fun isReservedKeyword(identifier: String): Boolean {
        val keywords = setOf(
            "graph", "digraph", "subgraph", "node", "edge", "strict",
            "Graph", "Digraph", "Subgraph", "Node", "Edge", "Strict"
        )
        return keywords.contains(identifier)
    }
}