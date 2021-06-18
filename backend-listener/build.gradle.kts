plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":backend-core"))
    implementation(project(":backend-api-model"))

    implementation("com.rarible.core:rarible-core-kafka:${versions.raribleCore}")
    implementation("com.rarible.core:rarible-core-daemon:${versions.raribleCore}")
    //implementation("com.rarible.core:rarible-core-telemetry-starter:${versions.raribleCore}")
}