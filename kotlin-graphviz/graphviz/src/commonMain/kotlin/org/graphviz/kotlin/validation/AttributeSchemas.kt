package org.graphviz.kotlin.validation

import org.graphviz.kotlin.model.AttributeValue
import org.graphviz.kotlin.model.Color

/**
 * Defines and validates attribute schemas for different graph elements.
 * Provides comprehensive validation rules for known Graphviz attributes.
 */
class AttributeSchemas {
    
    private val graphAttributes = buildGraphAttributeSchema()
    private val nodeAttributes = buildNodeAttributeSchema()
    private val edgeAttributes = buildEdgeAttributeSchema()
    
    /**
     * Validate an attribute against the appropriate schema.
     */
    fun validateAttribute(
        context: AttributeContext,
        name: String,
        value: AttributeValue
    ): AttributeValidationResult {
        val schema = when (context) {
            AttributeContext.GRAPH -> graphAttributes
            AttributeContext.NODE -> nodeAttributes
            AttributeContext.EDGE -> edgeAttributes
        }
        
        val attributeRule = schema[name]
        
        // If attribute is not in schema, it's still valid (custom attributes allowed)
        if (attributeRule == null) {
            return AttributeValidationResult.Valid
        }
        
        // Validate type
        if (!attributeRule.acceptedTypes.any { it.matches(value) }) {
            return AttributeValidationResult.Invalid(
                reason = "Attribute '$name' expects ${attributeRule.expectedTypeDescription} but got ${value::class.simpleName}",
                expectedType = attributeRule.expectedTypeDescription
            )
        }
        
        // Validate value constraints
        val constraintResult = attributeRule.validateValue?.invoke(value)
        if (constraintResult != null) {
            return AttributeValidationResult.Invalid(
                reason = constraintResult,
                expectedType = attributeRule.expectedTypeDescription
            )
        }
        
        return AttributeValidationResult.Valid
    }
    
    /**
     * Get attribute documentation for a given context.
     */
    fun getAttributeInfo(context: AttributeContext, name: String): AttributeInfo? {
        val schema = when (context) {
            AttributeContext.GRAPH -> graphAttributes
            AttributeContext.NODE -> nodeAttributes
            AttributeContext.EDGE -> edgeAttributes
        }
        
        return schema[name]?.let { rule ->
            AttributeInfo(
                name = name,
                description = rule.description,
                expectedType = rule.expectedTypeDescription,
                defaultValue = rule.defaultValue,
                validValues = rule.validValues
            )
        }
    }
    
    /**
     * Get all known attributes for a context.
     */
    fun getKnownAttributes(context: AttributeContext): Set<String> {
        return when (context) {
            AttributeContext.GRAPH -> graphAttributes.keys
            AttributeContext.NODE -> nodeAttributes.keys
            AttributeContext.EDGE -> edgeAttributes.keys
        }
    }
    
    private fun buildGraphAttributeSchema(): Map<String, AttributeRule> {
        return mapOf(
            "bgcolor" to AttributeRule(
                description = "Background color for drawing",
                acceptedTypes = listOf(AttributeTypeMatch.COLOR),
                expectedTypeDescription = "color"
            ),
            "center" to AttributeRule(
                description = "Center drawing on page",
                acceptedTypes = listOf(AttributeTypeMatch.BOOLEAN),
                expectedTypeDescription = "boolean"
            ),
            "charset" to AttributeRule(
                description = "Character encoding",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string",
                validValues = listOf("UTF-8", "iso-8859-1", "Latin1")
            ),
            "fontname" to AttributeRule(
                description = "Font family",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string"
            ),
            "fontsize" to AttributeRule(
                description = "Font size in points",
                acceptedTypes = listOf(AttributeTypeMatch.NUMBER),
                expectedTypeDescription = "number",
                validateValue = { value ->
                    if (value is AttributeValue.NumberValue && value.value <= 0) {
                        "Font size must be positive"
                    } else null
                }
            ),
            "label" to AttributeRule(
                description = "Text label",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string"
            ),
            "rankdir" to AttributeRule(
                description = "Direction of graph layout",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string",
                validValues = listOf("TB", "BT", "LR", "RL"),
                validateValue = { value ->
                    if (value is AttributeValue.StringValue && 
                        value.value !in listOf("TB", "BT", "LR", "RL")) {
                        "rankdir must be one of: TB, BT, LR, RL"
                    } else null
                }
            ),
            "splines" to AttributeRule(
                description = "Edge routing style",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string",
                validValues = listOf("none", "line", "polyline", "curved", "ortho", "spline")
            )
        )
    }
    
