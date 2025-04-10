package org.example

import com.oracle.svm.core.annotate.Alias
import com.oracle.svm.core.annotate.RecomputeFieldValue
import com.oracle.svm.core.annotate.TargetClass
import org.msgpack.core.buffer.MessageBuffer

/**
 * This class provides substitutions for the MessageBuffer class in the native image.
 * It handles the Unsafe operations that are problematic in GraalVM native image.
 */
@TargetClass(MessageBuffer::class)
final class MessageBufferSubstitute {

    /**
     * Substitute for the ARRAY_BYTE_BASE_OFFSET field in MessageBuffer.
     * This field is computed using Unsafe.arrayBaseOffset(byte[].class) in the original class.
     */
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayBaseOffset, declClass = ByteArray::class)
    private companion object {
        @JvmField
        val ARRAY_BYTE_BASE_OFFSET: Long = 0L

        /**
         * Substitute for the ARRAY_BYTE_INDEX_SCALE field in MessageBuffer.
         * This field is computed using Unsafe.arrayIndexScale(byte[].class) in the original class.
         */
        @Alias
        @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayIndexScale, declClass = ByteArray::class)
        @JvmField
        val ARRAY_BYTE_INDEX_SCALE: Int = 0
    }
}