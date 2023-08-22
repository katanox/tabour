plugins { `jvm-test-suite` }

dependencies {
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.coroutines.jdk)

    api(libs.aws.sqs)

    testImplementation(testLibs.kotlin.test.coroutines)
    testImplementation(testLibs.bundles.kotlin.test)
    testImplementation(testLibs.mockk)
}

group = "com.katanox.tabour"

version = "1.0-beta"

description = "Tabour Core"

testing {
    suites {
        val test by getting(JvmTestSuite::class) { useJUnitJupiter() }

        register("integrationTest", JvmTestSuite::class) {
            dependencies {
                implementation(project())
                implementation("org.testcontainers:testcontainers:1.18.3")
                implementation("org.testcontainers:localstack:1.18.3")
                implementation(testLibs.kotlin.test.coroutines)
                implementation.bundle(testLibs.bundles.kotlin.test)
            }

            targets { all { testTask.configure { shouldRunAfter(test) } } }
        }
    }
}
