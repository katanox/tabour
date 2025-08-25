import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins { `jvm-test-suite` }

dependencies {
    implementation(project(":core"))
    runtimeOnly(libs.kotlin.reflect)
    implementation(libs.coroutines.core)
    implementation(libs.spring.boot.autoconfigure)
}

group = "com.katanox.tabour"

description = "Tabour Spring Boot"

@Suppress("UnstableApiUsage")
testing {
    suites @Suppress("UnstableApiUsage") {
        val test by getting(JvmTestSuite::class) { useJUnitJupiter() }

        register("integrationTest", JvmTestSuite::class) {
            dependencies {
                implementation(project())
                implementation(project(":core"))
                implementation.bundle(testLibs.bundles.kotlin.test)
                implementation(testLibs.spring.boot.starter.test)
                implementation(testLibs.kotlin.test.coroutines)
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

val compileKotlin: KotlinCompile by tasks

compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}
