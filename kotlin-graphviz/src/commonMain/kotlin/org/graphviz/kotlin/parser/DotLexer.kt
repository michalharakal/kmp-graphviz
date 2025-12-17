package org.graphviz.kotlin.parser

/**
 * Lexical analyzer for the DOT language.
 * 
 * This lexer tokenizes DOT language input, handling keywords, identifiers, operators,
 * string literals, comments, and whitespace. It provides line and column tracking
 * for error reporting.
 */
class DotLexer(private val input: String) {
    private var position = 0
    private var line = 1
    private var column = 1
    private var tokenStart = 0
    
    companion object {
        private val KEYWORDS = mapOf(
            "graph" to DotTokenType.GRAPH,
            "digraph" to DotTokenType.DIGRAPH,
            "subgraph" to DotTokenType.SUBGRAPH,
            "node" to DotTokenType.NODE,
            "edge" to DotTokenType.EDGE,
            "strict" to DotTokenType.STRICT
        )
        
        private val SINGLE_CHAR_TOKENS = mapOf(
            '{' to DotTokenType.LBRACE,
            '}' to DotTokenType.RBRACE,
            '[' to DotTokenType.LBRACKET,
            ']' to DotTokenType.RBRACKET,
            '(' to DotTokenType.LPAREN,
            ')' to DotTokenType.RPAREN,
            ';' to DotTokenType.SEMICOLON,
            ',' to DotTokenType.COMMA,
            '=' to DotTokenType.EQUALS
        )
    }
    
    /**
     * Get the current character without advancing the position.
     */
    private fun peek(offset: Int = 0): Char? {
        val pos = position + offset
        return if (pos < input.length) input[pos] else null
    }
    
    /**
     * Get the current character and advance the position.
     */
    private fun advance(): Char? {
        if (position >= input.length) return null
        
        val char = input[position]
        position++
        
        if (char == '\n') {
            line++
            column = 1
        } else {
            column++
        }
        
        return char
    }
    
    /**
     * Mark the start of a new token.
     */
    private fun markTokenStart() {
        tokenStart = position
    }
    
    /**
     * Create a token from the current token start to the current position.
     */
    private fun createToken(type: DotTokenType, value: String? = null): DotToken {
        val tokenValue = value ?: input.substring(tokenStart, position)
        val startLine = line
        val startColumn = column - tokenValue.length
        
        return DotToken(
            type = type,
            value = tokenValue,
            line = startLine,
            column = startColumn,
            startPosition = tokenStart,
            endPosition = position
        )
    }
    
    /**
     * Skip whitespace characters.
     */
    private fun skipWhitespace(): DotToken? {
        markTokenStart()
        val startLine = line
        val startColumn = column
        
        while (peek()?.isWhitespace() == true && peek() != '\n') {
            advance()
        }
        
        return if (position > tokenStart) {
            DotToken(
                type = DotTokenType.WHITESPACE,
                value = input.substring(tokenStart, position),
                line = startLine,
                column = startColumn,
                startPosition = tokenStart,
                endPosition = position
            )
        } else {
            null
        }
    }
    
    /**
     * Handle newline characters.
     */
    private fun handleNewline(): DotToken {
        markTokenStart()
        val startLine = line
        val startColumn = column
        
        advance() // consume the newline
        
        return DotToken(
            type = DotTokenType.NEWLINE,
            value = "\n",
            line = startLine,
            column = startColumn,
            startPosition = tokenStart,
            endPosition = position
        )
    }
    
    /**
     * Handle single-line comments (// or #).
     */
    private fun handleComment(): DotToken {
        markTokenStart()
        val startLine = line
        val startColumn = column
        
        // Skip the comment marker
        if (peek() == '/' && peek(1) == '/') {
            advance() // /
            advance() // /
        } else if (peek() == '#') {
            advance() // #
        }
        
        // Read until end of line or end of input
        while (peek() != null && peek() != '\n') {
            advance()
        }
        
        return DotToken(
            type = DotTokenType.COMMENT,
            value = input.substring(tokenStart, position),
            line = startLine,
            column = startColumn,
            startPosition = tokenStart,
            endPosition = position
        )
    }
    
