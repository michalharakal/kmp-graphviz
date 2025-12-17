package org.graphviz.kotlin.generator

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.graphviz.kotlin.model.*

class DotGeneratorTest : FunSpec({
    
    test("generate simple directed graph") {
        val graph = GraphImpl(
            id = "G",
            isDirected = true,
            nodes = setOf(
                NodeImpl("A", AttributeMap.empty(), null),
                NodeImpl("B", AttributeMap.empty(), null)
            ),
            edges = setOf(
                EdgeImpl(
                    NodeImpl("A", AttributeMap.empty(), null),
                    NodeImpl("B", AttributeMap.empty(), null),
                    AttributeMap.empty(),
                    emptyList()
                )
            ),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val output = DotGenerator.generate(graph)
        
        output shouldContain "digraph G {"
        output shouldContain "A;"
        output shouldContain "B;"
        output shouldContain "A -> B;"
        output shouldContain "}"
    }
    
    test("generate simple undirected graph") {
        val graph = GraphImpl(
            id = "G",
            isDirected = false,
            nodes = setOf(
                NodeImpl("A", AttributeMap.empty(), null),
                NodeImpl("B", AttributeMap.empty(), null)
            ),
            edges = setOf(
                EdgeImpl(
                    NodeImpl("A", AttributeMap.empty(), null),
                    NodeImpl("B", AttributeMap.empty(), null),
                    AttributeMap.empty(),
                    emptyList()
                )
            ),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val output = DotGenerator.generate(graph)
        
        output shouldContain "graph G {"
        output shouldContain "A -- B;"
    }
    
    test("generate graph with node attributes") {
        val nodeAttributes = AttributeMap.builder()
            .setString("label", "Node A")
            .setString("shape", "box")
            .build()
        
        val graph = GraphImpl(
            id = "G",
            isDirected = true,
            nodes = setOf(
                NodeImpl("A", nodeAttributes, null)
            ),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val output = DotGenerator.generate(graph)
        
        output shouldContain "A ["
        output shouldContain "label=\"Node A\""
        output shouldContain "shape=\"box\""
        output shouldContain "];"
    }
    
    test("generate graph with edge attributes") {
        val nodeA = NodeImpl("A", AttributeMap.empty(), null)
        val nodeB = NodeImpl("B", AttributeMap.empty(), null)
        
        val edgeAttributes = AttributeMap.builder()
            .setString("label", "edge label")
            .setString("color", "red")
            .build()
        
        val graph = GraphImpl(
            id = "G",
            isDirected = true,
            nodes = setOf(nodeA, nodeB),
            edges = setOf(
                EdgeImpl(nodeA, nodeB, edgeAttributes, emptyList())
            ),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val output = DotGenerator.generate(graph)
        
        output shouldContain "A -> B ["
        output shouldContain "color=\"red\""
        output shouldContain "label=\"edge label\""
        output shouldContain "];"
    }
    
    test("generate graph with graph attributes") {
        val graphAttributes = AttributeMap.builder()
            .setString("rankdir", "LR")
            .setString("bgcolor", "white")
            .build()
        
        val graph = GraphImpl(
            id = "G",
            isDirected = true,
            nodes = emptySet(),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = graphAttributes
        )
        
        val output = DotGenerator.generate(graph)
        
        output shouldContain "bgcolor=\"white\";"
        output shouldContain "rankdir=\"LR\";"
    }
    
    test("escape special characters in identifiers") {
        val graph = GraphImpl(
            id = "My Graph",
            isDirected = true,
            nodes = setOf(
                NodeImpl("node with spaces", AttributeMap.empty(), null)
            ),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val output = DotGenerator.generate(graph)
        
        output shouldContain "digraph \"My Graph\" {"
        output shouldContain "\"node with spaces\";"
    }
    
    test("escape special characters in string values") {
        val nodeAttributes = AttributeMap.builder()
            .setString("label", "Line 1\\nLine 2")
            .build()
        
        val graph = GraphImpl(
            id = "G",
            isDirected = true,
            nodes = setOf(
                NodeImpl("A", nodeAttributes, null)
            ),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val output = DotGenerator.generate(graph)
        
        output shouldContain "label=\"Line 1\\\\nLine 2\""
    }
    
    test("generate graph with subgraph") {
        val subgraphNode = NodeImpl("S1", AttributeMap.empty(), null)
        val subgraph = GraphImpl(
            id = "cluster_0",
            isDirected = true,
            nodes = setOf(subgraphNode),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.builder()
                .setString("label", "Subgraph")
                .build()
        )
        
        val graph = GraphImpl(
            id = "G",
            isDirected = true,
            nodes = setOf(
                NodeImpl("A", AttributeMap.empty(), null)
            ),
            edges = emptySet(),
            subgraphs = setOf(subgraph),
            attributes = AttributeMap.empty()
        )
        
        val output = DotGenerator.generate(graph)
        
        output shouldContain "digraph G {"
        output shouldContain "subgraph cluster_0 {"
        output shouldContain "label=\"Subgraph\";"
        output shouldContain "S1;"
    }
    
    test("handle numeric identifiers") {
        val graph = GraphImpl(
            id = "G",
            isDirected = true,
            nodes = setOf(
                NodeImpl("123", AttributeMap.empty(), null)
            ),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val output = DotGenerator.generate(graph)
        
        // Numeric identifiers should be quoted
        output shouldContain "\"123\";"
    }
    
    test("handle keywords as identifiers") {
        val graph = GraphImpl(
            id = "G",
            isDirected = true,
            nodes = setOf(
                NodeImpl("node", AttributeMap.empty(), null),
                NodeImpl("graph", AttributeMap.empty(), null)
            ),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val output = DotGenerator.generate(graph)
        
        // Keywords should be quoted
        output shouldContain "\"node\";"
        output shouldContain "\"graph\";"
    }
    
    test("format number attributes correctly") {
        val nodeAttributes = AttributeMap.builder()
            .set(AttributeKey.WIDTH, 2.5)
            .set(AttributeKey.HEIGHT, 3.0)
            .build()
        
        val graph = GraphImpl(
            id = "G",
            isDirected = true,
            nodes = setOf(
                NodeImpl("A", nodeAttributes, null)
            ),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val output = DotGenerator.generate(graph)
        
        output shouldContain "width=2.5"
        output shouldContain "height=3" // Should not have .0
    }
    
    test("generate empty graph") {
        val graph = GraphImpl(
            id = "Empty",
            isDirected = true,
            nodes = emptySet(),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val output = DotGenerator.generate(graph)
        
        output shouldContain "digraph Empty {"
        output shouldContain "}"
        output shouldNotContain ";"
    }
    
    test("use custom indentation") {
        val options = DotGeneratorOptions(indentString = "    ")
        
        val graph = GraphImpl(
            id = "G",
            isDirected = true,
            nodes = setOf(
                NodeImpl("A", AttributeMap.empty(), null)
            ),
            edges = emptySet(),
            subgraphs = emptySet(),
            attributes = AttributeMap.empty()
        )
        
        val output = DotGenerator(options).generate(graph)
        
        output shouldContain "    A;"
    }
})
