package com.christophsens.s3overflow

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

/**
 * Points to an S3 object holding an offloaded payload. Serializes to/from the
 * JSON string that is used as the message pointer.
 *
 * The pointer is written in the format of the AWS Java extended client libraries, a Jackson
 * type-information wrapper: `["software.amazon.payloadoffloading.PayloadS3Pointer",{"s3BucketName":"...","s3Key":"..."}]`.
 * Reading also accepts the plain `{"s3BucketName":"...","s3Key":"..."}` object written by
 * s3overflow before 2.0.0.
 */
@Serializable
data class PayloadS3Pointer(
    val s3BucketName: String,
    val s3Key: String,
) {
    fun toJson(): String =
        buildJsonArray {
            add(JsonPrimitive(JAVA_POINTER_CLASS_NAME))
            add(json.encodeToJsonElement(serializer(), this@PayloadS3Pointer))
        }.toString()

    companion object {
        /** Type id that Jackson's default typing writes for the AWS Java libraries' pointer class. */
        const val JAVA_POINTER_CLASS_NAME: String = "software.amazon.payloadoffloading.PayloadS3Pointer"

        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(pointerJson: String): PayloadS3Pointer {
            val pointer =
                when (val element = json.parseToJsonElement(pointerJson)) {
                    is JsonObject -> element
                    is JsonArray -> unwrapJavaPointer(element)
                    else -> throw SerializationException("S3 pointer must be a JSON object or array: $pointerJson")
                }
            return json.decodeFromJsonElement(serializer(), pointer)
        }

        private fun unwrapJavaPointer(array: JsonArray): JsonObject {
            val typeId = (array.getOrNull(0) as? JsonPrimitive)?.takeIf { it.isString }?.content
            val pointer = array.getOrNull(1) as? JsonObject
            if (array.size != 2 || typeId != JAVA_POINTER_CLASS_NAME || pointer == null) {
                throw SerializationException("Unsupported S3 pointer format: $array")
            }
            return pointer
        }
    }
}
