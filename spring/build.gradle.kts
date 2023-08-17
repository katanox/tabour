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
