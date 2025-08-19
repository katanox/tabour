@file:Suppress("UnstableApiUsage")

plugins { `jvm-test-suite` }

dependencies {
    implementation(libs.coroutines.core)

    api(libs.aws.sqs)
    api(libs.aws.sqs.kotlin)
    api(libs.klogging)

    testImplementation(testLibs.kotlin.test.coroutines)
    testImplementation(testLibs.bundles.kotlin.test)
    testImplementation(testLibs.mockk)
}

group = "com.katanox.tabour"

description = "Tabour Core"

@Suppress("UnstableApiUsage")
testing {
    suites {
        val test by getting(JvmTestSuite::class) { useJUnitJupiter() }

        register("integrationTest", JvmTestSuite::class) {
            dependencies {
                implementation(project())
                implementation(testLibs.kotlin.test.coroutines)
                implementation.bundle(testLibs.bundles.testcontainers)
                implementation.bundle(testLibs.bundles.awaitility)
                implementation.bundle(testLibs.bundles.kotlin.test)
            }

            targets { all { testTask.configure { shouldRunAfter(test) } } }
        }
    }
}

@Suppress("UnstableApiUsage")
tasks.named("check") { dependsOn(testing.suites.named("integrationTest")) }

kotlin.target.compilations
    .getByName("integrationTest")
    .associateWith(kotlin.target.compilations.getByName("main"))
