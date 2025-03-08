plugins { `jvm-test-suite` }

dependencies {
    implementation(project(":core"))
    implementation(libs.ktor.server)
}

group = "com.katanox.tabour"