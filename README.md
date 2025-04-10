# Ably CLI Tool

A command-line tool for connecting to Ably channels and reading messages in real-time.

## Features

- Connect to any Ably channel using your API key
- Subscribe to all events or filter by a specific event name
- Display messages in a readable format with timestamps
- Control verbosity of Ably logs

## Building

```bash
./gradlew build
```

## Usage

```bash
./gradlew run --args="-k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME"
```

### Command Line Options

- `-k, --api-key`: Your Ably API key (required)
- `-c, --channel`: The Ably channel name to connect to (required)
- `-e, --event`: Specific event name to subscribe to (optional, defaults to all events)
- `-q, --quiet`: Disable verbose Ably logs (optional, defaults to false)
- `-d, --debug`: Show debug information including raw message structure (optional, defaults to false)

### Examples

Connect to a channel and listen to all events:
```bash
./gradlew run --args="-k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME"
```

Connect to a channel and listen to a specific event:
```bash
./gradlew run --args="-k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME -e my-event"
```

Connect to a channel with minimal logging:
```bash
./gradlew run --args="-k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME -q"
```

Connect to a channel with debug information:
```bash
./gradlew run --args="-k YOUR_ABLY_API_KEY -c YOUR_CHANNEL_NAME -d"
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