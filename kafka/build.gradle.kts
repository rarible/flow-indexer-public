plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation("io.projectreactor.kafka:reactor-kafka")
    implementation("com.rarible.core:rarible-core-application:1.2-SNAPSHOT")
    implementation("org.apache.kafka:kafka-clients:2.5.1")
    implementation("org.springframework.boot:spring-boot-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-consul-config")
    implementation("org.springframework.cloud:spring-cloud-starter-consul-discovery")
    implementation(rootProject.libs.bundles.rarible.core)
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}