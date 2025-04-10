#!/bin/bash

# This script helps set up the GraalVM environment for building native images

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

echo ""
echo "You can now build your native image with:"
echo "./gradlew nativeCompile"