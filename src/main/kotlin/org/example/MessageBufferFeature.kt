package org.example

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization
import org.graalvm.nativeimage.hosted.RuntimeReflection
import org.msgpack.core.buffer.MessageBuffer
import org.msgpack.core.MessagePack
import org.msgpack.core.MessageUnpacker
import org.msgpack.core.MessagePacker
import org.msgpack.core.buffer.MessageBufferInput
import org.msgpack.core.buffer.MessageBufferOutput
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * This feature handles the MessageBuffer class initialization at build time.
 * It forces MessageBuffer related classes to initialize at runtime to avoid issues
 * with Unsafe operations in GraalVM native image.
 */
class MessageBufferFeature : Feature {
    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
        try {
            // Register important MessagePack classes for runtime initialization
            println("MessageBufferFeature: Registering MessagePack classes for runtime initialization")
            
            // Register the MessageBuffer class for runtime initialization
            RuntimeClassInitialization.initializeAtRunTime(MessageBuffer::class.java)
            
            // Also register other MessagePack classes that might cause issues
            RuntimeClassInitialization.initializeAtRunTime(MessagePack::class.java)
            RuntimeClassInitialization.initializeAtRunTime(MessageUnpacker::class.java)
            RuntimeClassInitialization.initializeAtRunTime(MessagePacker::class.java)
            RuntimeClassInitialization.initializeAtRunTime(MessageBufferInput::class.java)
            RuntimeClassInitialization.initializeAtRunTime(MessageBufferOutput::class.java)
            
            // Register additional MessageBuffer classes by name
            registerClassForRuntimeInit("org.msgpack.core.buffer.MessageBufferU")
            registerClassForRuntimeInit("org.msgpack.core.buffer.MessageBufferBE")
            registerClassForRuntimeInit("org.msgpack.core.buffer.MessageBufferLE")
            
            // Also register the MessageBuffer class and its fields for reflection
            val messageBufferClass = MessageBuffer::class.java
            RuntimeReflection.register(messageBufferClass)
            
            for (field in messageBufferClass.declaredFields) {
                RuntimeReflection.register(field)
            }
            
            for (method in messageBufferClass.declaredMethods) {
                RuntimeReflection.register(method)
            }
            
            println("MessageBufferFeature: Successfully registered MessagePack classes for runtime initialization")
        } catch (e: Exception) {
            println("MessageBufferFeature: Error registering MessagePack classes: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun registerClassForRuntimeInit(className: String) {
        try {
            val clazz = Class.forName(className)
            RuntimeClassInitialization.initializeAtRunTime(clazz)
            println("MessageBufferFeature: Registered $className for runtime initialization")
        } catch (e: ClassNotFoundException) {
            println("MessageBufferFeature: Class not found: $className")
        } catch (e: Exception) {
            println("MessageBufferFeature: Error registering $className: ${e.message}")
        }
    }
}