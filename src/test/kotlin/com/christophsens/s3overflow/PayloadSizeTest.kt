package com.christophsens.s3overflow

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class PayloadSizeTest {
    @Test
    fun `counts ascii characters as one byte each`() {
        assertThat(payloadSizeInBytes("hello")).isEqualTo(5)
    }

    @Test
    fun `counts multi-byte utf-8 characters correctly`() {
        assertThat(payloadSizeInBytes("héllo")).isEqualTo(6)
    }

    @Test
    fun `sqs allows 1 MiB and sns 256 KiB by default`() {
        assertThat(SQS_MAX_MESSAGE_SIZE_BYTES).isEqualTo(1_048_576)
        assertThat(SNS_DEFAULT_MAX_MESSAGE_SIZE_BYTES).isEqualTo(262_144)
    }
}
