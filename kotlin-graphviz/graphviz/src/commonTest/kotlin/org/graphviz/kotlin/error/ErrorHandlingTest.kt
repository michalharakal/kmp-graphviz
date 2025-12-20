package org.graphviz.kotlin.error

import org.graphviz.kotlin.model.*
import org.graphviz.kotlin.validation.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Basic tests for the error handling system.
 */
class ErrorHandlingTest {
    
    @Test
    fun testGraphvizErrorCreation() {
        val parseError = ErrorUtils.parseError(
            line = 10,
            column = 5,
            position = 100,
            message = "Unexpected token",
            context = "digraph G { node1 -> }"
        )
        
        assertEquals(ErrorCategory.PARSE, parseError.category)
        assertTrue(parseError.isRecoverable)
        assertTrue(parseError.message?.contains("line 10") == true)
    }
    
    @Test
    fun testValidationErrorCreation() {
        val validationError = ErrorUtils.attributeValidationError(
            elementId = "node1",
            attributeName = "color",
            actualValue = "invalid-color",
            expectedValue = "valid color name or hex value",
            validationRule = "Color must be a valid color name or hex value"
        )
        
        assertEquals(ErrorCategory.VALIDATION, validationError.category)
        assertEquals(ValidationType.ATTRIBUTE_VALUE, validationError.validationType)
        assertTrue(validationError.isRecoverable)
    }
    
    @Test
    fun testErrorReporting() {
        val error = ErrorUtils.internalError(
            component = "TestComponent",
            operation = "testOperation",
            message = "Test internal error"
        )
        
        val report = ErrorReporting.generateErrorReport(error)
        assertEquals(ErrorCategory.INTERNAL, report.category)
        assertEquals(ErrorSeverity.CRITICAL, report.severity)
        assertFalse(report.isRecoverable)
    }
    
    @Test
    fun testSimpleInputValidator() {
        val validator = SimpleInputValidator()
        
        // Valid identifier
        val validResult = validator.validateIdentifier("validNode", "test")
        assertTrue(validResult.isValid)
        
        // Invalid identifier (empty)
        val invalidResult = validator.validateIdentifier("", "test")
        assertFalse(invalidResult.isValid)
        assertTrue(invalidResult is ValidationResult.Invalid)
        assertTrue(invalidResult.errors.isNotEmpty())
    }
    
    @Test
    fun testErrorUtilsExtensions() {
        val parseError = ErrorUtils.parseError(1, 1, 0, "test")
        
        assertTrue(parseError.isType<GraphvizError.ParseError>())
        assertFalse(parseError.isType<GraphvizError.ValidationError>())
        
        val asParseError = parseError.asType<GraphvizError.ParseError>()
        assertEquals(parseError, asParseError)
        
        val asValidationError = parseError.asType<GraphvizError.ValidationError>()
        assertEquals(null, asValidationError)
    }
    
    @Test
    fun testErrorRecoveryResult() {
        val successResult = RecoveryResult.Success("test value")
        assertTrue(successResult.isSuccess)
        assertEquals("test value", successResult.getOrNull())
        
        val error = ErrorUtils.internalError("test", "test", "test error")
        val failedResult = RecoveryResult.Failed(error, "test reason")
        assertFalse(failedResult.isSuccess)
        assertEquals(null, failedResult.getOrNull())
    }
    
    @Test
    fun testErrorFormatting() {
        val error = ErrorUtils.parseError(5, 10, 50, "Missing semicolon")
        
        val briefFormat = ErrorUtils.formatError(error, ErrorFormat.BRIEF)
        assertTrue(briefFormat.contains("line 5"))
        
        val detailedFormat = ErrorUtils.formatError(error, ErrorFormat.DETAILED)
        assertTrue(detailedFormat.contains("DOT parsing failed"))
    }
}