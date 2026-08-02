package com.christophsens.s3overflow

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Points to an S3 object holding an offloaded payload. Serializes to/from the
 * JSON string that is used as the message pointer.
 */
@Serializable
data class PayloadS3Pointer(
    val s3BucketName: String,
    val s3Key: String,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(pointerJson: String): PayloadS3Pointer = json.decodeFromString(serializer(), pointerJson)
    }
}
