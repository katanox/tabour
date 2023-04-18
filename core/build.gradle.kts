plugins {
    kotlin("jvm") version "1.8.20"
    `maven-publish`
}

repositories { maven { url = uri("https://repo.maven.apache.org/maven2/") } }

dependencies {
    implementation(libs.kotlin.jdk8)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.coroutines.jdk)

    api(libs.aws.sqs)

    testImplementation(testLibs.kotlin.test.coroutines)
    testImplementation(testLibs.bundles.kotlin.test)
    testImplementation(testLibs.mockk)
}

group = "com.katanox.tabour"

version = "0.3.0"

description = "Tabour Core"

kotlin { jvmToolchain(19) }

tasks.test { useJUnitPlatform() }

// java { withSourcesJar() }

publishing { publications.create<MavenPublication>("maven") { from(components["java"]) } }
