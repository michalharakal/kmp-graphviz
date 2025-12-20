package org.graphviz.kotlin.error

/**
 * Utilities for error reporting, debugging, and user-friendly error messages.
 * Provides structured error information and debugging context.
 */
object ErrorReporting {
    
    /**
     * Generate a comprehensive error report for debugging purposes.
     */
    fun generateErrorReport(error: GraphvizError): ErrorReport {
        return ErrorReport(
            error = error,
            timestamp = 0L, // Platform-specific timestamp would be implemented in expect/actual
            category = error.category,
            severity = determineSeverity(error),
            isRecoverable = error.isRecoverable,
            context = error.context,
            recoveryActions = error.recoveryActions,
            debugInfo = extractDebugInfo(error),
            stackTrace = error.message ?: "" // Simplified for multiplatform compatibility
        )
    }
    
    /**
     * Generate a user-friendly error message with actionable guidance.
     */
    fun generateUserMessage(error: GraphvizError): UserErrorMessage {
        val summary = generateErrorSummary(error)
        val explanation = generateErrorExplanation(error)
        val actions = error.recoveryActions.takeIf { it.isNotEmpty() }
            ?: generateDefaultActions(error)
        
        return UserErrorMessage(
            summary = summary,
            explanation = explanation,
            suggestedActions = actions,
            technicalDetails = if (shouldIncludeTechnicalDetails(error)) {
                generateTechnicalDetails(error)
            } else null
        )
    }
    
    /**
     * Format an error for console output with appropriate styling.
     */
    fun formatForConsole(error: GraphvizError, includeStackTrace: Boolean = false): String {
        val report = generateErrorReport(error)
        val userMessage = generateUserMessage(error)
        
        return buildString {
            // Header
            appendLine("❌ ${report.severity.displayName}: ${userMessage.summary}")
            appendLine()
            
            // Explanation
            if (userMessage.explanation.isNotEmpty()) {
                appendLine("📝 Details:")
                appendLine(userMessage.explanation.prependIndent("   "))
                appendLine()
            }
            
            // Context information
            when (val context = error.context) {
                is ErrorContext.Parse -> {
                    appendLine("📍 Location: Line ${context.line}, Column ${context.column}")
                    if (context.context.isNotEmpty()) {
                        appendLine("🔍 Context: ${context.context}")
                    }
                    if (context.expectedTokens.isNotEmpty()) {
                        appendLine("💡 Expected: ${context.expectedTokens.joinToString(", ")}")
                    }
                    if (context.actualToken.isNotEmpty()) {
                        appendLine("❗ Found: ${context.actualToken}")
                    }
                }
                is ErrorContext.Validation -> {
                    appendLine("🎯 Element: ${context.elementId}")
                    if (context.attributeName != null) {
                        appendLine("🏷️  Attribute: ${context.attributeName}")
                    }
                    appendLine("📋 Rule: ${context.rule}")
                }
                is ErrorContext.Layout -> {
                    appendLine("⚙️  Algorithm: ${context.algorithm}")
                    appendLine("📊 Graph: ${context.nodeCount} nodes, ${context.edgeCount} edges")
                    appendLine("🔄 Phase: ${context.phase}")
                }
                is ErrorContext.Render -> {
                    appendLine("🎨 Format: ${context.format}")
                    appendLine("🔄 Phase: ${context.phase}")
                    if (context.elementType != null && context.elementId != null) {
                        appendLine("🎯 Element: ${context.elementType} '${context.elementId}'")
                    }
                }
                is ErrorContext.Platform -> {
                    appendLine("💻 Platform: ${context.platform}")
                    appendLine("🔧 Feature: ${context.feature}")
                }
                is ErrorContext.Resource -> {
                    appendLine("📈 Resource: ${context.type}")
                    appendLine("📊 Requested: ${context.requested}")
                    if (context.available != null) {
                        appendLine("📉 Available: ${context.available}")
                    }
                }
                is ErrorContext.Configuration -> {
                    appendLine("⚙️  Config: ${context.type}")
                    appendLine("🔑 Key: ${context.key}")
                    appendLine("💾 Value: ${context.value}")
                }
                is ErrorContext.Internal -> {
                    appendLine("🔧 Component: ${context.component}")
                    appendLine("⚡ Operation: ${context.operation}")
                }
            }
            
            // Suggested actions
            if (userMessage.suggestedActions.isNotEmpty()) {
                appendLine()
                appendLine("💡 Suggested Actions:")
                userMessage.suggestedActions.forEach { action ->
                    appendLine("   • $action")
                }
            }
            
            // Technical details
            if (userMessage.technicalDetails != null) {
                appendLine()
                appendLine("🔧 Technical Details:")
                appendLine(userMessage.technicalDetails.prependIndent("   "))
            }
            
            // Stack trace
            if (includeStackTrace && error.message != null) {
                appendLine()
                appendLine("📚 Stack Trace:")
                appendLine((error.message ?: "").prependIndent("   "))
            }
        }
    }
    
