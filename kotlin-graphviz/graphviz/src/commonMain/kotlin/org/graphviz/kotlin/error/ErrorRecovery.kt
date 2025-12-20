package org.graphviz.kotlin.error

/**
 * Provides error recovery and fallback mechanisms for handling GraphvizError instances.
 * Supports graceful degradation and alternative approaches when primary operations fail.
 */
object ErrorRecovery {
    
    /**
     * Attempt to recover from a GraphvizError using available fallback strategies.
     */
    fun <T> recover(error: GraphvizError, fallbackStrategies: List<FallbackStrategy<T>>): RecoveryResult<T> {
        if (!error.isRecoverable) {
            return RecoveryResult.Unrecoverable(error)
        }
        
        for (strategy in fallbackStrategies) {
            if (strategy.canHandle(error)) {
                try {
                    val result = strategy.execute(error)
                    return RecoveryResult.Recovered(result, strategy.description, error)
                } catch (e: Exception) {
                    // Continue to next strategy
                    continue
                }
            }
        }
        
        return RecoveryResult.Failed(error, "No suitable fallback strategy found")
    }
    
    /**
     * Create a recovery context with multiple fallback options.
     */
    fun <T> withFallbacks(
        primaryOperation: () -> T,
        vararg fallbackStrategies: FallbackStrategy<T>
    ): RecoveryResult<T> {
        return try {
            val result = primaryOperation()
            RecoveryResult.Success(result)
        } catch (e: GraphvizError) {
            recover(e, fallbackStrategies.toList())
        } catch (e: Exception) {
            val internalError = GraphvizError.InternalError(
                component = "ErrorRecovery",
                operation = "withFallbacks",
                internalMessage = "Unexpected exception during primary operation",
                debugInfo = mapOf("exceptionType" to e::class.simpleName.orEmpty()),
                cause = e
            )
            RecoveryResult.Unrecoverable(internalError)
        }
    }
    
    /**
     * Execute an operation with automatic retry on recoverable errors.
     */
    fun <T> withRetry(
        operation: () -> T,
        maxAttempts: Int = 3,
        retryDelay: Long = 100,
        shouldRetry: (GraphvizError) -> Boolean = { it.isRecoverable }
    ): RecoveryResult<T> {
        var lastError: GraphvizError? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                val result = operation()
                return RecoveryResult.Success(result)
            } catch (e: GraphvizError) {
                lastError = e
                if (!shouldRetry(e) || attempt == maxAttempts - 1) {
                    // Don't continue if we shouldn't retry or this is the last attempt
                } else {
                    // Simple delay simulation (multiplatform compatible)
                    // In a real implementation, this would use platform-specific delay mechanisms
                    var counter = 0
                    while (counter < retryDelay) {
                        counter++
                    }
                }
            } catch (e: Exception) {
                val internalError = GraphvizError.InternalError(
                    component = "ErrorRecovery",
                    operation = "withRetry",
                    internalMessage = "Unexpected exception during retry operation",
                    debugInfo = mapOf(
                        "attempt" to (attempt + 1).toString(),
                        "exceptionType" to e::class.simpleName.orEmpty()
                    ),
                    cause = e
                )
                return RecoveryResult.Unrecoverable(internalError)
            }
        }
        
        return RecoveryResult.Failed(
            lastError ?: GraphvizError.InternalError(
                component = "ErrorRecovery",
                operation = "withRetry",
                internalMessage = "Retry failed without capturing error"
            ),
            "Operation failed after $maxAttempts attempts"
        )
    }
    
    /**
     * Provide graceful degradation by returning a simplified result when the full operation fails.
     */
    fun <T, S> withGracefulDegradation(
        fullOperation: () -> T,
        simplifiedOperation: () -> S,
        degradationMessage: String = "Using simplified approach due to error"
    ): DegradationResult<T, S> {
        return try {
            val result = fullOperation()
            DegradationResult.Full(result)
        } catch (e: GraphvizError) {
            try {
                val simplifiedResult = simplifiedOperation()
                DegradationResult.Degraded(simplifiedResult, degradationMessage, e)
            } catch (fallbackError: Exception) {
                val combinedError = GraphvizError.InternalError(
                    component = "ErrorRecovery",
                    operation = "withGracefulDegradation",
                    internalMessage = "Both full and simplified operations failed",
                    debugInfo = mapOf(
                        "primaryError" to e.message.orEmpty(),
                        "fallbackError" to fallbackError.message.orEmpty()
                    ),
                    cause = e
                )
                DegradationResult.Failed(combinedError)
            }
        } catch (e: Exception) {
            val internalError = GraphvizError.InternalError(
                component = "ErrorRecovery",
                operation = "withGracefulDegradation",
                internalMessage = "Unexpected exception during full operation",
                debugInfo = mapOf("exceptionType" to e::class.simpleName.orEmpty()),
                cause = e
            )
            DegradationResult.Failed(internalError)
        }
    }
}

