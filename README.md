# s3overflow

Kotlin reimplementation of [payload-offloading-java-common-lib-for-aws](https://github.com/awslabs/payload-offloading-java-common-lib-for-aws):
transparently offloads message payloads that exceed the SQS/SNS 256 KB limit to S3.

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
    if (payloadSizeInBytes(message) > SQS_SNS_MAX_INLINE_PAYLOAD_SIZE_BYTES) {
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

Once published to Maven Central:

```kotlin
dependencies {
    implementation("com.christoph-sens:s3overflow:0.1.0")
}
```

```xml
<dependency>
  <groupId>com.christoph-sens</groupId>
  <artifactId>s3overflow</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Publishing (maintainers)

Publishing uses the [Vanniktech Maven Publish plugin](https://github.com/vanniktech/gradle-maven-publish-plugin)
against Sonatype's Central Publishing Portal. This requires a Central account with the
`com.christoph-sens` namespace verified (via a DNS TXT record on `christoph-sens.com`) and a GPG
signing key. Set the following in `~/.gradle/gradle.properties` (never commit these):

```properties
mavenCentralUsername=...
mavenCentralPassword=...
signing.keyId=...
signing.password=...
signing.secretKeyRingFile=...
```

Then bump `version` in [build.gradle.kts](build.gradle.kts) and run:

```bash
./gradlew publishToMavenCentral
```

## Contributing

Contributions are welcome — see [CONTRIBUTING](CONTRIBUTING.md). This project follows the
[Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md).

## License

This project is licensed under the Apache License, Version 2.0 — see [LICENSE](LICENSE).

It is an independent Kotlin reimplementation inspired by the API design of
[payload-offloading-java-common-lib-for-aws](https://github.com/awslabs/payload-offloading-java-common-lib-for-aws)
(Copyright Amazon.com, Inc. or its affiliates, also licensed under Apache-2.0).
No source code was taken from the original; see [NOTICE](NOTICE) for details on provenance.
