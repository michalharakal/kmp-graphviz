package org.graphviz.kotlin.parser

import org.graphviz.kotlin.model.*

/**
 * Recursive descent parser for the DOT language.
 * 
 * This parser implements the DOT language grammar to parse graph declarations,
 * node statements, edge statements, attribute lists, and subgraph definitions.
 * It supports both directed and undirected graph syntax.
 */
class DotParser(private val tokens: List<DotToken>) {
    private var position = 0
    
    companion object {
        /**
         * Parse DOT language input from a string.
         */
        fun parse(input: String): ParseResult<Graph> {
            return try {
                val lexer = DotLexer(input)
                val tokens = lexer.tokenizeFiltered()
                val parser = DotParser(tokens)
                parser.parseGraph()
            } catch (e: Exception) {
                ParseResult.Error(
                    message = "Lexical analysis failed: ${e.message}",
                    line = 1,
                    column = 1,
                    position = 0,
                    cause = e
                )
            }
        }
    }
    
    /**
     * Get the current token without advancing.
     */
    private fun peek(offset: Int = 0): DotToken? {
        val pos = position + offset
        return if (pos < tokens.size) tokens[pos] else null
    }
    
    /**
     * Get the current token and advance the position.
     */
    private fun advance(): DotToken? {
        return if (position < tokens.size) {
            tokens[position++]
        } else {
            null
        }
    }
    
    /**
     * Check if the current token matches the expected type.
     */
    private fun check(type: DotTokenType): Boolean {
        return peek()?.type == type
    }
    
    /**
     * Consume a token of the expected type, or return an error.
     */
    private fun consume(type: DotTokenType, message: String? = null): ParseResult<DotToken> {
        val token = peek()
        return if (token?.type == type) {
            advance()
            ParseResult.Success(token)
        } else {
            val errorMessage = message ?: "Expected ${type.name} but found ${token?.type?.name ?: "EOF"}"
            createError(errorMessage, token)
        }
    }
    
    /**
     * Create an error result with current position information.
     */
    private fun createError(message: String, token: DotToken? = null): ParseResult.Error {
        val currentToken = token ?: peek()
        return ParseResult.Error(
            message = message,
            line = currentToken?.line ?: 1,
            column = currentToken?.column ?: 1,
            position = currentToken?.startPosition ?: 0,
            context = getContext(currentToken)
        )
    }
    
    /**
     * Get context information for error reporting.
     */
    private fun getContext(token: DotToken?): String {
        if (token == null) return "End of input"
        
        val start = maxOf(0, position - 3)
        val end = minOf(tokens.size, position + 3)
        val contextTokens = tokens.subList(start, end)
        
        return contextTokens.joinToString(" ") { 
            if (it == token) ">>>${it.value}<<<" else it.value 
        }
    }
    
    /**
     * Parse the top-level graph.
     */
    fun parseGraph(): ParseResult<Graph> {
        // Handle optional 'strict' keyword
        val isStrict = if (check(DotTokenType.STRICT)) {
            advance()
            true
        } else {
            false
        }
        
        // Parse graph type (graph or digraph)
        val graphTypeResult = when {
            check(DotTokenType.GRAPH) -> {
                advance()
                ParseResult.Success(false) // undirected
            }
            check(DotTokenType.DIGRAPH) -> {
                advance()
                ParseResult.Success(true) // directed
            }
            else -> createError("Expected 'graph' or 'digraph'")
        }
        
        if (graphTypeResult.isError) return graphTypeResult as ParseResult.Error
        val isDirected = (graphTypeResult as ParseResult.Success).value
        
        // Parse optional graph ID
        val graphId = if (check(DotTokenType.IDENTIFIER)) {
            advance()!!.value
        } else {
            "G" // default graph name
        }
        
        // Parse graph body
        val bodyResult = parseGraphBody(graphId, isDirected, isStrict)
        if (bodyResult.isError) return bodyResult
        
        // Ensure we've consumed all tokens
        if (!check(DotTokenType.EOF)) {
            return createError("Unexpected tokens after graph definition")
        }
        
        return bodyResult
    }
    