    /**
     * Generate a structured error summary for logging systems.
     */
    fun generateStructuredLog(error: GraphvizError): Map<String, Any> {
        val report = generateErrorReport(error)
        
        return buildMap {
            put("timestamp", report.timestamp)
            put("category", report.category.name)
            put("severity", report.severity.name)
            put("message", error.message ?: "")
            put("recoverable", report.isRecoverable)
            
            // Context-specific fields
            when (val context = error.context) {
                is ErrorContext.Parse -> {
                    put("line", context.line)
                    put("column", context.column)
                    put("position", context.position)
                    put("parseContext", context.context)
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
                    context.elementType?.let { put("elementType", it) }
                    context.elementId?.let { put("elementId", it) }
                }
                is ErrorContext.Platform -> {
                    put("platform", context.platform)
                    put("feature", context.feature)
                    put("workaroundAvailable", context.workaroundAvailable)
                }
                is ErrorContext.Resource -> {
                    put("resourceType", context.type.name)
                    put("requested", context.requested)
                    context.available?.let { put("available", it) }
                }
                is ErrorContext.Configuration -> {
                    put("configType", context.type)
                    put("configKey", context.key)
                    put("configValue", context.value)
                }
                is ErrorContext.Internal -> {
                    put("component", context.component)
                    put("operation", context.operation)
                    put("debugInfo", context.debugInfo)
                }
            }
            
            // Recovery actions
            if (report.recoveryActions.isNotEmpty()) {
                put("recoveryActions", report.recoveryActions)
            }
            
            // Cause information
            error.cause?.let { cause ->
                put("causeType", cause::class.simpleName ?: "Unknown")
                put("causeMessage", cause.message ?: "")
            }
        }
    }
    
    private fun determineSeverity(error: GraphvizError): ErrorSeverity {
        return when (error.category) {
            ErrorCategory.INTERNAL -> ErrorSeverity.CRITICAL
            ErrorCategory.RESOURCE -> ErrorSeverity.HIGH
            ErrorCategory.PLATFORM -> if (error.isRecoverable) ErrorSeverity.MEDIUM else ErrorSeverity.HIGH
            ErrorCategory.LAYOUT -> ErrorSeverity.MEDIUM
            ErrorCategory.RENDER -> ErrorSeverity.MEDIUM
            ErrorCategory.VALIDATION -> ErrorSeverity.LOW
            ErrorCategory.PARSE -> ErrorSeverity.LOW
            ErrorCategory.CONFIGURATION -> ErrorSeverity.LOW
        }
    }
    
    private fun extractDebugInfo(error: GraphvizError): Map<String, String> {
        val info = mutableMapOf<String, String>()
        
        info["errorType"] = error::class.simpleName ?: "Unknown"
        info["category"] = error.category.name
        info["recoverable"] = error.isRecoverable.toString()
        
        when (val context = error.context) {
            is ErrorContext.Internal -> info.putAll(context.debugInfo)
            is ErrorContext.Parse -> {
                info["parsePosition"] = "${context.line}:${context.column}"
                info["parseContext"] = context.context
            }
            is ErrorContext.Layout -> {
                info["graphSize"] = "${context.nodeCount}n,${context.edgeCount}e"
                info["layoutPhase"] = context.phase.name
            }
            else -> {
                // Add context-specific debug info as needed
            }
        }
        
        error.cause?.let { cause ->
            info["causeType"] = cause::class.simpleName ?: "Unknown"
            info["causeMessage"] = cause.message ?: ""
        }
        
        return info
    }
    
    private fun generateErrorSummary(error: GraphvizError): String {
        return when (error) {
            is GraphvizError.ParseError -> "DOT parsing failed at line ${error.line}"
            is GraphvizError.ValidationError -> "Validation failed for ${error.elementId}"
            is GraphvizError.LayoutError -> "Layout calculation failed using ${error.algorithm}"
            is GraphvizError.RenderError -> "Rendering failed for ${error.format} format"
            is GraphvizError.PlatformError -> "Platform limitation on ${error.platform}"
            is GraphvizError.ResourceError -> "Insufficient ${error.resourceType.name.lowercase()}"
            is GraphvizError.ConfigurationError -> "Invalid configuration in ${error.configType}"
            is GraphvizError.InternalError -> "Internal error in ${error.component}"
        }
    }
    
