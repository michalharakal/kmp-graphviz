# Technology Stack & Build System

## Build Systems
Graphviz supports two build systems:

### Autotools (Primary/Complete)
- **Primary build system** - supports all features and components
- **Configure**: `./autogen.sh && ./configure --prefix=/path/to/install`
- **Build**: `make`
- **Install**: `make install`
- **Test**: `python3 -m pytest tests` (requires proper environment setup)

### CMake (Secondary/Incomplete)
- **Limited support** - only builds subset of binaries and libraries
- **Configure**: `cmake -DCMAKE_INSTALL_PREFIX=/path/to/install -B build -S .`
- **Build**: `cmake --build build`
- **Install**: `cmake --install build`

## Core Technologies

### Languages & Standards
- **C**: C17 standard (primary language)
- **C++**: C++17 standard (secondary language)
- **Python**: Python 3.9+ (for testing and tooling)
- **SWIG**: Language bindings generation

### Key Dependencies
- **Required**: Bison 3.0+, Flex, Python 3 (interpreter)
- **Optional**: Cairo, Expat, GD, Ghostscript, GTK, Pango, Qt, Zlib
- **Language bindings**: TCL, Perl, PHP, Python, Ruby, Java, C#, Go, Lua, R, Guile

### Compiler Requirements
- **POSIX compliance** with specific exceptions for Windows (MSVC/MinGW)
- **Warning flags**: Extensive warning configuration (-Wall, -Wextra, etc.)
- **Standards**: No VLAs, no printf "%zu", avoid direct exit() calls

## Common Commands

### Development Build
```bash
# Full development setup
./autogen.sh
PREFIX=$(mktemp -d)
./configure --prefix=${PREFIX}
make && make install

# Run tests with custom installation
env PATH=${PREFIX}/bin:${PATH} \
    LD_LIBRARY_PATH=${PREFIX}/lib \
    python3 -m pytest tests
```

### Code Quality
```bash
# Format code (LLVM style)
clang-format -i file.c

# Lint Python code
pylint --rcfile=.pylintrc tests/

# Memory debugging
export CFLAGS="-g3 -fsanitize=address"
export CXXFLAGS="-g3 -fsanitize=address"
```

### Performance Builds
```bash
# Optimized build
env CFLAGS="-O3 -flto -DNDEBUG -march=native -mtune=native -g" \
    CXXFLAGS="-O3 -flto -DNDEBUG -march=native -mtune=native -g" \
    ./configure
```

## Plugin Architecture
- **Dynamic loading**: Uses libltdl for on-demand plugin loading
- **Plugin types**: Layout engines, output formats, device renderers
- **Configuration**: Plugins configured via `config${GVPLUGIN_CURRENT}` file

## Testing Framework
- **Primary**: pytest (Python-based test framework)
- **C/C++ tests**: Compiled and run via pytest helpers
- **Coverage**: Available with `--enable-debug` and coverage flags
- **CI/CD**: GitLab CI with multiple platform testing