    private fun buildNodeAttributeSchema(): Map<String, AttributeRule> {
        return mapOf(
            "color" to AttributeRule(
                description = "Node border color",
                acceptedTypes = listOf(AttributeTypeMatch.COLOR),
                expectedTypeDescription = "color"
            ),
            "fillcolor" to AttributeRule(
                description = "Node fill color",
                acceptedTypes = listOf(AttributeTypeMatch.COLOR),
                expectedTypeDescription = "color"
            ),
            "fontcolor" to AttributeRule(
                description = "Font color",
                acceptedTypes = listOf(AttributeTypeMatch.COLOR),
                expectedTypeDescription = "color"
            ),
            "fontname" to AttributeRule(
                description = "Font family",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string"
            ),
            "fontsize" to AttributeRule(
                description = "Font size in points",
                acceptedTypes = listOf(AttributeTypeMatch.NUMBER),
                expectedTypeDescription = "number",
                validateValue = { value ->
                    if (value is AttributeValue.NumberValue && value.value <= 0) {
                        "Font size must be positive"
                    } else null
                }
            ),
            "height" to AttributeRule(
                description = "Minimum node height",
                acceptedTypes = listOf(AttributeTypeMatch.NUMBER),
                expectedTypeDescription = "number",
                validateValue = { value ->
                    if (value is AttributeValue.NumberValue && value.value < 0) {
                        "Height must be non-negative"
                    } else null
                }
            ),
            "label" to AttributeRule(
                description = "Text label",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string"
            ),
            "penwidth" to AttributeRule(
                description = "Border width",
                acceptedTypes = listOf(AttributeTypeMatch.NUMBER),
                expectedTypeDescription = "number",
                validateValue = { value ->
                    if (value is AttributeValue.NumberValue && value.value < 0) {
                        "Pen width must be non-negative"
                    } else null
                }
            ),
            "shape" to AttributeRule(
                description = "Node shape",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string",
                validValues = listOf(
                    "box", "polygon", "ellipse", "oval", "circle", "point", "egg", "triangle",
                    "plaintext", "plain", "diamond", "trapezium", "parallelogram", "house",
                    "pentagon", "hexagon", "septagon", "octagon", "doublecircle", "doubleoctagon",
                    "tripleoctagon", "invtriangle", "invtrapezium", "invhouse", "Mdiamond",
                    "Msquare", "Mcircle", "rect", "rectangle", "square", "star", "none",
                    "underline", "cylinder", "note", "tab", "folder", "box3d", "component",
                    "promoter", "cds", "terminator", "utr", "primersite", "restrictionsite",
                    "fivepoverhang", "threepoverhang", "noverhang", "assembly", "signature",
                    "insulator", "ribosite", "rnastab", "proteasesite", "proteinstab",
                    "rpromoter", "rarrow", "larrow", "lpromoter"
                )
            ),
            "style" to AttributeRule(
                description = "Node style",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string",
                validValues = listOf("solid", "dashed", "dotted", "bold", "rounded", "filled", "striped", "wedged")
            ),
            "width" to AttributeRule(
                description = "Minimum node width",
                acceptedTypes = listOf(AttributeTypeMatch.NUMBER),
                expectedTypeDescription = "number",
                validateValue = { value ->
                    if (value is AttributeValue.NumberValue && value.value < 0) {
                        "Width must be non-negative"
                    } else null
                }
            )
        )
    }
    
