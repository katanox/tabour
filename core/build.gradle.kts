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

description =
    "Tabour Core"
