plugins { `jvm-test-suite` }

dependencies {
    implementation(project(":core"))
    implementation(libs.ktor.server.core)
}

group = "com.katanox.tabour"