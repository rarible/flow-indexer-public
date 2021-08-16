plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":backend-api-model"))
    implementation(project(":util"))
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation(rootProject.libs.bundles.flow.models)
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
