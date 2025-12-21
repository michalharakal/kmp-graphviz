package org.graphviz.kotlin.visual

import org.graphviz.kotlin.model.*

/**
 * Generates comprehensive test cases for visual regression testing.
 * Covers all node shapes, edge styles, arrowhead types, and complex layouts.
 */
object TestCaseGenerator {
    
    /**
     * Generates test cases covering all node shapes.
     */
    fun generateNodeShapeTests(): List<TestCase> {
        val nodeShapes = listOf(
            // Basic shapes
            "box", "circle", "ellipse", "oval", "point", "egg", "triangle", "plaintext",
            "plain", "diamond", "trapezium", "parallelogram", "house", "pentagon",
            "hexagon", "septagon", "octagon", "doublecircle", "doubleoctagon",
            "tripleoctagon", "invtriangle", "invtrapezium", "invhouse", "Mdiamond",
            "Msquare", "Mcircle", "rect", "rectangle", "square", "star", "none",
            "underline", "cylinder", "note", "tab", "folder", "box3d", "component",
            
            // Biological shapes
            "promoter", "cds", "terminator", "utr", "primersite", "restrictionsite",
            "fivepoverhang", "threepoverhang", "noverhang", "assembly", "signature",
            "insulator", "ribosite", "rnastab", "proteasesite", "proteinstab",
            "rpromoter", "rarrow", "larrow", "lpromoter",
            
            // Record shapes
            "record", "Mrecord",
            
            // Additional shapes for comprehensive coverage
            "polygon", "septagon", "octagon", "doublecircle", "doubleoctagon", "tripleoctagon"
        )
        
        return nodeShapes.map { shape ->
            TestCase(
                name = "node_shape_$shape",
                description = "Test node shape: $shape",
                dotContent = generateNodeShapeDot(shape),
                category = TestCategory.NODE_SHAPES,
                expectedElements = listOf("node1"),
                tags = setOf("shape", shape)
            )
        }
    }
    
    /**
     * Generates test cases covering all edge styles.
     */
    fun generateEdgeStyleTests(): List<TestCase> {
        val edgeStyles = listOf(
            "solid", "dashed", "dotted", "bold", "invis"
        )
        
        val edgeColors = listOf(
            "black", "red", "blue", "green", "yellow", "purple", "orange", "gray"
        )
        
        val basicStyleTests = edgeStyles.map { style ->
            TestCase(
                name = "edge_style_$style",
                description = "Test edge style: $style",
                dotContent = generateEdgeStyleDot(style),
                category = TestCategory.EDGE_STYLES,
                expectedElements = listOf("node1", "node2", "edge1-2"),
                tags = setOf("style", style)
            )
        }
        
        val colorTests = edgeColors.map { color ->
            TestCase(
                name = "edge_color_$color",
                description = "Test edge color: $color",
                dotContent = generateEdgeColorDot(color),
                category = TestCategory.EDGE_STYLES,
                expectedElements = listOf("node1", "node2", "edge1-2"),
                tags = setOf("color", color)
            )
        }
        
        val penwidthTests = listOf(1.0, 2.0, 3.0, 5.0, 10.0).map { width ->
            TestCase(
                name = "edge_penwidth_${width.toInt()}",
                description = "Test edge penwidth: $width",
                dotContent = generateEdgePenwidthDot(width),
                category = TestCategory.EDGE_STYLES,
                expectedElements = listOf("node1", "node2", "edge1-2"),
                tags = setOf("penwidth", width.toString())
            )
        }
        
        return basicStyleTests + colorTests + penwidthTests
    }
    
