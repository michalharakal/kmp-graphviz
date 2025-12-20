package org.graphviz.kotlin

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Simple test to verify Kotest framework is working
 */
class SimpleTest : StringSpec({
    
    "simple test should pass" {
        val result = 1 + 1
        result shouldBe 2
    }
    
    "string test should pass" {
        val text = "hello"
        text.length shouldBe 5
    }
})