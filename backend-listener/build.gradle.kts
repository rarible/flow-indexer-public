plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":backend-core"))
    implementation(project(":backend-api-model"))

    implementation("com.rarible.core:rarible-core-kafka:1.2-SNAPSHOT")
    implementation("com.rarible.core:rarible-core-daemon:1.2-SNAPSHOT")
}