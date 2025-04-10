# Solutions for Common GraalVM Native Image Errors

## Error 5: Kotlin Companion Object Annotation Issues with GraalVM 21

If you're encountering errors like:

```
e: file:///Users/brannt/forgoodai/ktcli/src/main/kotlin/org/example/MessageBufferSubstitutions.kt:19:5 This annotation is not applicable to target 'companion object'. Applicable targets: field, constructor, function, getter, setter, expression
e: file:///Users/brannt/forgoodai/ktcli/src/main/kotlin/org/example/MessageBufferSubstitutions.kt:20:5 This annotation is not applicable to target 'companion object'. Applicable targets: field, expression
```

This error occurs because GraalVM substitution annotations like `@Alias` and `@RecomputeFieldValue` cannot be applied directly to Kotlin companion objects.

### Solution for Kotlin Companion Object Annotation Issues

1. Restructure your `MessageBufferSubstitutions.kt` to apply annotations to fields directly instead of a companion object:

   ```kotlin
   @TargetClass(MessageBuffer::class)
   final class MessageBufferSubstitute {
       /**
        * Substitute for the ARRAY_BYTE_BASE_OFFSET field in MessageBuffer.
        */
       @Alias
       @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayBaseOffset, declClass = ByteArray::class)
       internal val ARRAY_BYTE_BASE_OFFSET: Long = 0L
   
       /**
        * Substitute for the ARRAY_BYTE_INDEX_SCALE field in MessageBuffer.
        */
       @Alias
       @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayIndexScale, declClass = ByteArray::class)
       internal val ARRAY_BYTE_INDEX_SCALE: Int = 0
   }
   ```

2. When dealing with more complex class initialization issues during GraalVM native image compilation, add proper initialization directives:

   ```kotlin
   // In build.gradle.kts
   // Initialize specific packages at build time
   buildArgs.add("--initialize-at-build-time=org.example,kotlin.jvm.internal,kotlin.reflect,org.slf4j,ch.qos.logback")
   buildArgs.add("--initialize-at-build-time=kotlin.jvm.internal.PropertyReference1Impl")
   
   // Force build to continue even with issues
   buildArgs.add("--no-fallback")
   buildArgs.add("-Dio.netty.noUnsafe=true")
   buildArgs.add("-H:-UseServiceLoaderFeature")
   ```

3. If you encounter errors with the `SubstitutionFiles` option in GraalVM 21, remove it from your build file as it's no longer supported:

   ```kotlin
   // Remove this line
   buildArgs.add("-H:SubstitutionFiles=${projectDir}/src/main/resources/META-INF/native-image/substitutions.json")
   
   // Use the Feature mechanism instead via org.graalvm.nativeimage.hosted.Feature
   ```

## Error 1: "Cannot query the value of property 'javaLauncher'"

If you're encountering the error:

```
> Task :nativeCompile FAILED
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':nativeCompile'.
> Cannot query the value of property 'javaLauncher' because it has no value available.
```

This error occurs because the Gradle plugin for GraalVM Native Image can't find a Java launcher to use for building the native image. This typically happens when the plugin can't locate a GraalVM installation.

## Error 2: "Java version mismatch"

If you're encountering an error like:

```
Error: Unable to load 'org.example.MainKt' due to a Java version mismatch.
Please take one of the following actions:
 1) Recompile the source files for your application using Java 21, then try running native-image again
 2) Use a version of native-image corresponding to the version of Java with which you compiled the source files for your application
Root cause: java.lang.UnsupportedClassVersionError: org/example/MainKt has been compiled by a more recent version of the Java Runtime (class file version 67.0), this version of the Java Runtime only recognizes class file versions up to 65.0
```

This error occurs because your code was compiled with a newer Java version (e.g., Java 23) than the one used by your GraalVM installation (e.g., Java 21). The class file version 67.0 corresponds to Java 23, while 65.0 corresponds to Java 21.

### Solution for Java Version Mismatch

