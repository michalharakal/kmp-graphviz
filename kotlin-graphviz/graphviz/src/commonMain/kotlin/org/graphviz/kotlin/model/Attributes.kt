package org.graphviz.kotlin.model

/**
 * Represents different types of attribute values supported by Graphviz.
 */
sealed class AttributeValue {
    data class StringValue(val value: String) : AttributeValue()
    data class NumberValue(val value: Double) : AttributeValue()
    data class ColorValue(val value: Color) : AttributeValue()
    data class BooleanValue(val value: Boolean) : AttributeValue()
    
    /**
     * Convert the attribute value to its string representation for DOT output.
     */
    fun toDotString(): String = when (this) {
        is StringValue -> if (needsQuoting(value)) "\"$value\"" else value
        is NumberValue -> value.toString()
        is ColorValue -> value.toDotString()
        is BooleanValue -> value.toString()
    }
    
    private fun needsQuoting(str: String): Boolean {
        return str.isEmpty() || 
               str.contains(' ') || 
               str.contains('\t') || 
               str.contains('\n') || 
               str.contains('"') ||
               str.any { !it.isLetterOrDigit() && it != '_' }
    }
}

/**
 * Represents a color value that can be specified in various formats.
 */
sealed class Color {
    data class Named(val name: String) : Color()
    data class Hex(val value: String) : Color() {
        init {
            require(value.matches(Regex("#[0-9A-Fa-f]{6}"))) { 
                "Hex color must be in format #RRGGBB, got: $value" 
            }
        }
    }
    data class RGB(val red: Int, val green: Int, val blue: Int) : Color() {
        init {
            require(red in 0..255) { "Red component must be 0-255, got: $red" }
            require(green in 0..255) { "Green component must be 0-255, got: $green" }
            require(blue in 0..255) { "Blue component must be 0-255, got: $blue" }
        }
    }
    
    fun toDotString(): String = when (this) {
        is Named -> name
        is Hex -> value
        is RGB -> "#${red.toString(16).padStart(2, '0')}${green.toString(16).padStart(2, '0')}${blue.toString(16).padStart(2, '0')}"
    }
    
    companion object {
        // Common colors for convenience
        val Red = Named("red")
        val Green = Named("green")
        val Blue = Named("blue")
        val Yellow = Named("yellow")
        val Orange = Named("orange")
        val Purple = Named("purple")
        val Pink = Named("pink")
        val Brown = Named("brown")
        val Gray = Named("gray")
        val Black = Named("black")
        val White = Named("white")
        
        // Light colors
        val LightRed = Named("lightcoral")
        val LightGreen = Named("lightgreen")
        val LightBlue = Named("lightblue")
        val LightYellow = Named("lightyellow")
        val LightGray = Named("lightgray")
        val LightPink = Named("lightpink")
        
        // Dark colors
        val DarkRed = Named("darkred")
        val DarkGreen = Named("darkgreen")
        val DarkBlue = Named("darkblue")
        val DarkGray = Named("darkgray")
    }
}

/**
 * Type-safe key for accessing attributes in an AttributeMap.
 */
data class AttributeKey<T>(
    val name: String,
    val type: AttributeType<T>,
    val defaultValue: T? = null
) {
    companion object {
        // Common attribute keys
        val LABEL = AttributeKey("label", AttributeType.STRING)
        val COLOR = AttributeKey("color", AttributeType.COLOR)
        val STYLE = AttributeKey("style", AttributeType.STRING)
        val SHAPE = AttributeKey("shape", AttributeType.STRING)
        val WIDTH = AttributeKey("width", AttributeType.NUMBER)
        val HEIGHT = AttributeKey("height", AttributeType.NUMBER)
        val FONTSIZE = AttributeKey("fontsize", AttributeType.NUMBER)
        val FONTNAME = AttributeKey("fontname", AttributeType.STRING)
        val FILLCOLOR = AttributeKey("fillcolor", AttributeType.COLOR)
        val PENWIDTH = AttributeKey("penwidth", AttributeType.NUMBER)
    }
}

/**
 * Defines the type of an attribute for validation and conversion.
 */
sealed class AttributeType<T> {
    object STRING : AttributeType<String>()
    object NUMBER : AttributeType<Double>()
    object COLOR : AttributeType<Color>()
    object BOOLEAN : AttributeType<Boolean>()
    
    fun validate(value: AttributeValue): Boolean = when (this) {
        STRING -> value is AttributeValue.StringValue
        NUMBER -> value is AttributeValue.NumberValue
        COLOR -> value is AttributeValue.ColorValue
        BOOLEAN -> value is AttributeValue.BooleanValue
    }
    
    @Suppress("UNCHECKED_CAST")
    fun extract(value: AttributeValue): T = when (this) {
        STRING -> (value as AttributeValue.StringValue).value as T
        NUMBER -> (value as AttributeValue.NumberValue).value as T
        COLOR -> (value as AttributeValue.ColorValue).value as T
        BOOLEAN -> (value as AttributeValue.BooleanValue).value as T
    }
}

/**
 * Result of attribute validation.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
    
    val isValid: Boolean get() = this is Valid
}

/**
 * Immutable map of attributes with type-safe access and validation.
 */
