package org.example

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection
import java.lang.reflect.Executable
import java.lang.reflect.Field

/**
 * GraalVM Feature to register classes for reflection at build time.
 * This is an alternative approach to using reflection-config.json.
 */
class NativeImageSupport : Feature {
    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
        try {
            // Register MessagePack classes for reflection
            registerClassForReflection("org.msgpack.core.buffer.MessageBuffer")
            registerClassForReflection("org.msgpack.core.buffer.MessageBufferU")
            registerClassForReflection("org.msgpack.core.buffer.MessageBufferBE")
            registerClassForReflection("org.msgpack.core.buffer.MessageBufferLE")
            registerClassForReflection("org.msgpack.core.MessagePack")
            registerClassForReflection("org.msgpack.core.MessagePack\$UnpackerConfig")
            registerClassForReflection("org.msgpack.core.buffer.ArrayBufferInput")
            registerClassForReflection("org.msgpack.core.MessageUnpacker")
            registerClassForReflection("org.msgpack.core.MessageFormat")
            registerClassForReflection("org.msgpack.core.MessagePacker")
            registerClassForReflection("org.msgpack.core.buffer.MessageBufferOutput")
            registerClassForReflection("org.msgpack.core.buffer.MessageBufferInput")
            registerClassForReflection("org.msgpack.core.buffer.ByteBufferInput")

            // Register all MessageFormat values (enum)
            registerClassForReflection("org.msgpack.core.MessageFormat\$Values")

            // Register Ably SDK classes for reflection
            registerClassForReflection("io.ably.lib.types.ProtocolMessage")
            registerClassForReflection("io.ably.lib.types.ProtocolSerializer")
            registerClassForReflection("io.ably.lib.transport.WebSocketTransport")
            registerClassForReflection("io.ably.lib.transport.WebSocketTransport\$WsClient")
            registerClassForReflection("io.ably.lib.types.ProtocolMessage\$Action")
            registerClassForReflection("io.ably.lib.types.ProtocolMessage\$Flag")

            // Note: initializeAtBuildTime is not needed here as we're using
            // the --initialize-at-build-time flags in the build.gradle.kts file

            println("NativeImageSupport: Successfully registered MessagePack and Ably classes for reflection")
        } catch (e: Exception) {
            println("NativeImageSupport: Error registering classes for reflection: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun registerClassForReflection(className: String) {
        try {
            val clazz = Class.forName(className)
            RuntimeReflection.register(clazz)

            // Register constructors
            clazz.constructors.forEach { constructor ->
                RuntimeReflection.register(constructor)
            }

            // Register methods
            clazz.methods.forEach { method ->
                RuntimeReflection.register(method)
            }

            // Register fields
            clazz.fields.forEach { field ->
                RuntimeReflection.register(field)
            }

            // Register declared constructors
            clazz.declaredConstructors.forEach { constructor ->
                RuntimeReflection.register(constructor)
            }

            // Register declared methods
            clazz.declaredMethods.forEach { method ->
                RuntimeReflection.register(method)
            }

            // Register declared fields
            clazz.declaredFields.forEach { field ->
                RuntimeReflection.register(field)
            }

            println("NativeImageSupport: Registered $className for reflection")
        } catch (e: ClassNotFoundException) {
            println("NativeImageSupport: Class not found: $className")
        } catch (e: Exception) {
            println("NativeImageSupport: Error registering $className: ${e.message}")
        }
    }
}