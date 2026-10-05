package com.christophsens.s3overflow

/** SQS rejects messages (body + attributes) larger than this; raised from 256 KiB to 1 MiB in August 2025. */
const val SQS_MAX_MESSAGE_SIZE_BYTES: Int = 1024 * 1024

/**
 * SNS rejects messages (body + attributes) larger than this unless the topic's `MaximumMessageSize`
 * attribute is raised (up to 1 MiB).
 */
const val SNS_DEFAULT_MAX_MESSAGE_SIZE_BYTES: Int = 256 * 1024

/** The limit shared by SQS and SNS before SQS raised its limit to 1 MiB. */
@Deprecated(
    "SQS and SNS limits differ now; use SQS_MAX_MESSAGE_SIZE_BYTES or SNS_DEFAULT_MAX_MESSAGE_SIZE_BYTES.",
    ReplaceWith("SNS_DEFAULT_MAX_MESSAGE_SIZE_BYTES"),
)
const val SQS_SNS_MAX_INLINE_PAYLOAD_SIZE_BYTES: Int = SNS_DEFAULT_MAX_MESSAGE_SIZE_BYTES

/**
 * UTF-8 byte size of [text], e.g. to decide whether it needs to be offloaded to S3. Counts without
 * encoding, so large payloads aren't copied just to be measured. Unpaired surrogates count as one
 * byte, matching the replacement character that `String.toByteArray(Charsets.UTF_8)` writes.
 */
fun payloadSizeInBytes(text: String): Long {
    var bytes = 0L
    var i = 0
    while (i < text.length) {
        val char = text[i]
        bytes +=
            when {
                char.code < 0x80 -> 1
                char.code < 0x800 -> 2
                char.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate() -> 4.also { i++ }
                char.isSurrogate() -> 1
                else -> 3
            }
        i++
    }
    return bytes
}
