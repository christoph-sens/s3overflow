package com.christophsens.s3overflow

import java.nio.charset.StandardCharsets

/** SQS/SNS reject payloads larger than this when sent inline. */
const val SQS_SNS_MAX_INLINE_PAYLOAD_SIZE_BYTES: Int = 256 * 1024

/** UTF-8 byte size of [text], e.g. to decide whether it needs to be offloaded to S3. */
fun payloadSizeInBytes(text: String): Long = text.toByteArray(StandardCharsets.UTF_8).size.toLong()
