#!/usr/bin/env kotlin

@file:DependsOn("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")

import org.graphviz.kotlin.builder.*
import org.graphviz.kotlin.model.*
import org.graphviz.kotlin.renderer.*

/**
 * Manual test to verify Property 5: SVG validity and structure
 * **Feature: kotlin-multiplatform-port, Property 5: SVG validity and structure**
 * **Validates: Requirements 4.1, 4.4**
 */

fun main() {
    println("Testing Property 5: SVG validity and structure")
    
    // Test 1: Basic graph
    println("Test 1: Basic graph with nodes and edges")
    val builder = GraphBuilder("test-graph", true)
    
    builder.node("A") {
        position(0.0, 0.0)
        label("Node A")
        shape("ellipse")
    }
    builder.node("B") {
        position(100.0, 50.0)
        label("Node B")
        shape("box")
    }
    
    builder.edge("A", "B") {
        controlPoints(listOf(Point(0.0, 0.0), Point(100.0, 50.0)))
        label("Edge A->B")
    }
    
    val graph = builder.build()
    val renderer = DefaultSvgRenderer()
    val options = RenderOptions.default()
    
    try {
        val svg = renderer.render(graph, options)
        
        // Verify SVG validity properties
        val checks = listOf(
            "XML declaration" to svg.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"),
            "DOCTYPE" to svg.contains("<!DOCTYPE svg"),
            "SVG root element" to svg.contains("<svg"),
            "SVG closing element" to svg.contains("</svg>"),
            "SVG namespace" to svg.contains("xmlns=\"http://www.w3.org/2000/svg\""),
            "XLink namespace" to svg.contains("xmlns:xlink=\"http://www.w3.org/1999/xlink\""),
            "SVG version" to svg.contains("version=\"1.1\""),
            "Width attribute" to svg.contains("width=\""),
            "Height attribute" to svg.contains("height=\""),
            "ViewBox attribute" to svg.contains("viewBox=\""),
            "Node A element" to svg.contains("id=\"node-A\""),
            "Node B element" to svg.contains("id=\"node-B\""),
            "Edge element" to svg.contains("id=\"edge-A-B\"")
        )
        
        var allPassed = true
        checks.forEach { (name, passed) ->
            if (passed) {
                println("✓ $name")
            } else {
                println("✗ $name")
                allPassed = false
            }
        }
        
        if (allPassed) {
            println("✓ All basic SVG validity checks passed!")
        } else {
            println("✗ Some SVG validity checks failed!")
            return
        }
        
        // Test ViewBox format
        val viewBoxRegex = """viewBox="([^"]+)"""".toRegex()
        viewBoxRegex.find(svg)?.let { match ->
            val viewBoxStr = match.groupValues[1]
            val parts = viewBoxStr.split(" ")
            if (parts.size == 4) {
                val viewBoxWidth = parts[2].toDoubleOrNull()
                val viewBoxHeight = parts[3].toDoubleOrNull()
                if (viewBoxWidth != null && viewBoxHeight != null && viewBoxWidth > 0.0 && viewBoxHeight > 0.0) {
                    println("✓ ViewBox format is valid: $viewBoxStr")
                } else {
                    println("✗ ViewBox dimensions are invalid: $viewBoxStr")
                    return
                }
            } else {
                println("✗ ViewBox format is invalid: $viewBoxStr")
                return
            }
        } ?: run {
            println("✗ ViewBox not found in SVG")
            return
        }
        
        println("\n✓ Property 5 test PASSED: SVG validity and structure verified")
        
    } catch (e: Exception) {
        println("✗ Property 5 test FAILED: ${e.message}")
        e.printStackTrace()
    }
}