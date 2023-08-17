dependencies {
    api(libs.aws.sqs)
    implementation(project(":core"))
    implementation("com.google.protobuf:protobuf-java-util:3.22.3")
    testImplementation(testLibs.bundles.kotlin.test)
}

group = "com.katanox.tabour"

version = "1.0-beta"

description = "Tabour Proto"