    /**
     * Generates test cases covering all arrowhead types.
     */
    fun generateArrowheadTests(): List<TestCase> {
        val arrowheads = listOf(
            "normal", "inv", "dot", "invdot", "odot", "invodot", "none", "tee",
            "empty", "invempty", "diamond", "odiamond", "ediamond", "crow", "box",
            "obox", "open", "halfopen", "vee"
        )
        
        val arrowSizes = listOf(0.5, 1.0, 1.5, 2.0)
        
        val basicArrowheadTests = arrowheads.flatMap { arrowhead ->
            listOf(
                TestCase(
                    name = "arrowhead_$arrowhead",
                    description = "Test arrowhead: $arrowhead",
                    dotContent = generateArrowheadDot(arrowhead, "arrowhead"),
                    category = TestCategory.ARROWHEADS,
                    expectedElements = listOf("node1", "node2", "edge1-2"),
                    tags = setOf("arrowhead", arrowhead)
                ),
                TestCase(
                    name = "arrowtail_$arrowhead",
                    description = "Test arrowtail: $arrowhead",
                    dotContent = generateArrowheadDot(arrowhead, "arrowtail"),
                    category = TestCategory.ARROWHEADS,
                    expectedElements = listOf("node1", "node2", "edge1-2"),
                    tags = setOf("arrowtail", arrowhead)
                )
            )
        }
        
        val arrowSizeTests = arrowSizes.map { size ->
            TestCase(
                name = "arrowsize_${size.toString().replace(".", "_")}",
                description = "Test arrowsize: $size",
                dotContent = generateArrowSizeDot(size),
                category = TestCategory.ARROWHEADS,
                expectedElements = listOf("node1", "node2", "edge1-2"),
                tags = setOf("arrowsize", size.toString())
            )
        }
        
        val combinedArrowTests = listOf(
            TestCase(
                name = "combined_arrows_both_ends",
                description = "Test arrows on both ends",
                dotContent = generateCombinedArrowDot(),
                category = TestCategory.ARROWHEADS,
                expectedElements = listOf("node1", "node2", "edge1-2"),
                tags = setOf("combined", "both")
            )
        )
        
        return basicArrowheadTests + arrowSizeTests + combinedArrowTests
    }
    
    /**
     * Generates test cases for text rendering variations.
     */
    fun generateTextRenderingTests(): List<TestCase> {
        val basicTextTests = listOf(
            TestCase(
                name = "text_simple_labels",
                description = "Simple text labels",
                dotContent = generateTextLabelDot("Simple Label"),
                category = TestCategory.TEXT_RENDERING,
                expectedElements = listOf("node1"),
                tags = setOf("text", "simple")
            ),
            TestCase(
                name = "text_multiline_labels",
                description = "Multi-line text labels",
                dotContent = generateTextLabelDot("Line 1\\nLine 2\\nLine 3"),
                category = TestCategory.TEXT_RENDERING,
                expectedElements = listOf("node1"),
                tags = setOf("text", "multiline")
            ),
            TestCase(
                name = "text_special_characters",
                description = "Text with special characters",
                dotContent = generateTextLabelDot("Special: <>&\"'"),
                category = TestCategory.TEXT_RENDERING,
                expectedElements = listOf("node1"),
                tags = setOf("text", "special")
            ),
            TestCase(
                name = "text_html_labels",
                description = "HTML-like labels",
                dotContent = generateHtmlLabelDot(),
                category = TestCategory.TEXT_RENDERING,
                expectedElements = listOf("node1"),
                tags = setOf("text", "html")
            ),
            TestCase(
                name = "text_font_variations",
                description = "Different font variations",
                dotContent = generateFontVariationDot(),
                category = TestCategory.TEXT_RENDERING,
                expectedElements = listOf("node1", "node2", "node3"),
                tags = setOf("text", "fonts")
            )
        )
        
        val fontSizeTests = listOf(8, 10, 12, 14, 16, 18, 24, 36).map { size ->
            TestCase(
                name = "text_fontsize_$size",
                description = "Font size: $size",
                dotContent = generateFontSizeDot(size),
                category = TestCategory.TEXT_RENDERING,
                expectedElements = listOf("node1"),
                tags = setOf("text", "fontsize", size.toString())
            )
        }
        
        val fontFamilyTests = listOf("Arial", "Times", "Courier", "Helvetica").map { font ->
            TestCase(
                name = "text_fontfamily_${font.lowercase()}",
                description = "Font family: $font",
                dotContent = generateFontFamilyDot(font),
                category = TestCategory.TEXT_RENDERING,
                expectedElements = listOf("node1"),
                tags = setOf("text", "fontfamily", font.lowercase())
            )
        }
        
        val textColorTests = listOf("red", "blue", "green", "purple").map { color ->
            TestCase(
                name = "text_color_$color",
                description = "Text color: $color",
                dotContent = generateTextColorDot(color),
                category = TestCategory.TEXT_RENDERING,
                expectedElements = listOf("node1"),
                tags = setOf("text", "color", color)
            )
        }
        
        val complexTextTests = listOf(
            TestCase(
                name = "text_unicode_characters",
                description = "Unicode characters",
                dotContent = generateUnicodeLabelDot(),
                category = TestCategory.TEXT_RENDERING,
                expectedElements = listOf("node1"),
                tags = setOf("text", "unicode")
            ),
            TestCase(
                name = "text_long_labels",
                description = "Very long text labels",
                dotContent = generateLongLabelDot(),
                category = TestCategory.TEXT_RENDERING,
                expectedElements = listOf("node1"),
                tags = setOf("text", "long")
            ),
            TestCase(
                name = "text_empty_labels",
                description = "Empty labels",
                dotContent = generateEmptyLabelDot(),
                category = TestCategory.TEXT_RENDERING,
                expectedElements = listOf("node1"),
                tags = setOf("text", "empty")
            )
        )
        
        return basicTextTests + fontSizeTests + fontFamilyTests + textColorTests + complexTextTests
    }
    
