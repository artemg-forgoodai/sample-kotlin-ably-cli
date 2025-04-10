# Building with GraalVM Native Image

This document explains how to build and run the Ably CLI application using GraalVM Native Image.

## Prerequisites

- GraalVM 21 or later
- Gradle 8.0 or later

## Building the Native Image

To build the native image, run:

```bash
./gradlew nativeCompile
```

This will create a native executable in the `build/native/nativeCompile` directory.

## Running the Native Image

After building, you can run the native executable directly:

```bash
./build/native/nativeCompile/ably-cli -k YOUR_API_KEY -c YOUR_CHANNEL_NAME
```

## Troubleshooting

If you encounter any issues with the native image, you can try the following:

1. Run the application with the GraalVM agent to generate configuration files:

```bash
./gradlew run
```

2. Check the generated configuration files in `src/main/resources/META-INF/native-image/`.

3. If you still encounter issues, try building with the `--verbose` flag:

```bash
./gradlew nativeCompile --info
```

## Configuration Files

The following configuration files are used by GraalVM Native Image:

- `reflect-config.json`: Specifies classes that need reflection support
- `resource-config.json`: Specifies resources that need to be included in the native image
- `jni-config.json`: Specifies JNI access patterns
- `native-image.properties`: Specifies additional build arguments

## Notes on MessagePack

The Ably SDK uses MessagePack for serialization. The MessagePack library uses reflection to load different implementations of `MessageBuffer` based on the platform. In the native image, we need to ensure that these classes are initialized at build time and available for reflection.

## Implementation Details

This project uses several approaches to ensure proper reflection support in the native image:

1. **Configuration Files**: The `reflect-config.json`, `resource-config.json`, and `jni-config.json` files specify which classes, resources, and JNI access patterns need to be included in the native image.

2. **MessagePackInitializer**: A custom initializer class that ensures the MessagePack classes are properly loaded at runtime.

3. **NativeImageSupport**: A GraalVM Feature implementation that registers the MessagePack classes for reflection at build time.

4. **Build Arguments**: The `--initialize-at-build-time` flags ensure that the MessagePack classes are properly initialized during the native image build.

## Common Issues

### ClassNotFoundException

If you encounter a `ClassNotFoundException` for a class that is loaded via reflection, you need to add it to the `reflect-config.json` file or register it programmatically using the `RuntimeReflection` API.

### ExceptionInInitializerError

If you encounter an `ExceptionInInitializerError`, it means that a class initialization failed. You may need to initialize the class at build time using the `--initialize-at-build-time` flag or defer initialization to runtime using the `--initialize-at-run-time` flag.