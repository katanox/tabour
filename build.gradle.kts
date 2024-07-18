import java.net.URI

plugins {
    kotlin("jvm") version "1.9.22"
    `maven-publish`
    id("com.ncorti.ktfmt.gradle") version "0.17.0"
    id("org.jetbrains.dokka") version "1.9.10"
    signing
}

val tabourVersion = rootProject.file("version").readText().trim()

repositories { mavenCentral() }

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    apply(plugin = "com.ncorti.ktfmt.gradle")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "signing")

    version = tabourVersion

    repositories { mavenCentral() }

    dependencies { implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22") }

    kotlin { jvmToolchain(21) }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    publishing {
        publications {
            repositories {
                maven {
                    url = URI.create("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                    authentication {
                        credentials {
                            username = System.getenv("OSSRH_USERNAME")
                                ?.ifEmpty { throw RuntimeException("OSSRH_USERNAME environment variable is not set") }
                                ?: throw RuntimeException("OSSRH_USERNAME environment variable is not set")
                            password = System.getenv("OSSRH_PASSWORD")
                                ?.ifEmpty { throw RuntimeException("OSSHR_PASSWORD environment variable is not set") }
                                ?: throw RuntimeException("OSSHR_PASSWORD  environment variable is not set")
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
                                email = "katanox@developers.com"
                            }
                        }
                        scm {
                            url = "https://github.com/katanox/tabour"
                        }
                    }
                }
            }
        }
    }

    signing {
        sign(publishing.publications["gpr"])
    }

    ktfmt { kotlinLangStyle() }

    tasks.test { useJUnitPlatform() }
}
