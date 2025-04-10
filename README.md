# Ably CLI Tool

A command-line tool for connecting to Ably channels and reading messages in real-time.

## Features

- Connect to any Ably channel using your API key
- Subscribe to all events or filter by a specific event name
- Display messages in a readable format with timestamps
- Control verbosity of Ably logs
- Native executable support via GraalVM Native Image

## Building

### Standard JVM Build

```bash
./gradlew build
```

### Native Image Build

To build a standalone native executable:

1. Set up GraalVM environment:
   ```bash
   # Make the setup script executable
   chmod +x setup-graalvm.sh
   
   # Set up GraalVM environment
   source ./setup-graalvm.sh
   ```
> **Note**: You must set the `GRAALVM_HOME` environment variable to point to your GraalVM installation directory before running the build scripts. The native image build will fail if this environment variable is not set correctly.

2. Build the native image:
   ```bash
   # Using the provided script (recommended)
   chmod +x build-native-image.sh
   ./build-native-image.sh
   
   # Or using Gradle directly
   ./gradlew nativeCompile
   ```

3. The native executable will be created at `build/native/nativeCompile/ably-cli`

## Usage

### Running with Gradle

```bash
./gradlew run --args="-k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME"
```

### Running the Native Executable

After building the native image, you can run it directly:

```bash
./build/native/nativeCompile/ably-cli -k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME
```

The native executable starts instantly and has a smaller memory footprint compared to the JVM version.

### Command Line Options

- `-k, --api-key`: Your Ably API key (required)
- `-c, --channel`: The Ably channel name to connect to (required)
- `-e, --event`: Specific event name to subscribe to (optional, defaults to all events)
- `-q, --quiet`: Disable verbose Ably logs (optional, defaults to false)
- `-d, --debug`: Show debug information including raw message structure (optional, defaults to false)

### Examples

Connect to a channel and listen to all events:
```bash
# Using Gradle
./gradlew run --args="-k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME"

# Using native executable
./build/native/nativeCompile/ably-cli -k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME
```

Connect to a channel and listen to a specific event:
```bash
# Using Gradle
./gradlew run --args="-k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME -e my-event"

# Using native executable
./build/native/nativeCompile/ably-cli -k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME -e my-event
```

Connect to a channel with minimal logging:
```bash
# Using Gradle
./gradlew run --args="-k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME -q"

# Using native executable
./build/native/nativeCompile/ably-cli -k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME -q
```

Connect to a channel with debug information:
```bash
# Using Gradle
./gradlew run --args="-k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME -d"

# Using native executable
./build/native/nativeCompile/ably-cli -k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME -d
```

## Output

The tool will display messages in the following format:

```
────────────────────────────────────────────────────────────────────────────────
Timestamp: 2023-04-15T12:34:56.789
Event: event-name
Client ID: client-id
Connection ID: connection-id
Data: {"key": "value"}
────────────────────────────────────────────────────────────────────────────────
```

Press Ctrl+C to exit the application.

## Native Image Benefits

The native executable offers several advantages:

- **Faster startup time**: Starts in milliseconds instead of seconds
- **Lower memory footprint**: Uses significantly less memory than the JVM version
- **No warmup time**: Delivers peak performance immediately
- **Self-contained**: No need to install a JVM on the target machine

For more details on GraalVM Native Image, see the [GraalVM_Native_Image_Guide.md](GraalVM_Native_Image_Guide.md) file.