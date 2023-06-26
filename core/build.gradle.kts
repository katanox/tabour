plugins {
    kotlin("jvm") version "1.9.0-RC"
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

version = "1.0-RC"

description = "Tabour Core"

kotlin { jvmToolchain(17) }

tasks.test { useJUnitPlatform() }

publishing {
    publications {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/katanox/tabour")
                credentials {
                    username = "gpopides"
                    password = System.getenv("TABOUR_TOKEN")
                }
            }
        }
        publications { register<MavenPublication>("gpr") { from(components["java"]) } }
    }
}