    /**
     * Generates complex layout test cases with subgraphs and clusters.
     */
    fun generateComplexLayoutTests(): List<TestCase> {
        val basicLayoutTests = listOf(
            TestCase(
                name = "layout_simple_hierarchy",
                description = "Simple hierarchical layout",
                dotContent = generateSimpleHierarchyDot(),
                category = TestCategory.COMPLEX_LAYOUTS,
                expectedElements = listOf("root", "child1", "child2", "grandchild1", "grandchild2"),
                tags = setOf("layout", "hierarchy")
            ),
            TestCase(
                name = "layout_clusters",
                description = "Layout with clusters",
                dotContent = generateClusterDot(),
                category = TestCategory.COMPLEX_LAYOUTS,
                expectedElements = listOf("a", "b", "c", "d", "e", "f"),
                tags = setOf("layout", "clusters")
            ),
            TestCase(
                name = "layout_subgraphs",
                description = "Layout with subgraphs",
                dotContent = generateSubgraphDot(),
                category = TestCategory.COMPLEX_LAYOUTS,
                expectedElements = listOf("s1", "s2", "s3", "t1", "t2", "t3"),
                tags = setOf("layout", "subgraphs")
            ),
            TestCase(
                name = "layout_mixed_directions",
                description = "Mixed edge directions",
                dotContent = generateMixedDirectionDot(),
                category = TestCategory.COMPLEX_LAYOUTS,
                expectedElements = listOf("a", "b", "c", "d"),
                tags = setOf("layout", "mixed")
            ),
            TestCase(
                name = "layout_cycles",
                description = "Layout with cycles",
                dotContent = generateCycleDot(),
                category = TestCategory.COMPLEX_LAYOUTS,
                expectedElements = listOf("a", "b", "c", "d"),
                tags = setOf("layout", "cycles")
            )
        )
        
        val advancedLayoutTests = listOf(
            TestCase(
                name = "layout_nested_clusters",
                description = "Nested clusters",
                dotContent = generateNestedClusterDot(),
                category = TestCategory.COMPLEX_LAYOUTS,
                expectedElements = listOf("a", "b", "c", "d", "e", "f", "g", "h"),
                tags = setOf("layout", "nested", "clusters")
            ),
            TestCase(
                name = "layout_cross_cluster_edges",
                description = "Cross-cluster edges",
                dotContent = generateCrossClusterEdgesDot(),
                category = TestCategory.COMPLEX_LAYOUTS,
                expectedElements = listOf("a", "b", "c", "d", "e", "f"),
                tags = setOf("layout", "cross", "clusters")
            ),
            TestCase(
                name = "layout_rank_constraints",
                description = "Rank constraints",
                dotContent = generateRankConstraintsDot(),
                category = TestCategory.COMPLEX_LAYOUTS,
                expectedElements = listOf("a", "b", "c", "d", "e", "f"),
                tags = setOf("layout", "rank", "constraints")
            ),
            TestCase(
                name = "layout_wide_graph",
                description = "Wide graph layout",
                dotContent = generateWideGraphDot(),
                category = TestCategory.COMPLEX_LAYOUTS,
                expectedElements = (1..10).map { "node$it" },
                tags = setOf("layout", "wide")
            ),
            TestCase(
                name = "layout_deep_hierarchy",
                description = "Deep hierarchical layout",
                dotContent = generateDeepHierarchyDot(),
                category = TestCategory.COMPLEX_LAYOUTS,
                expectedElements = listOf("level1", "level2", "level3", "level4", "level5"),
                tags = setOf("layout", "deep", "hierarchy")
            )
        )
        
        val specialLayoutTests = listOf(
            TestCase(
                name = "layout_undirected_graph",
                description = "Undirected graph layout",
                dotContent = generateUndirectedGraphDot(),
                category = TestCategory.COMPLEX_LAYOUTS,
                expectedElements = listOf("a", "b", "c", "d", "e"),
                tags = setOf("layout", "undirected")
            ),
            TestCase(
                name = "layout_mixed_graph_types",
                description = "Mixed directed/undirected",
                dotContent = generateMixedGraphTypesDot(),
                category = TestCategory.COMPLEX_LAYOUTS,
                expectedElements = listOf("a", "b", "c", "d"),
                tags = setOf("layout", "mixed", "types")
            )
        )
        
        return basicLayoutTests + advancedLayoutTests + specialLayoutTests
    }
    
