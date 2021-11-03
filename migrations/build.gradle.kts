plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":backend-core"))
    implementation(project(":util"))

    implementation(platform("io.mongock:mongock-bom:5.0.21.RC"))
    implementation("io.mongock:mongock-springboot")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}
