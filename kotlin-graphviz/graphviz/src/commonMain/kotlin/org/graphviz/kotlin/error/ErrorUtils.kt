package org.graphviz.kotlin.error

/**
 * Utility functions for working with GraphvizError instances and error handling patterns.
 * Provides convenience methods for error creation, transformation, and handling.
 */
object ErrorUtils {
    
    /**
     * Create a parse error with context information.
     */
    fun parseError(
        line: Int,
        column: Int,
        position: Int,
        message: String,
        context: String = "",
        expectedTokens: List<String> = emptyList(),
        actualToken: String = "",
        cause: Throwable? = null
    ): GraphvizError.ParseError {
        return GraphvizError.ParseError(
            line = line,
            column = column,
            position = position,
            parseContext = message,
            expectedTokens = expectedTokens,
            actualToken = actualToken,
            cause = cause
        )
    }
    
    /**
     * Create a validation error for attribute validation failures.
     */
    fun attributeValidationError(
        elementId: String,
        attributeName: String,
        actualValue: String,
        expectedValue: String? = null,
        validationRule: String,
        cause: Throwable? = null
    ): GraphvizError.ValidationError {
        return GraphvizError.ValidationError(
            validationType = ValidationType.ATTRIBUTE_VALUE,
            elementId = elementId,
            attributeName = attributeName,
            expectedValue = expectedValue,
            actualValue = actualValue,
            validationRule = validationRule,
            cause = cause
        )
    }
    
    /**
     * Create a validation error for graph structure issues.
     */
    fun structureValidationError(
        elementId: String,
        validationRule: String,
        cause: Throwable? = null
    ): GraphvizError.ValidationError {
        return GraphvizError.ValidationError(
            validationType = ValidationType.GRAPH_STRUCTURE,
            elementId = elementId,
            validationRule = validationRule,
            cause = cause
        )
    }
    
    /**
     * Create a layout error with algorithm and phase information.
     */
    fun layoutError(
        algorithm: String,
        phase: LayoutPhase,
        nodeCount: Int,
        edgeCount: Int,
        message: String,
        fallbackAvailable: Boolean = false,
        cause: Throwable? = null
    ): GraphvizError.LayoutError {
        return GraphvizError.LayoutError(
            algorithm = algorithm,
            phase = phase,
            nodeCount = nodeCount,
            edgeCount = edgeCount,
            layoutMessage = message,
            fallbackAvailable = fallbackAvailable,
            cause = cause
        )
    }
    
    /**
     * Create a render error with format and phase information.
     */
    fun renderError(
        format: String,
        phase: RenderPhase,
        message: String,
        elementType: String? = null,
        elementId: String? = null,
        cause: Throwable? = null
    ): GraphvizError.RenderError {
        return GraphvizError.RenderError(
            format = format,
            renderPhase = phase,
            elementType = elementType,
            elementId = elementId,
            renderMessage = message,
            cause = cause
        )
    }
    
    /**
     * Create a platform error with feature limitation information.
     */
    fun platformError(
        platform: String,
        feature: String,
        limitation: String,
        workaroundAvailable: Boolean = false,
        cause: Throwable? = null
    ): GraphvizError.PlatformError {
        return GraphvizError.PlatformError(
            platform = platform,
            feature = feature,
            limitation = limitation,
            workaroundAvailable = workaroundAvailable,
            cause = cause
        )
    }
    
    /**
     * Create a resource error with resource type and usage information.
     */
    fun resourceError(
        resourceType: ResourceType,
        requested: String,
        available: String? = null,
        threshold: String? = null,
        cause: Throwable? = null
    ): GraphvizError.ResourceError {
        return GraphvizError.ResourceError(
            resourceType = resourceType,
            requested = requested,
            available = available,
            threshold = threshold,
            cause = cause
        )
    }
    
    /**
     * Create a configuration error with setting information.
     */
    fun configurationError(
        configType: String,
        configKey: String,
        configValue: String,
        message: String,
        validValues: List<String> = emptyList(),
        cause: Throwable? = null
    ): GraphvizError.ConfigurationError {
        return GraphvizError.ConfigurationError(
            configType = configType,
            configKey = configKey,
            configValue = configValue,
            validValues = validValues,
            configMessage = message,
            cause = cause
        )
    }
    
