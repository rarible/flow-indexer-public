plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":backend-api-model"))
    implementation(project(":backend-core"))
    implementation(project(":util"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation(rootProject.libs.bundles.rarible.core)
    implementation(rootProject.libs.rarible.core.starter)
    implementation("org.springdoc:springdoc-openapi-ui:1.5.9")
    implementation("org.springdoc:springdoc-openapi-webflux-ui:1.5.9")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    destinationDirectory.set(file("./target/boot"))
}