package org.graphviz.kotlin.parser

/**
 * Represents different types of tokens in the DOT language.
 */
enum class DotTokenType {
    // Keywords
    GRAPH,
    DIGRAPH,
    SUBGRAPH,
    NODE,
    EDGE,
    STRICT,
    
    // Operators and punctuation
    LBRACE,        // {
    RBRACE,        // }
    LBRACKET,      // [
    RBRACKET,      // ]
    LPAREN,        // (
    RPAREN,        // )
    SEMICOLON,     // ;
    COMMA,         // ,
    EQUALS,        // =
    ARROW,         // ->
    EDGE_OP,       // -- (undirected edge)
    
    // Literals
    IDENTIFIER,
    STRING_LITERAL,
    NUMBER_LITERAL,
    
    // Special tokens
    NEWLINE,
    WHITESPACE,
    COMMENT,
    EOF,
    
    // Error token
    ERROR
}

/**
 * Represents a token in the DOT language with position information.
 */
data class DotToken(
    val type: DotTokenType,
    val value: String,
    val line: Int,
    val column: Int,
    val startPosition: Int,
    val endPosition: Int
) {
    /**
     * Check if this token is a keyword.
     */
    val isKeyword: Boolean
        get() = type in setOf(
            DotTokenType.GRAPH,
            DotTokenType.DIGRAPH,
            DotTokenType.SUBGRAPH,
            DotTokenType.NODE,
            DotTokenType.EDGE,
            DotTokenType.STRICT
        )
    
    /**
     * Check if this token is an operator.
     */
    val isOperator: Boolean
        get() = type in setOf(
            DotTokenType.LBRACE,
            DotTokenType.RBRACE,
            DotTokenType.LBRACKET,
            DotTokenType.RBRACKET,
            DotTokenType.LPAREN,
            DotTokenType.RPAREN,
            DotTokenType.SEMICOLON,
            DotTokenType.COMMA,
            DotTokenType.EQUALS,
            DotTokenType.ARROW,
            DotTokenType.EDGE_OP
        )
    
    /**
     * Check if this token is a literal value.
     */
    val isLiteral: Boolean
        get() = type in setOf(
            DotTokenType.IDENTIFIER,
            DotTokenType.STRING_LITERAL,
            DotTokenType.NUMBER_LITERAL
        )
    
    /**
     * Check if this token should be ignored during parsing (whitespace, comments, newlines).
     */
    val isIgnorable: Boolean
        get() = type in setOf(
            DotTokenType.WHITESPACE,
            DotTokenType.COMMENT,
            DotTokenType.NEWLINE
        )
    
    override fun toString(): String {
        return "DotToken(type=$type, value='$value', line=$line, column=$column)"
    }
}

/**
 * Position information for error reporting.
 */
data class Position(
    val line: Int,
    val column: Int,
    val index: Int
) {
    override fun toString(): String = "line $line, column $column"
}