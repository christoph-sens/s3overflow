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
}
