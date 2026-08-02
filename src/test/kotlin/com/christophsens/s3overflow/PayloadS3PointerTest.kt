package com.christophsens.s3overflow

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class PayloadS3PointerTest {
    @Test
    fun `round trips through json`() {
        val pointer = PayloadS3Pointer(s3BucketName = "my-bucket", s3Key = "my-key")

        val restored = PayloadS3Pointer.fromJson(pointer.toJson())

        assertThat(restored).isEqualTo(pointer)
    }

    @Test
    fun `serializes bucket and key as json fields`() {
        val json = PayloadS3Pointer(s3BucketName = "my-bucket", s3Key = "my-key").toJson()

        assertThat(json)
            .contains("\"s3BucketName\":\"my-bucket\"")
            .contains("\"s3Key\":\"my-key\"")
    }
}
