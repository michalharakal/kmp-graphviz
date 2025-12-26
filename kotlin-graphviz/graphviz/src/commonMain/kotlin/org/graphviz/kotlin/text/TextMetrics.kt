package org.graphviz.kotlin.text

import org.graphviz.kotlin.model.Point
import kotlin.math.*

/**
 * Text metrics system that matches original Graphviz font measurement behavior.
 * This provides precise text bounding box calculations compatible with Graphviz output.
 */
interface TextMetrics {
    /**
     * Measure the bounding box of text with the given font properties.
     * 
     * @param text The text to measure
     * @param fontFamily Font family name (e.g., "Arial", "Times New Roman")
     * @param fontSize Font size in points
     * @param fontStyle Font style (normal, italic, bold, etc.)
     * @return Text bounding box with precise dimensions
     */
    fun measureText(
        text: String,
        fontFamily: String = "Arial",
        fontSize: Double = 12.0,
        fontStyle: FontStyle = FontStyle.NORMAL
    ): TextBounds
    
    /**
     * Calculate the baseline offset for text positioning.
     * This matches Graphviz text baseline calculations.
     */
    fun getBaselineOffset(fontSize: Double, fontFamily: String = "Arial"): Double
    
    /**
     * Calculate line height for multi-line text.
     * This matches Graphviz line spacing behavior.
     */
    fun getLineHeight(fontSize: Double, fontFamily: String = "Arial"): Double
    
    /**
     * Get the advance width of a single character.
     * Used for precise character positioning.
     */
    fun getCharacterWidth(
        char: Char,
        fontFamily: String = "Arial",
        fontSize: Double = 12.0,
        fontStyle: FontStyle = FontStyle.NORMAL
    ): Double
}

/**
 * Text bounding box with precise dimensions matching Graphviz calculations.
 */
data class TextBounds(
    /**
     * Width of the text bounding box.
     */
    val width: Double,
    
    /**
     * Height of the text bounding box.
     */
    val height: Double,
    
    /**
     * Ascent (height above baseline).
     */
    val ascent: Double,
    
    /**
     * Descent (height below baseline).
     */
    val descent: Double,
    
    /**
     * Leading (additional space between lines).
     */
    val leading: Double,
    
    /**
     * Left bearing (offset from origin to left edge).
     */
    val leftBearing: Double,
    
    /**
     * Right bearing (offset from right edge to advance width).
     */
    val rightBearing: Double
) {
    /**
     * Total advance width (width + bearings).
     */
    val advanceWidth: Double get() = width + leftBearing + rightBearing
    
    /**
     * Total line height (ascent + descent + leading).
     */
    val totalHeight: Double get() = ascent + descent + leading
    
    /**
     * Baseline Y position relative to top of bounding box.
     */
    val baselineY: Double get() = ascent
}

/**
 * Font style enumeration matching Graphviz font styles.
 */
enum class FontStyle(val cssValue: String, val graphvizValue: String) {
    NORMAL("normal", ""),
    ITALIC("italic", "italic"),
    BOLD("bold", "bold"),
    BOLD_ITALIC("italic", "bold,italic");
    
    companion object {
        fun fromGraphviz(style: String): FontStyle {
            return when (style.lowercase()) {
                "italic" -> ITALIC
                "bold" -> BOLD
                "bold,italic", "italic,bold" -> BOLD_ITALIC
                else -> NORMAL
            }
        }
    }
}

/**
 * Default text metrics implementation using mathematical font models.
 * This provides consistent cross-platform text measurement that matches
 * original Graphviz behavior as closely as possible.
 */
class DefaultTextMetrics : TextMetrics {
    
    companion object {
        // Font metrics constants based on typical font characteristics
        // These values are calibrated to match Graphviz text measurements
        private val FONT_METRICS = mapOf(
            "arial" to FontMetricsData(
                ascent = 0.75,
                descent = 0.25,
                leading = 0.15,
                avgCharWidth = 0.55,
                spaceWidth = 0.28,
                xHeight = 0.52
            ),
            "times" to FontMetricsData(
                ascent = 0.76,
                descent = 0.24,
                leading = 0.12,
                avgCharWidth = 0.50,
                spaceWidth = 0.25,
                xHeight = 0.48
            ),
            "courier" to FontMetricsData(
                ascent = 0.75,
                descent = 0.25,
                leading = 0.10,
                avgCharWidth = 0.60,
                spaceWidth = 0.60,
                xHeight = 0.50
            ),
            "helvetica" to FontMetricsData(
                ascent = 0.74,
                descent = 0.26,
                leading = 0.14,
                avgCharWidth = 0.56,
                spaceWidth = 0.28,
                xHeight = 0.53
            )
        )
        
        // Character width multipliers for different character classes
        private val CHAR_WIDTH_MULTIPLIERS = mapOf(
            'i' to 0.3, 'l' to 0.3, 'j' to 0.3, 'f' to 0.35, 't' to 0.4,
            'r' to 0.45, 'I' to 0.35, '1' to 0.5, '.' to 0.3, ',' to 0.3,
            ':' to 0.3, ';' to 0.3, '!' to 0.35, '|' to 0.3,
            'm' to 0.85, 'w' to 0.8, 'M' to 0.85, 'W' to 0.9,
            '@' to 0.95, '#' to 0.7, '%' to 0.8, '&' to 0.75,
            ' ' to 0.28 // space character
        )
    }
    