    /**
     * Parse the graph body (statements within braces).
     */
    private fun parseGraphBody(id: String, isDirected: Boolean, isStrict: Boolean): ParseResult<Graph> {
        val openBraceResult = consume(DotTokenType.LBRACE, "Expected '{' to start graph body")
        if (openBraceResult.isError) return openBraceResult as ParseResult.Error
        
        val nodes = mutableMapOf<String, NodeImpl>()
        val edges = mutableListOf<EdgeImpl>()
        val subgraphs = mutableListOf<Graph>()
        val graphAttributes = AttributeMap.builder()
        
        // Parse statements until closing brace
        while (!check(DotTokenType.RBRACE) && !check(DotTokenType.EOF)) {
            val statementResult = parseStatement(nodes, edges, subgraphs, graphAttributes, isDirected)
            if (statementResult.isError) return statementResult as ParseResult.Error
            
            // Optional semicolon
            if (check(DotTokenType.SEMICOLON)) {
                advance()
            }
        }
        
        val closeBraceResult = consume(DotTokenType.RBRACE, "Expected '}' to close graph body")
        if (closeBraceResult.isError) return closeBraceResult as ParseResult.Error
        
        return try {
            val graph = GraphImpl(
                id = id,
                isDirected = isDirected,
                nodes = nodes.values.toSet(),
                edges = edges.toSet(),
                subgraphs = subgraphs.toSet(),
                attributes = graphAttributes.build()
            )
            ParseResult.Success(graph)
        } catch (e: Exception) {
            createError("Failed to create graph: ${e.message}")
        }
    }
    
    /**
     * Parse a statement (node, edge, attribute, or subgraph).
     */
    private fun parseStatement(
        nodes: MutableMap<String, NodeImpl>,
        edges: MutableList<EdgeImpl>,
        subgraphs: MutableList<Graph>,
        graphAttributes: AttributeMapBuilder,
        isDirected: Boolean
    ): ParseResult<Unit> {
        
        return when {
            // Subgraph
            check(DotTokenType.SUBGRAPH) -> parseSubgraph(subgraphs, isDirected)
            
            // Graph/node/edge attributes
            check(DotTokenType.GRAPH) || check(DotTokenType.NODE) || check(DotTokenType.EDGE) -> {
                parseAttributeStatement(graphAttributes)
            }
            
            // Node, edge statement, or direct attribute assignment
            check(DotTokenType.IDENTIFIER) -> {
                // Look ahead to see if this is a direct attribute assignment (identifier = value)
                if (peek(1)?.type == DotTokenType.EQUALS) {
                    parseDirectAttributeAssignment(graphAttributes)
                } else {
                    parseNodeOrEdgeStatement(nodes, edges, isDirected)
                }
            }
            
            else -> createError("Expected statement (node, edge, subgraph, or attribute)") as ParseResult<Unit>
        }
    }
    
    /**
     * Parse a subgraph definition.
     */
    private fun parseSubgraph(subgraphs: MutableList<Graph>, isDirected: Boolean): ParseResult<Unit> {
        advance() // consume 'subgraph'
        
        // Optional subgraph ID
        val subgraphId = if (check(DotTokenType.IDENTIFIER)) {
            advance()!!.value
        } else {
            "subgraph_${subgraphs.size}"
        }
        
        val subgraphResult = parseGraphBody(subgraphId, isDirected, false)
        if (subgraphResult.isError) return subgraphResult as ParseResult.Error
        
        subgraphs.add((subgraphResult as ParseResult.Success).value)
        return ParseResult.Success(Unit)
    }
    
    /**
     * Parse a direct attribute assignment (key = value).
     */
    private fun parseDirectAttributeAssignment(graphAttributes: AttributeMapBuilder): ParseResult<Unit> {
        val attributeResult = parseAttribute()
        if (attributeResult.isError) return attributeResult as ParseResult.Error
        
        val (key, value) = (attributeResult as ParseResult.Success).value
        graphAttributes.setRaw(key, value)
        
        return ParseResult.Success(Unit)
    }
    
