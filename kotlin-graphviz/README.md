# Kotlin Graphviz

A Kotlin Multiplatform port of the Graphviz graph visualization library.

## Supported Platforms

- **JVM** - Java Virtual Machine
- **Android** - Android applications
- **iOS** - iOS applications (arm64, x64, simulatorArm64)
- **JavaScript** - Browser and Node.js environments

## Project Structure

```
src/
├── commonMain/kotlin/          # Shared code across all platforms
├── commonTest/kotlin/          # Shared tests across all platforms
├── jvmMain/kotlin/            # JVM-specific code
├── jvmTest/kotlin/            # JVM-specific tests
├── androidMain/kotlin/        # Android-specific code
├── androidTest/kotlin/        # Android-specific tests
├── iosMain/kotlin/            # iOS-specific code
├── iosTest/kotlin/            # iOS-specific tests
├── jsMain/kotlin/             # JavaScript-specific code
└── jsTest/kotlin/             # JavaScript-specific tests
```

## Testing Framework

This project uses [Kotest](https://kotest.io/) for property-based testing and unit testing across all platforms.

## Building

```bash
# Build all targets
./gradlew build

# Run JVM tests
./gradlew jvmTest

# Run JavaScript tests
./gradlew jsTest

# Run all tests
./gradlew allTests
```

## Requirements

- Kotlin 2.2.20+
- Gradle 9.0+
- JDK 11+

## License

Eclipse Public License 1.0