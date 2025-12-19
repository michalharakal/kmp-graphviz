package org.graphviz.kotlin.parser

import org.graphviz.kotlin.model.Graph

/**
 * Result of parsing a DOT language input.
 */
sealed class ParseResult<out T> {
    /**
     * Successful parse result.
     */
    data class Success<T>(val value: T) : ParseResult<T>()
    
    /**
     * Parse error with detailed information.
     */
    data class Error(
        val message: String,
        val line: Int,
        val column: Int,
        val position: Int,
        val context: String = "",
        val cause: Throwable? = null
    ) : ParseResult<Nothing>() {
        
        override fun toString(): String {
            return "Parse error at line $line, column $column: $message" +
                   if (context.isNotEmpty()) "\nContext: $context" else ""
        }
    }
    
    /**
     * Check if the result is successful.
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Check if the result is an error.
     */
    val isError: Boolean get() = this is Error
    
    /**
     * Get the value if successful, or null if error.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Error -> null
    }
    
    /**
     * Get the value if successful, or throw an exception if error.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Error -> throw DotParseException(message, line, column, position, context, cause)
    }
    
    /**
     * Transform the success value if present.
     */
    inline fun <R> map(transform: (T) -> R): ParseResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Error -> this
    }
    
    /**
     * Transform the success value if present, allowing for failure.
     */
    inline fun <R> flatMap(transform: (T) -> ParseResult<R>): ParseResult<R> = when (this) {
        is Success -> transform(value)
        is Error -> this
    }
}

/**
 * Exception thrown when parsing fails.
 */
class DotParseException(
    message: String,
    val line: Int,
    val column: Int,
    val position: Int,
    val context: String = "",
    cause: Throwable? = null
) : Exception(message, cause) {
    
    override fun toString(): String {
        return "DotParseException at line $line, column $column: $message" +
               if (context.isNotEmpty()) "\nContext: $context" else ""
    }
}