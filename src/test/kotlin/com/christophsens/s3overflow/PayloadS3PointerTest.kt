package com.christophsens.s3overflow

import kotlinx.serialization.SerializationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import kotlin.test.Test

/**
 * Written by `software.amazon.payloadoffloading.PayloadS3Pointer.toJson()` from payloadoffloading-common
 * 2.2.0, the library behind amazon-sqs-java-extended-client-lib and amazon-sns-java-extended-client-lib.
 */
private const val JAVA_POINTER_JSON =
    """["software.amazon.payloadoffloading.PayloadS3Pointer",""" +
        """{"s3BucketName":"my-payload-bucket","s3Key":"prefix/0b6c7f0e-6a1f-4d0e-9a3e-2f1c5d8e7a90"}]"""

private val JAVA_POINTER = PayloadS3Pointer("my-payload-bucket", "prefix/0b6c7f0e-6a1f-4d0e-9a3e-2f1c5d8e7a90")

class PayloadS3PointerTest {
    @Test
    fun `round trips through json`() {
        val pointer = PayloadS3Pointer(s3BucketName = "my-bucket", s3Key = "my-key")

        val restored = PayloadS3Pointer.fromJson(pointer.toJson())

        assertThat(restored).isEqualTo(pointer)
    }

    @Test
    fun `writes the same json as the AWS Java libraries`() {
        assertThat(JAVA_POINTER.toJson()).isEqualTo(JAVA_POINTER_JSON)
    }

    @Test
    fun `reads a pointer written by the AWS Java libraries`() {
        assertThat(PayloadS3Pointer.fromJson(JAVA_POINTER_JSON)).isEqualTo(JAVA_POINTER)
    }

    @Test
    fun `reads the plain object format written before 2_0_0`() {
        val restored = PayloadS3Pointer.fromJson("""{"s3BucketName":"my-bucket","s3Key":"my-key"}""")

        assertThat(restored).isEqualTo(PayloadS3Pointer("my-bucket", "my-key"))
    }

    @Test
    fun `ignores unknown fields`() {
        val restored =
            PayloadS3Pointer.fromJson(
                """["${PayloadS3Pointer.JAVA_POINTER_CLASS_NAME}",{"s3BucketName":"b","s3Key":"k","extra":1}]""",
            )

        assertThat(restored).isEqualTo(PayloadS3Pointer("b", "k"))
    }

    @Test
    fun `rejects a wrapper with a different type id`() {
        assertThatThrownBy { PayloadS3Pointer.fromJson("""["com.example.Other",{"s3BucketName":"b","s3Key":"k"}]""") }
            .isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `rejects json that is neither an object nor an array`() {
        assertThatThrownBy { PayloadS3Pointer.fromJson("\"just a string\"") }
            .isInstanceOf(SerializationException::class.java)
    }
}