class AttributeMap private constructor(
    private val attributes: Map<String, AttributeValue>
) {
    
    /**
     * Get a typed attribute value.
     */
    fun <T> get(key: AttributeKey<T>): T? {
        val value = attributes[key.name] ?: return key.defaultValue
        return if (key.type.validate(value)) {
            key.type.extract(value)
        } else {
            key.defaultValue
        }
    }
    
    /**
     * Get a raw attribute value.
     */
    fun getRaw(name: String): AttributeValue? = attributes[name]
    
    /**
     * Set a typed attribute value, returning a new AttributeMap.
     */
    fun <T> set(key: AttributeKey<T>, value: T): AttributeMap {
        val attributeValue = when (key.type) {
            AttributeType.STRING -> AttributeValue.StringValue(value as String)
            AttributeType.NUMBER -> AttributeValue.NumberValue(value as Double)
            AttributeType.COLOR -> AttributeValue.ColorValue(value as Color)
            AttributeType.BOOLEAN -> AttributeValue.BooleanValue(value as Boolean)
        }
        return AttributeMap(attributes + (key.name to attributeValue))
    }
    
    /**
     * Set a raw attribute value, returning a new AttributeMap.
     */
    fun setRaw(name: String, value: AttributeValue): AttributeMap {
        return AttributeMap(attributes + (name to value))
    }
    
    /**
     * Remove an attribute, returning a new AttributeMap.
     */
    fun remove(key: String): AttributeMap {
        return AttributeMap(attributes - key)
    }
    
    /**
     * Get all attribute names.
     */
    val keys: Set<String> get() = attributes.keys
    
    /**
     * Check if the map contains a specific attribute.
     */
    fun contains(key: String): Boolean = attributes.containsKey(key)
    
    /**
     * Check if the map is empty.
     */
    val isEmpty: Boolean get() = attributes.isEmpty()
    
    /**
     * Get the number of attributes.
     */
    val size: Int get() = attributes.size
    
    /**
     * Validate all attributes against known schemas.
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        for ((name, value) in attributes) {
            // Basic validation - more sophisticated validation can be added later
            when (value) {
                is AttributeValue.ColorValue -> {
                    // Color validation is handled in the Color class constructor
                }
                is AttributeValue.NumberValue -> {
                    if (!value.value.isFinite()) {
                        errors.add("Attribute '$name' has invalid number value: ${value.value}")
                    }
                }
                is AttributeValue.StringValue -> {
                    // String values are generally always valid
                }
                is AttributeValue.BooleanValue -> {
                    // Boolean values are always valid
                }
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Merge with another AttributeMap, with values from the other map taking precedence.
     */
    fun merge(other: AttributeMap): AttributeMap {
        return AttributeMap(attributes + other.attributes)
    }
    
    /**
     * Convert to a map of string representations for DOT output.
     */
    fun toDotMap(): Map<String, String> {
        return attributes.mapValues { (_, value) -> value.toDotString() }
    }
    
    override fun equals(other: Any?): Boolean {
        return other is AttributeMap && attributes == other.attributes
    }
    
    override fun hashCode(): Int = attributes.hashCode()
    
    override fun toString(): String = "AttributeMap($attributes)"
    
    companion object {
        /**
         * Create an empty AttributeMap.
         */
        fun empty(): AttributeMap = AttributeMap(emptyMap())
        
        /**
         * Create an AttributeMap from a map of string values.
         */
        fun fromStrings(attributes: Map<String, String>): AttributeMap {
            val attributeValues = attributes.mapValues { (_, value) ->
                AttributeValue.StringValue(value)
            }
            return AttributeMap(attributeValues)
        }
        
        /**
         * Create an AttributeMap from raw attribute values.
         */
        fun fromValues(attributes: Map<String, AttributeValue>): AttributeMap {
            return AttributeMap(attributes.toMap())
        }
        
        /**
         * Builder for creating AttributeMaps fluently.
         */
        fun builder(): AttributeMapBuilder = AttributeMapBuilder()
    }
}

/**
 * Builder class for creating AttributeMaps fluently.
 */
class AttributeMapBuilder {
    private val attributes = mutableMapOf<String, AttributeValue>()
    
    fun <T> set(key: AttributeKey<T>, value: T): AttributeMapBuilder {
        val attributeValue = when (key.type) {
            AttributeType.STRING -> AttributeValue.StringValue(value as String)
            AttributeType.NUMBER -> AttributeValue.NumberValue(value as Double)
            AttributeType.COLOR -> AttributeValue.ColorValue(value as Color)
            AttributeType.BOOLEAN -> AttributeValue.BooleanValue(value as Boolean)
        }
        attributes[key.name] = attributeValue
        return this
    }
    
    fun setRaw(name: String, value: AttributeValue): AttributeMapBuilder {
        attributes[name] = value
        return this
    }
    
    fun setString(name: String, value: String): AttributeMapBuilder {
        attributes[name] = AttributeValue.StringValue(value)
        return this
    }
    
    fun build(): AttributeMap = AttributeMap.fromValues(attributes)
}