    /**
     * Generates pathological test cases (edge cases).
     */
    fun generatePathologicalTests(): List<TestCase> {
        val basicPathologicalTests = listOf(
            TestCase(
                name = "pathological_empty_graph",
                description = "Empty graph",
                dotContent = "digraph empty { }",
                category = TestCategory.PATHOLOGICAL,
                expectedElements = emptyList(),
                tags = setOf("pathological", "empty")
            ),
            TestCase(
                name = "pathological_single_node",
                description = "Single node graph",
                dotContent = "digraph single { node1; }",
                category = TestCategory.PATHOLOGICAL,
                expectedElements = listOf("node1"),
                tags = setOf("pathological", "single")
            ),
            TestCase(
                name = "pathological_disconnected",
                description = "Disconnected components",
                dotContent = generateDisconnectedDot(),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = listOf("a", "b", "c", "d"),
                tags = setOf("pathological", "disconnected")
            ),
            TestCase(
                name = "pathological_self_loops",
                description = "Self loops",
                dotContent = generateSelfLoopDot(),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = listOf("a", "b"),
                tags = setOf("pathological", "selfloop")
            ),
            TestCase(
                name = "pathological_multiple_edges",
                description = "Multiple edges between same nodes",
                dotContent = generateMultipleEdgesDot(),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = listOf("a", "b"),
                tags = setOf("pathological", "multiple")
            ),
            TestCase(
                name = "pathological_large_graph",
                description = "Large graph (100 nodes)",
                dotContent = generateLargeGraphDot(100),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = (1..100).map { "node$it" },
                tags = setOf("pathological", "large")
            )
        )
        
        val additionalPathologicalTests = listOf(
            TestCase(
                name = "pathological_very_large_graph",
                description = "Very large graph (500 nodes)",
                dotContent = generateLargeGraphDot(500),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = (1..500).map { "node$it" },
                tags = setOf("pathological", "very_large")
            ),
            TestCase(
                name = "pathological_dense_graph",
                description = "Dense graph with many edges",
                dotContent = generateDenseGraphDot(20),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = (1..20).map { "node$it" },
                tags = setOf("pathological", "dense")
            ),
            TestCase(
                name = "pathological_long_chain",
                description = "Long chain of nodes",
                dotContent = generateLongChainDot(50),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = (1..50).map { "node$it" },
                tags = setOf("pathological", "chain")
            ),
            TestCase(
                name = "pathological_star_graph",
                description = "Star graph with central node",
                dotContent = generateStarGraphDot(30),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = listOf("center") + (1..30).map { "node$it" },
                tags = setOf("pathological", "star")
            ),
            TestCase(
                name = "pathological_complete_graph",
                description = "Complete graph (all nodes connected)",
                dotContent = generateCompleteGraphDot(10),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = (1..10).map { "node$it" },
                tags = setOf("pathological", "complete")
            ),
            TestCase(
                name = "pathological_bipartite_graph",
                description = "Bipartite graph",
                dotContent = generateBipartiteGraphDot(10, 10),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = (1..10).map { "left$it" } + (1..10).map { "right$it" },
                tags = setOf("pathological", "bipartite")
            ),
            TestCase(
                name = "pathological_many_self_loops",
                description = "Many nodes with self loops",
                dotContent = generateManySelfLoopsDot(20),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = (1..20).map { "node$it" },
                tags = setOf("pathological", "many_selfloops")
            ),
            TestCase(
                name = "pathological_parallel_edges",
                description = "Many parallel edges",
                dotContent = generateParallelEdgesDot(10),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = listOf("a", "b"),
                tags = setOf("pathological", "parallel")
            ),
            TestCase(
                name = "pathological_deeply_nested_clusters",
                description = "Deeply nested clusters",
                dotContent = generateDeeplyNestedClustersDot(5),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = (1..5).map { "node$it" },
                tags = setOf("pathological", "nested", "deep")
            ),
            TestCase(
                name = "pathological_many_disconnected_components",
                description = "Many disconnected components",
                dotContent = generateManyDisconnectedComponentsDot(20),
                category = TestCategory.PATHOLOGICAL,
                expectedElements = (1..20).map { "comp${it}_node" },
                tags = setOf("pathological", "disconnected", "many")
            )
        )
        
        return basicPathologicalTests + additionalPathologicalTests
    }
    
    /**
     * Generates all test cases.
     */
    fun generateAllTestCases(): List<TestCase> {
        return generateNodeShapeTests() +
                generateEdgeStyleTests() +
                generateArrowheadTests() +
                generateTextRenderingTests() +
                generateComplexLayoutTests() +
                generatePathologicalTests()
    }
    
    // Private helper methods for generating DOT content
    
    private fun generateNodeShapeDot(shape: String): String {
        return """
            digraph test {
                node1 [shape=$shape, label="$shape"];
            }
        """.trimIndent()
    }
    
    private fun generateEdgeStyleDot(style: String): String {
        return """
            digraph test {
                node1 -> node2 [style=$style, label="$style"];
            }
        """.trimIndent()
    }
    
    private fun generateEdgeColorDot(color: String): String {
        return """
            digraph test {
                node1 -> node2 [color=$color, label="$color"];
            }
        """.trimIndent()
    }
    
    private fun generateEdgePenwidthDot(width: Double): String {
        return """
            digraph test {
                node1 -> node2 [penwidth=$width, label="width $width"];
            }
        """.trimIndent()
    }
    
    private fun generateArrowheadDot(arrowhead: String, attribute: String): String {
        return """
            digraph test {
                node1 -> node2 [$attribute=$arrowhead, label="$arrowhead"];
            }
        """.trimIndent()
    }
    
