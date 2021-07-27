plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("org.openapi.generator")
}


dependencies {
    implementation(project(":backend-api-model"))
    implementation(project(":backend-core"))
    implementation(project(":util"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.rarible.protocol:flow-protocol-model-nft:2.0.0-SNAPSHOT")
//    implementation("com.rarible.protocol:flow-protocol-model-common:2.0.1-SNAPSHOT")
    implementation("com.rarible.protocol:flow-protocol-api-nft:2.0.0-SNAPSHOT")
    implementation("com.rarible.protocol:flow-protocol-api-nft-order:2.0.0-SNAPSHOT")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation(rootProject.libs.bundles.rarible.core)
    implementation(rootProject.libs.rarible.core.starter)
    implementation("org.springframework.boot:spring-boot-starter-websocket")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    destinationDirectory.set(file("./target/boot"))
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
}