    private data class FontMetricsData(
        val ascent: Double,
        val descent: Double,
        val leading: Double,
        val avgCharWidth: Double,
        val spaceWidth: Double,
        val xHeight: Double
    )
    
    override fun measureText(
        text: String,
        fontFamily: String,
        fontSize: Double,
        fontStyle: FontStyle
    ): TextBounds {
        if (text.isEmpty()) {
            return TextBounds(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
        
        val metrics = getFontMetrics(fontFamily)
        val styleMultiplier = getStyleMultiplier(fontStyle)
        
        // Calculate text width by summing character widths
        var totalWidth = 0.0
        for (char in text) {
            totalWidth += getCharacterWidth(char, fontFamily, fontSize, fontStyle)
        }
        
        // Apply style adjustments
        val adjustedWidth = totalWidth * styleMultiplier
        
        // Calculate height components
        val ascent = fontSize * metrics.ascent
        val descent = fontSize * metrics.descent
        val leading = fontSize * metrics.leading
        val height = ascent + descent
        
        // Calculate bearings (simplified model)
        val leftBearing = fontSize * 0.02 // Small left margin
        val rightBearing = fontSize * 0.02 // Small right margin
        
        return TextBounds(
            width = adjustedWidth,
            height = height,
            ascent = ascent,
            descent = descent,
            leading = leading,
            leftBearing = leftBearing,
            rightBearing = rightBearing
        )
    }
    
    override fun getBaselineOffset(fontSize: Double, fontFamily: String): Double {
        val metrics = getFontMetrics(fontFamily)
        return fontSize * metrics.ascent
    }
    
    override fun getLineHeight(fontSize: Double, fontFamily: String): Double {
        val metrics = getFontMetrics(fontFamily)
        return fontSize * (metrics.ascent + metrics.descent + metrics.leading)
    }
    
    override fun getCharacterWidth(
        char: Char,
        fontFamily: String,
        fontSize: Double,
        fontStyle: FontStyle
    ): Double {
        val metrics = getFontMetrics(fontFamily)
        val baseWidth = fontSize * metrics.avgCharWidth
        
        // Apply character-specific width multiplier
        val charMultiplier = CHAR_WIDTH_MULTIPLIERS[char] ?: 1.0
        val styleMultiplier = getStyleMultiplier(fontStyle)
        
        return baseWidth * charMultiplier * styleMultiplier
    }
    
    /**
     * Get font metrics for a given font family.
     */
    private fun getFontMetrics(fontFamily: String): FontMetricsData {
        val normalizedName = fontFamily.lowercase().replace("-", "").replace(" ", "")
        
        return when {
            normalizedName.contains("arial") || normalizedName.contains("sans") -> 
                FONT_METRICS["arial"]!!
            normalizedName.contains("times") || normalizedName.contains("serif") -> 
                FONT_METRICS["times"]!!
            normalizedName.contains("courier") || normalizedName.contains("mono") -> 
                FONT_METRICS["courier"]!!
            normalizedName.contains("helvetica") -> 
                FONT_METRICS["helvetica"]!!
            else -> FONT_METRICS["arial"]!! // Default fallback
        }
    }
    
    /**
     * Get width multiplier for font style.
     */
    private fun getStyleMultiplier(fontStyle: FontStyle): Double {
        return when (fontStyle) {
            FontStyle.NORMAL -> 1.0
            FontStyle.ITALIC -> 1.0 // Italic doesn't change width significantly
            FontStyle.BOLD -> 1.1 // Bold is slightly wider
            FontStyle.BOLD_ITALIC -> 1.1 // Bold italic same as bold
        }
    }
}

/**
 * Multi-line text measurement and positioning utilities.
 */
object MultiLineTextUtils {
    
    /**
     * Measure multi-line text with proper line spacing.
     */
    fun measureMultiLineText(
        text: String,
        fontFamily: String = "Arial",
        fontSize: Double = 12.0,
        fontStyle: FontStyle = FontStyle.NORMAL,
        textMetrics: TextMetrics = DefaultTextMetrics()
    ): MultiLineTextBounds {
        val lines = splitTextIntoLines(text)
        if (lines.isEmpty()) {
            return MultiLineTextBounds(emptyList(), 0.0, 0.0)
        }
        
        val lineBounds = lines.map { line ->
            textMetrics.measureText(line, fontFamily, fontSize, fontStyle)
        }
        
        val maxWidth = lineBounds.maxOfOrNull { it.width } ?: 0.0
        val lineHeight = textMetrics.getLineHeight(fontSize, fontFamily)
        val totalHeight = lineHeight * lines.size
        
        return MultiLineTextBounds(lineBounds, maxWidth, totalHeight)
    }
    
    /**
     * Calculate positions for each line of multi-line text.
     */
    fun calculateLinePositions(
        bounds: MultiLineTextBounds,
        basePosition: Point,
        alignment: TextAlignment = TextAlignment.CENTER,
        textMetrics: TextMetrics = DefaultTextMetrics()
    ): List<Point> {
        val positions = mutableListOf<Point>()
        val lineHeight = bounds.totalHeight / bounds.lineBounds.size
        
        for (i in bounds.lineBounds.indices) {
            val lineBounds = bounds.lineBounds[i]
            val y = basePosition.y - bounds.totalHeight / 2 + (i + 0.5) * lineHeight
            
            val x = when (alignment) {
                TextAlignment.LEFT -> basePosition.x - bounds.maxWidth / 2
                TextAlignment.CENTER -> basePosition.x - lineBounds.width / 2
                TextAlignment.RIGHT -> basePosition.x + bounds.maxWidth / 2 - lineBounds.width
            }
            
            positions.add(Point(x, y))
        }
        
        return positions
    }
    
    /**
     * Split text into lines handling Graphviz line break sequences.
     */
    private fun splitTextIntoLines(text: String): List<String> {
        return text
            .replace("\\n", "\n")
            .replace("\\l", "\n") // Graphviz left-aligned line break
            .replace("\\r", "\n") // Graphviz right-aligned line break
            .split("\n")
    }
}

/**
 * Multi-line text bounds information.
 */
data class MultiLineTextBounds(
    val lineBounds: List<TextBounds>,
    val maxWidth: Double,
    val totalHeight: Double
)

/**
 * Text alignment options.
 */
enum class TextAlignment {
    LEFT, CENTER, RIGHT
}

/**
 * Utility functions for coordinate calculations related to text.
 */
object TextCoordinateUtils {
    
    /**
     * Calculate anchor point for text based on alignment.
     */
    fun calculateAnchorPoint(
        bounds: TextBounds,
        position: Point,
        horizontalAlign: TextAlignment = TextAlignment.CENTER,
        verticalAlign: VerticalAlignment = VerticalAlignment.MIDDLE
    ): Point {
        val x = when (horizontalAlign) {
            TextAlignment.LEFT -> position.x
            TextAlignment.CENTER -> position.x - bounds.width / 2
            TextAlignment.RIGHT -> position.x - bounds.width
        }
        
        val y = when (verticalAlign) {
            VerticalAlignment.TOP -> position.y
            VerticalAlignment.MIDDLE -> position.y - bounds.height / 2
            VerticalAlignment.BASELINE -> position.y - bounds.ascent
            VerticalAlignment.BOTTOM -> position.y - bounds.height
        }
        
        return Point(x, y)
    }
    
    /**
     * Calculate text bounding rectangle for collision detection.
     */
    fun calculateTextRectangle(
        bounds: TextBounds,
        position: Point,
        horizontalAlign: TextAlignment = TextAlignment.CENTER,
        verticalAlign: VerticalAlignment = VerticalAlignment.MIDDLE
    ): TextRectangle {
        val anchor = calculateAnchorPoint(bounds, position, horizontalAlign, verticalAlign)
        
        return TextRectangle(
            x = anchor.x,
            y = anchor.y,
            width = bounds.width,
            height = bounds.height
        )
    }
}

/**
 * Vertical alignment options for text.
 */
enum class VerticalAlignment {
    TOP, MIDDLE, BASELINE, BOTTOM
}

/**
 * Rectangle representing text bounds for layout calculations.
 */
data class TextRectangle(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
) {
    val left: Double get() = x
    val right: Double get() = x + width
    val top: Double get() = y
    val bottom: Double get() = y + height
    val centerX: Double get() = x + width / 2
    val centerY: Double get() = y + height / 2
    
    /**
     * Check if this rectangle intersects with another.
     */
    fun intersects(other: TextRectangle): Boolean {
        return !(right <= other.left || left >= other.right || 
                bottom <= other.top || top >= other.bottom)
    }
}