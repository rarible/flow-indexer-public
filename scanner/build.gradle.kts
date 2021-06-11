
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":backend-api-model"))
//    implementation("com.rarible.core:rarible-core-kafka:1.2-SNAPSHOT")


    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
//    implementation("org.springframework.cloud:spring-cloud-starter-consul-config:3.0.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")


    implementation("io.grpc:grpc-stub:1.37.0")
    implementation("io.grpc:grpc-core:1.37.0")
    implementation("io.grpc:grpc-protobuf:1.37.0")
    implementation("org.onflow:flow-jvm-sdk:0.1.1")
    implementation("net.devh:grpc-client-spring-boot-starter:2.12.0.RELEASE")

    testImplementation(project(":backend-api-model"))

    implementation("org.springframework.boot:spring-boot-starter-batch")
    runtimeOnly("org.hsqldb:hsqldb")
    testImplementation("org.springframework.batch:spring-batch-test")
}

