# Project Structure & Organization

## Top-Level Directory Layout

### Core Implementation
- **`lib/`** - Core Graphviz libraries (C/C++)
  - `cgraph/` - Main graph data structure library
  - `gvc/` - Graphviz context and rendering coordination
  - `pathplan/` - Path planning and routing algorithms
  - `common/` - Shared utilities and data structures
  - `util/` - General utility functions
  - Layout engines: `dotgen/`, `neatogen/`, `fdpgen/`, `sfdpgen/`, `twopigen/`, `circogen/`, `osage/`, `patchwork/`
  - Specialized: `edgepaint/`, `mingle/`, `pack/`, `label/`, `ortho/`, `vpsc/`

- **`plugin/`** - Modular plugin system
  - `core/` - Essential core plugins
  - `dot_layout/`, `neato_layout/` - Layout engine plugins
  - Output formats: `gd/`, `cairo/`, `rsvg/`, `webp/`, `poppler/`
  - Platform-specific: `quartz/` (macOS), `gdiplus/` (Windows), `xlib/` (X11)

- **`cmd/`** - Command-line tools and applications
  - `dot/` - Main dot command (uses libraries)
  - `tools/` - Utility commands (gml2gv, graphml2gv, etc.)
  - `gvedit/` - Graph editor GUI
  - `smyrna/` - Large graph viewer
  - Specialized tools: `edgepaint/`, `gvmap/`, `gvpr/`, `mingle/`

### Language Bindings & Interfaces
- **`tclpkg/`** - Language bindings (despite name, includes many languages)
  - `gv/` - Main SWIG-generated bindings
  - `tcldot/`, `gdtclft/` - TCL-specific packages
  - `tclpathplan/` - TCL pathplan bindings

### Build & Configuration
- **`cmake/`** - CMake build system modules and find scripts
- **`m4/`** - Autotools M4 macros
- **`config/`** - Build configuration scripts
- **`ci/`** - Continuous integration scripts and configurations

### Documentation & Examples
- **`doc/`** - Documentation source files
- **`graphs/`** - Example graph files
  - `directed/` - Directed graph examples
  - `undirected/` - Undirected graph examples
- **`share/`** - Shared data files and examples
- **`dot.demo/`** - API usage examples
- **`plugin.demo/`** - Plugin development examples

### Testing & Quality
- **`tests/`** - Test suite (pytest-based)
  - Regression tests, performance tests, C++ API tests
  - Reference outputs and test data
- **`contrib/`** - Community contributions and utilities

### Platform-Specific
- **`windows/`** - Windows-specific build files and dependencies
- **`macosx/`** - macOS-specific files and Xcode project
- **`debian/`** - Debian packaging files
- **`redhat/`** - RPM packaging specifications

## Key Files & Conventions

### Build Configuration
- **`configure.ac`** - Autotools configuration (primary)
- **`CMakeLists.txt`** - CMake configuration (secondary)
- **`Makefile.am`** - Automake templates
- **`.clang-format`** - Code formatting (LLVM style)
- **`.pylintrc`** - Python linting configuration

### Project Metadata
- **`graphviz_version.h.in`** - Version information template
- **`gen_version.py`** - Version generation script
- **`CHANGELOG.md`** - Release notes and changes
- **`CONTRIBUTING.md`** - Contributor guidelines
- **`DEVELOPERS.md`** - Developer documentation

## Naming Conventions

### Libraries
- **Prefix**: `lib` (e.g., `libcgraph`, `libgvc`)
- **Headers**: Installed to `include/graphviz/`
- **Pkg-config**: `.pc` files for library discovery

### Plugins
- **Naming**: `gvplugin_<type>_<name>`
- **Configuration**: Version-specific config files
- **Loading**: Dynamic loading via libltdl

### Commands
- **Layout engines**: `dot`, `neato`, `fdp`, `sfdp`, `twopi`, `circo`, `osage`, `patchwork`
- **Utilities**: Descriptive names (e.g., `gml2gv`, `dot2gxl`)
- **Tools**: Prefixed with `gv` when appropriate (e.g., `gvedit`, `gvmap`)

## Development Workflow Directories
- **`.gitlab/`** - GitLab-specific CI/CD configuration
- **`.git/`** - Git repository metadata
- **`.kiro/`** - Kiro IDE configuration and steering files

## Architecture Patterns
- **Layered**: Libraries build upon each other (cgraph → gvc → plugins)
- **Plugin-based**: Extensible via dynamically loaded plugins
- **Multi-platform**: Conditional compilation for different OS/compilers
- **Language-agnostic**: Core in C with bindings for many languages