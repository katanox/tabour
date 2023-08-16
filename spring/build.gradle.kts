plugins {
    kotlin("jvm") version "1.9.0"
    `maven-publish`
}

repositories { maven { url = uri("https://repo.maven.apache.org/maven2/") } }

dependencies {
    implementation(project(":core"))
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation(libs.kotlin.jdk8)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.coroutines.jdk)
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.1.2")
}

group = "com.katanox.tabour"

version = "1.0-beta"

description = "Tabour Spring Boot"

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
