plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")

}

dependencies {
    implementation(project(":backend-core"))
    implementation(project(":backend-api-model"))
    implementation(project(":util"))


    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation(rootProject.libs.bundles.rarible.core)
    implementation(rootProject.libs.rarible.core.kafka)


}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    destinationDirectory.set(file("./target/boot"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
