plugins {
    kotlin("jvm") version "1.9.22"
    `maven-publish`
    id("com.ncorti.ktfmt.gradle") version "0.17.0"
    id("org.jetbrains.dokka") version "1.9.10"
}

val tabourVersion = rootProject.file("version").readText().trim()

repositories { mavenCentral() }

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    apply(plugin = "com.ncorti.ktfmt.gradle")
    apply(plugin = "org.jetbrains.dokka")

    version = tabourVersion

    repositories { mavenCentral() }

    dependencies { implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22") }

    kotlin { jvmToolchain(21) }

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

    ktfmt { kotlinLangStyle() }

    tasks.test { useJUnitPlatform() }
}