1. Update your Kotlin JVM toolchain in build.gradle.kts to match your GraalVM Java version:

   ```kotlin
   kotlin {
       jvmToolchain(21) // Change this to match your GraalVM Java version
   }
   ```

2. Clean and rebuild your project:

   ```bash
   # Run the provided script
   chmod +x rebuild-with-java21.sh
   ./rebuild-with-java21.sh

   # Or manually
   ./gradlew clean build
   ```

3. Try building the native image again:

   ```bash
   ./gradlew nativeCompile
   ```

### Alternative Solutions

If you prefer to keep using Java 23 for development:

1. **Option 1**: Upgrade your GraalVM to a version that supports Java 23
   - Download and install GraalVM based on JDK 23
   - Update your GRAALVM_HOME environment variable

2. **Option 2**: Configure Gradle to use different JDKs for compilation and native image building
   - This is more complex and requires careful configuration of Gradle toolchains

3. **Option 3**: Use the `--ignore-version-check` flag with native-image (not recommended)
   - This might lead to runtime issues
   - Add to your build.gradle.kts:
     ```kotlin
     graalvmNative {
         binaries {
             named("main") {
                 buildArgs.add("--ignore-version-check")
             }
         }
     }
     ```

## General Setup for GraalVM Native Image

Before attempting to solve specific errors, make sure you have GraalVM set up correctly:

1. Set up GraalVM environment variables:

   ```bash
   # Set GRAALVM_HOME to your GraalVM installation directory
   export GRAALVM_HOME=/path/to/graalvm

   # Set JAVA_HOME to the same directory
   export JAVA_HOME=$GRAALVM_HOME

   # Add GraalVM binaries to your PATH
   export PATH=$GRAALVM_HOME/bin:$PATH
   ```

2. Run the setup script we've provided:

   ```bash
   chmod +x setup-graalvm.sh
   source ./setup-graalvm.sh
   ```

3. Verify your GraalVM installation:

   ```bash
   # Check Java version
   java -version

   # Check native-image availability
   native-image --version
   ```

## Error 3: "The reflection configuration file does not exist"

If you're encountering an error like:

```
Error: The reflection configuration file "/path/to/reflect-config.json" does not exist.
```

This error occurs because GraalVM can't find the reflection configuration file at the specified path. This typically happens when the path to the configuration file is incorrect or the file doesn't exist.

### Solution for Missing Reflection Configuration File

1. Make sure the reflection configuration file exists at the specified path:

   ```bash
   # Check if the file exists
   ls -la src/main/resources/META-INF/native-image/reflect-config.json
   ```

2. Update your build.gradle.kts to use absolute paths for configuration files:

   ```kotlin
   graalvmNative {
       binaries {
           named("main") {
               // Use absolute paths to configuration files
               buildArgs.add("-H:ReflectionConfigurationFiles=${projectDir}/src/main/resources/META-INF/native-image/reflect-config.json")
               buildArgs.add("-H:ResourceConfigurationFiles=${projectDir}/src/main/resources/META-INF/native-image/resource-config.json")
           }
       }
   }
   ```

3. If you're still having issues, try generating the configuration files using the agent:

   ```bash
   # Run with the agent to generate configuration files
   ./gradlew -Pagent run --args="-k YOUR_API_KEY -c YOUR_CHANNEL_NAME"

   # Copy the generated files to the resources directory
   ./gradlew metadataCopy
   ```

## Alternative Solutions

If the above doesn't work, try one of these alternatives:

### Option 1: Use the GRAALVM_HOME environment variable

The plugin can use the `GRAALVM_HOME` environment variable to find GraalVM. Make sure it's set correctly:

```bash
export GRAALVM_HOME=/path/to/graalvm
./gradlew nativeCompile
```

### Option 2: Specify the Java home in gradle.properties

Create or edit the `gradle.properties` file in the project root:

```properties
org.gradle.java.home=/path/to/graalvm
```

### Option 3: Use the command line

You can specify the Java home on the command line:

```bash
./gradlew -Dorg.gradle.java.home=/path/to/graalvm nativeCompile
```