/**
 * Result of an error recovery attempt.
 */
sealed class RecoveryResult<out T> {
    /**
     * Operation succeeded without needing recovery.
     */
    data class Success<T>(val value: T) : RecoveryResult<T>()
    
    /**
     * Operation failed but was recovered using a fallback strategy.
     */
    data class Recovered<T>(
        val value: T,
        val strategyUsed: String,
        val originalError: GraphvizError
    ) : RecoveryResult<T>()
    
    /**
     * Operation failed and recovery was attempted but failed.
     */
    data class Failed(
        val error: GraphvizError,
        val reason: String
    ) : RecoveryResult<Nothing>()
    
    /**
     * Operation failed with an unrecoverable error.
     */
    data class Unrecoverable(val error: GraphvizError) : RecoveryResult<Nothing>()
    
    /**
     * Check if the result represents success (either direct or recovered).
     */
    val isSuccess: Boolean get() = this is Success || this is Recovered
    
    /**
     * Get the value if successful, or null if failed.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Recovered -> value
        is Failed -> null
        is Unrecoverable -> null
    }
    
    /**
     * Get the value if successful, or throw the error if failed.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Recovered -> value
        is Failed -> throw error
        is Unrecoverable -> throw error
    }
}

/**
 * Result of a graceful degradation attempt.
 */
sealed class DegradationResult<out T, out S> {
    /**
     * Full operation succeeded.
     */
    data class Full<T>(val value: T) : DegradationResult<T, Nothing>()
    
    /**
     * Full operation failed but simplified operation succeeded.
     */
    data class Degraded<S>(
        val value: S,
        val message: String,
        val originalError: GraphvizError
    ) : DegradationResult<Nothing, S>()
    
    /**
     * Both full and simplified operations failed.
     */
    data class Failed(val error: GraphvizError) : DegradationResult<Nothing, Nothing>()
    
    /**
     * Check if any result was obtained (full or degraded).
     */
    val hasResult: Boolean get() = this is Full || this is Degraded
}

/**
 * Strategy for handling specific types of errors with fallback behavior.
 */
abstract class FallbackStrategy<T> {
    /**
     * Description of what this strategy does.
     */
    abstract val description: String
    
    /**
     * Check if this strategy can handle the given error.
     */
    abstract fun canHandle(error: GraphvizError): Boolean
    
    /**
     * Execute the fallback strategy.
     */
    abstract fun execute(error: GraphvizError): T
}

/**
 * Common fallback strategies for different error types.
 */
object CommonFallbackStrategies {
    
    /**
     * Fallback strategy for layout errors - try simpler layout algorithm.
     */
    class SimpleLayoutFallback<T>(
        private val simpleLayoutOperation: (String) -> T
    ) : FallbackStrategy<T>() {
        override val description = "Use simpler layout algorithm"
        
        override fun canHandle(error: GraphvizError): Boolean {
            return error is GraphvizError.LayoutError && error.fallbackAvailable
        }
        
        override fun execute(error: GraphvizError): T {
            val layoutError = error as GraphvizError.LayoutError
            return simpleLayoutOperation(layoutError.algorithm)
        }
    }
    
    /**
     * Fallback strategy for rendering errors - use basic rendering.
     */
    class BasicRenderFallback<T>(
        private val basicRenderOperation: (String) -> T
    ) : FallbackStrategy<T>() {
        override val description = "Use basic rendering without advanced features"
        
        override fun canHandle(error: GraphvizError): Boolean {
            return error is GraphvizError.RenderError
        }
        
        override fun execute(error: GraphvizError): T {
            val renderError = error as GraphvizError.RenderError
            return basicRenderOperation(renderError.format)
        }
    }
    
    /**
     * Fallback strategy for platform errors - use platform-agnostic approach.
     */
    class PlatformAgnosticFallback<T>(
        private val agnosticOperation: (String) -> T
    ) : FallbackStrategy<T>() {
        override val description = "Use platform-agnostic implementation"
        
        override fun canHandle(error: GraphvizError): Boolean {
            return error is GraphvizError.PlatformError && error.workaroundAvailable
        }
        
        override fun execute(error: GraphvizError): T {
            val platformError = error as GraphvizError.PlatformError
            return agnosticOperation(platformError.platform)
        }
    }
    
    /**
     * Fallback strategy for resource errors - use reduced complexity.
     */
    class ReducedComplexityFallback<T>(
        private val reducedOperation: (ResourceType) -> T
    ) : FallbackStrategy<T>() {
        override val description = "Reduce complexity to fit available resources"
        
        override fun canHandle(error: GraphvizError): Boolean {
            return error is GraphvizError.ResourceError
        }
        
        override fun execute(error: GraphvizError): T {
            val resourceError = error as GraphvizError.ResourceError
            return reducedOperation(resourceError.resourceType)
        }
    }
}