    /**
     * Create an internal error with component and operation information.
     */
    fun internalError(
        component: String,
        operation: String,
        message: String,
        debugInfo: Map<String, String> = emptyMap(),
        cause: Throwable? = null
    ): GraphvizError.InternalError {
        return GraphvizError.InternalError(
            component = component,
            operation = operation,
            internalMessage = message,
            debugInfo = debugInfo,
            cause = cause
        )
    }
    
    /**
     * Wrap a generic exception as a GraphvizError.
     */
    fun wrapException(
        exception: Throwable,
        component: String,
        operation: String,
        additionalContext: Map<String, String> = emptyMap()
    ): GraphvizError {
        return when (exception) {
            is GraphvizError -> exception
            else -> GraphvizError.InternalError(
                component = component,
                operation = operation,
                internalMessage = exception.message ?: "Unexpected exception: ${exception::class.simpleName}",
                debugInfo = additionalContext + mapOf(
                    "exceptionType" to (exception::class.simpleName ?: "Unknown"),
                    "originalMessage" to (exception.message ?: "")
                ),
                cause = exception
            )
        }
    }
    
    /**
     * Chain multiple errors together, preserving the original cause.
     */
    fun chainErrors(
        primaryError: GraphvizError,
        secondaryError: GraphvizError,
        component: String,
        operation: String
    ): GraphvizError.InternalError {
        return GraphvizError.InternalError(
            component = component,
            operation = operation,
            internalMessage = "Multiple errors occurred: ${primaryError.message}; ${secondaryError.message}",
            debugInfo = mapOf(
                "primaryError" to primaryError::class.simpleName.orEmpty(),
                "secondaryError" to secondaryError::class.simpleName.orEmpty(),
                "primaryMessage" to (primaryError.message ?: ""),
                "secondaryMessage" to (secondaryError.message ?: "")
            ),
            cause = primaryError
        )
    }
    
    /**
     * Extract error information for logging or debugging.
     */
    fun extractErrorInfo(error: GraphvizError): Map<String, Any> {
        return buildMap {
            put("type", error::class.simpleName ?: "Unknown")
            put("category", error.category.name)
            put("message", error.message ?: "")
            put("recoverable", error.isRecoverable)
            
            when (val context = error.context) {
                is ErrorContext.Parse -> {
                    put("line", context.line)
                    put("column", context.column)
                    put("position", context.position)
                }
                is ErrorContext.Validation -> {
                    put("validationType", context.type.name)
                    put("elementId", context.elementId)
                    context.attributeName?.let { put("attributeName", it) }
                }
                is ErrorContext.Layout -> {
                    put("algorithm", context.algorithm)
                    put("phase", context.phase.name)
                    put("nodeCount", context.nodeCount)
                    put("edgeCount", context.edgeCount)
                }
                is ErrorContext.Render -> {
                    put("format", context.format)
                    put("phase", context.phase.name)
                }
                is ErrorContext.Platform -> {
                    put("platform", context.platform)
                    put("feature", context.feature)
                }
                is ErrorContext.Resource -> {
                    put("resourceType", context.type.name)
                    put("requested", context.requested)
                }
                is ErrorContext.Configuration -> {
                    put("configType", context.type)
                    put("configKey", context.key)
                }
                is ErrorContext.Internal -> {
                    put("component", context.component)
                    put("operation", context.operation)
                    if (context.debugInfo.isNotEmpty()) {
                        put("debugInfo", context.debugInfo)
                    }
                }
            }
            
            error.cause?.let { cause ->
                put("causeType", cause::class.simpleName ?: "Unknown")
                put("causeMessage", cause.message ?: "")
            }
        }
    }
    
    /**
     * Check if an error is of a specific category.
     */
    fun isCategory(error: GraphvizError, category: ErrorCategory): Boolean {
        return error.category == category
    }
    