### Option 4: Manually configure the toolchain in build.gradle.kts

If you know the exact version and vendor of your GraalVM installation, you can configure it explicitly:

```kotlin
graalvmNative {
    binaries {
        named("main") {
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(17)) // Change to your Java version
                vendor.set(JvmVendorSpec.matching("GraalVM Community")) // Or "Oracle Corporation" for Oracle GraalVM
            })
        }
    }
}
```

## Verifying Your GraalVM Installation

To verify that GraalVM is correctly installed and configured:

```bash
# Check Java version - should show GraalVM
$GRAALVM_HOME/bin/java -version

# Check if native-image is installed
$GRAALVM_HOME/bin/native-image --version
```

If native-image is not installed, install it:

```bash
$GRAALVM_HOME/bin/gu install native-image
```

## Error 4: "MessageTypeException: Expected Map, but got Integer" and "RecomputeFieldValue.ArrayBaseOffset automatic substitution failed"

If you're encountering errors like:

```
org.msgpack.core.MessageTypeException: Expected Map, but got Integer (04)
        at org.msgpack.core.MessageUnpacker.unexpected(MessageUnpacker.java:596)
        at org.msgpack.core.MessageUnpacker.unpackMapHeader(MessageUnpacker.java:1380)
        at io.ably.lib.types.ProtocolMessage.readMsgpack(ProtocolMessage.java:170)
        at io.ably.lib.types.ProtocolMessage.fromMsgpack(ProtocolMessage.java:238)
        at io.ably.lib.types.ProtocolSerializer.readMsgpack(ProtocolSerializer.java:21)
        at io.ably.lib.transport.WebSocketTransport$WsClient.onMessage(WebSocketTransport.java:192)
```

And during compilation:

```
Warning: RecomputeFieldValue.ArrayBaseOffset automatic substitution failed. The automatic substitution registration was attempted because a call to jdk.internal.misc.Unsafe.arrayBaseOffset(Class) was detected in the static initializer of org.msgpack.core.buffer.MessageBuffer. Detailed failure reason(s): Could not determine the field where the value produced by the call to jdk.internal.misc.Unsafe.arrayBaseOffset(Class) for the array base offset computation is stored. The call is not directly followed by a field store or by a sign extend node followed directly by a field store.
```

These errors occur because:
1. The MessagePack serialization/deserialization classes are not properly registered for reflection in the native image
2. The MessageBuffer class uses Unsafe operations that GraalVM cannot automatically handle during native image generation

### Solution for MessagePack Serialization Issues

1. Add the missing Ably SDK classes to the reflection configuration file (`reflect-config.json`):

   ```json
   {
     "name":"io.ably.lib.types.ProtocolMessage",
     "allDeclaredFields":true,
     "allPublicFields":true,
     "allDeclaredMethods":true,
     "allPublicMethods":true,
     "allDeclaredConstructors":true,
     "allPublicConstructors":true
   },
   {
     "name":"io.ably.lib.types.ProtocolSerializer",
     "allDeclaredFields":true,
     "allPublicFields":true,
     "allDeclaredMethods":true,
     "allPublicMethods":true,
     "allDeclaredConstructors":true,
     "allPublicConstructors":true
   }
   ```

2. Update the `NativeImageSupport` class to register these classes for reflection:

   ```kotlin
   // Register Ably SDK classes for reflection
   registerClassForReflection("io.ably.lib.types.ProtocolMessage")
   registerClassForReflection("io.ably.lib.types.ProtocolSerializer")
   registerClassForReflection("io.ably.lib.transport.WebSocketTransport")
   registerClassForReflection("io.ably.lib.transport.WebSocketTransport\$WsClient")
   ```

3. Add the MessagePack and Ably classes to be initialized at build time in `build.gradle.kts`:

   ```kotlin
   buildArgs.add("--initialize-at-build-time=org.msgpack.core.MessageUnpacker")
   buildArgs.add("--initialize-at-build-time=org.msgpack.core.MessageFormat")
   buildArgs.add("--initialize-at-build-time=io.ably.lib.types.ProtocolMessage")
   buildArgs.add("--initialize-at-build-time=io.ably.lib.types.ProtocolSerializer")
   ```

