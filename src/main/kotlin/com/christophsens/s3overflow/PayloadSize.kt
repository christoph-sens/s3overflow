package com.christophsens.s3overflow

import java.nio.charset.StandardCharsets

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

/** UTF-8 byte size of [text], e.g. to decide whether it needs to be offloaded to S3. */
fun payloadSizeInBytes(text: String): Long = text.toByteArray(StandardCharsets.UTF_8).size.toLong()
