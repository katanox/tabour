plugins {
    kotlin("jvm") version "1.9.0"
    `maven-publish`
    id("com.ncorti.ktfmt.gradle") version "0.12.0"
}

repositories { mavenCentral() }

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    apply(plugin = "com.ncorti.ktfmt.gradle")

    repositories { mavenCentral() }

    dependencies { implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0") }

    kotlin { jvmToolchain(17) }

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
