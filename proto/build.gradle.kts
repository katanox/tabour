plugins { alias(libs.plugins.protobuf) }

dependencies {
    api(libs.aws.sqs)
    implementation(project(":core"))
    implementation(libs.protobuf.java.util)
    testImplementation(testLibs.bundles.kotlin.test)
}

group = "com.katanox.tabour"

description = "Tabour Proto"
