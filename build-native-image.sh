#!/bin/bash

# Script to build a GraalVM native image for the Ably CLI application

set -e

echo "Building Ably CLI native image..."

# Step 0: Set up GraalVM environment
echo "Step 0: Setting up GraalVM environment..."
if [ -f "./setup-graalvm.sh" ]; then
  chmod +x ./setup-graalvm.sh
  source ./setup-graalvm.sh
else
  echo "Warning: setup-graalvm.sh not found. Make sure GRAALVM_HOME is set correctly."
  if [ -z "$GRAALVM_HOME" ]; then
    echo "GRAALVM_HOME is not set. Please set it to your GraalVM installation directory."
    echo "For example:"
    echo "  export GRAALVM_HOME=/path/to/graalvm"
    exit 1
  fi
fi

# Step 1: Clean and build the project
echo "Step 1: Cleaning and building the project..."
./gradlew clean build

# Step 2: Generate metadata using the agent (optional - only if you need to update the metadata)
if [ "$1" == "--with-agent" ]; then
  echo "Step 2: Generating metadata using the agent..."
  echo "Please provide your Ably API key and channel name when prompted."
  read -p "Ably API Key: " API_KEY
  read -p "Channel Name: " CHANNEL_NAME

  ./gradlew -Pagent run --args="-k $API_KEY -c $CHANNEL_NAME"

  echo "Step 3: Copying metadata to resources directory..."
  ./gradlew metadataCopy
fi

# Step 4: Verify configuration files exist
echo "Step 4: Verifying configuration files..."
CONFIG_DIR="src/main/resources/META-INF/native-image"
if [ ! -f "$CONFIG_DIR/reflect-config.json" ]; then
  echo "Warning: reflect-config.json not found at $CONFIG_DIR"
  echo "Creating a basic reflect-config.json file..."
  mkdir -p "$CONFIG_DIR"
  cat > "$CONFIG_DIR/reflect-config.json" << EOF
[
  {
    "name": "org.example.MainKt",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true
  }
]
EOF
fi

if [ ! -f "$CONFIG_DIR/resource-config.json" ]; then
  echo "Warning: resource-config.json not found at $CONFIG_DIR"
  echo "Creating a basic resource-config.json file..."
  mkdir -p "$CONFIG_DIR"
  cat > "$CONFIG_DIR/resource-config.json" << EOF
{
  "resources": {
    "includes": [
      {
        "pattern": "\\\\QMETA-INF/services/.*\\\\E"
      },
      {
        "pattern": "\\\\Qlogback.xml\\\\E"
      }
    ]
  },
  "bundles": []
}
EOF
fi

# Step 5: Build the native image
echo "Step 5: Building the native image..."
./gradlew nativeCompile

# Step 6: Verify the build
if [ -f "build/native/nativeCompile/ably-cli" ]; then
  echo "Native image built successfully!"
  echo "You can find the executable at: build/native/nativeCompile/ably-cli"
  echo "Run it with: ./build/native/nativeCompile/ably-cli -k YOUR_API_KEY -c YOUR_CHANNEL_NAME"
else
  echo "Failed to build native image. Check the logs for errors."
  exit 1
fi