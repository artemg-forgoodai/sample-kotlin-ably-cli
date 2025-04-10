package org.example

import org.msgpack.core.MessagePack
import org.msgpack.core.buffer.MessageBuffer
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * This class is used to initialize the MessagePack classes at runtime.
 * It ensures that the MessageBuffer class and its subclasses are properly loaded
 * and that the Unsafe operations are handled correctly.
 */
object MessagePackInitializer {
    /**
     * Initialize the MessagePack classes.
     * This method should be called early in the application startup.
     */
    fun initialize() {
        try {
            // Initialize MessageBuffer fields that use Unsafe
            initializeMessageBufferFields()

            // Force initialization of MessageBuffer class
            val bufferClass = MessageBuffer::class.java

            // Force initialization of MessagePack class
            val packClass = MessagePack::class.java

            // Create a simple MessagePack object to ensure all classes are loaded
            val packer = MessagePack.newDefaultBufferPacker()

            // Pack different types of data to ensure all format handlers are initialized
            packer.packInt(42)
            packer.packMapHeader(2)
            packer.packString("key1")
            packer.packString("value1")
            packer.packString("key2")
            packer.packInt(123)

            // Get the byte array and create an unpacker to test unpacking
            val bytes = packer.toByteArray()
            packer.close()

            // Test unpacking to ensure unpacker is properly initialized
            val unpacker = MessagePack.newDefaultUnpacker(bytes)
            val value1 = unpacker.unpackInt()
            val mapSize = unpacker.unpackMapHeader()
            val key1 = unpacker.unpackString()
            val val1 = unpacker.unpackString()
            val key2 = unpacker.unpackString()
            val val2 = unpacker.unpackInt()
            unpacker.close()

            println("MessagePack initialized successfully")
            println("Tested packing and unpacking: int=$value1, map with $mapSize entries")
        } catch (e: Exception) {
            println("Error initializing MessagePack: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Initialize the MessageBuffer fields that use Unsafe operations.
     * This is needed for GraalVM native image.
     */
    private fun initializeMessageBufferFields() {
        try {
            // Get the MessageBuffer class
            val messageBufferClass = MessageBuffer::class.java

            // Check if the fields are already initialized
            val baseOffsetField = messageBufferClass.getDeclaredField("ARRAY_BYTE_BASE_OFFSET")
            makeFieldAccessible(baseOffsetField)
            val baseOffset = baseOffsetField.getLong(null)

            val indexScaleField = messageBufferClass.getDeclaredField("ARRAY_BYTE_INDEX_SCALE")
            makeFieldAccessible(indexScaleField)
            val indexScale = indexScaleField.getInt(null)

            // If the fields are already initialized with non-zero values, we don't need to do anything
            if (baseOffset != 0L && indexScale != 0) {
                println("MessageBuffer fields already initialized: ARRAY_BYTE_BASE_OFFSET=$baseOffset, ARRAY_BYTE_INDEX_SCALE=$indexScale")
                return
            }

            // Calculate the values that would normally be set by Unsafe
            val byteArrayBaseOffset = 16L // Common value for most JVMs
            val byteArrayIndexScale = 1   // Always 1 for byte arrays

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

            println("MessageBuffer fields initialized: ARRAY_BYTE_BASE_OFFSET=$byteArrayBaseOffset, ARRAY_BYTE_INDEX_SCALE=$byteArrayIndexScale")
        } catch (e: Exception) {
            println("Error initializing MessageBuffer fields: ${e.message}")
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

            // Try an alternative approach for Java 9+
            try {
                val unsafeClass = Class.forName("sun.misc.Unsafe")
                val unsafeField = unsafeClass.getDeclaredField("theUnsafe")
                unsafeField.isAccessible = true
                val unsafe = unsafeField.get(null)

                val objectFieldOffsetMethod = unsafeClass.getDeclaredMethod("objectFieldOffset", Field::class.java)
                val offset = objectFieldOffsetMethod.invoke(unsafe, field) as Long

                val putObjectMethod = unsafeClass.getDeclaredMethod("putObject", Any::class.java, Long::class.java, Any::class.java)
                putObjectMethod.invoke(unsafe, target, offset, value)

                println("Set final field using Unsafe")
            } catch (e2: Exception) {
                println("Error setting final field using Unsafe: ${e2.message}")
                e2.printStackTrace()
            }
        }
    }
}