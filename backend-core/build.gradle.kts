plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
    id("org.springframework.boot")
    id("idea")
}

dependencies {
    implementation(project(":backend-api-model"))
    implementation(project(":util"))
    implementation("org.springframework.data:spring-data-commons")
    implementation(rootProject.libs.bundles.flow.models)
    implementation(rootProject.libs.blockchain.scanner.model)
    implementation(rootProject.libs.rarible.core.kafka)
    implementation(rootProject.libs.rarible.currency.starer)
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