    private fun generateErrorExplanation(error: GraphvizError): String {
        return when (error) {
            is GraphvizError.ParseError -> {
                "The DOT language parser encountered invalid syntax. " +
                "This usually means there's a typo or structural error in the graph definition."
            }
            is GraphvizError.ValidationError -> {
                "The graph structure or attributes don't meet the required constraints. " +
                "This could be due to invalid attribute values or broken references."
            }
            is GraphvizError.LayoutError -> {
                "The layout algorithm couldn't position the graph elements properly. " +
                "This might be due to graph complexity or algorithm limitations."
            }
            is GraphvizError.RenderError -> {
                "The renderer couldn't generate the requested output format. " +
                "This could be due to unsupported features or rendering constraints."
            }
            is GraphvizError.PlatformError -> {
                "The current platform doesn't support the requested feature. " +
                "This is a limitation of the target environment."
            }
            is GraphvizError.ResourceError -> {
                "The operation requires more system resources than are available. " +
                "Consider reducing the graph size or increasing available resources."
            }
            is GraphvizError.ConfigurationError -> {
                "The provided configuration contains invalid settings. " +
                "Check the configuration values against the documentation."
            }
            is GraphvizError.InternalError -> {
                "An unexpected internal error occurred. This indicates a bug in the library. " +
                "Please report this issue with the provided debug information."
            }
        }
    }
    
    private fun generateDefaultActions(error: GraphvizError): List<String> {
        return when (error.category) {
            ErrorCategory.PARSE -> listOf(
                "Check DOT syntax for typos or structural errors",
                "Validate graph structure and attribute syntax",
                "Refer to DOT language documentation"
            )
            ErrorCategory.VALIDATION -> listOf(
                "Verify attribute names and values",
                "Check graph structure consistency",
                "Review validation rules in documentation"
            )
            ErrorCategory.LAYOUT -> listOf(
                "Try a different layout algorithm",
                "Simplify the graph structure",
                "Check for cycles or degenerate cases"
            )
            ErrorCategory.RENDER -> listOf(
                "Try a different output format",
                "Check element attributes and positioning",
                "Use basic rendering options"
            )
            ErrorCategory.PLATFORM -> listOf(
                "Use platform-compatible features only",
                "Check multiplatform documentation",
                "Consider alternative approaches"
            )
            ErrorCategory.RESOURCE -> listOf(
                "Reduce graph complexity",
                "Increase available system resources",
                "Use streaming or chunked processing"
            )
            ErrorCategory.CONFIGURATION -> listOf(
                "Check configuration documentation",
                "Verify setting names and values",
                "Use default configuration as reference"
            )
            ErrorCategory.INTERNAL -> listOf(
                "Report this as a bug",
                "Include debug information in report",
                "Try alternative approaches if available"
            )
        }
    }
    
    private fun shouldIncludeTechnicalDetails(error: GraphvizError): Boolean {
        return error.category == ErrorCategory.INTERNAL || 
               error.cause != null ||
               (error.context is ErrorContext.Internal && (error.context as ErrorContext.Internal).debugInfo.isNotEmpty())
    }
    
    private fun generateTechnicalDetails(error: GraphvizError): String {
        return buildString {
            appendLine("Error Type: ${error::class.simpleName}")
            appendLine("Category: ${error.category}")
            appendLine("Recoverable: ${error.isRecoverable}")
            
            error.cause?.let { cause ->
                appendLine("Caused by: ${cause::class.simpleName}: ${cause.message}")
            }
            
            if (error.context is ErrorContext.Internal) {
                val internalContext = error.context as ErrorContext.Internal
                if (internalContext.debugInfo.isNotEmpty()) {
                    appendLine("Debug Info:")
                    internalContext.debugInfo.forEach { (key, value) ->
                        appendLine("  $key: $value")
                    }
                }
            }
        }
    }
}

/**
 * Comprehensive error report for debugging and logging.
 */
data class ErrorReport(
    val error: GraphvizError,
    val timestamp: Long,
    val category: ErrorCategory,
    val severity: ErrorSeverity,
    val isRecoverable: Boolean,
    val context: ErrorContext,
    val recoveryActions: List<String>,
    val debugInfo: Map<String, String>,
    val stackTrace: String
)

/**
 * User-friendly error message with actionable guidance.
 */
data class UserErrorMessage(
    val summary: String,
    val explanation: String,
    val suggestedActions: List<String>,
    val technicalDetails: String? = null
)

/**
 * Error severity levels for prioritization and handling.
 */
enum class ErrorSeverity(val displayName: String) {
    LOW("Warning"),
    MEDIUM("Error"),
    HIGH("Critical Error"),
    CRITICAL("Fatal Error")
}