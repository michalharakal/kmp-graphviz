package org.graphviz.kotlin.error

import org.graphviz.kotlin.model.*
import org.graphviz.kotlin.validation.*
import kotlin.test.*

/**
 * Comprehensive unit tests for error handling system.
 * Tests error generation, message quality, and recovery mechanisms.
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.5
 */
class ErrorHandlingUnitTest {
    
    // Test error generation for various invalid inputs
    
    @Test
    fun testParseErrorGeneration() {
        // Test basic parse error creation
        val parseError = ErrorUtils.parseError(
            line = 15,
            column = 8,
            position = 142,
            message = "Unexpected token ';'",
            context = "digraph G { node1 -> node2; }",
            expectedTokens = listOf("identifier", "string"),
            actualToken = ";"
        )
        
        assertEquals(ErrorCategory.PARSE, parseError.category)
        assertTrue(parseError.isRecoverable)
        assertTrue(parseError.message?.contains("line 15") == true)
        assertTrue(parseError.message?.contains("column 8") == true)
        assertEquals(15, parseError.line)
        assertEquals(8, parseError.column)
        assertEquals(142, parseError.position)
        assertEquals(listOf("identifier", "string"), parseError.expectedTokens)
        assertEquals(";", parseError.actualToken)
    }
    
    @Test
    fun testValidationErrorGeneration() {
        // Test attribute validation error
        val attrError = ErrorUtils.attributeValidationError(
            elementId = "node1",
            attributeName = "color",
            actualValue = "invalid-color-123",
            expectedValue = "valid color name or hex value",
            validationRule = "Color must be a valid color name or hex value"
        )
        
        assertEquals(ErrorCategory.VALIDATION, attrError.category)
        assertEquals(ValidationType.ATTRIBUTE_VALUE, attrError.validationType)
        assertEquals("node1", attrError.elementId)
        assertEquals("color", attrError.attributeName)
        assertEquals("invalid-color-123", attrError.actualValue)
        assertTrue(attrError.isRecoverable)
        
        // Test structure validation error
        val structError = ErrorUtils.structureValidationError(
            elementId = "graph1",
            validationRule = "Graph must contain at least one node"
        )
        
        assertEquals(ErrorCategory.VALIDATION, structError.category)
        assertEquals(ValidationType.GRAPH_STRUCTURE, structError.validationType)
        assertEquals("graph1", structError.elementId)
        assertTrue(structError.isRecoverable)
    }
    
    @Test
    fun testLayoutErrorGeneration() {
        val layoutError = ErrorUtils.layoutError(
            algorithm = "dot",
            phase = LayoutPhase.RANKING,
            nodeCount = 1000,
            edgeCount = 5000,
            message = "Cycle detection failed",
            fallbackAvailable = true
        )
        
        assertEquals(ErrorCategory.LAYOUT, layoutError.category)
        assertEquals("dot", layoutError.algorithm)
        assertEquals(LayoutPhase.RANKING, layoutError.phase)
        assertEquals(1000, layoutError.nodeCount)
        assertEquals(5000, layoutError.edgeCount)
        assertTrue(layoutError.fallbackAvailable)
        assertTrue(layoutError.isRecoverable)
    }
    
    @Test
    fun testRenderErrorGeneration() {
        val renderError = ErrorUtils.renderError(
            format = "SVG",
            phase = RenderPhase.NODE_RENDERING,
            message = "Failed to render complex path",
            elementType = "edge",
            elementId = "edge1"
        )
        
        assertEquals(ErrorCategory.RENDER, renderError.category)
        assertEquals("SVG", renderError.format)
        assertEquals(RenderPhase.NODE_RENDERING, renderError.renderPhase)
        assertEquals("edge", renderError.elementType)
        assertEquals("edge1", renderError.elementId)
        assertTrue(renderError.isRecoverable)
    }
    
    @Test
    fun testPlatformErrorGeneration() {
        val platformError = ErrorUtils.platformError(
            platform = "iOS",
            feature = "file system access",
            limitation = "Sandboxed environment restricts file operations",
            workaroundAvailable = true
        )
        
        assertEquals(ErrorCategory.PLATFORM, platformError.category)
        assertEquals("iOS", platformError.platform)
        assertEquals("file system access", platformError.feature)
        assertTrue(platformError.workaroundAvailable)
        assertTrue(platformError.isRecoverable)
    }
    
