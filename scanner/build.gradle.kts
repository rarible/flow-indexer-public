
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":backend-api-model"))
    implementation(project(":util"))

    implementation("org.springframework.boot:spring-boot-starter-data-mongodb:2.5.4")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive:2.5.4")
    implementation("org.springframework.boot:spring-boot-starter-webflux:2.5.4")
    implementation("org.springframework.boot:spring-boot-starter-websocket:2.5.4")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap:3.0.3")

    implementation(rootProject.libs.flow.sdk)
    implementation(rootProject.libs.bundles.rarible.core) {
        exclude("org.springframework.boot:spring-boot-starter-web")
    }
    implementation(rootProject.libs.rarible.core.kafka)
    implementation(rootProject.libs.blockchain.scanner.core)
    implementation(rootProject.libs.blockchain.scanner.flow)
    testImplementation(project(":backend-api-model"))
    testImplementation(project(":util"))
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    destinationDirectory.set(file("./target/boot"))
}
