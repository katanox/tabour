plugins { id("com.google.protobuf") version ("0.9.4") }

dependencies {
    api(libs.aws.sqs)
    implementation(project(":core"))
    implementation("com.google.protobuf:protobuf-java-util:4.28.2")
    testImplementation(testLibs.bundles.kotlin.test)
}

group = "com.katanox.tabour"

description = "Tabour Proto"
