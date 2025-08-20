plugins {
  kotlin("jvm").version(libs.versions.kotlin)
  `maven-publish`
  alias(libs.plugins.dokka)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.jreleaser)
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
  plugins.apply("org.jreleaser")

  version = tabourVersion

  repositories { mavenCentral() }

  kotlin { jvmToolchain(21) }

  java {
    withJavadocJar()
    withSourcesJar()
  }

  publishing {
    repositories {
      maven { url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI() }
    }
    publications {
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

  jreleaser {
    signing {
      setActive("ALWAYS")
      armored = true
    }

    gitRootSearch = true

    deploy {
      maven {
        mavenCentral {
          create("sonatype") {
            version = tabourVersion
            setActive("ALWAYS")
            url = "https://central.sonatype.com/api/v1/publisher"
            stagingRepository("build/staging-deploy")
            applyMavenCentralRules = false
          }
        }
      }
    }
  }
}
