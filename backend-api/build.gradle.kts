import kotlin.collections.mapOf

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("org.openapi.generator")
}

dependencies {
    implementation(project(":backend-core"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation(rootProject.libs.bundles.rarible.core)
    implementation(rootProject.libs.rarible.core.starter)
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    destinationDirectory.set(file("./target/boot"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn("openApiGenerate")
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$rootDir/spec/nft-api.yaml")
    outputDir.set("$buildDir/generated")
    apiPackage.set("com.rarible.flow.api")
    invokerPackage.set("com.rarible.flow.invoker")
    modelPackage.set("com.rarible.flow.api.model")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8"
        )
    )
}