package org.graphviz.kotlin.error

/**
 * Comprehensive sealed class hierarchy for all Graphviz error categories.
 * Provides context-rich error messages with debugging information and support
 * for error recovery and fallback mechanisms.
 */
sealed class GraphvizError : Exception {
    
    /**
     * The error category for classification and handling.
     */
    abstract val category: ErrorCategory
    
    /**
     * Whether this error allows for recovery or fallback mechanisms.
     */
    abstract val isRecoverable: Boolean
    
    /**
     * Additional context information for debugging.
     */
    abstract val context: ErrorContext
    
    /**
     * Suggested recovery actions, if any.
     */
    abstract val recoveryActions: List<String>
    
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable?) : super(message, cause)
    
    /**
     * Parse errors occur during DOT language parsing.
     */
    data class ParseError(
        val line: Int,
        val column: Int,
        val position: Int,
        val parseContext: String,
        val expectedTokens: List<String> = emptyList(),
        val actualToken: String = "",
        override val cause: Throwable? = null
    ) : GraphvizError("Parse error at line $line, column $column: ${parseContext}") {
        
        override val category = ErrorCategory.PARSE
        override val isRecoverable = true
        override val context = ErrorContext.Parse(line, column, position, parseContext, expectedTokens, actualToken)
        override val recoveryActions = listOf(
            "Check DOT syntax at line $line, column $column",
            if (expectedTokens.isNotEmpty()) "Expected one of: ${expectedTokens.joinToString(", ")}" else "",
            if (actualToken.isNotEmpty()) "Found: '$actualToken'" else "",
            "Verify graph structure and attribute syntax"
        ).filter { it.isNotEmpty() }
    }
    
    /**
     * Validation errors occur when graph structure or attributes are invalid.
     */
    data class ValidationError(
        val validationType: ValidationType,
        val elementId: String,
        val attributeName: String? = null,
        val expectedValue: String? = null,
        val actualValue: String? = null,
        val validationRule: String,
        override val cause: Throwable? = null
    ) : GraphvizError("Validation error in $elementId: $validationRule") {
        
        override val category = ErrorCategory.VALIDATION
        override val isRecoverable = true
        override val context = ErrorContext.Validation(validationType, elementId, attributeName, expectedValue, actualValue, validationRule)
        override val recoveryActions = buildList {
            add("Fix validation issue in $elementId")
            if (attributeName != null) {
                add("Check attribute '$attributeName'")
                if (expectedValue != null) add("Expected: $expectedValue")
                if (actualValue != null) add("Actual: $actualValue")
            }
            add("Verify: $validationRule")
        }
    }
    
    /**
     * Layout errors occur during graph layout calculation.
     */
    data class LayoutError(
        val algorithm: String,
        val phase: LayoutPhase,
        val nodeCount: Int,
        val edgeCount: Int,
        val layoutMessage: String,
        val fallbackAvailable: Boolean = false,
        override val cause: Throwable? = null
    ) : GraphvizError("Layout error in $algorithm ($phase): $layoutMessage") {
        
        override val category = ErrorCategory.LAYOUT
        override val isRecoverable = fallbackAvailable
        override val context = ErrorContext.Layout(algorithm, phase, nodeCount, edgeCount, layoutMessage, fallbackAvailable)
        override val recoveryActions = buildList {
            add("Layout failed in $algorithm during $phase")
            add("Graph size: $nodeCount nodes, $edgeCount edges")
            if (fallbackAvailable) {
                add("Try simpler layout algorithm")
                add("Consider reducing graph complexity")
            } else {
                add("Check graph structure for cycles or degenerate cases")
                add("Verify node and edge attributes")
            }
        }
    }
    
    /**
     * Rendering errors occur during output generation.
     */
    data class RenderError(
        val format: String,
        val renderPhase: RenderPhase,
        val elementType: String? = null,
        val elementId: String? = null,
        val renderMessage: String,
        override val cause: Throwable? = null
    ) : GraphvizError("Render error in $format ($renderPhase): $renderMessage") {
        
        override val category = ErrorCategory.RENDER
        override val isRecoverable = true
        override val context = ErrorContext.Render(format, renderPhase, elementType, elementId, renderMessage)
        override val recoveryActions = buildList {
            add("Rendering failed in $format during $renderPhase")
            if (elementType != null && elementId != null) {
                add("Problem with $elementType '$elementId'")
            }
            add("Check output format compatibility")
            add("Verify element attributes and positioning")
            add("Try alternative rendering options")
        }
    }
    
    /**
     * Platform errors occur due to target-specific limitations.
     */
    data class PlatformError(
        val platform: String,
        val feature: String,
        val limitation: String,
        val workaroundAvailable: Boolean = false,
        override val cause: Throwable? = null
    ) : GraphvizError("Platform error on $platform: $limitation") {
        
        override val category = ErrorCategory.PLATFORM
        override val isRecoverable = workaroundAvailable
        override val context = ErrorContext.Platform(platform, feature, limitation, workaroundAvailable)
        override val recoveryActions = buildList {
            add("Feature '$feature' not supported on $platform")
            add("Limitation: $limitation")
            if (workaroundAvailable) {
                add("Alternative approach available")
                add("Check platform-specific documentation")
            } else {
                add("Consider using different target platform")
                add("Check multiplatform compatibility")
            }
        }
    }
    
    /**
     * Resource errors occur when system resources are insufficient.
     */
    data class ResourceError(
        val resourceType: ResourceType,
        val requested: String,
        val available: String? = null,
        val threshold: String? = null,
        override val cause: Throwable? = null
    ) : GraphvizError("Resource error: insufficient $resourceType") {
        
        override val category = ErrorCategory.RESOURCE
        override val isRecoverable = true
        override val context = ErrorContext.Resource(resourceType, requested, available, threshold)
        override val recoveryActions = buildList {
            add("Insufficient $resourceType")
            add("Requested: $requested")
            if (available != null) add("Available: $available")
            if (threshold != null) add("Threshold: $threshold")
            add("Reduce graph complexity")
            add("Increase available resources")
            add("Use streaming or chunked processing")
        }
    }
    
    /**
     * Configuration errors occur due to invalid settings or options.
     */
    data class ConfigurationError(
        val configType: String,
        val configKey: String,
        val configValue: String,
        val validValues: List<String> = emptyList(),
        val configMessage: String,
        override val cause: Throwable? = null
    ) : GraphvizError("Configuration error in $configType: $configMessage") {
        
        override val category = ErrorCategory.CONFIGURATION
        override val isRecoverable = true
        override val context = ErrorContext.Configuration(configType, configKey, configValue, validValues, configMessage)
        override val recoveryActions = buildList {
            add("Invalid configuration in $configType")
            add("Key: $configKey")
            add("Value: $configValue")
            if (validValues.isNotEmpty()) {
                add("Valid values: ${validValues.joinToString(", ")}")
            }
            add("Check configuration documentation")
        }
    }
    
    /**
     * Internal errors indicate bugs or unexpected conditions.
     */
    data class InternalError(
        val component: String,
        val operation: String,
        val internalMessage: String,
        val debugInfo: Map<String, String> = emptyMap(),
        override val cause: Throwable? = null
    ) : GraphvizError("Internal error in $component.$operation: $internalMessage") {
        
        override val category = ErrorCategory.INTERNAL
        override val isRecoverable = false
        override val context = ErrorContext.Internal(component, operation, internalMessage, debugInfo)
        override val recoveryActions = listOf(
            "This is an internal error - please report as a bug",
            "Component: $component",
            "Operation: $operation",
            "Include debug information in bug report"
        )
    }
}