    private fun generateArrowSizeDot(size: Double): String {
        return """
            digraph test {
                node1 -> node2 [arrowsize=$size, label="size $size"];
            }
        """.trimIndent()
    }
    
    private fun generateCombinedArrowDot(): String {
        return """
            digraph test {
                node1 -> node2 [arrowhead=diamond, arrowtail=dot, dir=both, label="both ends"];
            }
        """.trimIndent()
    }
    
    private fun generateTextLabelDot(label: String): String {
        return """
            digraph test {
                node1 [label="$label"];
            }
        """.trimIndent()
    }
    
    private fun generateHtmlLabelDot(): String {
        return """
            digraph test {
                node1 [label=<<B>Bold</B><BR/><I>Italic</I><BR/><U>Underline</U>>];
            }
        """.trimIndent()
    }
    
    private fun generateFontVariationDot(): String {
        return """
            digraph test {
                node1 [label="Normal", fontname="Arial"];
                node2 [label="Bold", fontname="Arial", style=bold];
                node3 [label="Large", fontname="Arial", fontsize=20];
            }
        """.trimIndent()
    }
    
    private fun generateFontSizeDot(size: Int): String {
        return """
            digraph test {
                node1 [label="Size $size", fontsize=$size];
            }
        """.trimIndent()
    }
    
    private fun generateFontFamilyDot(font: String): String {
        return """
            digraph test {
                node1 [label="$font Font", fontname="$font"];
            }
        """.trimIndent()
    }
    
    private fun generateTextColorDot(color: String): String {
        return """
            digraph test {
                node1 [label="$color text", fontcolor=$color];
            }
        """.trimIndent()
    }
    
    private fun generateUnicodeLabelDot(): String {
        return """
            digraph test {
                node1 [label="Unicode: αβγ δεζ ηθι κλμ"];
            }
        """.trimIndent()
    }
    
    private fun generateLongLabelDot(): String {
        return """
            digraph test {
                node1 [label="This is a very long label that should test text wrapping and positioning in various rendering scenarios"];
            }
        """.trimIndent()
    }
    
    private fun generateEmptyLabelDot(): String {
        return """
            digraph test {
                node1 [label=""];
            }
        """.trimIndent()
    }
    
    private fun generateSimpleHierarchyDot(): String {
        return """
            digraph hierarchy {
                root -> child1;
                root -> child2;
                child1 -> grandchild1;
                child2 -> grandchild2;
            }
        """.trimIndent()
    }
    
    private fun generateClusterDot(): String {
        return """
            digraph clusters {
                subgraph cluster_0 {
                    style=filled;
                    color=lightgrey;
                    node [style=filled,color=white];
                    a -> b -> c;
                    label = "Cluster 1";
                }
                subgraph cluster_1 {
                    node [style=filled];
                    d -> e -> f;
                    label = "Cluster 2";
                    color=blue;
                }
                a -> d;
            }
        """.trimIndent()
    }
    
    private fun generateSubgraphDot(): String {
        return """
            digraph subgraphs {
                subgraph source {
                    s1 -> s2 -> s3;
                }
                subgraph target {
                    t1 -> t2 -> t3;
                }
                s2 -> t2;
            }
        """.trimIndent()
    }
    
    private fun generateMixedDirectionDot(): String {
        return """
            digraph mixed {
                a -> b;
                b -> c;
                c -> d;
                d -> a;
                a -> c;
            }
        """.trimIndent()
    }
    
    private fun generateCycleDot(): String {
        return """
            digraph cycle {
                a -> b -> c -> d -> a;
            }
        """.trimIndent()
    }
    
    private fun generateNestedClusterDot(): String {
        return """
            digraph nested {
                subgraph cluster_outer {
                    label = "Outer Cluster";
                    subgraph cluster_inner1 {
                        label = "Inner 1";
                        a -> b;
                    }
                    subgraph cluster_inner2 {
                        label = "Inner 2";
                        c -> d;
                    }
                    e -> f;
                }
                g -> h;
                a -> g;
            }
        """.trimIndent()
    }
    
