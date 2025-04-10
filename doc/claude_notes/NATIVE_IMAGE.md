# Building and Using GraalVM Native Image for Ably CLI

This document explains how to build a native executable of the Ably CLI application using GraalVM Native Image.

## Prerequisites

1. Install GraalVM (version 22.3.0 or later recommended)
   - Download from [GraalVM website](https://www.graalvm.org/downloads/)
   - Set the `GRAALVM_HOME` environment variable to point to your GraalVM installation
   - Add `$GRAALVM_HOME/bin` to your PATH

2. Install the Native Image tool:
   ```
   $GRAALVM_HOME/bin/gu install native-image
   ```

3. Install required system dependencies:
   - On Linux: `sudo apt-get install build-essential zlib1g-dev` (Ubuntu) or `sudo yum install gcc glibc-devel zlib-devel` (Oracle Linux)
   - On macOS: `xcode-select --install`
   - On Windows: Install Visual Studio 2022 with the Windows 11 SDK

## Building the Native Image

### Option 1: Using the Provided Scripts (Recommended)

We've provided scripts to simplify the build process:

1. Set up the GraalVM environment:
   ```
   chmod +x setup-graalvm.sh
   source ./setup-graalvm.sh
   ```

2. Build the native image:
   ```
   chmod +x build-native-image.sh
   ./build-native-image.sh
   ```

   If you want to generate metadata using the agent (recommended for the first time):
   ```
   ./build-native-image.sh --with-agent
   ```

### Option 2: Using Gradle Plugin Directly

The project is configured with the GraalVM Native Image Gradle plugin. To build the native executable:

1. Make sure GraalVM is properly set up:
   ```
   export JAVA_HOME=$GRAALVM_HOME
   export PATH=$GRAALVM_HOME/bin:$PATH
   ```

2. Generate the reflection and resource configuration files by running the application with the agent:
   ```
   ./gradlew -Pagent run --args="-k YOUR_API_KEY -c YOUR_CHANNEL_NAME"
   ```

3. Copy the generated metadata to the resources directory:
   ```
   ./gradlew metadataCopy
   ```

4. Build the native executable:
   ```
   ./gradlew nativeCompile
   ```

5. The native executable will be created in `build/native/nativeCompile/ably-cli`

### Option 3: Manual Native Image Build

If you prefer to build the native image manually:

1. Build the application JAR:
   ```
   ./gradlew build
   ```

2. Use the `native-image` command directly:
   ```
   native-image --fallback \
     --enable-url-protocols=https \
     -H:ReflectionConfigurationFiles=src/main/resources/META-INF/native-image/reflect-config.json \
     -H:ResourceConfigurationFiles=src/main/resources/META-INF/native-image/resource-config.json \
     -cp build/libs/ktcli-1.0-SNAPSHOT.jar:build/libs/* \
     org.example.MainKt \
     ably-cli
   ```

## Running the Native Executable

Once built, you can run the native executable directly:

```
./build/native/nativeCompile/ably-cli -k YOUR_API_KEY -c YOUR_CHANNEL_NAME
```

Or if you built it manually:

```
./ably-cli -k YOUR_API_KEY -c YOUR_CHANNEL_NAME
```

## Troubleshooting

### Common Issues

1. **"Cannot query the value of property 'javaLauncher' because it has no value available"**:
   - Make sure GraalVM is properly installed and `GRAALVM_HOME` is set
   - Run the `setup-graalvm.sh` script to configure your environment
   - Make sure the toolchain detection is enabled in the build.gradle.kts file

2. **"Class not found during image building"**:
   - This usually indicates a reflection issue. Run the application with the agent to generate proper reflection configuration:
     ```
     ./gradlew -Pagent run --args="-k YOUR_API_KEY -c YOUR_CHANNEL_NAME"
     ./gradlew metadataCopy
     ```

3. **"Resource not found"**:
   - Add the missing resource pattern to the `resource-config.json` file

### Debugging Tips

1. Try building with debug information:
   ```
   ./gradlew nativeCompile --debug-native
   ```

2. For more detailed logs during native image generation:
   ```
   ./gradlew nativeCompile --verbose
   ```

3. If you're still having issues, try building with the fallback mode:
   ```
   ./gradlew nativeCompile -Pfallback
   ```

4. Check the GraalVM version:
   ```
   $GRAALVM_HOME/bin/java -version
   ```

5. Verify that native-image is installed:
   ```
   $GRAALVM_HOME/bin/native-image --version
   ```

## Performance Comparison

Native images offer several advantages over running the application on the JVM:

1. **Faster startup time**: Native images start in milliseconds, compared to seconds for JVM applications
2. **Lower memory footprint**: Native images use significantly less memory
3. **No warmup time**: Native images deliver peak performance immediately
4. **Self-contained**: No need to install a JVM on the target machine

## Additional Resources

- [GraalVM Native Image Documentation](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Gradle Plugin for GraalVM Native Image](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)
- [Reachability Metadata Repository](https://github.com/oracle/graalvm-reachability-metadata)