    /**
     * Parse an attribute statement (graph/node/edge attributes).
     */
    private fun parseAttributeStatement(graphAttributes: AttributeMapBuilder): ParseResult<Unit> {
        val attributeType = advance()!! // consume graph/node/edge
        
        val attributesResult = parseAttributeList()
        if (attributesResult.isError) return attributesResult as ParseResult.Error
        
        val attributes = (attributesResult as ParseResult.Success).value
        
        // For now, we only handle graph attributes
        // Node and edge default attributes would be handled differently in a full implementation
        if (attributeType.type == DotTokenType.GRAPH) {
            attributes.keys.forEach { key ->
                val value = attributes.getRaw(key)
                if (value != null) {
                    graphAttributes.setRaw(key, value)
                }
            }
        }
        
        return ParseResult.Success(Unit)
    }
    
    /**
     * Parse a node or edge statement.
     */
    private fun parseNodeOrEdgeStatement(
        nodes: MutableMap<String, NodeImpl>,
        edges: MutableList<EdgeImpl>,
        isDirected: Boolean
    ): ParseResult<Unit> {
        
        // Parse the first node ID
        val firstNodeResult = consume(DotTokenType.IDENTIFIER, "Expected node identifier")
        if (firstNodeResult.isError) return firstNodeResult as ParseResult.Error
        
        val firstNodeId = (firstNodeResult as ParseResult.Success).value.value
        
        // Check if this is an edge statement
        val edgeOp = if (isDirected) DotTokenType.ARROW else DotTokenType.EDGE_OP
        
        if (check(edgeOp)) {
            return parseEdgeStatement(firstNodeId, nodes, edges, isDirected)
        } else {
            return parseNodeStatement(firstNodeId, nodes)
        }
    }
    
    /**
     * Parse a node statement.
     */
    private fun parseNodeStatement(nodeId: String, nodes: MutableMap<String, NodeImpl>): ParseResult<Unit> {
        // Get or create the node
        val existingNode = nodes[nodeId]
        val nodeAttributes = if (existingNode != null) {
            AttributeMap.builder().apply {
                existingNode.attributes.keys.forEach { key ->
                    val value = existingNode.attributes.getRaw(key)
                    if (value != null) {
                        setRaw(key, value)
                    }
                }
            }
        } else {
            AttributeMap.builder()
        }
        
        // Parse optional attribute list
        if (check(DotTokenType.LBRACKET)) {
            val attributesResult = parseAttributeList()
            if (attributesResult.isError) return attributesResult as ParseResult.Error
            
            val attributes = (attributesResult as ParseResult.Success).value
            attributes.keys.forEach { key ->
                val value = attributes.getRaw(key)
                if (value != null) {
                    nodeAttributes.setRaw(key, value)
                }
            }
        }
        
        // Create or update the node
        val node = NodeImpl(
            id = nodeId,
            attributes = nodeAttributes.build(),
            position = existingNode?.position
        )
        
        nodes[nodeId] = node
        return ParseResult.Success(Unit)
    }
    
