# GraalVM Native Image Guide for Kotlin Projects

This guide provides solutions for common issues when compiling a Kotlin application to a native binary using GraalVM Native Image, especially with libraries that use reflection or unsafe operations.

## Issue 1: Kotlin Companion Object Annotation Issues

**Error:**
```
This annotation is not applicable to target 'companion object'. Applicable targets: field, constructor, function, getter, setter, expression
```

**Solution:**

GraalVM substitution annotations like `@Alias` and `@RecomputeFieldValue` cannot be applied directly to Kotlin companion objects. Instead of:

```kotlin
@TargetClass(MessageBuffer::class)
final class MessageBufferSubstitute {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayBaseOffset, declClass = ByteArray::class)
    private companion object {
        @JvmField
        val ARRAY_BYTE_BASE_OFFSET: Long = 0L
    }
}
```

Use this approach:

```kotlin
@TargetClass(MessageBuffer::class)
final class MessageBufferSubstitute {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayBaseOffset, declClass = ByteArray::class)
    internal val ARRAY_BYTE_BASE_OFFSET: Long = 0L
}
```

## Issue 2: Class Initialization Management

Native image compilation requires careful management of which classes are initialized at build time vs. runtime.

**Error:**
```
Error: An object of type 'kotlin.jvm.internal.PropertyReference1Impl' was found in the image heap. This type, however, is marked for initialization at image run time...
```

**Solution:**

Add specific class initialization directives in `build.gradle.kts`:

```kotlin
// Initialize Kotlin reflection classes at build time
buildArgs.add("--initialize-at-build-time=kotlin.jvm.internal.PropertyReference1Impl")
buildArgs.add("--initialize-at-build-time=kotlin.jvm.internal.Reflection")

// Initialize problematic classes at runtime
buildArgs.add("--initialize-at-run-time=org.msgpack.core.buffer.DirectBufferAccess")
```

## Issue 3: Deprecated Substitution Files Option

**Error:**
```
Error: Could not find option 'SubstitutionFiles'
```

**Solution:**

Remove the SubstitutionFiles option:
```kotlin
// Remove this line
buildArgs.add("-H:SubstitutionFiles=${projectDir}/src/main/resources/META-INF/native-image/substitutions.json")

// Use the Feature mechanism instead
// Add a class that implements org.graalvm.nativeimage.hosted.Feature
```

## Issue 4: Random/SecureRandom in Image Heap

**Error:**
```
Error: Detected an instance of Random/SplittableRandom class in the image heap.
```

**Solution:**

Initialize classes using SecureRandom at runtime:
```kotlin
buildArgs.add("--initialize-at-run-time=io.ably.lib.util.Crypto")
```

## Issue 5: VCDiff Library Initialization

**Error:**
```
Error: An object of type 'com.davidehrmann.vcdiff.util.ZeroInitializedAdler32' was found in the image heap.
```

**Solution:**

Either initialize at build time:
```kotlin
buildArgs.add("--initialize-at-build-time=com.davidehrmann.vcdiff.util.ZeroInitializedAdler32")
```

Or mark the classes using it for runtime initialization:
```kotlin
buildArgs.add("--initialize-at-run-time=com.davidehrmann.vcdiff")
```

## Complete Configuration Example

Here's a complete example for `build.gradle.kts`:

```kotlin
graalvmNative {
    binaries {
        named("main") {
            imageName.set("my-app")
            mainClass.set("org.example.MainKt")
            debug.set(false)
            verbose.set(true)
            
            // Initialize specific classes at build time
            buildArgs.add("--initialize-at-build-time=org.example")
            buildArgs.add("--initialize-at-build-time=kotlin.jvm.internal.PropertyReference1Impl")
            buildArgs.add("--initialize-at-build-time=kotlin.jvm.internal.Reflection")
            buildArgs.add("--initialize-at-build-time=com.davidehrmann.vcdiff.util.ZeroInitializedAdler32")
            
            // Initialize problematic classes at runtime
            buildArgs.add("--initialize-at-run-time=org.msgpack.core.buffer.DirectBufferAccess")
            buildArgs.add("--initialize-at-run-time=io.ably.lib.util.Crypto")
            
            // Force Netty to avoid Unsafe operations
            buildArgs.add("-Dio.netty.noUnsafe=true")
            
            // Add configuration files
            buildArgs.add("-H:ReflectionConfigurationFiles=${projectDir}/src/main/resources/META-INF/native-image/reflect-config.json")
            buildArgs.add("-H:ResourceConfigurationFiles=${projectDir}/src/main/resources/META-INF/native-image/resource-config.json")
            
            // Add detailed error reporting
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("-H:+PrintClassInitialization")
        }
    }
}
```

## Debugging Initialization Issues

When you encounter new initialization issues:

1. **Identify the problematic classes**:
   ```
   ./gradlew nativeCompile --info
   ```

2. **Trace class initialization**:
   ```
   buildArgs.add("--trace-class-initialization=com.example.ProblemClass")
   ```

3. **Try the fallback approach** when you need a working build while you solve the initialization issues:
   ```kotlin
   fallback.set(true)
   buildArgs.add("-H:+AllowDeprecatedInitializeAllClassesAtBuildTime")
   ```

4. **Check initialization report** in `build/native/nativeCompile/reports/class_initialization_report_*.csv`

## Best Practices

1. **Start small** - Begin with a minimal application and add dependencies incrementally
2. **Use Feature mechanism** for complex substitutions rather than annotation-based substitutions
3. **Create custom initializers** for classes with complex runtime behavior
4. **Run GraalVM agent** to generate reflection configuration
5. **Separate build-time and runtime components** - Keep static configuration in build-time components

## Further Resources

- [GraalVM Native Image Documentation](https://www.graalvm.org/reference-manual/native-image/)
- [Class Initialization in Native Image](https://www.graalvm.org/reference-manual/native-image/ClassInitialization/)
- [Reflection in Native Image](https://www.graalvm.org/reference-manual/native-image/Reflection/)
- [Substitutions in Native Image](https://www.graalvm.org/reference-manual/native-image/Substitutions/)