# CLAUDE.md - Guide for AI Assistants

## Build Commands
- Build project: `./gradlew clean build`
- Run application: `./gradlew run`
- Build native image: `./gradlew nativeCompile` or `./build-native-image.sh`
- Run all tests: `./gradlew test`
- Run single test: `./gradlew test --tests "org.example.TestClassName.testMethodName"`

## Code Style Guidelines
- **Naming**: Classes=PascalCase, Functions/Variables=camelCase, Constants=UPPER_SNAKE_CASE
- **Types**: Use Kotlin type inference where appropriate, explicit declarations when necessary
- **Nullability**: Use Kotlin's nullable types (e.g., `String?`) and non-null assertions (`!!`) sparingly
- **Imports**: No wildcards, alphabetized, organized by package
- **Error Handling**: Use try-catch for expected exceptions, log with context information
- **Indentation**: 4 spaces
- **Documentation**: KDoc style for classes/methods, contextual inline comments
- **Logging**: SLF4J with Logback, appropriate log levels

## Project Structure
- Main code: `src/main/kotlin/`
- Resources: `src/main/resources/`
- Tests: `src/test/kotlin/`