package org.graphviz.kotlin.visual

import org.graphviz.kotlin.model.Point

/**
 * Parses SVG content for detailed structural comparison.
 * Extracts elements, coordinates, attributes, and other structural information.
 */
object SvgParser {
    
    /**
     * Parses SVG content into a structured representation.
     */
    fun parse(svgContent: String): ParsedSvg {
        val elements = extractElements(svgContent)
        val coordinates = extractCoordinates(svgContent)
        val attributes = extractAttributes(svgContent)
        val viewport = extractViewport(svgContent)
        val textElements = extractTextElements(svgContent)
        val pathElements = extractPathElements(svgContent)
        
        return ParsedSvg(
            elements = elements,
            coordinates = coordinates,
            attributes = attributes,
            viewport = viewport,
            textElements = textElements,
            pathElements = pathElements
        )
    }
    
    /**
     * Compares two parsed SVG structures for detailed analysis.
     */
    fun compare(svg1: ParsedSvg, svg2: ParsedSvg, tolerances: ComparisonTolerances): DetailedComparison {
        val elementComparison = compareElements(svg1.elements, svg2.elements)
        val coordinateComparison = compareCoordinates(svg1.coordinates, svg2.coordinates, tolerances)
        val attributeComparison = compareAttributes(svg1.attributes, svg2.attributes, tolerances)
        val textComparison = compareTextElements(svg1.textElements, svg2.textElements)
        val pathComparison = comparePathElements(svg1.pathElements, svg2.pathElements, tolerances)
        val viewportComparison = compareViewports(svg1.viewport, svg2.viewport, tolerances)
        
        return DetailedComparison(
            elements = elementComparison,
            coordinates = coordinateComparison,
            attributes = attributeComparison,
            text = textComparison,
            paths = pathComparison,
            viewport = viewportComparison
        )
    }
    
    // Private extraction methods
    
    private fun extractElements(svgContent: String): List<SvgElement> {
        val elements = mutableListOf<SvgElement>()
        
        // Extract all elements with IDs
        val elementPattern = Regex("""<(\w+)[^>]*id="([^"]+)"[^>]*(?:/>|>.*?</\1>)""", RegexOption.DOT_MATCHES_ALL)
        
        elementPattern.findAll(svgContent).forEach { match ->
            val elementType = match.groupValues[1]
            val elementId = match.groupValues[2]
            val fullMatch = match.value
            
            elements.add(SvgElement(
                type = elementType,
                id = elementId,
                content = fullMatch
            ))
        }
        
        return elements
    }
    
