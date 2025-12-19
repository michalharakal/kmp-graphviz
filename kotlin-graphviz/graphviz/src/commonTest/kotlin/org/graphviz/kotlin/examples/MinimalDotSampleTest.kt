package org.graphviz.kotlin.examples

import kotlin.test.Test
import kotlin.test.assertEquals
import org.graphviz.kotlin.builder.GraphBuilder
import org.graphviz.kotlin.generator.DotGenerator

/**
 * Minimal DOT sample: a directed graph G with a single edge A -> B.
 */
class MinimalDotSampleTest {

    @Test
    fun minimalDotGraph() {
        val graph = GraphBuilder(id = "G", isDirected = true)
            .apply {
                edge("A", "B")
            }
            .build()

        val dot = DotGenerator.generate(graph)

        val expected = buildString {
            append("digraph G {\n")
            append("  A;\n")
            append("  B;\n")
            append("  A -> B;\n")
            append("}\n")
        }

        assertEquals(expected, dot)
    }
}
