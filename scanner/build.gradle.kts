
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":backend-core"))
    implementation(project(":backend-api-model"))
    implementation(project(":util"))
    implementation(platform("io.mongock:mongock-bom:5.0.21.RC"))
    implementation("io.mongock:mongock-springboot")
    implementation("io.mongock:mongodb-springdata-v3-driver")

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")

    implementation(rootProject.libs.flow.sdk)
    implementation(rootProject.libs.bundles.rarible.core) {
        exclude("org.springframework.boot:spring-boot-starter-web")
    }
    implementation(rootProject.libs.rarible.core.kafka)
    implementation(rootProject.libs.blockchain.scanner.flow)
    implementation(rootProject.libs.bundles.flow.models)
    implementation(rootProject.libs.rarible.currency.starer)
    testImplementation(project(":backend-api-model"))
    testImplementation(project(":util"))
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    destinationDirectory.set(file("./target/boot"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