    private fun extractCoordinates(svgContent: String): Map<String, ElementCoordinates> {
        val coordinates = mutableMapOf<String, ElementCoordinates>()
        
        // Extract transform coordinates
        val transformPattern = Regex("""id="([^"]+)"[^>]*transform="translate\(([^,]+),([^)]+)\)"""")
        transformPattern.findAll(svgContent).forEach { match ->
            val elementId = match.groupValues[1]
            val x = match.groupValues[2].toDoubleOrNull() ?: 0.0
            val y = match.groupValues[3].toDoubleOrNull() ?: 0.0
            coordinates[elementId] = ElementCoordinates(Point(x, y), CoordinateType.TRANSFORM)
        }
        
        // Extract direct position coordinates (x, y attributes)
        val positionPattern = Regex("""id="([^"]+)"[^>]*x="([^"]+)"[^>]*y="([^"]+)"""")
        positionPattern.findAll(svgContent).forEach { match ->
            val elementId = match.groupValues[1]
            val x = match.groupValues[2].toDoubleOrNull() ?: 0.0
            val y = match.groupValues[3].toDoubleOrNull() ?: 0.0
            if (!coordinates.containsKey(elementId)) {
                coordinates[elementId] = ElementCoordinates(Point(x, y), CoordinateType.POSITION)
            }
        }
        
        // Extract center coordinates (cx, cy attributes)
        val centerPattern = Regex("""id="([^"]+)"[^>]*cx="([^"]+)"[^>]*cy="([^"]+)"""")
        centerPattern.findAll(svgContent).forEach { match ->
            val elementId = match.groupValues[1]
            val cx = match.groupValues[2].toDoubleOrNull() ?: 0.0
            val cy = match.groupValues[3].toDoubleOrNull() ?: 0.0
            if (!coordinates.containsKey(elementId)) {
                coordinates[elementId] = ElementCoordinates(Point(cx, cy), CoordinateType.CENTER)
            }
        }
        
        return coordinates
    }
    
    private fun extractAttributes(svgContent: String): Map<String, Map<String, String>> {
        val attributes = mutableMapOf<String, Map<String, String>>()
        
        val elementPattern = Regex("""<(\w+)[^>]*id="([^"]+)"[^>]*>""")
        elementPattern.findAll(svgContent).forEach { match ->
            val elementId = match.groupValues[2]
            val elementContent = match.value
            
            val elementAttributes = mutableMapOf<String, String>()
            val attributePattern = Regex("""(\w+)="([^"]*)"""")
            
            attributePattern.findAll(elementContent).forEach { attrMatch ->
                val attrName = attrMatch.groupValues[1]
                val attrValue = attrMatch.groupValues[2]
                if (attrName != "id") { // Skip the id attribute itself
                    elementAttributes[attrName] = attrValue
                }
            }
            
            attributes[elementId] = elementAttributes
        }
        
        return attributes
    }
    
    private fun extractViewport(svgContent: String): SvgViewport? {
        val svgPattern = Regex("""<svg[^>]*width="([^"]*)"[^>]*height="([^"]*)"[^>]*viewBox="([^"]*)"[^>]*>""")
        val match = svgPattern.find(svgContent)
        
        return if (match != null) {
            val width = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val height = match.groupValues[2].toDoubleOrNull() ?: 0.0
            val viewBox = match.groupValues[3].split("\\s+".toRegex()).mapNotNull { it.toDoubleOrNull() }
            
            if (viewBox.size == 4) {
                SvgViewport(
                    width = width,
                    height = height,
                    viewBoxX = viewBox[0],
                    viewBoxY = viewBox[1],
                    viewBoxWidth = viewBox[2],
                    viewBoxHeight = viewBox[3]
                )
            } else null
        } else null
    }
    
    private fun extractTextElements(svgContent: String): List<TextElement> {
        val textElements = mutableListOf<TextElement>()
        
        val textPattern = Regex("""<text[^>]*id="([^"]*)"[^>]*>([^<]*)</text>""")
        textPattern.findAll(svgContent).forEach { match ->
            val elementId = match.groupValues[1]
            val textContent = match.groupValues[2].trim()
            
            // Extract text attributes
            val fullMatch = match.value
            val attributes = mutableMapOf<String, String>()
            val attrPattern = Regex("""(\w+)="([^"]*)"""")
            
            attrPattern.findAll(fullMatch).forEach { attrMatch ->
                attributes[attrMatch.groupValues[1]] = attrMatch.groupValues[2]
            }
            
            textElements.add(TextElement(
                id = elementId,
                content = textContent,
                attributes = attributes
            ))
        }
        
        return textElements
    }
    
    private fun extractPathElements(svgContent: String): List<PathElement> {
        val pathElements = mutableListOf<PathElement>()
        
        val pathPattern = Regex("""<path[^>]*id="([^"]*)"[^>]*d="([^"]*)"[^>]*/>""")
        pathPattern.findAll(svgContent).forEach { match ->
            val elementId = match.groupValues[1]
            val pathData = match.groupValues[2]
            
            // Extract path attributes
            val fullMatch = match.value
            val attributes = mutableMapOf<String, String>()
            val attrPattern = Regex("""(\w+)="([^"]*)"""")
            
            attrPattern.findAll(fullMatch).forEach { attrMatch ->
                attributes[attrMatch.groupValues[1]] = attrMatch.groupValues[2]
            }
            
            pathElements.add(PathElement(
                id = elementId,
                pathData = pathData,
                attributes = attributes
            ))
        }
        
        return pathElements
    }
    
    // Private comparison methods
    
    private fun compareElements(elements1: List<SvgElement>, elements2: List<SvgElement>): ElementComparison {
        val ids1 = elements1.map { it.id }.toSet()
        val ids2 = elements2.map { it.id }.toSet()
        
        val common = ids1.intersect(ids2)
        val missing = ids2 - ids1
        val extra = ids1 - ids2
        
        val typeMatches = mutableMapOf<String, Boolean>()
        for (id in common) {
            val type1 = elements1.find { it.id == id }?.type
            val type2 = elements2.find { it.id == id }?.type
            typeMatches[id] = type1 == type2
        }
        
        return ElementComparison(
            commonElements = common.toList(),
            missingElements = missing.toList(),
            extraElements = extra.toList(),
            typeMatches = typeMatches
        )
    }
    
    private fun compareCoordinates(
        coords1: Map<String, ElementCoordinates>,
        coords2: Map<String, ElementCoordinates>,
        tolerances: ComparisonTolerances
    ): CoordinateComparison {
        val deviations = mutableListOf<CoordinateDeviation>()
        var maxDeviation = 0.0
        var totalDeviation = 0.0
        var comparedElements = 0
        
        for ((elementId, coords2Val) in coords2) {
            val coords1Val = coords1[elementId]
            if (coords1Val != null) {
                val deviation = coords1Val.position.distanceTo(coords2Val.position)
                deviations.add(CoordinateDeviation(
                    elementId = elementId,
                    kotlinCoordinate = coords1Val.position,
                    referenceCoordinate = coords2Val.position,
                    deviation = deviation
                ))
                maxDeviation = maxOf(maxDeviation, deviation)
                totalDeviation += deviation
                comparedElements++
            }
        }
        
        val averageDeviation = if (comparedElements > 0) totalDeviation / comparedElements else 0.0
        val withinTolerance = maxDeviation <= tolerances.maxCoordinateDeviation
        
        return CoordinateComparison(
            deviations = deviations,
            maxDeviation = maxDeviation,
            averageDeviation = averageDeviation,
            withinTolerance = withinTolerance
        )
    }
    
    private fun compareAttributes(
        attrs1: Map<String, Map<String, String>>,
        attrs2: Map<String, Map<String, String>>,
        tolerances: ComparisonTolerances
    ): AttributeComparison {
        val matches = mutableListOf<AttributeMatch>()
        var totalAttributes = 0
        var matchingAttributes = 0
        
        for ((elementId, elementAttrs2) in attrs2) {
            val elementAttrs1 = attrs1[elementId] ?: emptyMap()
            
            for ((attrName, value2) in elementAttrs2) {
                totalAttributes++
                val value1 = elementAttrs1[attrName]
                
                val isMatch = when {
                    value1 == null -> false
                    attrName in tolerances.numericAttributes -> {
                        val num1 = value1.toDoubleOrNull()
                        val num2 = value2.toDoubleOrNull()
                        if (num1 != null && num2 != null) {
                            kotlin.math.abs(num1 - num2) <= tolerances.numericTolerance
                        } else {
                            value1 == value2
                        }
                    }
                    else -> value1 == value2
                }
                
                if (isMatch) matchingAttributes++
                
                matches.add(AttributeMatch(
                    elementId = elementId,
                    attributeName = attrName,
                    kotlinValue = value1,
                    referenceValue = value2,
                    matches = isMatch
                ))
            }
        }
        
        val matchPercentage = if (totalAttributes > 0) matchingAttributes.toDouble() / totalAttributes else 1.0
        
        return AttributeComparison(
            matches = matches,
            totalAttributes = totalAttributes,
            matchingAttributes = matchingAttributes,
            matchPercentage = matchPercentage
        )
    }
    
    private fun compareTextElements(text1: List<TextElement>, text2: List<TextElement>): TextComparison {
        val matches = mutableListOf<TextMatch>()
        
        for (text2Element in text2) {
            val text1Element = text1.find { it.id == text2Element.id }
            val contentMatches = text1Element?.content == text2Element.content
            
            matches.add(TextMatch(
                elementId = text2Element.id,
                kotlinContent = text1Element?.content,
                referenceContent = text2Element.content,
                contentMatches = contentMatches
            ))
        }
        
        val totalTexts = text2.size
        val matchingTexts = matches.count { it.contentMatches }
        val matchPercentage = if (totalTexts > 0) matchingTexts.toDouble() / totalTexts else 1.0
        
        return TextComparison(
            matches = matches,
            totalTexts = totalTexts,
            matchingTexts = matchingTexts,
            matchPercentage = matchPercentage
        )
    }
    
    private fun comparePathElements(
        paths1: List<PathElement>,
        paths2: List<PathElement>,
        tolerances: ComparisonTolerances
    ): PathComparison {
        val matches = mutableListOf<PathMatch>()
        
        for (path2Element in paths2) {
            val path1Element = paths1.find { it.id == path2Element.id }
            val pathMatches = path1Element?.pathData == path2Element.pathData
            
            matches.add(PathMatch(
                elementId = path2Element.id,
                kotlinPathData = path1Element?.pathData,
                referencePathData = path2Element.pathData,
                pathMatches = pathMatches
            ))
        }
        
        val totalPaths = paths2.size
        val matchingPaths = matches.count { it.pathMatches }
        val matchPercentage = if (totalPaths > 0) matchingPaths.toDouble() / totalPaths else 1.0
        
        return PathComparison(
            matches = matches,
            totalPaths = totalPaths,
            matchingPaths = matchingPaths,
            matchPercentage = matchPercentage
        )
    }
    
    private fun compareViewports(viewport1: SvgViewport?, viewport2: SvgViewport?, tolerances: ComparisonTolerances): ViewportComparison {
        return when {
            viewport1 == null && viewport2 == null -> ViewportComparison(true, null)
            viewport1 == null || viewport2 == null -> ViewportComparison(false, "One viewport is missing")
            else -> {
                val widthMatch = kotlin.math.abs(viewport1.width - viewport2.width) <= tolerances.numericTolerance
                val heightMatch = kotlin.math.abs(viewport1.height - viewport2.height) <= tolerances.numericTolerance
                val viewBoxMatch = kotlin.math.abs(viewport1.viewBoxX - viewport2.viewBoxX) <= tolerances.numericTolerance &&
                                  kotlin.math.abs(viewport1.viewBoxY - viewport2.viewBoxY) <= tolerances.numericTolerance &&
                                  kotlin.math.abs(viewport1.viewBoxWidth - viewport2.viewBoxWidth) <= tolerances.numericTolerance &&
                                  kotlin.math.abs(viewport1.viewBoxHeight - viewport2.viewBoxHeight) <= tolerances.numericTolerance
                
                val matches = widthMatch && heightMatch && viewBoxMatch
                val message = if (!matches) "Viewport dimensions or viewBox do not match within tolerance" else null
                
                ViewportComparison(matches, message)
            }
        }
    }
}

// Data classes for parsed SVG structure

data class ParsedSvg(
    val elements: List<SvgElement>,
    val coordinates: Map<String, ElementCoordinates>,
    val attributes: Map<String, Map<String, String>>,
    val viewport: SvgViewport?,
    val textElements: List<TextElement>,
    val pathElements: List<PathElement>
)

data class SvgElement(
    val type: String,
    val id: String,
    val content: String
)

data class ElementCoordinates(
    val position: Point,
    val type: CoordinateType
)

enum class CoordinateType {
    TRANSFORM, POSITION, CENTER
}

data class SvgViewport(
    val width: Double,
    val height: Double,
    val viewBoxX: Double,
    val viewBoxY: Double,
    val viewBoxWidth: Double,
    val viewBoxHeight: Double
)

data class TextElement(
    val id: String,
    val content: String,
    val attributes: Map<String, String>
)

data class PathElement(
    val id: String,
    val pathData: String,
    val attributes: Map<String, String>
)

// Data classes for detailed comparison

data class DetailedComparison(
    val elements: ElementComparison,
    val coordinates: CoordinateComparison,
    val attributes: AttributeComparison,
    val text: TextComparison,
    val paths: PathComparison,
    val viewport: ViewportComparison
)

data class ElementComparison(
    val commonElements: List<String>,
    val missingElements: List<String>,
    val extraElements: List<String>,
    val typeMatches: Map<String, Boolean>
)

data class CoordinateComparison(
    val deviations: List<CoordinateDeviation>,
    val maxDeviation: Double,
    val averageDeviation: Double,
    val withinTolerance: Boolean
)

data class AttributeComparison(
    val matches: List<AttributeMatch>,
    val totalAttributes: Int,
    val matchingAttributes: Int,
    val matchPercentage: Double
)

data class TextComparison(
    val matches: List<TextMatch>,
    val totalTexts: Int,
    val matchingTexts: Int,
    val matchPercentage: Double
)

data class TextMatch(
    val elementId: String,
    val kotlinContent: String?,
    val referenceContent: String,
    val contentMatches: Boolean
)

data class PathComparison(
    val matches: List<PathMatch>,
    val totalPaths: Int,
    val matchingPaths: Int,
    val matchPercentage: Double
)

data class PathMatch(
    val elementId: String,
    val kotlinPathData: String?,
    val referencePathData: String,
    val pathMatches: Boolean
)

data class ViewportComparison(
    val matches: Boolean,
    val message: String?
)