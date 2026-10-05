package com.christophsens.s3overflow

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.decodeToString
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * [PayloadStore] backed by an S3 bucket. Encryption at rest is expected to be configured on the
 * bucket itself (SSE-S3 or SSE-KMS default bucket encryption) rather than per request.
 *
 * Pointers arrive in messages, so whoever can send to a queue or topic controls them. Reads and
 * deletes are therefore only performed for pointers into [bucketName] or [additionalBucketNames];
 * any other pointer is rejected with an [IllegalArgumentException]. Use a bucket dedicated to
 * payloads, since a pointer may still name any key in an accepted bucket.
 */
class S3BackedPayloadStore(
    private val s3Client: S3Client,
    private val bucketName: String,
    /** Further buckets whose pointers are accepted, e.g. those of other producers on the same queue. */
    additionalBucketNames: Set<String> = emptySet(),
) : PayloadStore {
    private val acceptedBucketNames = additionalBucketNames + bucketName

    override suspend fun storeOriginalPayload(payload: String, s3Key: String): String {
        s3Client.putObject(
            PutObjectRequest {
                bucket = bucketName
                key = s3Key
                body = ByteStream.fromString(payload)
            },
        )
        logger.debug { "S3 object created, bucket: $bucketName, key: $s3Key" }

        return PayloadS3Pointer(bucketName, s3Key).toJson()
    }

    override suspend fun getOriginalPayload(payloadPointer: String): String {
        val pointer = acceptedPointer(payloadPointer)

        val payload =
            s3Client.getObject(
                GetObjectRequest {
                    bucket = pointer.s3BucketName
                    key = pointer.s3Key
                },
            ) { response -> response.body?.decodeToString() }
                ?: error("S3 object at ${pointer.s3BucketName}/${pointer.s3Key} has no body")

        logger.debug { "S3 object read, bucket: ${pointer.s3BucketName}, key: ${pointer.s3Key}" }
        return payload
    }

    override suspend fun deleteOriginalPayload(payloadPointer: String) {
        val pointer = acceptedPointer(payloadPointer)

        s3Client.deleteObject(
            DeleteObjectRequest {
                bucket = pointer.s3BucketName
                key = pointer.s3Key
            },
        )
        logger.debug { "S3 object deleted, bucket: ${pointer.s3BucketName}, key: ${pointer.s3Key}" }
    }

    private fun acceptedPointer(payloadPointer: String): PayloadS3Pointer {
        val pointer = PayloadS3Pointer.fromJson(payloadPointer)
        require(pointer.s3BucketName in acceptedBucketNames) {
            "S3 pointer to bucket ${pointer.s3BucketName} rejected; accepted buckets: ${acceptedBucketNames.sorted()}"
        }
        return pointer
    }
}
