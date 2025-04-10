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
    // Ably SDK
    implementation("io.ably:ably-java:1.2.25")

    // MessagePack (used by Ably SDK)
    implementation("org.msgpack:msgpack-core:0.9.3")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Command line parsing
    implementation("com.github.ajalt.clikt:clikt:4.2.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // GraalVM SDK for native image configuration
    compileOnly("org.graalvm.nativeimage:svm:23.1.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21) // Changed from 23 to 21 to match GraalVM version
}

application {
    mainClass.set("org.example.MainKt")
}

graalvmNative {
    // Enable toolchain detection to find GraalVM
    toolchainDetection.set(true)

    binaries {
        named("main") {
            imageName.set("ably-cli")
            mainClass.set("org.example.MainKt")
            debug.set(false)
            verbose.set(true)
            fallback.set(true) // Changed to true to help with initial build
            buildArgs.add("--enable-url-protocols=https")

            // Enable reflection for the Ably SDK and other libraries
            // Configuration files are automatically picked up from META-INF/native-image/

            // Initialize classes at build time
            buildArgs.add("--initialize-at-build-time=org.msgpack")
            buildArgs.add("--initialize-at-build-time=io.ably.lib.types")

            // Add substitution for MessageBuffer
            buildArgs.add("-H:ReflectionConfigurationFiles=${projectDir}/src/main/resources/META-INF/native-image/reflect-config.json")
            buildArgs.add("-H:ResourceConfigurationFiles=${projectDir}/src/main/resources/META-INF/native-image/resource-config.json")
            buildArgs.add("-H:SubstitutionFiles=${projectDir}/src/main/resources/META-INF/native-image/substitutions.json")

            // Enable more detailed error reporting
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("-H:+PrintClassInitialization")

            // Add unsafe access
            buildArgs.add("--allow-incomplete-classpath")
            buildArgs.add("--report-unsupported-elements-at-runtime")

            // Add specific options for MessagePack
            buildArgs.add("-H:+AddAllCharsets")

            // Add options for handling Unsafe operations
            buildArgs.add("-H:+AllowVMInspection")
            buildArgs.add("-H:+UnlockExperimentalVMOptions")
            buildArgs.add("-H:ReflectionConfigurationResources=META-INF/native-image/reflect-config.json")

            // Allow incomplete classpath
            buildArgs.add("--allow-incomplete-classpath")

            // Report exceptions for classes that could not be found
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