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

group = "com.katanox.tabour-core"

version = "1.0-RC"

description = "Tabour Proto"

kotlin { jvmToolchain(17) }

tasks.test { useJUnitPlatform() }

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        groupId = "com.katanox.tabour"
        artifactId = "proto"
        version = "1.0-RC"

        pom {
            name.set("Tabour")
            description.set("Kotlin library to consume queues .")
            url.set("https://github.com/katanox/tabour")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                   name.set("Ahmad Shabib")
                    email.set("a.shabib@katanox.com")
                    organization.set("Katanox")
                    id.set("ahmad.s")
                }
                developer {
                    name.set("George Popides")
                    email.set("g.popides@katanox.com")
                    organization.set("Katanox")
                    id.set("gp")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/katanox/tabour.git")
                developerConnection.set("scm:git:git@github.com:katanox/tabour.git")
                url.set("https://github.com/katanox/tabour")
            }
        }
    }
}
