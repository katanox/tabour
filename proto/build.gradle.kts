plugins {
    kotlin("jvm") version "1.9.0-RC"

    `maven-publish`
}

repositories { maven { url = uri("https://repo.maven.apache.org/maven2/") } }

dependencies {
    api(libs.aws.sqs)
    implementation(project(":core"))
    implementation("com.google.protobuf:protobuf-java-util:3.22.3")
    testImplementation(testLibs.bundles.kotlin.test)
}

group = "com.katanox.tabour"

version = "1.3-RC"

description = "Tabour Proto"

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
