package org.graphviz.kotlin.examples

import org.graphviz.kotlin.builder.*
import org.graphviz.kotlin.model.Graph

/**
 * Minimal example of the affine transform used in an MLP: y = w·x + b
 *
 * This builds a tiny computation graph with nodes for input x, weight w,
 * bias b, and output y. Edges are labeled to indicate the operations.
 */
object MlpLinearExample {

    fun createWxPlusBGraph(): Graph = digraph("wx_plus_b") {
        // Layout left-to-right for readability
        attribute("rankdir", "LR")

        // Nodes
        node("x") { label("x (input)") }
        node("w") { label("w (weight)") }
        node("b") { label("b (bias)") }
        node("y") { label("y (output)\n y = w*x + b") }

        // Intermediate (implicit) multiply and add shown via labeled edges to y
        // Edge x -> y labeled with the weight multiplier w
        edge("x", "y") { label("* w") }

        // Edge b -> y labeled with + b to indicate bias addition
        edge("b", "y") { label("+ b") }

        // Optional styling
        attribute("labelloc", "t")
        attribute("label", "MLP linear unit: y = w·x + b")
    }
}
