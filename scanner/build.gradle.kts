
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":backend-api-model"))
    implementation(project(":util"))

    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")

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
