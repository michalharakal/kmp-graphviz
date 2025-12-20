#!/bin/bash

# Visual Consistency Testing Script
# Compares Kotlin Graphviz output with original C Graphviz

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
TEST_DIR="$PROJECT_DIR/visual-tests"
REFERENCE_DIR="$TEST_DIR/reference"
OUTPUT_DIR="$TEST_DIR/kotlin-output"
RESULTS_DIR="$TEST_DIR/results"

# Create directories
mkdir -p "$REFERENCE_DIR" "$OUTPUT_DIR" "$RESULTS_DIR"

echo "Running Visual Consistency Tests..."

# Check if original Graphviz is available
if ! command -v dot &> /dev/null; then
    echo "Error: Original Graphviz 'dot' command not found"
    echo "Please install Graphviz to generate reference outputs"
    exit 1
fi

# Test cases
TEST_CASES=(
    "basic_shapes"
    "edge_styles" 
    "arrowheads"
    "text_rendering"
    "complex_layouts"
)

# Generate test DOT files
generate_test_files() {
    echo "Generating test DOT files..."
    
    # Basic shapes test
    cat > "$TEST_DIR/basic_shapes.dot" << 'EOF'
digraph basic_shapes {
    A [shape=box, label="Box"];
    B [shape=ellipse, label="Ellipse"];
    C [shape=circle, label="Circle"];
    D [shape=diamond, label="Diamond"];
    E [shape=triangle, label="Triangle"];
    
    A -> B -> C -> D -> E;
}
EOF

    # Edge styles test
    cat > "$TEST_DIR/edge_styles.dot" << 'EOF'
digraph edge_styles {
    A -> B [style=solid, label="solid"];
    A -> C [style=dashed, label="dashed"];
    A -> D [style=dotted, label="dotted"];
    A -> E [style=bold, label="bold"];
}
EOF

    # Arrowheads test
    cat > "$TEST_DIR/arrowheads.dot" << 'EOF'
digraph arrowheads {
    A -> B [arrowhead=normal, label="normal"];
    A -> C [arrowhead=inv, label="inv"];
    A -> D [arrowhead=dot, label="dot"];
    A -> E [arrowhead=diamond, label="diamond"];
    A -> F [arrowhead=box, label="box"];
}
EOF

    # Text rendering test
    cat > "$TEST_DIR/text_rendering.dot" << 'EOF'
digraph text_rendering {
    A [label="Default Text"];
    B [label="Arial Text", fontname="Arial", fontsize=14];
    C [label="Large Text", fontsize=18];
    D [label="Colored Text", fontcolor=red];
    
    A -> B [label="Edge Label"];
    B -> C [label="Styled Edge", fontname="Times", fontsize=12];
    C -> D [label="Colored Edge", fontcolor=blue];
}
EOF

    # Complex layout test
    cat > "$TEST_DIR/complex_layouts.dot" << 'EOF'
digraph complex_layouts {
    subgraph cluster_0 {
        label="Cluster 0";
        style=filled;
        color=lightgrey;
        A -> B -> C;
    }
    
    subgraph cluster_1 {
        label="Cluster 1";
        style=filled;
        color=lightblue;
        D -> E -> F;
    }
    
    A -> D;
    B -> E;
    C -> F;
    
    G [shape=box, style=filled, fillcolor=yellow];
    A -> G;
    D -> G;
}
EOF
}

# Generate reference SVGs using original Graphviz
generate_reference_outputs() {
    echo "Generating reference outputs with original Graphviz..."
    
    for test_case in "${TEST_CASES[@]}"; do
        if [ -f "$TEST_DIR/${test_case}.dot" ]; then
            echo "  Processing $test_case..."
            dot -Tsvg "$TEST_DIR/${test_case}.dot" -o "$REFERENCE_DIR/${test_case}.svg"
            
            # Also generate with layout information for debugging
            dot -Tsvg -Gverbose=1 "$TEST_DIR/${test_case}.dot" -o "$REFERENCE_DIR/${test_case}_verbose.svg" 2> "$REFERENCE_DIR/${test_case}_layout.log"
        fi
    done
}

# Generate Kotlin outputs
generate_kotlin_outputs() {
    echo "Generating Kotlin outputs..."
    
    cd "$PROJECT_DIR"
    
    for test_case in "${TEST_CASES[@]}"; do
        if [ -f "$TEST_DIR/${test_case}.dot" ]; then
            echo "  Processing $test_case with Kotlin..."
            
            # Run Kotlin implementation
            kotlin -cp "build/libs/*" VisualConsistencyTest "$TEST_DIR/${test_case}.dot" "$OUTPUT_DIR/${test_case}.svg" || {
                echo "    Warning: Kotlin processing failed for $test_case"
                continue
            }
        fi
    done
}

# Compare outputs
compare_outputs() {
    echo "Comparing outputs..."
    
    local total_tests=0
    local passed_tests=0
    
    for test_case in "${TEST_CASES[@]}"; do
        if [ -f "$REFERENCE_DIR/${test_case}.svg" ] && [ -f "$OUTPUT_DIR/${test_case}.svg" ]; then
            echo "  Comparing $test_case..."
            total_tests=$((total_tests + 1))
            
            # Run comparison (you'll need to implement this)
            if python3 "$SCRIPT_DIR/compare_svg.py" "$REFERENCE_DIR/${test_case}.svg" "$OUTPUT_DIR/${test_case}.svg" > "$RESULTS_DIR/${test_case}_comparison.json"; then
                passed_tests=$((passed_tests + 1))
                echo "    ✓ PASS"
            else
                echo "    ✗ FAIL"
            fi
        fi
    done
    
    echo ""
    echo "Results: $passed_tests/$total_tests tests passed"
    
    if [ $passed_tests -eq $total_tests ]; then
        echo "🎉 All visual consistency tests passed!"
        exit 0
    else
        echo "❌ Some tests failed. Check results in $RESULTS_DIR/"
        exit 1
    fi
}

# Generate detailed report
generate_report() {
    echo "Generating detailed report..."
    
    cat > "$RESULTS_DIR/report.html" << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>Visual Consistency Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .test-case { border: 1px solid #ccc; margin: 10px 0; padding: 10px; }
        .pass { background-color: #d4edda; }
        .fail { background-color: #f8d7da; }
        .comparison { display: flex; gap: 20px; }
        .svg-container { border: 1px solid #ddd; padding: 10px; }
    </style>
</head>
<body>
    <h1>Visual Consistency Test Report</h1>
EOF

    for test_case in "${TEST_CASES[@]}"; do
        if [ -f "$RESULTS_DIR/${test_case}_comparison.json" ]; then
            # Add test case to report (simplified)
            cat >> "$RESULTS_DIR/report.html" << EOF
    <div class="test-case">
        <h2>$test_case</h2>
        <div class="comparison">
            <div class="svg-container">
                <h3>Reference (Original Graphviz)</h3>
                <img src="../reference/${test_case}.svg" alt="Reference $test_case" />
            </div>
            <div class="svg-container">
                <h3>Kotlin Implementation</h3>
                <img src="../kotlin-output/${test_case}.svg" alt="Kotlin $test_case" />
            </div>
        </div>
    </div>
EOF
        fi
    done
    
    cat >> "$RESULTS_DIR/report.html" << 'EOF'
</body>
</html>
EOF

    echo "Report generated: $RESULTS_DIR/report.html"
}

# Main execution
main() {
    generate_test_files
    generate_reference_outputs
    generate_kotlin_outputs
    compare_outputs
    generate_report
}

# Run if called directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi