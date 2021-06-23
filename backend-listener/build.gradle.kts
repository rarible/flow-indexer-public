plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":backend-core"))
    implementation(project(":backend-api-model"))

    implementation(libs.rarible.core.kafka)
    implementation(libs.rarible.core.daemon)
    //implementation("com.rarible.core:rarible-core-telemetry-starter:${versions.raribleCore}")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    destinationDirectory.set(file("../target/boot"))
}