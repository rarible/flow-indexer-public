
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":backend-api-model"))
    implementation("com.rarible.core:rarible-core-kafka:1.2-SNAPSHOT")


    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")


    implementation("io.grpc:grpc-stub:1.37.0")
    implementation("io.grpc:grpc-core:1.37.0")
    implementation("io.grpc:grpc-protobuf:1.37.0")
    implementation("com.nftco:flow-jvm-sdk:0.2.4")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation(rootProject.libs.bundles.rarible.core)
    implementation(rootProject.libs.rarible.core.kafka)

    testImplementation(project(":backend-api-model"))
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    destinationDirectory.set(file("./target/boot"))
}
