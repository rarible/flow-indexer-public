plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":backend-core"))
    implementation("com.rarible.core:rarible-core-kafka:1.2-SNAPSHOT")
}