    @Test
    fun testResourceErrorGeneration() {
        val resourceError = ErrorUtils.resourceError(
            resourceType = ResourceType.MEMORY,
            requested = "500MB",
            available = "200MB",
            threshold = "400MB"
        )
        
        assertEquals(ErrorCategory.RESOURCE, resourceError.category)
        assertEquals(ResourceType.MEMORY, resourceError.resourceType)
        assertEquals("500MB", resourceError.requested)
        assertEquals("200MB", resourceError.available)
        assertEquals("400MB", resourceError.threshold)
        assertTrue(resourceError.isRecoverable) // Resource errors are recoverable
    }
    
    @Test
    fun testConfigurationErrorGeneration() {
        val configError = ErrorUtils.configurationError(
            configType = "layout",
            configKey = "algorithm",
            configValue = "invalid-algo",
            message = "Unknown layout algorithm",
            validValues = listOf("dot", "neato", "fdp", "circo")
        )
        
        assertEquals(ErrorCategory.CONFIGURATION, configError.category)
        assertEquals("layout", configError.configType)
        assertEquals("algorithm", configError.configKey)
        assertEquals("invalid-algo", configError.configValue)
        assertEquals(listOf("dot", "neato", "fdp", "circo"), configError.validValues)
        assertTrue(configError.isRecoverable)
    }
    
    @Test
    fun testInternalErrorGeneration() {
        val debugInfo = mapOf(
            "threadId" to "main",
            "memoryUsage" to "150MB",
            "stackDepth" to "15"
        )
        
        val internalError = ErrorUtils.internalError(
            component = "LayoutEngine",
            operation = "calculatePositions",
            message = "Unexpected null pointer in position calculation",
            debugInfo = debugInfo
        )
        
        assertEquals(ErrorCategory.INTERNAL, internalError.category)
        assertEquals("LayoutEngine", internalError.component)
        assertEquals("calculatePositions", internalError.operation)
        assertEquals(debugInfo, internalError.debugInfo)
        assertFalse(internalError.isRecoverable) // Internal errors are not recoverable
    }
    
    // Test error message quality and context information
    
    @Test
    fun testErrorMessageQuality() {
        val parseError = ErrorUtils.parseError(
            line = 5,
            column = 12,
            position = 67,
            message = "Missing closing brace",
            context = "digraph G { node1 -> node2"
        )
        
        // Test brief format
        val briefMessage = ErrorUtils.formatError(parseError, ErrorFormat.BRIEF)
        assertTrue(briefMessage.contains("line 5"))
        assertTrue(briefMessage.contains("column 12"))
        
        // Test detailed format
        val detailedMessage = ErrorUtils.formatError(parseError, ErrorFormat.DETAILED)
        assertTrue(detailedMessage.contains("DOT parsing failed"))
        assertTrue(detailedMessage.contains("invalid syntax"))
        
        // Test console format
        val consoleMessage = ErrorUtils.formatError(parseError, ErrorFormat.CONSOLE)
        assertTrue(consoleMessage.isNotEmpty())
        
        // Test debug format
        val debugMessage = ErrorUtils.formatError(parseError, ErrorFormat.DEBUG)
        assertTrue(debugMessage.isNotEmpty())
    }
    
    @Test
    fun testErrorContextInformation() {
        val validationError = ErrorUtils.attributeValidationError(
            elementId = "node1",
            attributeName = "shape",
            actualValue = "invalid-shape",
            expectedValue = "box, circle, ellipse, etc.",
            validationRule = "Shape must be a valid node shape"
        )
        
        val errorInfo = ErrorUtils.extractErrorInfo(validationError)
        
        assertEquals("ValidationError", errorInfo["type"])
        assertEquals("VALIDATION", errorInfo["category"])
        assertEquals(true, errorInfo["recoverable"])
        assertEquals("ATTRIBUTE_VALUE", errorInfo["validationType"])
        assertEquals("node1", errorInfo["elementId"])
        assertEquals("shape", errorInfo["attributeName"])
    }
    
    @Test
    fun testErrorReportGeneration() {
        val layoutError = ErrorUtils.layoutError(
            algorithm = "neato",
            phase = LayoutPhase.POSITIONING,
            nodeCount = 50,
            edgeCount = 100,
            message = "Spring model convergence failed",
            fallbackAvailable = true
        )
        
        val report = ErrorReporting.generateErrorReport(layoutError)
        
        assertEquals(ErrorCategory.LAYOUT, report.category)
        assertEquals(ErrorSeverity.MEDIUM, report.severity)
        assertTrue(report.isRecoverable)
        assertEquals(0L, report.timestamp) // Platform-specific timestamp is 0L in common implementation
        assertTrue(report.debugInfo.isNotEmpty())
    }
    