    private fun buildEdgeAttributeSchema(): Map<String, AttributeRule> {
        return mapOf(
            "arrowhead" to AttributeRule(
                description = "Arrow shape at head",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string",
                validValues = listOf(
                    "normal", "inv", "dot", "invdot", "odot", "invodot", "none", "tee",
                    "empty", "invempty", "diamond", "odiamond", "ediamond", "crow", "box",
                    "obox", "open", "halfopen", "vee"
                )
            ),
            "arrowtail" to AttributeRule(
                description = "Arrow shape at tail",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string",
                validValues = listOf(
                    "normal", "inv", "dot", "invdot", "odot", "invodot", "none", "tee",
                    "empty", "invempty", "diamond", "odiamond", "ediamond", "crow", "box",
                    "obox", "open", "halfopen", "vee"
                )
            ),
            "color" to AttributeRule(
                description = "Edge color",
                acceptedTypes = listOf(AttributeTypeMatch.COLOR),
                expectedTypeDescription = "color"
            ),
            "dir" to AttributeRule(
                description = "Edge direction",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string",
                validValues = listOf("forward", "back", "both", "none"),
                validateValue = { value ->
                    if (value is AttributeValue.StringValue && 
                        value.value !in listOf("forward", "back", "both", "none")) {
                        "dir must be one of: forward, back, both, none"
                    } else null
                }
            ),
            "fontcolor" to AttributeRule(
                description = "Font color",
                acceptedTypes = listOf(AttributeTypeMatch.COLOR),
                expectedTypeDescription = "color"
            ),
            "fontname" to AttributeRule(
                description = "Font family",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string"
            ),
            "fontsize" to AttributeRule(
                description = "Font size in points",
                acceptedTypes = listOf(AttributeTypeMatch.NUMBER),
                expectedTypeDescription = "number",
                validateValue = { value ->
                    if (value is AttributeValue.NumberValue && value.value <= 0) {
                        "Font size must be positive"
                    } else null
                }
            ),
            "label" to AttributeRule(
                description = "Text label",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string"
            ),
            "penwidth" to AttributeRule(
                description = "Edge line width",
                acceptedTypes = listOf(AttributeTypeMatch.NUMBER),
                expectedTypeDescription = "number",
                validateValue = { value ->
                    if (value is AttributeValue.NumberValue && value.value < 0) {
                        "Pen width must be non-negative"
                    } else null
                }
            ),
            "style" to AttributeRule(
                description = "Edge style",
                acceptedTypes = listOf(AttributeTypeMatch.STRING),
                expectedTypeDescription = "string",
                validValues = listOf("solid", "dashed", "dotted", "bold", "invis")
            ),
            "weight" to AttributeRule(
                description = "Edge weight for layout",
                acceptedTypes = listOf(AttributeTypeMatch.NUMBER),
                expectedTypeDescription = "number",
                validateValue = { value ->
                    if (value is AttributeValue.NumberValue && value.value < 0) {
                        "Weight must be non-negative"
                    } else null
                }
            )
        )
    }
}

/**
 * Context in which an attribute is used.
 */
enum class AttributeContext {
    GRAPH,
    NODE,
    EDGE
}

/**
 * Result of attribute validation.
 */
sealed class AttributeValidationResult {
    object Valid : AttributeValidationResult()
    data class Invalid(val reason: String, val expectedType: String) : AttributeValidationResult()
}

/**
 * Information about an attribute.
 */
data class AttributeInfo(
    val name: String,
    val description: String,
    val expectedType: String,
    val defaultValue: String? = null,
    val validValues: List<String>? = null
)

/**
 * Rule for validating an attribute.
 */
private data class AttributeRule(
    val description: String,
    val acceptedTypes: List<AttributeTypeMatch>,
    val expectedTypeDescription: String,
    val defaultValue: String? = null,
    val validValues: List<String>? = null,
    val validateValue: ((AttributeValue) -> String?)? = null
)

/**
 * Matcher for attribute value types.
 */
private sealed class AttributeTypeMatch {
    abstract fun matches(value: AttributeValue): Boolean
    
    object STRING : AttributeTypeMatch() {
        override fun matches(value: AttributeValue) = value is AttributeValue.StringValue
    }
    
    object NUMBER : AttributeTypeMatch() {
        override fun matches(value: AttributeValue) = value is AttributeValue.NumberValue
    }
    
    object COLOR : AttributeTypeMatch() {
        override fun matches(value: AttributeValue) = value is AttributeValue.ColorValue
    }
    
    object BOOLEAN : AttributeTypeMatch() {
        override fun matches(value: AttributeValue) = value is AttributeValue.BooleanValue
    }
}