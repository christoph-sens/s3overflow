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
    fun `counts surrogate pairs as four bytes`() {
        assertThat(payloadSizeInBytes("a\uD83D\uDE00b")).isEqualTo(6)
    }

    @Test
    fun `matches the length of the utf-8 encoding`() {
        listOf("", "héllo wörld", "日本語テキスト", "emoji \uD83D\uDE00\uD83C\uDF89", "lone \uD83D high", "lone \uDE00 low", "end \uD83D")
            .forEach { text ->
                assertThat(payloadSizeInBytes(text)).describedAs(text).isEqualTo(text.toByteArray(Charsets.UTF_8).size.toLong())
            }
    }

    @Test
    fun `sqs allows 1 MiB and sns 256 KiB by default`() {
        assertThat(SQS_MAX_MESSAGE_SIZE_BYTES).isEqualTo(1_048_576)
        assertThat(SNS_DEFAULT_MAX_MESSAGE_SIZE_BYTES).isEqualTo(262_144)
    }
}