/**
 * Categories of errors for classification and handling.
 */
enum class ErrorCategory {
    PARSE,
    VALIDATION,
    LAYOUT,
    RENDER,
    PLATFORM,
    RESOURCE,
    CONFIGURATION,
    INTERNAL
}

/**
 * Types of validation errors.
 */
enum class ValidationType {
    GRAPH_STRUCTURE,
    NODE_ATTRIBUTES,
    EDGE_ATTRIBUTES,
    GRAPH_ATTRIBUTES,
    ATTRIBUTE_VALUE,
    REFERENCE_INTEGRITY
}

/**
 * Phases of layout calculation where errors can occur.
 */
enum class LayoutPhase {
    INITIALIZATION,
    RANKING,
    ORDERING,
    POSITIONING,
    EDGE_ROUTING,
    FINALIZATION
}

/**
 * Phases of rendering where errors can occur.
 */
enum class RenderPhase {
    INITIALIZATION,
    DOCUMENT_SETUP,
    NODE_RENDERING,
    EDGE_RENDERING,
    TEXT_RENDERING,
    FINALIZATION
}

/**
 * Types of system resources that can be exhausted.
 */
enum class ResourceType {
    MEMORY,
    COMPUTATION_TIME,
    STACK_DEPTH,
    FILE_HANDLES,
    NETWORK_BANDWIDTH
}

/**
 * Context information for different error types.
 */
sealed class ErrorContext {
    data class Parse(
        val line: Int,
        val column: Int,
        val position: Int,
        val context: String,
        val expectedTokens: List<String>,
        val actualToken: String
    ) : ErrorContext()
    
    data class Validation(
        val type: ValidationType,
        val elementId: String,
        val attributeName: String?,
        val expectedValue: String?,
        val actualValue: String?,
        val rule: String
    ) : ErrorContext()
    
    data class Layout(
        val algorithm: String,
        val phase: LayoutPhase,
        val nodeCount: Int,
        val edgeCount: Int,
        val message: String,
        val fallbackAvailable: Boolean
    ) : ErrorContext()
    
    data class Render(
        val format: String,
        val phase: RenderPhase,
        val elementType: String?,
        val elementId: String?,
        val message: String
    ) : ErrorContext()
    
    data class Platform(
        val platform: String,
        val feature: String,
        val limitation: String,
        val workaroundAvailable: Boolean
    ) : ErrorContext()
    
    data class Resource(
        val type: ResourceType,
        val requested: String,
        val available: String?,
        val threshold: String?
    ) : ErrorContext()
    
    data class Configuration(
        val type: String,
        val key: String,
        val value: String,
        val validValues: List<String>,
        val message: String
    ) : ErrorContext()
    
    data class Internal(
        val component: String,
        val operation: String,
        val message: String,
        val debugInfo: Map<String, String>
    ) : ErrorContext()
}