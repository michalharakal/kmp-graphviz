package org.graphviz.kotlin.builder

import org.graphviz.kotlin.model.*

/**
 * Mutable builder for constructing graphs programmatically.
 * Provides validation and ensures graph consistency.
 */
class GraphBuilder(
    private val id: String,
    private val isDirected: Boolean = true
) {
    private val nodes = mutableMapOf<String, NodeBuilder>()
    private val edges = mutableListOf<EdgeBuilder>()
    private val subgraphs = mutableListOf<GraphBuilder>()
    private val attributes = AttributeMap.builder()
    
    /**
     * Add a node to the graph.
     */
    fun node(id: String, configure: NodeBuilder.() -> Unit = {}): NodeBuilder {
        val nodeBuilder = nodes.getOrPut(id) { NodeBuilder(id) }
        nodeBuilder.configure()
        return nodeBuilder
    }
    
    /**
     * Add an edge between two nodes.
     */
    fun edge(sourceId: String, targetId: String, configure: EdgeBuilder.() -> Unit = {}): EdgeBuilder {
        // Ensure both nodes exist
        node(sourceId)
        node(targetId)
        
        val edgeBuilder = EdgeBuilder(sourceId, targetId)
        edgeBuilder.configure()
        edges.add(edgeBuilder)
        return edgeBuilder
    }
    
    /**
     * Add an edge between existing node builders.
     */
    fun edge(source: NodeBuilder, target: NodeBuilder, configure: EdgeBuilder.() -> Unit = {}): EdgeBuilder {
        return edge(source.id, target.id, configure)
    }
    
    /**
     * Add a subgraph to this graph.
     */
    fun subgraph(id: String, configure: GraphBuilder.() -> Unit = {}): GraphBuilder {
        val subgraphBuilder = GraphBuilder(id, isDirected)
        subgraphBuilder.configure()
        subgraphs.add(subgraphBuilder)
        return subgraphBuilder
    }
    
    /**
     * Set a graph attribute.
     */
    fun <T> attribute(key: AttributeKey<T>, value: T): GraphBuilder {
        attributes.set(key, value)
        return this
    }
    
    /**
     * Set a raw string attribute.
     */
    fun attribute(name: String, value: String): GraphBuilder {
        attributes.setString(name, value)
        return this
    }
    
    /**
     * Set multiple attributes from a map.
     */
    fun attributes(attrs: Map<String, String>): GraphBuilder {
        attrs.forEach { (name, value) ->
            attributes.setString(name, value)
        }
        return this
    }
    
    /**
     * Validate the current graph structure.
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate graph attributes
        val attrValidation = attributes.build().validate()
        if (!attrValidation.isValid) {
            errors.addAll((attrValidation as ValidationResult.Invalid).errors)
        }
        
        // Validate nodes
        for (nodeBuilder in nodes.values) {
            val nodeValidation = nodeBuilder.validate()
            if (!nodeValidation.isValid) {
                errors.addAll((nodeValidation as ValidationResult.Invalid).errors)
            }
        }
        
        // Validate edges
        for (edgeBuilder in edges) {
            val edgeValidation = edgeBuilder.validate()
            if (!edgeValidation.isValid) {
                errors.addAll((edgeValidation as ValidationResult.Invalid).errors)
            }
            
            // Check that edge endpoints exist
            if (!nodes.containsKey(edgeBuilder.sourceId)) {
                errors.add("Edge references non-existent source node: ${edgeBuilder.sourceId}")
            }
            if (!nodes.containsKey(edgeBuilder.targetId)) {
                errors.add("Edge references non-existent target node: ${edgeBuilder.targetId}")
            }
        }
        
        // Validate subgraphs
        for (subgraphBuilder in subgraphs) {
            val subgraphValidation = subgraphBuilder.validate()
            if (!subgraphValidation.isValid) {
                errors.addAll((subgraphValidation as ValidationResult.Invalid).errors)
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Build the immutable graph.
     * Throws an exception if validation fails.
     */
    fun build(): Graph {
        val validation = validate()
        if (!validation.isValid) {
            val errors = (validation as ValidationResult.Invalid).errors
            throw IllegalStateException("Graph validation failed: ${errors.joinToString(", ")}")
        }
        
        val builtNodes = nodes.values.map { it.build() }.toSet()
        val nodeMap = builtNodes.associateBy { it.id }
        
        val builtEdges = edges.map { edgeBuilder ->
            val source = nodeMap[edgeBuilder.sourceId]!!
            val target = nodeMap[edgeBuilder.targetId]!!
            edgeBuilder.build(source, target)
        }.toSet()
        
        val builtSubgraphs = subgraphs.map { it.build() }.toSet()
        
        return GraphImpl(
            id = id,
            isDirected = isDirected,
            nodes = builtNodes,
            edges = builtEdges,
            subgraphs = builtSubgraphs,
            attributes = attributes.build()
        )
    }
    
    /**
     * Get the current node count.
     */
    val nodeCount: Int get() = nodes.size
    
    /**
     * Get the current edge count.
     */
    val edgeCount: Int get() = edges.size
    
    /**
     * Get the current subgraph count.
     */
    val subgraphCount: Int get() = subgraphs.size
    
    /**
     * Check if a node with the given ID exists.
     */
    fun hasNode(id: String): Boolean = nodes.containsKey(id)
    
    /**
     * Get all node IDs.
     */
    val nodeIds: Set<String> get() = nodes.keys.toSet()
}