    /**
     * Handle multi-line comments (/* ... */).
     */
    private fun handleMultiLineComment(): DotToken {
        markTokenStart()
        val startLine = line
        val startColumn = column
        
        advance() // /
        advance() // *
        
        while (position < input.length) {
            if (peek() == '*' && peek(1) == '/') {
                advance() // *
                advance() // /
                break
            }
            advance()
        }
        
        return DotToken(
            type = DotTokenType.COMMENT,
            value = input.substring(tokenStart, position),
            line = startLine,
            column = startColumn,
            startPosition = tokenStart,
            endPosition = position
        )
    }
    
    /**
     * Handle string literals (quoted strings).
     */
    private fun handleStringLiteral(): DotToken {
        markTokenStart()
        val startLine = line
        val startColumn = column
        
        val quote = advance() // consume opening quote
        val content = StringBuilder()
        
        while (peek() != null && peek() != quote) {
            val char = peek()!!
            if (char == '\\') {
                advance() // consume backslash
                val escaped = peek()
                when (escaped) {
                    'n' -> content.append('\n')
                    't' -> content.append('\t')
                    'r' -> content.append('\r')
                    '\\' -> content.append('\\')
                    '"' -> content.append('"')
                    '\'' -> content.append('\'')
                    else -> {
                        content.append('\\')
                        if (escaped != null) content.append(escaped)
                    }
                }
                if (escaped != null) advance()
            } else {
                content.append(char)
                advance()
            }
        }
        
        if (peek() == quote) {
            advance() // consume closing quote
        }
        
        return DotToken(
            type = DotTokenType.STRING_LITERAL,
            value = content.toString(),
            line = startLine,
            column = startColumn,
            startPosition = tokenStart,
            endPosition = position
        )
    }
    
    /**
     * Handle numeric literals.
     */
    private fun handleNumber(): DotToken {
        markTokenStart()
        val startLine = line
        val startColumn = column
        
        // Handle optional negative sign
        if (peek() == '-') {
            advance()
        }
        
        // Read integer part
        while (peek()?.isDigit() == true) {
            advance()
        }
        
        // Handle decimal point and fractional part
        if (peek() == '.') {
            advance()
            while (peek()?.isDigit() == true) {
                advance()
            }
        }
        
        // Handle scientific notation
        if (peek()?.lowercaseChar() == 'e') {
            advance()
            if (peek() == '+' || peek() == '-') {
                advance()
            }
            while (peek()?.isDigit() == true) {
                advance()
            }
        }
        
        return DotToken(
            type = DotTokenType.NUMBER_LITERAL,
            value = input.substring(tokenStart, position),
            line = startLine,
            column = startColumn,
            startPosition = tokenStart,
            endPosition = position
        )
    }
    
    /**
     * Handle identifiers and keywords.
     */
    private fun handleIdentifier(): DotToken {
        markTokenStart()
        val startLine = line
        val startColumn = column
        
        // Read identifier characters
        while (peek()?.let { it.isLetterOrDigit() || it == '_' } == true) {
            advance()
        }
        
        val value = input.substring(tokenStart, position)
        val type = KEYWORDS[value.lowercase()] ?: DotTokenType.IDENTIFIER
        
        return DotToken(
            type = type,
            value = value,
            line = startLine,
            column = startColumn,
            startPosition = tokenStart,
            endPosition = position
        )
    }
    
    /**
     * Handle edge operators (-> and --).
     */
    private fun handleEdgeOperator(): DotToken {
        markTokenStart()
        val startLine = line
        val startColumn = column
        
        val first = advance()
        val second = advance()
        
        val type = when {
            first == '-' && second == '>' -> DotTokenType.ARROW
            first == '-' && second == '-' -> DotTokenType.EDGE_OP
            else -> DotTokenType.ERROR
        }
        
        return DotToken(
            type = type,
            value = input.substring(tokenStart, position),
            line = startLine,
            column = startColumn,
            startPosition = tokenStart,
            endPosition = position
        )
    }
    
    /**
     * Get the next token from the input.
     */
    fun nextToken(): DotToken {
        while (position < input.length) {
            val char = peek()!!
            
            when {
                // Whitespace (excluding newlines)
                char.isWhitespace() && char != '\n' -> {
                    val token = skipWhitespace()
                    if (token != null) return token
                }
                
                // Newlines
                char == '\n' -> return handleNewline()
                
                // Comments
                char == '#' -> return handleComment()
                char == '/' && peek(1) == '/' -> return handleComment()
                char == '/' && peek(1) == '*' -> return handleMultiLineComment()
                
                // String literals
                char == '"' || char == '\'' -> return handleStringLiteral()
                
                // Numbers
                char.isDigit() || (char == '-' && peek(1)?.isDigit() == true) -> {
                    return handleNumber()
                }
                
                // Edge operators
                char == '-' && (peek(1) == '>' || peek(1) == '-') -> {
                    return handleEdgeOperator()
                }
                
                // Single character tokens
                char in SINGLE_CHAR_TOKENS -> {
                    markTokenStart()
                    val startLine = line
                    val startColumn = column
                    advance()
                    return DotToken(
                        type = SINGLE_CHAR_TOKENS[char]!!,
                        value = char.toString(),
                        line = startLine,
                        column = startColumn,
                        startPosition = tokenStart,
                        endPosition = position
                    )
                }
                
                // Identifiers and keywords
                char.isLetter() || char == '_' -> return handleIdentifier()
                
                // Unknown character
                else -> {
                    markTokenStart()
                    val startLine = line
                    val startColumn = column
                    advance()
                    return DotToken(
                        type = DotTokenType.ERROR,
                        value = char.toString(),
                        line = startLine,
                        column = startColumn,
                        startPosition = tokenStart,
                        endPosition = position
                    )
                }
            }
        }
        
        // End of input
        return DotToken(
            type = DotTokenType.EOF,
            value = "",
            line = line,
            column = column,
            startPosition = position,
            endPosition = position
        )
    }
    
    /**
     * Tokenize the entire input and return a list of tokens.
     */
    fun tokenize(): List<DotToken> {
        val tokens = mutableListOf<DotToken>()
        
        while (true) {
            val token = nextToken()
            tokens.add(token)
            
            if (token.type == DotTokenType.EOF) {
                break
            }
        }
        
        return tokens
    }
    
    /**
     * Tokenize the input, filtering out ignorable tokens (whitespace, comments).
     */
    fun tokenizeFiltered(): List<DotToken> {
        return tokenize().filter { !it.isIgnorable }
    }
    
    /**
     * Get current position information.
     */
    fun getCurrentPosition(): Position {
        return Position(line, column, position)
    }
    
    /**
     * Check if there are more tokens to read.
     */
    fun hasMoreTokens(): Boolean {
        return position < input.length
    }
}