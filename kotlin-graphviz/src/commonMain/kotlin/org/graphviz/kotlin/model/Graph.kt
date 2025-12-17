package org.graphviz.kotlin.model

/**
 * Immutable representation of a graph with nodes, edges, and attributes.
 */
interface Graph {
    val id: String
    val isDirected: Boolean
    val nodes: Set<Node>
    val edges: Set<Edge>
    val subgraphs: Set<Graph>
    val attributes: AttributeMap
    
    /**
     * Get a node by its ID.
     */
    fun getNode(id: String): Node?
    
    /**
     * Get all edges connected to a specific node.
     */
    fun getEdges(node: Node): Set<Edge>
    
    /**
     * Get all edges between two nodes.
     */
    fun getEdges(source: Node, target: Node): Set<Edge>
    
    /**
     * Check if the graph contains a specific node.
     */
    fun containsNode(node: Node): Boolean
    
    /**
     * Check if the graph contains a specific edge.
     */
    fun containsEdge(edge: Edge): Boolean
    
    /**
     * Get all nodes in the graph including those in subgraphs.
     */
    fun getAllNodes(): Set<Node>
    
    /**
     * Get all edges in the graph including those in subgraphs.
     */
    fun getAllEdges(): Set<Edge>
    
    /**
     * Calculate the bounding box of all positioned nodes.
     */
    fun getBoundingBox(): BoundingBox?
}

/**
 * Immutable representation of a graph node.
 */
interface Node {
    val id: String
    val attributes: AttributeMap
    val position: Point?
    
    /**
     * Create a copy of this node with a new position.
     */
    fun withPosition(position: Point): Node
    
    /**
     * Create a copy of this node with updated attributes.
     */
    fun withAttributes(attributes: AttributeMap): Node
    
    /**
     * Create a copy of this node with an additional attribute.
     */
    fun <T> withAttribute(key: AttributeKey<T>, value: T): Node
}

/**
 * Immutable representation of a graph edge.
 */
interface Edge {
    val source: Node
    val target: Node
    val attributes: AttributeMap
    val controlPoints: List<Point>
    
    /**
     * Create a copy of this edge with new control points.
     */
    fun withControlPoints(controlPoints: List<Point>): Edge
    
    /**
     * Create a copy of this edge with updated attributes.
     */
    fun withAttributes(attributes: AttributeMap): Edge
    
    /**
     * Create a copy of this edge with an additional attribute.
     */
    fun <T> withAttribute(key: AttributeKey<T>, value: T): Edge
    
    /**
     * Check if this edge connects the same nodes as another edge.
     */
    fun connectsSameNodes(other: Edge): Boolean
}

/**
 * Default implementation of the Graph interface.
 */
data class GraphImpl(
    override val id: String,
    override val isDirected: Boolean,
    override val nodes: Set<Node>,
    override val edges: Set<Edge>,
    override val subgraphs: Set<Graph>,
    override val attributes: AttributeMap
) : Graph {
    
    init {
        // Validate that all edges reference nodes that exist in this graph
        val allNodes = getAllNodes()
        for (edge in getAllEdges()) {
            require(allNodes.contains(edge.source)) {
                "Edge source node '${edge.source.id}' not found in graph '$id'"
            }
            require(allNodes.contains(edge.target)) {
                "Edge target node '${edge.target.id}' not found in graph '$id'"
            }
        }
    }
    
    private val nodeMap: Map<String, Node> by lazy {
        nodes.associateBy { it.id }
    }
    
    override fun getNode(id: String): Node? = nodeMap[id]
    
    override fun getEdges(node: Node): Set<Edge> {
        return edges.filter { it.source == node || it.target == node }.toSet()
    }
    
    override fun getEdges(source: Node, target: Node): Set<Edge> {
        return edges.filter { 
            (it.source == source && it.target == target) ||
            (!isDirected && it.source == target && it.target == source)
        }.toSet()
    }
    
    override fun containsNode(node: Node): Boolean = nodes.contains(node)
    
    override fun containsEdge(edge: Edge): Boolean = edges.contains(edge)
    
    override fun getAllNodes(): Set<Node> {
        return nodes + subgraphs.flatMap { it.getAllNodes() }
    }
    
    override fun getAllEdges(): Set<Edge> {
        return edges + subgraphs.flatMap { it.getAllEdges() }
    }
    
    override fun getBoundingBox(): BoundingBox? {
        val positionedNodes = getAllNodes().mapNotNull { it.position }
        return if (positionedNodes.isNotEmpty()) {
            BoundingBox.fromPoints(positionedNodes)
        } else {
            null
        }
    }
}

/**
 * Default implementation of the Node interface.
 */
data class NodeImpl(
    override val id: String,
    override val attributes: AttributeMap,
    override val position: Point?
) : Node {
    
    init {
        require(id.isNotBlank()) { "Node ID cannot be blank" }
    }
    
    override fun withPosition(position: Point): Node {
        return copy(position = position)
    }
    
    override fun withAttributes(attributes: AttributeMap): Node {
        return copy(attributes = attributes)
    }
    
    override fun <T> withAttribute(key: AttributeKey<T>, value: T): Node {
        return copy(attributes = attributes.set(key, value))
    }
}

/**
 * Default implementation of the Edge interface.
 */
data class EdgeImpl(
    override val source: Node,
    override val target: Node,
    override val attributes: AttributeMap,
    override val controlPoints: List<Point>
) : Edge {
    
    override fun withControlPoints(controlPoints: List<Point>): Edge {
        return copy(controlPoints = controlPoints)
    }
    
    override fun withAttributes(attributes: AttributeMap): Edge {
        return copy(attributes = attributes)
    }
    
    override fun <T> withAttribute(key: AttributeKey<T>, value: T): Edge {
        return copy(attributes = attributes.set(key, value))
    }
    
    override fun connectsSameNodes(other: Edge): Boolean {
        return (source == other.source && target == other.target) ||
               (source == other.target && target == other.source)
    }
}