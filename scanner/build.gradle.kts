
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

    implementation("io.grpc:grpc-stub:1.37.0")
    implementation("io.grpc:grpc-core:1.37.0")
    implementation("io.grpc:grpc-protobuf:1.37.0")
    implementation("com.nftco:flow-jvm-sdk:0.2.4")
    implementation(rootProject.libs.bundles.rarible.core) {
        exclude("org.springframework.boot:spring-boot-starter-web")
    }
    implementation(rootProject.libs.rarible.core.kafka)

    testImplementation(project(":backend-api-model"))
    testImplementation(project(":util"))
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    destinationDirectory.set(file("./target/boot"))
}