    /**
     * Parse an edge statement.
     */
    private fun parseEdgeStatement(
        firstNodeId: String,
        nodes: MutableMap<String, NodeImpl>,
        edges: MutableList<EdgeImpl>,
        isDirected: Boolean
    ): ParseResult<Unit> {
        
        val edgeOp = if (isDirected) DotTokenType.ARROW else DotTokenType.EDGE_OP
        var currentNodeId = firstNodeId
        
        // Ensure the first node exists
        if (!nodes.containsKey(currentNodeId)) {
            nodes[currentNodeId] = NodeImpl(currentNodeId, AttributeMap.empty(), null)
        }
        
        // Parse edge chain (A -> B -> C)
        while (check(edgeOp)) {
            advance() // consume edge operator
            
            val targetNodeResult = consume(DotTokenType.IDENTIFIER, "Expected target node identifier")
            if (targetNodeResult.isError) return targetNodeResult as ParseResult.Error
            
            val targetNodeId = (targetNodeResult as ParseResult.Success).value.value
            
            // Ensure the target node exists
            if (!nodes.containsKey(targetNodeId)) {
                nodes[targetNodeId] = NodeImpl(targetNodeId, AttributeMap.empty(), null)
            }
            
            // Create the edge
            val sourceNode = nodes[currentNodeId]!!
            val targetNode = nodes[targetNodeId]!!
            
            val edge = EdgeImpl(
                source = sourceNode,
                target = targetNode,
                attributes = AttributeMap.empty(),
                controlPoints = emptyList()
            )
            
            edges.add(edge)
            currentNodeId = targetNodeId
        }
        
        // Parse optional attribute list for the edge(s)
        if (check(DotTokenType.LBRACKET)) {
            val attributesResult = parseAttributeList()
            if (attributesResult.isError) return attributesResult as ParseResult.Error
            
            val attributes = (attributesResult as ParseResult.Success).value
            
            // Apply attributes to the last edge created
            if (edges.isNotEmpty()) {
                val lastEdge = edges.removeLastOrNull()
                if (lastEdge != null) {
                    val updatedEdge = EdgeImpl(
                        source = lastEdge.source,
                        target = lastEdge.target,
                        attributes = attributes,
                        controlPoints = lastEdge.controlPoints
                    )
                    edges.add(updatedEdge)
                }
            }
        }
        
        return ParseResult.Success(Unit)
    }
    
    /**
     * Parse an attribute list [key=value, key=value].
     */
    private fun parseAttributeList(): ParseResult<AttributeMap> {
        val openBracketResult = consume(DotTokenType.LBRACKET, "Expected '[' to start attribute list")
        if (openBracketResult.isError) return openBracketResult as ParseResult.Error
        
        val attributes = AttributeMap.builder()
        
        // Parse attributes until closing bracket
        while (!check(DotTokenType.RBRACKET) && !check(DotTokenType.EOF)) {
            val attributeResult = parseAttribute()
            if (attributeResult.isError) return attributeResult as ParseResult.Error
            
            val (key, value) = (attributeResult as ParseResult.Success).value
            attributes.setRaw(key, value)
            
            // Optional comma or semicolon
            if (check(DotTokenType.COMMA) || check(DotTokenType.SEMICOLON)) {
                advance()
            }
        }
        
        val closeBracketResult = consume(DotTokenType.RBRACKET, "Expected ']' to close attribute list")
        if (closeBracketResult.isError) return closeBracketResult as ParseResult.Error
        
        return ParseResult.Success(attributes.build())
    }
    
    /**
     * Parse a single attribute (key=value).
     */
    private fun parseAttribute(): ParseResult<Pair<String, AttributeValue>> {
        val keyResult = consume(DotTokenType.IDENTIFIER, "Expected attribute name")
        if (keyResult.isError) return keyResult as ParseResult.Error
        
        val key = (keyResult as ParseResult.Success).value.value
        
        val equalsResult = consume(DotTokenType.EQUALS, "Expected '=' after attribute name")
        if (equalsResult.isError) return equalsResult as ParseResult.Error
        
        val valueResult = parseAttributeValue()
        if (valueResult.isError) return valueResult as ParseResult.Error
        
        val value = (valueResult as ParseResult.Success).value
        
        return ParseResult.Success(key to value)
    }
    
    /**
     * Parse an attribute value (identifier, string, or number).
     */
    private fun parseAttributeValue(): ParseResult<AttributeValue> {
        val token = peek()
        
        return when (token?.type) {
            DotTokenType.IDENTIFIER -> {
                advance()
                ParseResult.Success(AttributeValue.StringValue(token.value))
            }
            
            DotTokenType.STRING_LITERAL -> {
                advance()
                ParseResult.Success(AttributeValue.StringValue(token.value))
            }
            
            DotTokenType.NUMBER_LITERAL -> {
                advance()
                try {
                    val number = token.value.toDouble()
                    ParseResult.Success(AttributeValue.NumberValue(number))
                } catch (e: NumberFormatException) {
                    createError("Invalid number format: ${token.value}")
                }
            }
            
            else -> createError("Expected attribute value (identifier, string, or number)")
        }
    }
}