    @Test
    fun testUserErrorMessageGeneration() {
        val configError = ErrorUtils.configurationError(
            configType = "render",
            configKey = "format",
            configValue = "xyz",
            message = "Unsupported output format",
            validValues = listOf("svg", "png", "pdf")
        )
        
        val userMessage = ErrorReporting.generateUserMessage(configError)
        
        assertTrue(userMessage.summary.contains("Invalid configuration"))
        assertTrue(userMessage.explanation.contains("configuration contains invalid settings"))
        assertTrue(userMessage.suggestedActions.isNotEmpty())
        assertTrue(userMessage.suggestedActions.any { it.contains("Check") })
    }
    
    // Test recovery mechanisms and fallback behavior
    
    @Test
    fun testBasicErrorRecovery() {
        val recoverableError = ErrorUtils.layoutError(
            algorithm = "complex-algo",
            phase = LayoutPhase.POSITIONING,
            nodeCount = 100,
            edgeCount = 200,
            message = "Complex algorithm failed",
            fallbackAvailable = true
        )
        
        val fallbackStrategy = object : FallbackStrategy<String>() {
            override val description = "Use simple layout"
            override fun canHandle(error: GraphvizError): Boolean = 
                error is GraphvizError.LayoutError && error.fallbackAvailable
            override fun execute(error: GraphvizError): String = "simple-layout-result"
        }
        
        val result = ErrorRecovery.recover(recoverableError, listOf(fallbackStrategy))
        
        assertTrue(result.isSuccess)
        assertTrue(result is RecoveryResult.Recovered)
        assertEquals("simple-layout-result", result.getOrNull())
    }
    
    @Test
    fun testUnrecoverableErrorHandling() {
        val unrecoverableError = ErrorUtils.internalError(
            component = "Core",
            operation = "initialize",
            message = "Critical system failure"
        )
        
        val fallbackStrategy = object : FallbackStrategy<String>() {
            override val description = "Generic fallback"
            override fun canHandle(error: GraphvizError): Boolean = true
            override fun execute(error: GraphvizError): String = "fallback-result"
        }
        
        val result = ErrorRecovery.recover(unrecoverableError, listOf(fallbackStrategy))
        
        assertFalse(result.isSuccess)
        assertTrue(result is RecoveryResult.Unrecoverable)
        assertEquals(null, result.getOrNull())
    }
    
    @Test
    fun testFallbackStrategyExecution() {
        val layoutError = ErrorUtils.layoutError(
            algorithm = "dot",
            phase = LayoutPhase.RANKING,
            nodeCount = 10,
            edgeCount = 15,
            message = "Ranking failed"
        )
        
        val simpleLayoutFallback = object : FallbackStrategy<String>() {
            override val description = "Use simple layout"
            override fun canHandle(error: GraphvizError): Boolean = 
                error is GraphvizError.LayoutError
            override fun execute(error: GraphvizError): String = 
                "Fallback layout for ${(error as GraphvizError.LayoutError).algorithm}"
        }
        
        assertTrue(simpleLayoutFallback.canHandle(layoutError))
        val result = simpleLayoutFallback.execute(layoutError)
        assertEquals("Fallback layout for dot", result)
    }
    
    @Test
    fun testWithFallbacksOperation() {
        var attemptCount = 0
        
        val primaryOperation = {
            attemptCount++
            if (attemptCount == 1) {
                throw ErrorUtils.layoutError(
                    algorithm = "test",
                    phase = LayoutPhase.POSITIONING,
                    nodeCount = 1,
                    edgeCount = 1,
                    message = "First attempt failed",
                    fallbackAvailable = true
                )
            }
            "success"
        }
        
        val fallbackStrategy = object : FallbackStrategy<String>() {
            override val description = "Test fallback"
            override fun canHandle(error: GraphvizError): Boolean = true
            override fun execute(error: GraphvizError): String = "fallback-success"
        }
        
        val result = ErrorRecovery.withFallbacks(primaryOperation, fallbackStrategy)
        
        assertTrue(result.isSuccess)
        assertTrue(result is RecoveryResult.Recovered)
        assertEquals("fallback-success", result.getOrNull())
    }
    
    @Test
    fun testRetryWithBackoff() {
        var attemptCount = 0
        
        val operation = {
            attemptCount++
            if (attemptCount < 3) {
                throw ErrorUtils.platformError(
                    platform = "test",
                    feature = "test-feature",
                    limitation = "Temporary failure",
                    workaroundAvailable = true
                )
            }
            "success-after-retries"
        }
        
        val result = ErrorRecovery.withRetry(
            operation = operation,
            maxAttempts = 3,
            retryDelay = 10 // Short delay for testing
        )
        
        assertTrue(result.isSuccess)
        assertEquals("success-after-retries", result.getOrNull())
        assertEquals(3, attemptCount)
    }
    
