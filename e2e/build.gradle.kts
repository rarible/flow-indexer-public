plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":backend-core"))
    implementation(project(":backend-api-model"))
    implementation(project(":util"))
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}