    private fun generateCrossClusterEdgesDot(): String {
        return """
            digraph cross_cluster {
                subgraph cluster_1 {
                    label = "Cluster 1";
                    a -> b -> c;
                }
                subgraph cluster_2 {
                    label = "Cluster 2";
                    d -> e -> f;
                }
                b -> e;
                c -> d;
            }
        """.trimIndent()
    }
    
    private fun generateRankConstraintsDot(): String {
        return """
            digraph ranks {
                {rank=same; a; b;}
                {rank=same; c; d;}
                {rank=same; e; f;}
                a -> c -> e;
                b -> d -> f;
                a -> d;
                b -> c;
            }
        """.trimIndent()
    }
    
    private fun generateWideGraphDot(): String {
        return """
            digraph wide {
                node1 -> node2 -> node3 -> node4 -> node5;
                node6 -> node7 -> node8 -> node9 -> node10;
                node1 -> node6;
                node5 -> node10;
            }
        """.trimIndent()
    }
    
    private fun generateDeepHierarchyDot(): String {
        return """
            digraph deep {
                level1 -> level2 -> level3 -> level4 -> level5;
            }
        """.trimIndent()
    }
    
    private fun generateUndirectedGraphDot(): String {
        return """
            graph undirected {
                a -- b -- c;
                c -- d -- e;
                e -- a;
            }
        """.trimIndent()
    }
    
    private fun generateMixedGraphTypesDot(): String {
        return """
            digraph mixed_types {
                subgraph directed_part {
                    a -> b;
                }
                subgraph undirected_part {
                    c -- d;
                }
                b -> c;
            }
        """.trimIndent()
    }
    
    private fun generateDisconnectedDot(): String {
        return """
            digraph disconnected {
                a -> b;
                c -> d;
            }
        """.trimIndent()
    }
    
    private fun generateSelfLoopDot(): String {
        return """
            digraph selfloop {
                a -> a;
                a -> b;
                b -> b;
            }
        """.trimIndent()
    }
    
    private fun generateMultipleEdgesDot(): String {
        return """
            digraph multiple {
                a -> b [label="edge1"];
                a -> b [label="edge2"];
                a -> b [label="edge3"];
            }
        """.trimIndent()
    }
    
    private fun generateLargeGraphDot(nodeCount: Int): String {
        val sb = StringBuilder()
        sb.appendLine("digraph large {")
        
        // Create nodes
        for (i in 1..nodeCount) {
            sb.appendLine("    node$i [label=\"Node $i\"];")
        }
        
        // Create edges (tree structure to avoid too many edges)
        for (i in 2..nodeCount) {
            val parent = i / 2
            sb.appendLine("    node$parent -> node$i;")
        }
        
        sb.appendLine("}")
        return sb.toString()
    }
    
    private fun generateDenseGraphDot(nodeCount: Int): String {
        val sb = StringBuilder()
        sb.appendLine("digraph dense {")
        
        // Create nodes
        for (i in 1..nodeCount) {
            sb.appendLine("    node$i [label=\"Node $i\"];")
        }
        
        // Create many edges (each node connects to several others)
        for (i in 1..nodeCount) {
            for (j in 1..minOf(5, nodeCount)) {
                if (i != j) {
                    sb.appendLine("    node$i -> node$j;")
                }
            }
        }
        
        sb.appendLine("}")
        return sb.toString()
    }
    
    private fun generateLongChainDot(nodeCount: Int): String {
        val sb = StringBuilder()
        sb.appendLine("digraph chain {")
        
        // Create chain of nodes
        for (i in 1 until nodeCount) {
            sb.appendLine("    node$i -> node${i + 1};")
        }
        
        sb.appendLine("}")
        return sb.toString()
    }
    
