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
    implementation(rootProject.libs.bundles.flow.models)
    implementation(rootProject.libs.bundles.flow.apis)
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation(rootProject.libs.bundles.rarible.core)
    implementation(rootProject.libs.rarible.core.starter)
    implementation(rootProject.libs.rarible.core.logging)

    api("com.querydsl:querydsl-mongodb:5.0.0")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    destinationDirectory.set(file("./target/boot"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