    @Test
    fun testRetryExhaustion() {
        var attemptCount = 0
        
        val operation = {
            attemptCount++
            throw ErrorUtils.configurationError(
                configType = "test",
                configKey = "test-key",
                configValue = "invalid",
                message = "Always fails"
            )
        }
        
        val result = ErrorRecovery.withRetry(
            operation = operation,
            maxAttempts = 2,
            retryDelay = 10
        )
        
        assertFalse(result.isSuccess)
        assertTrue(result is RecoveryResult.Failed)
        assertEquals(2, attemptCount)
    }
    
    // Test error chaining and transformation
    
    @Test
    fun testErrorChaining() {
        val primaryError = ErrorUtils.parseError(1, 1, 0, "Parse failed")
        val secondaryError = ErrorUtils.validationError("node1", "Invalid structure")
        
        val chainedError = ErrorUtils.chainErrors(
            primaryError = primaryError,
            secondaryError = secondaryError,
            component = "GraphProcessor",
            operation = "processGraph"
        )
        
        assertEquals(ErrorCategory.INTERNAL, chainedError.category)
        assertEquals("GraphProcessor", chainedError.component)
        assertEquals("processGraph", chainedError.operation)
        assertTrue(chainedError.message?.contains("Multiple errors occurred") == true)
        assertEquals(primaryError, chainedError.cause)
    }
    
    @Test
    fun testExceptionWrapping() {
        val originalException = IllegalArgumentException("Invalid argument")
        
        val wrappedError = ErrorUtils.wrapException(
            exception = originalException,
            component = "TestComponent",
            operation = "testOperation",
            additionalContext = mapOf("input" to "test-input")
        )
        
        assertTrue(wrappedError is GraphvizError.InternalError)
        assertEquals("TestComponent", wrappedError.component)
        assertEquals("testOperation", wrappedError.operation)
        assertEquals(originalException, wrappedError.cause)
        assertTrue(wrappedError.debugInfo.containsKey("exceptionType"))
        assertTrue(wrappedError.debugInfo.containsKey("input"))
    }
    
    @Test
    fun testErrorSummarization() {
        val errors = listOf(
            ErrorUtils.parseError(1, 1, 0, "Parse error 1"),
            ErrorUtils.parseError(2, 1, 10, "Parse error 2"),
            ErrorUtils.validationError("node1", "Validation error"),
            ErrorUtils.internalError("Core", "test", "Internal error")
        )
        
        val summary = ErrorUtils.summarizeErrors(errors)
        
        assertEquals(4, summary.totalErrors)
        assertEquals(2, summary.categoryCounts[ErrorCategory.PARSE])
        assertEquals(1, summary.categoryCounts[ErrorCategory.VALIDATION])
        assertEquals(1, summary.categoryCounts[ErrorCategory.INTERNAL])
        assertEquals(3, summary.recoverableCount)
        assertEquals(1, summary.unrecoverableCount)
        assertTrue(summary.hasRecoverableErrors)
        assertTrue(summary.hasUnrecoverableErrors)
        assertTrue(summary.isCritical)
    }
    
    // Test input validation error scenarios
    
    @Test
    fun testInputValidationErrors() {
        val validator = SimpleInputValidator()
        
        // Test empty identifier
        val emptyResult = validator.validateIdentifier("", "test context")
        assertFalse(emptyResult.isValid)
        assertTrue(emptyResult is ValidationResult.Invalid)
        assertTrue(emptyResult.errors.any { it.contains("cannot be empty") })
        
        // Test invalid identifier characters
        val invalidResult = validator.validateIdentifier("123invalid", "test context")
        assertFalse(invalidResult.isValid)
        assertTrue(invalidResult is ValidationResult.Invalid)
        assertTrue(invalidResult.errors.any { it.contains("invalid characters") })
        
        // Test reserved keyword
        val keywordResult = validator.validateIdentifier("graph", "test context")
        assertFalse(keywordResult.isValid)
        assertTrue(keywordResult is ValidationResult.Invalid)
        assertTrue(keywordResult.errors.any { it.contains("reserved keyword") })
        
        // Test invalid coordinates
        val coordResult = validator.validateCoordinate(Double.POSITIVE_INFINITY, "test context")
        assertFalse(coordResult.isValid)
        assertTrue(coordResult is ValidationResult.Invalid)
        assertTrue(coordResult.errors.any { it.contains("must be finite") })
    }
    
    // Helper method for creating test errors
    private fun ErrorUtils.validationError(elementId: String, rule: String): GraphvizError.ValidationError {
        return structureValidationError(elementId, rule)
    }
}