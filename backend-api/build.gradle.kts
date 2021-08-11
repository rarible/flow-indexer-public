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
    implementation("com.rarible.protocol:flow-protocol-model-common:2.0.2-SNAPSHOT")
    implementation("com.rarible.protocol:flow-protocol-model-nft:2.0.2-SNAPSHOT")
    implementation("com.rarible.protocol:flow-protocol-model-order:2.0.2-SNAPSHOT")
    implementation("com.rarible.protocol:flow-protocol-api-nft:2.0.2-SNAPSHOT")
    implementation("com.rarible.protocol:flow-protocol-api-nft-order:2.0.2-SNAPSHOT")
    implementation("com.rarible.protocol:flow-protocol-api-order:2.0.2-SNAPSHOT")
    implementation("com.rarible.protocol:flow-api:2.0.2-SNAPSHOT")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation(rootProject.libs.bundles.rarible.core)
    implementation(rootProject.libs.rarible.core.starter)
    implementation(rootProject.libs.rarible.core.logging)
    implementation("org.springframework.boot:spring-boot-starter-websocket")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    destinationDirectory.set(file("./target/boot"))
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
}
