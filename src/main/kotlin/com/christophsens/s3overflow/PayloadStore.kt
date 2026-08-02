package com.christophsens.s3overflow

import java.util.UUID

/** Stores payloads in a backing store that supports larger sizes than SQS/SNS allow inline. */
interface PayloadStore {
    /** Stores [payload] under [s3Key] and returns a pointer usable with [getOriginalPayload]. */
    suspend fun storeOriginalPayload(payload: String, s3Key: String): String

    /** Retrieves the payload previously stored under [payloadPointer]. */
    suspend fun getOriginalPayload(payloadPointer: String): String

    /** Deletes the payload previously stored under [payloadPointer]. */
    suspend fun deleteOriginalPayload(payloadPointer: String)
}

/** Convenience overload that generates a random S3 key. */
suspend fun PayloadStore.storeOriginalPayload(payload: String): String =
    storeOriginalPayload(payload, UUID.randomUUID().toString())
