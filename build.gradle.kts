import java.net.URI

plugins {
    kotlin("jvm").version(libs.versions.kotlin)
    `maven-publish`
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.nexus.publish)
    signing
}

val tabourVersion = rootProject.file("version").readText().trim()

repositories { mavenCentral() }

group = "com.katanox.tabour"

subprojects {
    plugins.apply("org.jetbrains.kotlin.jvm")
    plugins.apply("maven-publish")
    plugins.apply("com.ncorti.ktfmt.gradle")
    plugins.apply("org.jetbrains.dokka")
    plugins.apply("signing")

    version = tabourVersion

    repositories { mavenCentral() }

    kotlin { jvmToolchain(21) }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    publishing {
        publications {
            repositories {
                maven {
                    url =
                        URI.create(
                            "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                        )
                    authentication {
                        credentials {
                            username = System.getenv("OSSRH_USERNAME")
                            password = System.getenv("OSSRH_PASSWORD")
                        }
                    }
                }
            }

            publications {
                register<MavenPublication>("gpr") {
                    from(components["java"])

                    pom {
                        name = "Tabour"
                        description = "A Kotlin library to consume and produce SQS messages"
                        url = "https://github.com/katanox/tabour"
                        groupId = "com.katanox.tabour"
                        version = tabourVersion

                        licenses {
                            license {
                                name = "The Apache License, Version 2.0"
                                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                            }
                        }
                        developers {
                            developer {
                                id = "katanoxdevs"
                                name = "Katanox Developers"
                                email = "developers@katanox.com"
                            }
                        }
                        scm { url = "https://github.com/katanox/tabour" }
                    }
                }
            }
        }
    }

    signing { sign(publishing.publications["gpr"]) }

    ktfmt { kotlinLangStyle() }

    tasks.test { useJUnitPlatform() }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            )
        }
    }
}
