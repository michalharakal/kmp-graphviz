package org.graphviz.kotlin.generator

import org.graphviz.kotlin.model.*

/**
 * Generates DOT language output from graph data structures.
 * 
 * This generator produces syntactically correct DOT language text with consistent
 * indentation and readable formatting. It handles proper escaping of special characters
 * in identifiers and preserves all graph, node, and edge attributes.
 */
class DotGenerator(
    private val options: DotGeneratorOptions = DotGeneratorOptions()
) {
    
    companion object {
        /**
         * Generate DOT language output from a graph using default options.
         */
        fun generate(graph: Graph, options: DotGeneratorOptions = DotGeneratorOptions()): String {
            val generator = DotGenerator(options)
            return generator.generate(graph)
        }
    }
    
    /**
     * Generate DOT language output from a graph.
     */
    fun generate(graph: Graph): String {
        val builder = StringBuilder()
        generateGraph(graph, builder, 0)
        return builder.toString()
    }
    
    /**
     * Generate a graph declaration with all its contents.
     */
    private fun generateGraph(graph: Graph, builder: StringBuilder, indentLevel: Int) {
        val indent = getIndent(indentLevel)
        
        // Graph declaration
        val graphType = if (graph.isDirected) "digraph" else "graph"
        val graphId = escapeIdentifier(graph.id)
        
        builder.append("$indent$graphType $graphId {\n")
        
        // Graph attributes
        if (!graph.attributes.isEmpty) {
            generateGraphAttributes(graph.attributes, builder, indentLevel + 1)
        }
        
        // Nodes
        for (node in graph.nodes) {
            generateNode(node, builder, indentLevel + 1)
        }
        
        // Edges
        for (edge in graph.edges) {
            generateEdge(edge, graph.isDirected, builder, indentLevel + 1)
        }
        
        // Subgraphs
        for (subgraph in graph.subgraphs) {
            generateSubgraph(subgraph, builder, indentLevel + 1)
        }
        
        builder.append("$indent}\n")
    }
    
    /**
     * Generate graph-level attributes.
     */
    private fun generateGraphAttributes(attributes: AttributeMap, builder: StringBuilder, indentLevel: Int) {
        val indent = getIndent(indentLevel)
        
        for (key in attributes.keys.sorted()) {
            val value = attributes.getRaw(key)
            if (value != null) {
                val valueStr = formatAttributeValue(value)
                builder.append("$indent${escapeIdentifier(key)}=$valueStr;\n")
            }
        }
    }
    
    /**
     * Generate a node statement.
     */
    private fun generateNode(node: Node, builder: StringBuilder, indentLevel: Int) {
        val indent = getIndent(indentLevel)
        val nodeId = escapeIdentifier(node.id)
        
        builder.append("$indent$nodeId")
        
        // Node attributes
        if (!node.attributes.isEmpty) {
            builder.append(" ")
            generateAttributeList(node.attributes, builder)
        }
        
        builder.append(";\n")
    }
    
    /**
     * Generate an edge statement.
     */
    private fun generateEdge(edge: Edge, isDirected: Boolean, builder: StringBuilder, indentLevel: Int) {
        val indent = getIndent(indentLevel)
        val sourceId = escapeIdentifier(edge.source.id)
        val targetId = escapeIdentifier(edge.target.id)
        val edgeOp = if (isDirected) "->" else "--"
        
        builder.append("$indent$sourceId $edgeOp $targetId")
        
        // Edge attributes
        if (!edge.attributes.isEmpty) {
            builder.append(" ")
            generateAttributeList(edge.attributes, builder)
        }
        
        builder.append(";\n")
    }
    
    /**
     * Generate a subgraph declaration.
     */
    private fun generateSubgraph(subgraph: Graph, builder: StringBuilder, indentLevel: Int) {
        val indent = getIndent(indentLevel)
        val subgraphId = escapeIdentifier(subgraph.id)
        
        builder.append("${indent}subgraph $subgraphId {\n")
        
        // Subgraph attributes
        if (!subgraph.attributes.isEmpty) {
            generateGraphAttributes(subgraph.attributes, builder, indentLevel + 1)
        }
        
        // Nodes
        for (node in subgraph.nodes) {
            generateNode(node, builder, indentLevel + 1)
        }
        
        // Edges
        for (edge in subgraph.edges) {
            generateEdge(edge, subgraph.isDirected, builder, indentLevel + 1)
        }
        
        // Nested subgraphs
        for (nestedSubgraph in subgraph.subgraphs) {
            generateSubgraph(nestedSubgraph, builder, indentLevel + 1)
        }
        
        builder.append("$indent}\n")
    }
    
    /**
     * Generate an attribute list [key=value, key=value].
     */
    private fun generateAttributeList(attributes: AttributeMap, builder: StringBuilder) {
        builder.append("[")
        
        val sortedKeys = attributes.keys.sorted()
        val attributePairs = sortedKeys.mapNotNull { key ->
            val value = attributes.getRaw(key)
            if (value != null) {
                "${escapeIdentifier(key)}=${formatAttributeValue(value)}"
            } else {
                null
            }
        }
        
        builder.append(attributePairs.joinToString(", "))
        builder.append("]")
    }
    
    /**
     * Format an attribute value for DOT output.
     */
    private fun formatAttributeValue(value: AttributeValue): String {
        return when (value) {
            is AttributeValue.StringValue -> escapeString(value.value)
            is AttributeValue.NumberValue -> formatNumber(value.value)
            is AttributeValue.ColorValue -> escapeString(value.value.toDotString())
            is AttributeValue.BooleanValue -> value.value.toString()
        }
    }
    
    /**
     * Format a number value, removing unnecessary decimal places.
     */
    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }
    
    /**
     * Escape an identifier if it contains special characters.
     */
    private fun escapeIdentifier(id: String): String {
        // Check if the identifier needs quoting
        if (needsQuoting(id)) {
            return escapeString(id)
        }
        return id
    }
    
    /**
     * Check if a string needs quoting.
     */
    private fun needsQuoting(str: String): Boolean {
        if (str.isEmpty()) return true
        
        // Check if it's a number - numeric identifiers should be quoted
        if (str.toDoubleOrNull() != null) return true
        
        // Check if it starts with a digit
        if (str[0].isDigit()) return true
        
        // Check for special characters
        for (char in str) {
            if (!char.isLetterOrDigit() && char != '_') {
                return true
            }
        }
        
        // Check if it's a keyword
        if (isKeyword(str)) return true
        
        return false
    }
    
    /**
     * Check if a string is a DOT keyword.
     */
    private fun isKeyword(str: String): Boolean {
        val keywords = setOf(
            "graph", "digraph", "subgraph", "node", "edge",
            "strict", "Graph", "Digraph", "Subgraph", "Node", "Edge"
        )
        return keywords.contains(str)
    }
    
    /**
     * Escape a string value for DOT output.
     */
    private fun escapeString(str: String): String {
        val escaped = StringBuilder()
        escaped.append('"')
        
        for (char in str) {
            when (char) {
                '"' -> escaped.append("\\\"")
                '\\' -> escaped.append("\\\\")
                '\n' -> escaped.append("\\n")
                '\r' -> escaped.append("\\r")
                '\t' -> escaped.append("\\t")
                else -> escaped.append(char)
            }
        }
        
        escaped.append('"')
        return escaped.toString()
    }
    
    /**
     * Get the indentation string for a given level.
     */
    private fun getIndent(level: Int): String {
        return options.indentString.repeat(level)
    }
}

/**
 * Options for controlling DOT generation.
 */
data class DotGeneratorOptions(
    /**
     * The string to use for each level of indentation.
     */
    val indentString: String = "  ",
    
    /**
     * Whether to sort attributes alphabetically.
     */
    val sortAttributes: Boolean = true
)
