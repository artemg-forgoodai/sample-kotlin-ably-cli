package org.example

import org.graalvm.nativeimage.hosted.Feature
import org.msgpack.core.buffer.MessageBuffer
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * This feature manually initializes the MessageBuffer class fields that use Unsafe operations.
 * It's an alternative approach to using @TargetClass and @RecomputeFieldValue annotations.
 */
class MessageBufferFeature : Feature {
    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
        try {
            // Get the MessageBuffer class
            val messageBufferClass = MessageBuffer::class.java
            
            // Get the ARRAY_BYTE_BASE_OFFSET field
            val baseOffsetField = messageBufferClass.getDeclaredField("ARRAY_BYTE_BASE_OFFSET")
            makeFieldAccessible(baseOffsetField)
            
            // Get the ARRAY_BYTE_INDEX_SCALE field
            val indexScaleField = messageBufferClass.getDeclaredField("ARRAY_BYTE_INDEX_SCALE")
            makeFieldAccessible(indexScaleField)
            
            // Calculate the values that would normally be set by Unsafe
            val byteArrayBaseOffset = getByteArrayBaseOffset()
            val byteArrayIndexScale = getByteArrayIndexScale()
            
            // Set the values
            if (Modifier.isFinal(baseOffsetField.modifiers)) {
                setFinalField(baseOffsetField, null, byteArrayBaseOffset)
            } else {
                baseOffsetField.set(null, byteArrayBaseOffset)
            }
            
            if (Modifier.isFinal(indexScaleField.modifiers)) {
                setFinalField(indexScaleField, null, byteArrayIndexScale)
            } else {
                indexScaleField.set(null, byteArrayIndexScale)
            }
            
            println("MessageBufferFeature: Successfully initialized MessageBuffer fields")
            println("  ARRAY_BYTE_BASE_OFFSET = $byteArrayBaseOffset")
            println("  ARRAY_BYTE_INDEX_SCALE = $byteArrayIndexScale")
        } catch (e: Exception) {
            println("MessageBufferFeature: Error initializing MessageBuffer fields: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun makeFieldAccessible(field: Field) {
        if (!field.isAccessible) {
            field.isAccessible = true
        }
    }
    
    private fun setFinalField(field: Field, target: Any?, value: Any?) {
        makeFieldAccessible(field)
        
        try {
            // Get the modifiers field from Field class
            val modifiersField = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            
            // Remove the final modifier
            modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
            
            // Set the new value
            field.set(target, value)
        } catch (e: Exception) {
            println("Error setting final field: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun getByteArrayBaseOffset(): Long {
        // This is a common value for byte array base offset on most JVMs
        // For GraalVM native image, we need to use a fixed value
        return 16L
    }
    
    private fun getByteArrayIndexScale(): Int {
        // This is always 1 for byte arrays (each element is 1 byte)
        return 1
    }
}