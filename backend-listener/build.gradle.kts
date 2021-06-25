plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":daemon"))
    implementation(project(":kafka"))
    implementation(project(":backend-core"))
    implementation(project(":backend-api-model"))
    implementation("org.springframework.boot:spring-boot-actuator")
    implementation("org.apache.kafka:kafka-clients:2.5.1")

    implementation(rootProject.libs.bundles.rarible.core)

}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    destinationDirectory.set(file("./target/boot"))
}