    private fun generateStarGraphDot(nodeCount: Int): String {
        val sb = StringBuilder()
        sb.appendLine("digraph star {")
        
        // Central node connects to all others
        for (i in 1..nodeCount) {
            sb.appendLine("    center -> node$i;")
        }
        
        sb.appendLine("}")
        return sb.toString()
    }
    
    private fun generateCompleteGraphDot(nodeCount: Int): String {
        val sb = StringBuilder()
        sb.appendLine("digraph complete {")
        
        // Every node connects to every other node
        for (i in 1..nodeCount) {
            for (j in 1..nodeCount) {
                if (i != j) {
                    sb.appendLine("    node$i -> node$j;")
                }
            }
        }
        
        sb.appendLine("}")
        return sb.toString()
    }
    
    private fun generateBipartiteGraphDot(leftCount: Int, rightCount: Int): String {
        val sb = StringBuilder()
        sb.appendLine("digraph bipartite {")
        
        // Left side nodes
        for (i in 1..leftCount) {
            sb.appendLine("    left$i [label=\"Left $i\"];")
        }
        
        // Right side nodes
        for (i in 1..rightCount) {
            sb.appendLine("    right$i [label=\"Right $i\"];")
        }
        
        // Connect left to right
        for (i in 1..leftCount) {
            for (j in 1..rightCount) {
                sb.appendLine("    left$i -> right$j;")
            }
        }
        
        sb.appendLine("}")
        return sb.toString()
    }
    
    private fun generateManySelfLoopsDot(nodeCount: Int): String {
        val sb = StringBuilder()
        sb.appendLine("digraph many_selfloops {")
        
        // Each node has a self loop
        for (i in 1..nodeCount) {
            sb.appendLine("    node$i -> node$i;")
        }
        
        // Add some regular edges too
        for (i in 1 until nodeCount) {
            sb.appendLine("    node$i -> node${i + 1};")
        }
        
        sb.appendLine("}")
        return sb.toString()
    }
    
    private fun generateParallelEdgesDot(edgeCount: Int): String {
        val sb = StringBuilder()
        sb.appendLine("digraph parallel {")
        
        // Many parallel edges between same nodes
        for (i in 1..edgeCount) {
            sb.appendLine("    a -> b [label=\"edge$i\"];")
        }
        
        sb.appendLine("}")
        return sb.toString()
    }
    
    private fun generateDeeplyNestedClustersDot(depth: Int): String {
        val sb = StringBuilder()
        sb.appendLine("digraph deeply_nested {")
        
        // Create nested clusters
        for (i in 1..depth) {
            sb.appendLine("    subgraph cluster_$i {")
            sb.appendLine("        label = \"Cluster $i\";")
        }
        
        // Add a node in the innermost cluster
        sb.appendLine("        node$depth [label=\"Deep Node\"];")
        
        // Close all clusters
        for (i in 1..depth) {
            sb.appendLine("    }")
        }
        
        // Add nodes and edges outside clusters
        for (i in 1 until depth) {
            sb.appendLine("    node$i -> node${i + 1};")
        }
        
        sb.appendLine("}")
        return sb.toString()
    }
    
    private fun generateManyDisconnectedComponentsDot(componentCount: Int): String {
        val sb = StringBuilder()
        sb.appendLine("digraph many_disconnected {")
        
        // Create many small disconnected components
        for (i in 1..componentCount) {
            sb.appendLine("    comp${i}_node [label=\"Component $i\"];")
        }
        
        sb.appendLine("}")
        return sb.toString()
    }
}

/**
 * Represents a test case for visual regression testing.
 */
data class TestCase(
    val name: String,
    val description: String,
    val dotContent: String,
    val category: TestCategory,
    val expectedElements: List<String>,
    val tags: Set<String> = emptySet()
)

/**
 * Categories of test cases.
 */
enum class TestCategory {
    NODE_SHAPES,
    EDGE_STYLES,
    ARROWHEADS,
    TEXT_RENDERING,
    COMPLEX_LAYOUTS,
    PATHOLOGICAL
}