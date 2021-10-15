plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
    kotlin("kapt")
    id("org.springframework.boot")
    id("idea")
}

idea {
    module {
        val kaptMain = file("build/generated/source/kapt/main")
        sourceDirs.add(kaptMain)
        generatedSourceDirs.add(kaptMain)
    }
}

dependencies {
    implementation(project(":backend-api-model"))
    implementation(project(":util"))
    implementation("org.springframework.data:spring-data-commons")
    implementation(rootProject.libs.bundles.flow.models)
    implementation("com.querydsl:querydsl-mongodb:5.0.0")
    implementation(rootProject.libs.blockchain.scanner.model)
    api("com.querydsl:querydsl-apt:5.0.0")
    kapt("com.querydsl:querydsl-apt:5.0.0:general")
}
kapt {
    keepJavacAnnotationProcessors = true
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
