# s3overflow

[![Maven Central](https://img.shields.io/maven-central/v/com.christoph-sens/s3overflow)](https://central.sonatype.com/artifact/com.christoph-sens/s3overflow)
[![CI](https://github.com/christoph-sens/s3overflow/actions/workflows/ci.yml/badge.svg)](https://github.com/christoph-sens/s3overflow/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

Kotlin reimplementation of [payload-offloading-java-common-lib-for-aws](https://github.com/awslabs/payload-offloading-java-common-lib-for-aws):
transparently offloads message payloads that exceed the SQS/SNS message size limit to S3.

Part of a family: **s3overflow** (payload store) · [sqsoverflow](https://github.com/christoph-sens/sqsoverflow) (SQS client) · [snsoverflow](https://github.com/christoph-sens/snsoverflow) (SNS client).

> **Message size limits:** SQS accepts up to 1 MiB per message (`SQS_MAX_MESSAGE_SIZE_BYTES`); SNS topics
> accept 256 KiB by default (`SNS_DEFAULT_MAX_MESSAGE_SIZE_BYTES`) and up to 1 MiB when the
> `MaximumMessageSize` topic attribute is raised. The former shared constant
> `SQS_SNS_MAX_INLINE_PAYLOAD_SIZE_BYTES` (256 KiB) is deprecated.

## Why a reimplementation

The original is a Java 8/Maven library with AWS SDK v1 roots, Jackson-based JSON (de)serialization,
fully separate sync/async APIs, and optional client-side KMS encryption (CSE). This reimplementation
keeps only the actual core and deliberately simplifies it:

| Original | Here |
|---|---|
| AWS SDK for Java v2 (blocking) + separate `*Async` classes | [aws-sdk-kotlin](https://github.com/awslabs/aws-sdk-kotlin) with `suspend` functions — one API for both |
| `S3Dao` as its own layer over `S3Client` | dropped, `S3Client` is already a simple, mockable interface |
| Jackson + `JsonDataConverter` + `CountingOutputStream` | `kotlinx.serialization` for the JSON pointer; byte size via `String.toByteArray().size` |
| `PayloadStorageConfiguration(Base)` + `PayloadStorageAsyncConfiguration` (builder hierarchy) | two constructor parameters (`S3Client`, bucket name) |
| `ServerSideEncryptionStrategy`/`-Factory`/`AwsManagedCmk`/`CustomerKey` (client-side encryption) | deliberately dropped — encryption is configured via SSE-S3/SSE-KMS **at the bucket level**, not in client code |
| 18 classes | 4 files (`PayloadStore`, `S3BackedPayloadStore`, `PayloadS3Pointer`, `PayloadSize`) |

**Note:** the pointer's JSON format is deliberately a plain `{"s3BucketName":"...","s3Key":"..."}`
without Jackson's type-information wrapper — it is therefore *not* byte-identical to the original
pointer format. For a fresh build with no existing Java consumers, that's the simpler, more robust choice.

## Usage

```kotlin
val s3Client = S3Client.fromEnvironment { region = "eu-central-1" }
val store: PayloadStore = S3BackedPayloadStore(s3Client, bucketName = "my-payload-bucket")

// Only offload when necessary
val pointer =
    if (payloadSizeInBytes(message) > SQS_MAX_MESSAGE_SIZE_BYTES) {
        store.storeOriginalPayload(message) // generates a random S3 key automatically
    } else {
        message
    }

val original = store.getOriginalPayload(pointer)
store.deleteOriginalPayload(pointer)
```

## Build

```bash
./gradlew build
```

## Installation

```kotlin
dependencies {
    implementation("com.christoph-sens:s3overflow:<version>")
}
```

```xml
<dependency>
  <groupId>com.christoph-sens</groupId>
  <artifactId>s3overflow</artifactId>
  <version><version></version>
</dependency>
```

### Verifying a release

Every file published to Maven Central (jars, POM, Gradle module metadata) has a signed
[build provenance attestation](https://docs.github.com/en/actions/security-for-github-actions/using-artifact-attestations)
proving it was built by this repository's release workflow from the tagged commit. Verify a
downloaded file with the GitHub CLI:

```bash
gh attestation verify s3overflow-<version>.jar --repo christoph-sens/s3overflow
```

Releases published before provenance attestations were introduced have no attestation.

## Releasing (maintainers)

Releases are published to Maven Central by the [release workflow](.github/workflows/release.yml)
using the [Vanniktech Maven Publish plugin](https://github.com/vanniktech/gradle-maven-publish-plugin).
The version comes from the Git tag; there is no version to bump in the build file.

```bash
git tag v1.2.3
git push origin v1.2.3
```

The workflow builds and tests the tag, then waits for manual approval in the `maven-central`
environment before signing and publishing. The publish job attests the build provenance of the
published files before uploading them, then creates a GitHub release with generated notes and
the published jars attached. Maven Central releases are immutable: fix mistakes with a new patch release.
Running the workflow manually (`workflow_dispatch`) is a dry run that never publishes.

snsoverflow and sqsoverflow depend on s3overflow: release s3overflow first, let Dependabot
bump it in the two clients, then release those.

## Contributing

Contributions are welcome — see [CONTRIBUTING](CONTRIBUTING.md). This project follows the
[Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md).

## License

This project is licensed under the Apache License, Version 2.0 — see [LICENSE](LICENSE).

It is an independent Kotlin reimplementation inspired by the API design of
[payload-offloading-java-common-lib-for-aws](https://github.com/awslabs/payload-offloading-java-common-lib-for-aws)
(Copyright Amazon.com, Inc. or its affiliates, also licensed under Apache-2.0).
No source code was taken from the original; see [NOTICE](NOTICE) for details on provenance.
