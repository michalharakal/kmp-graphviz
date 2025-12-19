package org.graphviz.kotlin.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class DotLexerTest : FunSpec({
    
    test("should tokenize keywords correctly") {
        val input = "graph digraph subgraph node edge strict"
        val lexer = DotLexer(input)
        val tokens = lexer.tokenizeFiltered()
        
        tokens shouldHaveSize 7 // 6 keywords + EOF
        tokens[0].type shouldBe DotTokenType.GRAPH
        tokens[1].type shouldBe DotTokenType.DIGRAPH
        tokens[2].type shouldBe DotTokenType.SUBGRAPH
        tokens[3].type shouldBe DotTokenType.NODE
        tokens[4].type shouldBe DotTokenType.EDGE
        tokens[5].type shouldBe DotTokenType.STRICT
        tokens[6].type shouldBe DotTokenType.EOF
    }
    
    test("should tokenize operators correctly") {
        val input = "{ } [ ] ( ) ; , = -> --"
        val lexer = DotLexer(input)
        val tokens = lexer.tokenizeFiltered()
        
        tokens shouldHaveSize 12 // 11 operators + EOF
        tokens[0].type shouldBe DotTokenType.LBRACE
        tokens[1].type shouldBe DotTokenType.RBRACE
        tokens[2].type shouldBe DotTokenType.LBRACKET
        tokens[3].type shouldBe DotTokenType.RBRACKET
        tokens[4].type shouldBe DotTokenType.LPAREN
        tokens[5].type shouldBe DotTokenType.RPAREN
        tokens[6].type shouldBe DotTokenType.SEMICOLON
        tokens[7].type shouldBe DotTokenType.COMMA
        tokens[8].type shouldBe DotTokenType.EQUALS
        tokens[9].type shouldBe DotTokenType.ARROW
        tokens[10].type shouldBe DotTokenType.EDGE_OP
        tokens[11].type shouldBe DotTokenType.EOF
    }
    
    test("should tokenize identifiers correctly") {
        val input = "node1 _private myNode node_with_underscores"
        val lexer = DotLexer(input)
        val tokens = lexer.tokenizeFiltered()
        
        tokens shouldHaveSize 5 // 4 identifiers + EOF
        tokens.forEach { token ->
            if (token.type != DotTokenType.EOF) {
                token.type shouldBe DotTokenType.IDENTIFIER
            }
        }
        
        tokens[0].value shouldBe "node1"
        tokens[1].value shouldBe "_private"
        tokens[2].value shouldBe "myNode"
        tokens[3].value shouldBe "node_with_underscores"
    }
    
    test("should tokenize string literals correctly") {
        val input = "\"hello world\" 'single quoted' \"escaped \\\"quotes\\\"\""
        val lexer = DotLexer(input)
        val tokens = lexer.tokenizeFiltered()
        
        tokens shouldHaveSize 4 // 3 strings + EOF
        tokens[0].type shouldBe DotTokenType.STRING_LITERAL
        tokens[0].value shouldBe "hello world"
        
        tokens[1].type shouldBe DotTokenType.STRING_LITERAL
        tokens[1].value shouldBe "single quoted"
        
        tokens[2].type shouldBe DotTokenType.STRING_LITERAL
        tokens[2].value shouldBe "escaped \"quotes\""
    }
    
    test("should tokenize numbers correctly") {
        val input = "42 3.14 -5 1.5e10 -2.3e-4"
        val lexer = DotLexer(input)
        val tokens = lexer.tokenizeFiltered()
        
        tokens shouldHaveSize 6 // 5 numbers + EOF
        tokens.take(5).forEach { token ->
            token.type shouldBe DotTokenType.NUMBER_LITERAL
        }
        
        tokens[0].value shouldBe "42"
        tokens[1].value shouldBe "3.14"
        tokens[2].value shouldBe "-5"
        tokens[3].value shouldBe "1.5e10"
        tokens[4].value shouldBe "-2.3e-4"
    }
    
    test("should handle comments correctly") {
        val input = """
            // Single line comment
            # Hash comment
            /* Multi-line
               comment */
            graph test
        """.trimIndent()
        
        val lexer = DotLexer(input)
        val tokens = lexer.tokenize()
        
        // Should contain comment tokens when not filtered
        tokens.map { it.type } shouldContain DotTokenType.COMMENT
        
        // Should not contain comments when filtered
        val filteredTokens = tokens.filter { !it.isIgnorable }
        filteredTokens.none { it.type == DotTokenType.COMMENT } shouldBe true
        
        // Should still have the graph keyword
        filteredTokens.any { it.type == DotTokenType.GRAPH } shouldBe true
    }
    
    test("should track line and column numbers correctly") {
        val input = """
            graph test {
                node1 -> node2;
            }
        """.trimIndent()
        
        val lexer = DotLexer(input)
        val tokens = lexer.tokenizeFiltered()
        
        // Find the arrow token
        val arrowToken = tokens.first { it.type == DotTokenType.ARROW }
        arrowToken.line shouldBe 2
        arrowToken.column shouldBe 11 // Position of -> in "    node1 -> node2;"
    }
    
    test("should handle empty input") {
        val lexer = DotLexer("")
        val tokens = lexer.tokenizeFiltered()
        
        tokens shouldHaveSize 1
        tokens[0].type shouldBe DotTokenType.EOF
    }
    
    test("should handle whitespace-only input") {
        val lexer = DotLexer("   \t  \n  ")
        val tokens = lexer.tokenizeFiltered()
        
        tokens shouldHaveSize 1
        tokens[0].type shouldBe DotTokenType.EOF
    }
    
    test("should handle error tokens for invalid characters") {
        val input = "graph @ test"
        val lexer = DotLexer(input)
        val tokens = lexer.tokenizeFiltered()
        
        tokens shouldHaveSize 4 // graph, @, test, EOF
        tokens[0].type shouldBe DotTokenType.GRAPH
        tokens[1].type shouldBe DotTokenType.ERROR
        tokens[1].value shouldBe "@"
        tokens[2].type shouldBe DotTokenType.IDENTIFIER
        tokens[3].type shouldBe DotTokenType.EOF
    }
    
    test("should tokenize complete DOT graph") {
        val input = """
            digraph G {
                node [shape=box];
                A -> B [label="edge"];
                B -> C;
            }
        """.trimIndent()
        
        val lexer = DotLexer(input)
        val tokens = lexer.tokenizeFiltered()
        
        // Verify we get the expected sequence of tokens
        val expectedTypes = listOf(
            DotTokenType.DIGRAPH,
            DotTokenType.IDENTIFIER, // G
            DotTokenType.LBRACE,
            DotTokenType.NODE,
            DotTokenType.LBRACKET,
            DotTokenType.IDENTIFIER, // shape
            DotTokenType.EQUALS,
            DotTokenType.IDENTIFIER, // box
            DotTokenType.RBRACKET,
            DotTokenType.SEMICOLON,
            DotTokenType.IDENTIFIER, // A
            DotTokenType.ARROW,
            DotTokenType.IDENTIFIER, // B
            DotTokenType.LBRACKET,
            DotTokenType.IDENTIFIER, // label
            DotTokenType.EQUALS,
            DotTokenType.STRING_LITERAL, // "edge"
            DotTokenType.RBRACKET,
            DotTokenType.SEMICOLON,
            DotTokenType.IDENTIFIER, // B
            DotTokenType.ARROW,
            DotTokenType.IDENTIFIER, // C
            DotTokenType.SEMICOLON,
            DotTokenType.RBRACE,
            DotTokenType.EOF
        )
        
        tokens shouldHaveSize expectedTypes.size
        tokens.zip(expectedTypes).forEach { (token, expectedType) ->
            token.type shouldBe expectedType
        }
    }
})