/**
 * Mutable builder for constructing nodes.
 */
class NodeBuilder(val id: String) {
    private val attributes = AttributeMap.builder()
    private var position: Point? = null
    
    /**
     * Set the node position.
     */
    fun position(x: Double, y: Double): NodeBuilder {
        this.position = Point(x, y)
        return this
    }
    
    /**
     * Set the node position.
     */
    fun position(point: Point): NodeBuilder {
        this.position = point
        return this
    }
    
    /**
     * Set a node attribute.
     */
    fun <T> attribute(key: AttributeKey<T>, value: T): NodeBuilder {
        attributes.set(key, value)
        return this
    }
    
    /**
     * Set a raw string attribute.
     */
    fun attribute(name: String, value: String): NodeBuilder {
        attributes.setString(name, value)
        return this
    }
    
    /**
     * Set multiple attributes from a map.
     */
    fun attributes(attrs: Map<String, String>): NodeBuilder {
        attrs.forEach { (name, value) ->
            attributes.setString(name, value)
        }
        return this
    }
    
    /**
     * Set common node attributes with convenience methods.
     */
    fun label(label: String): NodeBuilder = attribute(AttributeKey.LABEL, label)
    fun shape(shape: String): NodeBuilder = attribute(AttributeKey.SHAPE, shape)
    fun color(color: Color): NodeBuilder = attribute(AttributeKey.COLOR, color)
    fun fillColor(color: Color): NodeBuilder = attribute(AttributeKey.FILLCOLOR, color)
    fun style(style: String): NodeBuilder = attribute(AttributeKey.STYLE, style)
    
    /**
     * Validate the node configuration.
     */
    fun validate(): ValidationResult {
        val attrValidation = attributes.build().validate()
        return if (attrValidation.isValid) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                (attrValidation as ValidationResult.Invalid).errors.map { 
                    "Node '$id': $it" 
                }
            )
        }
    }
    
    /**
     * Build the immutable node.
     */
    fun build(): Node {
        return NodeImpl(
            id = id,
            attributes = attributes.build(),
            position = position
        )
    }
}

/**
 * Mutable builder for constructing edges.
 */
class EdgeBuilder(val sourceId: String, val targetId: String) {
    private val attributes = AttributeMap.builder()
    private val controlPoints = mutableListOf<Point>()
    
    /**
     * Add a control point for edge routing.
     */
    fun controlPoint(x: Double, y: Double): EdgeBuilder {
        controlPoints.add(Point(x, y))
        return this
    }
    
    /**
     * Add a control point for edge routing.
     */
    fun controlPoint(point: Point): EdgeBuilder {
        controlPoints.add(point)
        return this
    }
    
    /**
     * Set all control points at once.
     */
    fun controlPoints(points: List<Point>): EdgeBuilder {
        controlPoints.clear()
        controlPoints.addAll(points)
        return this
    }
    
    /**
     * Set an edge attribute.
     */
    fun <T> attribute(key: AttributeKey<T>, value: T): EdgeBuilder {
        attributes.set(key, value)
        return this
    }
    
    /**
     * Set a raw string attribute.
     */
    fun attribute(name: String, value: String): EdgeBuilder {
        attributes.setString(name, value)
        return this
    }
    
    /**
     * Set multiple attributes from a map.
     */
    fun attributes(attrs: Map<String, String>): EdgeBuilder {
        attrs.forEach { (name, value) ->
            attributes.setString(name, value)
        }
        return this
    }
    
    /**
     * Set common edge attributes with convenience methods.
     */
    fun label(label: String): EdgeBuilder = attribute(AttributeKey.LABEL, label)
    fun color(color: Color): EdgeBuilder = attribute(AttributeKey.COLOR, color)
    fun style(style: String): EdgeBuilder = attribute(AttributeKey.STYLE, style)
    fun penWidth(width: Double): EdgeBuilder = attribute(AttributeKey.PENWIDTH, width)
    
    /**
     * Validate the edge configuration.
     */
    fun validate(): ValidationResult {
        val attrValidation = attributes.build().validate()
        return if (attrValidation.isValid) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                (attrValidation as ValidationResult.Invalid).errors.map { 
                    "Edge '$sourceId' -> '$targetId': $it" 
                }
            )
        }
    }
    
    /**
     * Build the immutable edge.
     */
    fun build(source: Node, target: Node): Edge {
        return EdgeImpl(
            source = source,
            target = target,
            attributes = attributes.build(),
            controlPoints = controlPoints.toList()
        )
    }
}

/**
 * DSL function for creating graphs.
 */
fun graph(id: String, directed: Boolean = true, configure: GraphBuilder.() -> Unit): Graph {
    val builder = GraphBuilder(id, directed)
    builder.configure()
    return builder.build()
}

/**
 * DSL function for creating directed graphs.
 */
fun digraph(id: String, configure: GraphBuilder.() -> Unit): Graph {
    return graph(id, directed = true, configure)
}

/**
 * DSL function for creating undirected graphs.
 */
fun undirectedGraph(id: String, configure: GraphBuilder.() -> Unit): Graph {
    return graph(id, directed = false, configure)
}