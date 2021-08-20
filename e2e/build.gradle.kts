plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":backend-core"))
    implementation(project(":backend-api-model"))
    implementation(project(":util"))

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation(rootProject.libs.bundles.rarible.core)
    implementation(rootProject.libs.rarible.core.kafka)
    implementation(rootProject.libs.bundles.flow.models)

    testFixturesApi(testFixtures(rootProject.libs.flow.sdk))
    testImplementation(rootProject.testLibs.bundles.kotest)
    testImplementation(rootProject.testLibs.rarible.core.test)

}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}
