# Comprehensive Guide to GraalVM Native Image for Kotlin CLI Projects

This guide provides a detailed walkthrough for setting up GraalVM Native Image compilation for Kotlin CLI applications. It covers prerequisites, configuration, common issues, and best practices based on real-world experience.

## Table of Contents

1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
3. [Project Setup](#project-setup)
4. [GraalVM Configuration](#graalvm-configuration)
5. [Handling Reflection](#handling-reflection)
6. [Class Initialization Strategy](#class-initialization-strategy)
7. [Dealing with Unsafe Operations](#dealing-with-unsafe-operations)
8. [Build Process](#build-process)
9. [Common Issues and Solutions](#common-issues-and-solutions)
10. [Best Practices](#best-practices)
11. [Performance Considerations](#performance-considerations)
12. [Resources](#resources)

## Introduction

GraalVM Native Image is a technology that compiles Java/Kotlin applications ahead-of-time into standalone executables. These executables start instantly, use less memory, and provide peak performance without warmup, making them ideal for CLI applications.

Benefits of using GraalVM Native Image:
- **Faster startup time**: Native images start in milliseconds, compared to seconds for JVM applications
- **Lower memory footprint**: Native images use significantly less memory
- **No warmup time**: Native images deliver peak performance immediately
- **Self-contained**: No need to install a JVM on the target machine

## Prerequisites

1. **GraalVM Installation**:
   - Download GraalVM (version 21 or later recommended) from [GraalVM website](https://www.graalvm.org/downloads/)
   - Set the `GRAALVM_HOME` environment variable to point to your GraalVM installation
   - Add `$GRAALVM_HOME/bin` to your PATH

2. **Native Image Tool Installation**:
   ```bash
   $GRAALVM_HOME/bin/gu install native-image
   ```

3. **System Dependencies**:
   - **Linux**: `sudo apt-get install build-essential zlib1g-dev` (Ubuntu) or `sudo yum install gcc glibc-devel zlib-devel` (Oracle Linux)
   - **macOS**: `xcode-select --install`
   - **Windows**: Install Visual Studio 2022 with the Windows 11 SDK

4. **Gradle 8.0+** or **Maven 3.8.0+**

## Project Setup

### Gradle Configuration (build.gradle.kts)

```kotlin
plugins {
    kotlin("jvm") version "2.1.10"
    application
    id("org.graalvm.buildtools.native") version "0.10.6"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Your dependencies here
    
    // GraalVM SDK for native image configuration
    compileOnly("org.graalvm.nativeimage:svm:23.1.1")
}

kotlin {
    jvmToolchain(21) // Match your GraalVM Java version
}

application {
    mainClass.set("org.example.MainKt")
}

graalvmNative {
    // Enable toolchain detection to find GraalVM
    toolchainDetection.set(true)

    binaries {
        named("main") {
            imageName.set("my-cli-app")
            mainClass.set("org.example.MainKt")
            debug.set(false)
            verbose.set(true)
            
            // Add build arguments here
            buildArgs.add("--enable-url-protocols=https")
            
            // Add configuration files
            buildArgs.add("-H:ReflectionConfigurationFiles=${projectDir}/src/main/resources/META-INF/native-image/reflect-config.json")
            buildArgs.add("-H:ResourceConfigurationFiles=${projectDir}/src/main/resources/META-INF/native-image/resource-config.json")
            
            // Enable detailed error reporting
            buildArgs.add("-H:+ReportExceptionStackTraces")
        }
    }
    
    // Configure the agent for generating metadata
    agent {
        enabled.set(true)
        defaultMode.set("standard")
        metadataCopy {
            inputTaskNames.add("run")
            outputDirectories.add("src/main/resources/META-INF/native-image/")
            mergeWithExisting.set(true)
        }
    }
    
    // Enable metadata repository support
    metadataRepository {
        enabled.set(true)
    }
}
```

### Maven Configuration (pom.xml)

```xml
<project>
    <!-- ... other configuration ... -->
    
    <properties>
        <graalvm.version>23.1.1</graalvm.version>
        <kotlin.version>2.1.10</kotlin.version>
        <native.maven.plugin.version>0.10.6</native.maven.plugin.version>
    </properties>
    
    <dependencies>
        <!-- ... other dependencies ... -->
        
        <dependency>
            <groupId>org.graalvm.nativeimage</groupId>
            <artifactId>svm</artifactId>
            <version>${graalvm.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <!-- ... other plugins ... -->
            
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <version>${native.maven.plugin.version}</version>
                <extensions>true</extensions>
                <executions>
                    <execution>
                        <id>build-native</id>
                        <goals>
                            <goal>compile-no-fork</goal>
                        </goals>
                        <phase>package</phase>
                    </execution>
                </executions>
                <configuration>
                    <imageName>my-cli-app</imageName>
                    <mainClass>org.example.MainKt</mainClass>
                    <buildArgs>
                        <buildArg>--enable-url-protocols=https</buildArg>
                        <buildArg>-H:ReflectionConfigurationFiles=${project.basedir}/src/main/resources/META-INF/native-image/reflect-config.json</buildArg>
                        <buildArg>-H:ResourceConfigurationFiles=${project.basedir}/src/main/resources/META-INF/native-image/resource-config.json</buildArg>
                        <buildArg>-H:+ReportExceptionStackTraces</buildArg>
                    </buildArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## GraalVM Configuration

### Environment Setup Script (setup-graalvm.sh)

Create a script to set up the GraalVM environment:

```bash
#!/bin/bash

# Check if GRAALVM_HOME is set
if [ -z "$GRAALVM_HOME" ]; then
  echo "GRAALVM_HOME is not set. Please set it to your GraalVM installation directory."
  echo "For example:"
  echo "  export GRAALVM_HOME=/path/to/graalvm"
  exit 1
fi

# Check if native-image is installed
if ! command -v "$GRAALVM_HOME/bin/native-image" &> /dev/null; then
  echo "native-image is not installed in your GraalVM installation."
  echo "Installing native-image..."
  "$GRAALVM_HOME/bin/gu" install native-image
fi

# Set environment variables
export PATH="$GRAALVM_HOME/bin:$PATH"
export JAVA_HOME="$GRAALVM_HOME"

echo "GraalVM environment set up successfully."
echo "GRAALVM_HOME: $GRAALVM_HOME"
echo "JAVA_HOME: $JAVA_HOME"
echo "PATH now includes: $GRAALVM_HOME/bin"

# Print GraalVM version
echo "GraalVM version:"
java -version

# Check if native-image is available
echo "Checking native-image installation:"
native-image --version
```

### Native Image Properties

Create a `native-image.properties` file in `src/main/resources/META-INF/native-image/`:

```properties
Args = --initialize-at-build-time=org.example,kotlin,ch.qos.logback,org.slf4j \
      --allow-incomplete-classpath \
      --report-unsupported-elements-at-runtime \
      -H:+ReportExceptionStackTraces \
      -H:+PrintClassInitialization \
      --enable-monitoring \
      -H:+AddAllCharsets
```

## Handling Reflection

GraalVM Native Image requires explicit configuration for reflection, resources, and JNI access.

### Reflection Configuration

Create a `reflect-config.json` file in `src/main/resources/META-INF/native-image/`:

```json
[
  {
    "name": "org.example.MainKt",
    "allDeclaredMethods": true,
    "allPublicMethods": true,
    "allDeclaredConstructors": true,
    "allPublicConstructors": true
  },
  {
    "name": "org.example.MyClass",
    "allDeclaredFields": true,
    "allPublicFields": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true,
    "allDeclaredConstructors": true,
    "allPublicConstructors": true
  }
]
```

### Resource Configuration

Create a `resource-config.json` file in `src/main/resources/META-INF/native-image/`:

```json
{
  "resources": {
    "includes": [
      {
        "pattern": "\\QMETA-INF/services/.*\\E"
      },
      {
        "pattern": "\\Qlogback.xml\\E"
      },
      {
        "pattern": "\\Qapplication.properties\\E"
      }
    ]
  },
  "bundles": []
}
```

### Programmatic Registration

You can also register classes for reflection programmatically using a GraalVM Feature:

```kotlin
package org.example

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection

class NativeImageSupport : Feature {
    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
        try {
            // Register classes for reflection
            registerClassForReflection("org.example.MyClass")
            registerClassForReflection("org.example.AnotherClass")
            
            println("NativeImageSupport: Successfully registered classes for reflection")
        } catch (e: Exception) {
            println("NativeImageSupport: Error registering classes for reflection: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun registerClassForReflection(className: String) {
        try {
            val clazz = Class.forName(className)
            RuntimeReflection.register(clazz)
            
            // Register constructors
            clazz.constructors.forEach { constructor ->
                RuntimeReflection.register(constructor)
            }
            
            // Register methods
            clazz.methods.forEach { method ->
                RuntimeReflection.register(method)
            }
            
            // Register fields
            clazz.fields.forEach { field ->
                RuntimeReflection.register(field)
            }
            
            // Register declared constructors
            clazz.declaredConstructors.forEach { constructor ->
                RuntimeReflection.register(constructor)
            }
            
            // Register declared methods
            clazz.declaredMethods.forEach { method ->
                RuntimeReflection.register(method)
            }
            
            // Register declared fields
            clazz.declaredFields.forEach { field ->
                RuntimeReflection.register(field)
            }
            
            println("NativeImageSupport: Registered $className for reflection")
        } catch (e: ClassNotFoundException) {
            println("NativeImageSupport: Class not found: $className")
        } catch (e: Exception) {
            println("NativeImageSupport: Error registering $className: ${e.message}")
        }
    }
}
```

## Class Initialization Strategy

GraalVM Native Image needs to know which classes should be initialized at build time versus runtime. This is crucial for correctness and performance.

### Build Time vs. Runtime Initialization

- **Build Time Initialization**: Classes initialized during native image generation. Faster at runtime but can cause issues with classes that depend on runtime state.
- **Runtime Initialization**: Classes initialized when the native image runs. Safer but slower at startup.

### Configuration in build.gradle.kts

```kotlin
graalvmNative {
    binaries {
        named("main") {
            // Initialize specific packages at build time
            buildArgs.add("--initialize-at-build-time=org.example")
            buildArgs.add("--initialize-at-build-time=kotlin")
            buildArgs.add("--initialize-at-build-time=ch.qos.logback")
            buildArgs.add("--initialize-at-build-time=org.slf4j")
            
            // Initialize specific classes at runtime
            buildArgs.add("--initialize-at-run-time=org.example.ClassWithRuntimeDependencies")
            buildArgs.add("--initialize-at-run-time=org.example.ClassUsingSecureRandom")
        }
    }
}
```

### Programmatic Configuration

You can also configure class initialization programmatically:

```kotlin
package org.example

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization

class ClassInitializationFeature : Feature {
    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
        try {
            // Register classes for runtime initialization
            RuntimeClassInitialization.initializeAtRunTime("org.example.ClassWithRuntimeDependencies")
            RuntimeClassInitialization.initializeAtRunTime("org.example.ClassUsingSecureRandom")
            
            // Register classes for build time initialization
            RuntimeClassInitialization.initializeAtBuildTime("org.example.SimpleClass")
            
            println("ClassInitializationFeature: Successfully configured class initialization")
        } catch (e: Exception) {
            println("ClassInitializationFeature: Error configuring class initialization: ${e.message}")
            e.printStackTrace()
        }
    }
}
```

## Dealing with Unsafe Operations

Some libraries use `sun.misc.Unsafe` or `jdk.internal.misc.Unsafe` for low-level operations, which can cause issues with GraalVM Native Image.

### Substitutions for Unsafe Operations

Create substitution classes to replace Unsafe operations:

```kotlin
package org.example

import com.oracle.svm.core.annotate.Alias
import com.oracle.svm.core.annotate.RecomputeFieldValue
import com.oracle.svm.core.annotate.TargetClass
import org.msgpack.core.buffer.MessageBuffer

@TargetClass(MessageBuffer::class)
final class MessageBufferSubstitute {
    /**
     * Substitute for the ARRAY_BYTE_BASE_OFFSET field in MessageBuffer.
     * This field is computed using Unsafe.arrayBaseOffset(byte[].class) in the original class.
     */
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayBaseOffset, declClass = ByteArray::class)
    internal val ARRAY_BYTE_BASE_OFFSET: Long = 0L

    /**
     * Substitute for the ARRAY_BYTE_INDEX_SCALE field in MessageBuffer.
     * This field is computed using Unsafe.arrayIndexScale(byte[].class) in the original class.
     */
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayIndexScale, declClass = ByteArray::class)
    internal val ARRAY_BYTE_INDEX_SCALE: Int = 0
}
```

### Kotlin Companion Object Issues

When working with Kotlin, be aware that GraalVM substitution annotations cannot be applied directly to companion objects:

```kotlin
// INCORRECT - Will cause compilation errors
@TargetClass(MessageBuffer::class)
final class MessageBufferSubstitute {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayBaseOffset, declClass = ByteArray::class)
    private companion object {
        @JvmField
        val ARRAY_BYTE_BASE_OFFSET: Long = 0L
    }
}

// CORRECT - Apply annotations to fields directly
@TargetClass(MessageBuffer::class)
final class MessageBufferSubstitute {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayBaseOffset, declClass = ByteArray::class)
    internal val ARRAY_BYTE_BASE_OFFSET: Long = 0L
}
```

## Build Process

### Using Gradle

```bash
# Set up GraalVM environment
source ./setup-graalvm.sh

# Generate reflection configuration with the agent
./gradlew -Pagent run --args="your-app-arguments"

# Copy the generated metadata to resources
./gradlew metadataCopy

# Build the native image
./gradlew nativeCompile
```

### Using Maven

```bash
# Set up GraalVM environment
source ./setup-graalvm.sh

# Generate reflection configuration with the agent
mvn -Pagent exec:exec

# Build the native image
mvn package -Pnative
```

### Manual Build

```bash
# Build the application JAR
./gradlew build

# Build the native image manually
native-image --fallback \
  --enable-url-protocols=https \
  -H:ReflectionConfigurationFiles=src/main/resources/META-INF/native-image/reflect-config.json \
  -H:ResourceConfigurationFiles=src/main/resources/META-INF/native-image/resource-config.json \
  -cp build/libs/my-app.jar:build/libs/* \
  org.example.MainKt \
  my-cli-app
```

## Common Issues and Solutions

### 1. "Cannot query the value of property 'javaLauncher'"

**Error:**
```
> Task :nativeCompile FAILED
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':nativeCompile'.
> Cannot query the value of property 'javaLauncher' because it has no value available.
```

**Solution:**
- Make sure GraalVM is properly installed and `GRAALVM_HOME` is set
- Run the `setup-graalvm.sh` script to configure your environment
- Make sure the toolchain detection is enabled in the build.gradle.kts file

### 2. "Java version mismatch"

**Error:**
```
Error: Unable to load 'org.example.MainKt' due to a Java version mismatch.
Root cause: java.lang.UnsupportedClassVersionError: org/example/MainKt has been compiled by a more recent version of the Java Runtime (class file version 67.0), this version of the Java Runtime only recognizes class file versions up to 65.0
```

**Solution:**
1. Update your Kotlin JVM toolchain to match your GraalVM Java version:
   ```kotlin
   kotlin {
       jvmToolchain(21) // Change this to match your GraalVM Java version
   }
   ```
2. Clean and rebuild your project:
   ```bash
   ./gradlew clean build
   ```

### 3. "The reflection configuration file does not exist"

**Error:**
```
Error: The reflection configuration file "/path/to/reflect-config.json" does not exist.
```

**Solution:**
1. Make sure the reflection configuration file exists at the specified path
2. Update your build.gradle.kts to use absolute paths for configuration files
3. Generate the configuration files using the agent:
   ```bash
   ./gradlew -Pagent run --args="your-app-arguments"
   ./gradlew metadataCopy
   ```

### 4. "Class not found during image building"

**Error:**
```
Error: Class not found during image building: org.example.MyClass
```

**Solution:**
- Add the missing class to the reflection configuration file
- Run the application with the agent to generate proper reflection configuration

### 5. "Detected an instance of Random/SplittableRandom class in the image heap"

**Error:**
```
Error: Detected an instance of Random/SplittableRandom class in the image heap.
```

**Solution:**
- Initialize classes using SecureRandom at runtime:
  ```kotlin
  buildArgs.add("--initialize-at-run-time=org.example.ClassUsingSecureRandom")
  ```

### 6. Kotlin Companion Object Annotation Issues

**Error:**
```
This annotation is not applicable to target 'companion object'. Applicable targets: field, constructor, function, getter, setter, expression
```

**Solution:**
- Restructure your code to apply annotations to fields directly instead of a companion object

## Best Practices

1. **Start Small**: Begin with a minimal application and add dependencies incrementally
2. **Use the Agent**: Generate reflection configuration using the GraalVM agent
3. **Separate Build-time and Runtime Components**: Keep static configuration in build-time components
4. **Avoid Unsafe Operations**: Use safe alternatives when possible
5. **Test Thoroughly**: Test your native image on all target platforms
6. **Monitor Memory Usage**: Native images have different memory characteristics than JVM applications
7. **Use Fallback Mode During Development**: Enable fallback mode to get a working build while solving issues
8. **Keep Configuration Files Updated**: Regenerate configuration files when adding new dependencies
9. **Use Feature Mechanism**: For complex substitutions, use the Feature mechanism rather than annotation-based substitutions
10. **Create Custom Initializers**: For classes with complex runtime behavior

## Performance Considerations

1. **Startup Time**: Native images start much faster than JVM applications
2. **Memory Footprint**: Native images use less memory but have different garbage collection characteristics
3. **Peak Performance**: Native images provide peak performance immediately without warmup
4. **Binary Size**: Native images are larger than JARs but don't require a JVM
5. **Build Time**: Native image compilation takes longer than regular Java/Kotlin compilation

## Resources

- [GraalVM Native Image Documentation](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Gradle Plugin for GraalVM Native Image](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)
- [Maven Plugin for GraalVM Native Image](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html)
- [Class Initialization in Native Image](https://www.graalvm.org/reference-manual/native-image/ClassInitialization/)
- [Reflection in Native Image](https://www.graalvm.org/reference-manual/native-image/Reflection/)
- [Substitutions in Native Image](https://www.graalvm.org/reference-manual/native-image/Substitutions/)
- [Reachability Metadata Repository](https://github.com/oracle/graalvm-reachability-metadata)