/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

rootProject.name = "tabour"

include("core", "proto", "spring", "plug")

val kotlinVersion = "1.9.22"
val coroutinesVersion = "1.8.0"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("kotlin-jdk8", "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
            library(
                "kotlin-coroutines",
                "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
            )
            library("kotlin-coroutines-jdk", "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
            library("aws-sqs", "software.amazon.awssdk:sqs:2.20.46")
        }

        create("testLibs") {
            library("kotlin-test-junit", "org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
            library("kotlin-test-main", "org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
            library("mockk", "io.mockk:mockk:1.13.9")
            library(
                "kotlin-test-coroutines",
                "org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion"
            )

            bundle(
                "kotlin-test",
                listOf("kotlin-test-main", "kotlin-test-junit")
            )
        }
    }
}
