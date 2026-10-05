package com.christophsens.s3overflow

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.DeleteObjectResponse
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectResponse
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import aws.smithy.kotlin.runtime.content.ByteStream
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import kotlin.test.Test

class S3BackedPayloadStoreTest {
    private val s3Client = mockk<S3Client>()
    private val store = S3BackedPayloadStore(s3Client, bucketName = "my-bucket")

    @Test
    fun `stores payload and returns a pointer`() =
        runTest {
            coEvery { s3Client.putObject(any<PutObjectRequest>()) } returns PutObjectResponse {}

            val pointer = store.storeOriginalPayload("hello world", s3Key = "my-key")

            assertThat(pointer).isEqualTo(PayloadS3Pointer("my-bucket", "my-key").toJson())
            coVerify {
                s3Client.putObject(
                    withArg<PutObjectRequest> {
                        assertThat(it.bucket).isEqualTo("my-bucket")
                        assertThat(it.key).isEqualTo("my-key")
                    },
                )
            }
        }

    @Test
    fun `generates a random key when none is given`() =
        runTest {
            coEvery { s3Client.putObject(any<PutObjectRequest>()) } returns PutObjectResponse {}

            val pointer = PayloadS3Pointer.fromJson(store.storeOriginalPayload("hello world"))

            assertThat(pointer.s3Key).isNotBlank()
        }

    @Test
    fun `retrieves the original payload from the pointer`() =
        runTest {
            coEvery {
                s3Client.getObject(any<GetObjectRequest>(), any<suspend (GetObjectResponse) -> String?>())
            } coAnswers {
                val block = secondArg<suspend (GetObjectResponse) -> String?>()
                block(GetObjectResponse { body = ByteStream.fromString("hello world") })
            }

            val pointer = PayloadS3Pointer(s3BucketName = "my-bucket", s3Key = "my-key").toJson()
            val payload = store.getOriginalPayload(pointer)

            assertThat(payload).isEqualTo("hello world")
        }

    @Test
    fun `deletes the payload for the pointer`() =
        runTest {
            coEvery { s3Client.deleteObject(any<DeleteObjectRequest>()) } returns DeleteObjectResponse {}

            val pointer = PayloadS3Pointer(s3BucketName = "my-bucket", s3Key = "my-key").toJson()
            store.deleteOriginalPayload(pointer)

            coVerify {
                s3Client.deleteObject(
                    withArg<DeleteObjectRequest> {
                        assertThat(it.bucket).isEqualTo("my-bucket")
                        assertThat(it.key).isEqualTo("my-key")
                    },
                )
            }
        }

    @Test
    fun `rejects reading a pointer into a foreign bucket`() {
        val pointer = PayloadS3Pointer(s3BucketName = "someone-elses-bucket", s3Key = "secret").toJson()

        assertThatThrownBy { runTest { store.getOriginalPayload(pointer) } }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("someone-elses-bucket")
        coVerify(exactly = 0) { s3Client.getObject(any<GetObjectRequest>(), any<suspend (GetObjectResponse) -> String?>()) }
    }

    @Test
    fun `rejects deleting a pointer into a foreign bucket`() {
        val pointer = PayloadS3Pointer(s3BucketName = "someone-elses-bucket", s3Key = "important").toJson()

        assertThatThrownBy { runTest { store.deleteOriginalPayload(pointer) } }
            .isInstanceOf(IllegalArgumentException::class.java)
        coVerify(exactly = 0) { s3Client.deleteObject(any<DeleteObjectRequest>()) }
    }

    @Test
    fun `accepts pointers into additional buckets`() =
        runTest {
            coEvery { s3Client.deleteObject(any<DeleteObjectRequest>()) } returns DeleteObjectResponse {}
            val sharedStore = S3BackedPayloadStore(s3Client, bucketName = "my-bucket", additionalBucketNames = setOf("other-producer"))

            sharedStore.deleteOriginalPayload(PayloadS3Pointer(s3BucketName = "other-producer", s3Key = "k").toJson())

            coVerify { s3Client.deleteObject(withArg<DeleteObjectRequest> { assertThat(it.bucket).isEqualTo("other-producer") }) }
        }
}
