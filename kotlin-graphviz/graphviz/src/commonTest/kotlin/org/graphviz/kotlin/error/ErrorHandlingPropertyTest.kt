package org.graphviz.kotlin.error

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.graphviz.kotlin.parser.DotParser

/**
 * Property-based tests for error handling with context.
 * 
 * **Feature: kotlin-multiplatform-port, Property 7: Error handling with context**
 * **Validates: Requirements 1.2, 7.1, 7.5**
 */
class ErrorHandlingPropertyTest : StringSpec({
    
    "Property 7: Error handling with context - parse errors provide descriptive messages" {
        checkAll(
            iterations = 100,
            Arb.choice(
                // Invalid DOT syntax patterns
                Arb.constant("digraph { node1 -> }"), // Missing target
                Arb.constant("digraph { -> node2 }"), // Missing source
                Arb.constant("digraph { node1 -> node2"), // Missing closing brace
                Arb.constant("graph { node1 -> node2 }"), // Wrong edge operator for undirected
                Arb.constant("digraph { node1 -- node2 }"), // Wrong edge operator for directed
                Arb.constant("digraph { \"unclosed string }"), // Unclosed string
                Arb.constant("digraph { node1[color=] }"), // Empty attribute value
                Arb.constant("digraph { node1[=red] }"), // Missing attribute name
                Arb.constant("digraph { 123invalid }") // Invalid identifier
            )
        ) { invalidDotString ->
            val result = DotParser.parse(invalidDotString)
            
            // Property: Parse errors should always be returned for invalid input
            result.isError shouldBe true
            
            // For this test, we're verifying that parse errors occur
            // The actual error details are implementation-specific
            // but the error should contain meaningful information
            val errorResult = result as org.graphviz.kotlin.parser.ParseResult.Error
            
            // Property: Parse errors should have descriptive context
            errorResult.message.isNotEmpty() shouldBe true
            
            // Property: Parse errors should include location information
            errorResult.line shouldNotBe 0
            errorResult.column shouldNotBe 0
        }
    }
    
    "Property 7: Error handling with context - validation errors provide element context" {
        checkAll(
            iterations = 100,
            Arb.string(1..20, "abcdefghijklmnopqrstuvwxyz"), // elementId
            Arb.string(1..15, "abcdefghijklmnopqrstuvwxyz"), // attributeName
            Arb.choice(
                Arb.constant(""), // Empty value
                Arb.constant("invalid-color-name"), // Invalid color
                Arb.constant("-1.5"), // Invalid numeric value
                Arb.constant("not-a-boolean"), // Invalid boolean
                Arb.constant("unknown-shape") // Invalid shape
            ), // invalidValue
            Arb.choice(
                Arb.constant("valid color name or hex value"),
                Arb.constant("positive number"),
                Arb.constant("true or false"),
                Arb.constant("valid shape name")
            ) // expectedValue
        ) { elementId, attributeName, invalidValue, expectedValue ->
            val validationRule = "Attribute must be $expectedValue"
            
            val error = ErrorUtils.attributeValidationError(
                elementId = elementId,
                attributeName = attributeName,
                actualValue = invalidValue,
                expectedValue = expectedValue,
                validationRule = validationRule
            )
            
            // Property: Validation errors should contain element context
            error.elementId shouldBe elementId
            error.attributeName shouldBe attributeName
            error.actualValue shouldBe invalidValue
            error.expectedValue shouldBe expectedValue
            error.validationRule shouldBe validationRule
            
            // Property: Validation errors should be recoverable
            error.isRecoverable shouldBe true
            error.category shouldBe ErrorCategory.VALIDATION
            
            // Property: Error message should contain element information
            error.message shouldContain elementId
            error.message shouldContain validationRule
            
            // Property: Recovery actions should be specific and actionable
            error.recoveryActions.isNotEmpty() shouldBe true
            error.recoveryActions.any { it.contains(elementId) } shouldBe true
            error.recoveryActions.any { it.contains(attributeName) } shouldBe true
        }
    }
    
    "Property 7: Error handling with context - layout errors provide algorithm context" {
        checkAll(
            iterations = 100,
            Arb.choice(
                Arb.constant("dot"),
                Arb.constant("neato"),
                Arb.constant("fdp"),
                Arb.constant("circo")
            ), // algorithm
            Arb.enum<LayoutPhase>(), // phase
            Arb.int(1..1000), // nodeCount
            Arb.int(0..2000), // edgeCount
            Arb.boolean() // fallbackAvailable
        ) { algorithm, phase, nodeCount, edgeCount, fallbackAvailable ->
            val layoutMessage = "Algorithm failed during ${phase.name.lowercase()}"
            
            val error = ErrorUtils.layoutError(
                algorithm = algorithm,
                phase = phase,
                nodeCount = nodeCount,
                edgeCount = edgeCount,
                message = layoutMessage,
                fallbackAvailable = fallbackAvailable
            )
            
            // Property: Layout errors should contain algorithm context
            error.algorithm shouldBe algorithm
            error.phase shouldBe phase
            error.nodeCount shouldBe nodeCount
            error.edgeCount shouldBe edgeCount
            error.layoutMessage shouldBe layoutMessage
            error.fallbackAvailable shouldBe fallbackAvailable
            
            // Property: Layout error recoverability depends on fallback availability
            error.isRecoverable shouldBe fallbackAvailable
            error.category shouldBe ErrorCategory.LAYOUT
            
            // Property: Error message should contain algorithm and phase information
            error.message shouldContain algorithm
            error.message shouldContain phase.name
            error.message shouldContain layoutMessage
            
            // Property: Recovery actions should mention graph size and algorithm
            error.recoveryActions.isNotEmpty() shouldBe true
            error.recoveryActions.any { it.contains(algorithm) } shouldBe true
            error.recoveryActions.any { it.contains("$nodeCount nodes") } shouldBe true
            error.recoveryActions.any { it.contains("$edgeCount edges") } shouldBe true
        }
    }
    
    "Property 7: Error handling with context - internal errors provide debugging context" {
        checkAll(
            iterations = 100,
            Arb.string(1..20, "abcdefghijklmnopqrstuvwxyz"), // component
            Arb.string(1..20, "abcdefghijklmnopqrstuvwxyz"), // operation
            Arb.string(1..50, "abcdefghijklmnopqrstuvwxyz "), // message
            Arb.map(
                Arb.string(1..10, "abcdefghijklmnopqrstuvwxyz"),
                Arb.string(1..20, "abcdefghijklmnopqrstuvwxyz "),
                minSize = 0,
                maxSize = 5
            ) // debugInfo
        ) { component, operation, message, debugInfo ->
            val error = ErrorUtils.internalError(
                component = component,
                operation = operation,
                message = message,
                debugInfo = debugInfo
            )
            
            // Property: Internal errors should contain component context
            error.component shouldBe component
            error.operation shouldBe operation
            error.internalMessage shouldBe message
            error.debugInfo shouldBe debugInfo
            
            // Property: Internal errors should not be recoverable
            error.isRecoverable shouldBe false
            error.category shouldBe ErrorCategory.INTERNAL
            
            // Property: Error message should contain component and operation
            error.message shouldContain component
            error.message shouldContain operation
            error.message shouldContain message
            
            // Property: Recovery actions should indicate this is a bug
            error.recoveryActions.isNotEmpty() shouldBe true
            error.recoveryActions.any { it.contains("bug") || it.contains("report") } shouldBe true
            error.recoveryActions.any { it.contains(component) } shouldBe true
        }
    }
    
    "Property 7: Error handling with context - error formatting preserves context" {
        checkAll(
            iterations = 100,
            Arb.choice(
                // Generate different types of errors
                Arb.bind(
                    Arb.int(1..100),
                    Arb.int(1..50),
                    Arb.string(1..30, "abcdefghijklmnopqrstuvwxyz ")
                ) { line, column, message ->
                    ErrorUtils.parseError(line, column, 0, message)
                },
                Arb.bind(
                    Arb.string(1..20, "abcdefghijklmnopqrstuvwxyz"),
                    Arb.string(1..15, "abcdefghijklmnopqrstuvwxyz"),
                    Arb.string(1..20, "abcdefghijklmnopqrstuvwxyz ")
                ) { elementId, attributeName, rule ->
                    ErrorUtils.attributeValidationError(elementId, attributeName, "invalid", "valid", rule)
                },
                Arb.bind(
                    Arb.string(1..20, "abcdefghijklmnopqrstuvwxyz"),
                    Arb.string(1..20, "abcdefghijklmnopqrstuvwxyz"),
                    Arb.string(1..30, "abcdefghijklmnopqrstuvwxyz ")
                ) { component, operation, message ->
                    ErrorUtils.internalError(component, operation, message)
                }
            ),
            Arb.enum<ErrorFormat>()
        ) { error, format ->
            val formattedError = ErrorUtils.formatError(error, format)
            
            // Property: Formatted errors should not be empty
            formattedError.isNotEmpty() shouldBe true
            
            // Property: Formatted errors should contain the original message
            if (error.message != null) {
                when (format) {
                    ErrorFormat.BRIEF -> {
                        formattedError shouldBe error.message
                    }
                    ErrorFormat.DETAILED, ErrorFormat.CONSOLE, ErrorFormat.DEBUG -> {
                        // These formats should contain more context
                        formattedError.length shouldNotBe error.message!!.length
                    }
                }
            }
            
            // Property: Console and debug formats should contain category information
            if (format == ErrorFormat.CONSOLE || format == ErrorFormat.DEBUG) {
                // Console format uses user-friendly category names, not raw enum names
                when (error.category) {
                    ErrorCategory.PARSE -> formattedError shouldContain "DOT parsing failed"
                    ErrorCategory.VALIDATION -> formattedError shouldContain "Validation failed"
                    ErrorCategory.LAYOUT -> formattedError shouldContain "Layout calculation failed"
                    ErrorCategory.RENDER -> formattedError shouldContain "Rendering failed"
                    ErrorCategory.PLATFORM -> formattedError shouldContain "Platform limitation"
                    ErrorCategory.RESOURCE -> formattedError shouldContain "Insufficient"
                    ErrorCategory.CONFIGURATION -> formattedError shouldContain "Invalid configuration"
                    ErrorCategory.INTERNAL -> formattedError shouldContain "Internal error"
                }
            }
        }
    }
    
    "Property 7: Error handling with context - error reporting generates structured information" {
        checkAll(
            iterations = 100,
            Arb.choice(
                // Generate different types of errors for reporting
                Arb.bind(
                    Arb.int(1..100),
                    Arb.int(1..50),
                    Arb.string(1..30, "abcdefghijklmnopqrstuvwxyz ")
                ) { line, column, message ->
                    ErrorUtils.parseError(line, column, 0, message)
                },
                Arb.bind(
                    Arb.string(1..20, "abcdefghijklmnopqrstuvwxyz"),
                    Arb.enum<LayoutPhase>(),
                    Arb.int(1..100),
                    Arb.int(0..200)
                ) { algorithm, phase, nodeCount, edgeCount ->
                    ErrorUtils.layoutError(algorithm, phase, nodeCount, edgeCount, "Test error")
                }
            )
        ) { error ->
            val report = ErrorReporting.generateErrorReport(error)
            val userMessage = ErrorReporting.generateUserMessage(error)
            val structuredLog = ErrorReporting.generateStructuredLog(error)
            
            // Property: Error reports should contain all essential information
            report.error shouldBe error
            report.category shouldBe error.category
            report.isRecoverable shouldBe error.isRecoverable
            report.context shouldBe error.context
            report.recoveryActions shouldBe error.recoveryActions
            
            // Property: User messages should be actionable
            userMessage.summary.isNotEmpty() shouldBe true
            userMessage.explanation.isNotEmpty() shouldBe true
            userMessage.suggestedActions.isNotEmpty() shouldBe true
            
            // Property: Structured logs should contain key-value pairs
            structuredLog shouldNotBe emptyMap<String, Any>()
            structuredLog["category"] shouldBe error.category.name
            structuredLog["recoverable"] shouldBe error.isRecoverable
            
            // Property: Message should contain error information
            val message = structuredLog["message"] as? String
            message shouldNotBe null
            message!!.isNotEmpty() shouldBe true
        }
    }
})