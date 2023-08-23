plugins { `jvm-test-suite` }

dependencies {
    implementation(project(":core"))
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.coroutines.jdk)
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.1.2")
}

group = "com.katanox.tabour"

version = "1.0-beta"

description = "Tabour Spring Boot"

testing {
    suites {
        val test by getting(JvmTestSuite::class) { useJUnitJupiter() }

        register("integrationTest", JvmTestSuite::class) {
            dependencies {
                implementation(project())
                implementation(project(":core"))
                implementation.bundle(testLibs.bundles.kotlin.test)
                implementation("org.springframework.boot:spring-boot-starter-test:3.1.2")
                implementation(testLibs.kotlin.test.coroutines)
            }

            targets { all { testTask.configure { shouldRunAfter(test) } } }
        }
    }
}

tasks.named("check") { dependsOn(testing.suites.named("integrationTest")) }

kotlin.target.compilations
    .getByName("integrationTest")
    .associateWith(kotlin.target.compilations.getByName("main"))