    /**
     * Check if an error is recoverable.
     */
    fun isRecoverable(error: GraphvizError): Boolean {
        return error.isRecoverable
    }
    
    /**
     * Get the root cause of an error chain.
     */
    fun getRootCause(error: GraphvizError): Throwable {
        var current: Throwable = error
        while (current.cause != null) {
            current = current.cause!!
        }
        return current
    }
    
    /**
     * Format an error for display in different contexts.
     */
    fun formatError(error: GraphvizError, format: ErrorFormat): String {
        return when (format) {
            ErrorFormat.BRIEF -> error.message ?: "Unknown error"
            ErrorFormat.DETAILED -> ErrorReporting.generateUserMessage(error).let { msg ->
                "${msg.summary}: ${msg.explanation}"
            }
            ErrorFormat.CONSOLE -> ErrorReporting.formatForConsole(error, includeStackTrace = false)
            ErrorFormat.DEBUG -> ErrorReporting.formatForConsole(error, includeStackTrace = true)
        }
    }
    
    /**
     * Create a summary of multiple errors.
     */
    fun summarizeErrors(errors: List<GraphvizError>): ErrorSummary {
        val categoryCounts = errors.groupingBy { it.category }.eachCount()
        val recoverableCount = errors.count { it.isRecoverable }
        val severityCounts = errors.map { ErrorReporting.generateErrorReport(it).severity }
            .groupingBy { it }.eachCount()
        
        return ErrorSummary(
            totalErrors = errors.size,
            categoryCounts = categoryCounts,
            recoverableCount = recoverableCount,
            severityCounts = severityCounts,
            mostSevere = severityCounts.keys.maxByOrNull { it.ordinal }
        )
    }
}

/**
 * Different formats for error display.
 */
enum class ErrorFormat {
    BRIEF,      // Just the error message
    DETAILED,   // Summary and explanation
    CONSOLE,    // Full console formatting without stack trace
    DEBUG       // Full console formatting with stack trace
}

/**
 * Summary of multiple errors for reporting.
 */
data class ErrorSummary(
    val totalErrors: Int,
    val categoryCounts: Map<ErrorCategory, Int>,
    val recoverableCount: Int,
    val severityCounts: Map<ErrorSeverity, Int>,
    val mostSevere: ErrorSeverity?
) {
    val unrecoverableCount: Int get() = totalErrors - recoverableCount
    val hasRecoverableErrors: Boolean get() = recoverableCount > 0
    val hasUnrecoverableErrors: Boolean get() = unrecoverableCount > 0
    val isCritical: Boolean get() = mostSevere == ErrorSeverity.CRITICAL
}

/**
 * Extension functions for working with GraphvizError instances.
 */

/**
 * Check if this error is of a specific type.
 */
inline fun <reified T : GraphvizError> GraphvizError.isType(): Boolean = this is T

/**
 * Cast this error to a specific type if possible.
 */
inline fun <reified T : GraphvizError> GraphvizError.asType(): T? = this as? T

/**
 * Transform this error using a mapping function.
 */
inline fun <T> GraphvizError.fold(
    onParseError: (GraphvizError.ParseError) -> T,
    onValidationError: (GraphvizError.ValidationError) -> T,
    onLayoutError: (GraphvizError.LayoutError) -> T,
    onRenderError: (GraphvizError.RenderError) -> T,
    onPlatformError: (GraphvizError.PlatformError) -> T,
    onResourceError: (GraphvizError.ResourceError) -> T,
    onConfigurationError: (GraphvizError.ConfigurationError) -> T,
    onInternalError: (GraphvizError.InternalError) -> T
): T = when (this) {
    is GraphvizError.ParseError -> onParseError(this)
    is GraphvizError.ValidationError -> onValidationError(this)
    is GraphvizError.LayoutError -> onLayoutError(this)
    is GraphvizError.RenderError -> onRenderError(this)
    is GraphvizError.PlatformError -> onPlatformError(this)
    is GraphvizError.ResourceError -> onResourceError(this)
    is GraphvizError.ConfigurationError -> onConfigurationError(this)
    is GraphvizError.InternalError -> onInternalError(this)
}