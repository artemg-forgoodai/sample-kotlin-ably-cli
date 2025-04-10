#!/bin/bash

# Script to clean and rebuild the project with Java 21

set -e

echo "Cleaning and rebuilding the project with Java 21..."

# Set up GraalVM environment
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

# Clean the project
echo "Cleaning the project..."
./gradlew clean

# Verify Java version
echo "Using Java version:"
java -version

# Build the project
echo "Building the project..."
./gradlew build

echo "Project has been rebuilt with Java 21."
echo "You can now build the native image with:"
echo "./gradlew nativeCompile"