4. Enhance the `MessagePackInitializer` to properly initialize all MessagePack classes:

   ```kotlin
   // Create a simple MessagePack object to ensure all classes are loaded
   val packer = MessagePack.newDefaultBufferPacker()

   // Pack different types of data to ensure all format handlers are initialized
   packer.packInt(42)
   packer.packMapHeader(2)
   packer.packString("key1")
   packer.packString("value1")
   packer.packString("key2")
   packer.packInt(123)

   // Get the byte array and create an unpacker to test unpacking
   val bytes = packer.toByteArray()
   packer.close()

   // Test unpacking to ensure unpacker is properly initialized
   val unpacker = MessagePack.newDefaultUnpacker(bytes)
   val value1 = unpacker.unpackInt()
   val mapSize = unpacker.unpackMapHeader()
   val key1 = unpacker.unpackString()
   val val1 = unpacker.unpackString()
   val key2 = unpacker.unpackString()
   val val2 = unpacker.unpackInt()
   unpacker.close()
   ```

5. Create a substitution class for MessageBuffer to handle the Unsafe operations:

   ```kotlin
   @TargetClass(MessageBuffer::class)
   final class MessageBufferSubstitute {

       /**
        * Substitute for the ARRAY_BYTE_BASE_OFFSET field in MessageBuffer.
        * This field is computed using Unsafe.arrayBaseOffset(byte[].class) in the original class.
        */
       @Alias
       @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayBaseOffset, declClass = ByteArray::class)
       private static val ARRAY_BYTE_BASE_OFFSET: Long = 0L

       /**
        * Substitute for the ARRAY_BYTE_INDEX_SCALE field in MessageBuffer.
        * This field is computed using Unsafe.arrayIndexScale(byte[].class) in the original class.
        */
       @Alias
       @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayIndexScale, declClass = ByteArray::class)
       private static val ARRAY_BYTE_INDEX_SCALE: Int = 0
   }
   ```

6. Update the build.gradle.kts file to use package-level initialization and add more detailed error reporting:

   ```kotlin
   // Initialize classes at build time (package level)
   buildArgs.add("--initialize-at-build-time=org.msgpack")
   buildArgs.add("--initialize-at-build-time=io.ably.lib.types")

   // Add substitution for MessageBuffer
   buildArgs.add("-H:ReflectionConfigurationFiles=${projectDir}/src/main/resources/META-INF/native-image/reflect-config.json")
   buildArgs.add("-H:ResourceConfigurationFiles=${projectDir}/src/main/resources/META-INF/native-image/resource-config.json")

   // Enable more detailed error reporting
   buildArgs.add("-H:+ReportExceptionStackTraces")
   buildArgs.add("-H:+PrintClassInitialization")

   // Add unsafe access
   buildArgs.add("--allow-incomplete-classpath")
   buildArgs.add("--report-unsupported-elements-at-runtime")

   // Add specific options for MessagePack
   buildArgs.add("-H:+AddAllCharsets")
   ```

7. Update the native-image.properties file with the same options:

   ```properties
   Args = --initialize-at-build-time=org.msgpack,io.ably.lib.types \
         --allow-incomplete-classpath \
         --report-unsupported-elements-at-runtime \
         -H:+ReportExceptionStackTraces \
         -H:+PrintClassInitialization \
         -H:+AddAllCharsets
   ```

8. Rebuild the native image:

   ```bash
   ./gradlew clean nativeCompile
   ```

## Additional Resources

For more detailed information, please refer to:
- The `NATIVE_IMAGE.md` file in this project
- The [GraalVM Native Image documentation](https://www.graalvm.org/latest/reference-manual/native-image/)
- The [Gradle plugin for GraalVM Native Image documentation](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)
- The [MessagePack for Java documentation](https://github.com/msgpack/msgpack-java)