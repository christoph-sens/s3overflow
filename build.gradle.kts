plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
    `java-library`
}

group = "com.christoph-sens"
// Set by the release workflow from the Git tag (vX.Y.Z -> X.Y.Z).
version = providers.gradleProperty("releaseVersion").getOrElse("0.0.0-SNAPSHOT")

kotlin {
    jvmToolchain(25)
}

dependencies {
    api(libs.aws.sdk.kotlin.s3)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.slf4j.simple)
}

// The Kotlin Gradle plugin resolves Bouncy Castle 1.84 for its publishing validation tasks
// (GHSA-qp49-qgx5-5m26, GHSA-9pwp-9qqc-pr26). Build-time only; align all BC modules on the fixed release.
configurations.matching { it.name == "kotlinBouncyCastleConfiguration" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.bouncycastle") {
            useVersion("1.86")
            because("Bouncy Castle < 1.85 has known vulnerabilities")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

mavenPublishing {
    pom {
        name.set("s3overflow")
        description.set(
            "Kotlin library for offloading SQS/SNS message payloads that exceed the message size " +
                "limit to S3, based on aws-sdk-kotlin and coroutines.",
        )
        url.set("https://github.com/christoph-sens/s3overflow")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("christoph-sens")
                name.set("Christoph Sens")
                url.set("https://github.com/christoph-sens")
            }
        }
        scm {
            url.set("https://github.com/christoph-sens/s3overflow")
            connection.set("scm:git:git://github.com/christoph-sens/s3overflow.git")
            developerConnection.set("scm:git:ssh://git@github.com/christoph-sens/s3overflow.git")
        }
    }

    publishToMavenCentral()
    signAllPublications()
}
