buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-noarg:1.5.21")
    }
}

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
    kotlin("kapt")
    id("org.springframework.boot")
    id("org.jetbrains.kotlin.plugin.noarg") version "1.5.21"
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
    implementation("com.querydsl:querydsl-mongodb:5.0.0")
    implementation("com.querydsl:querydsl-apt:5.0.0")
    kapt("com.querydsl:querydsl-apt:5.0.0")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation(rootProject.libs.bundles.flow.models)
}
kapt {
    keepJavacAnnotationProcessors = true
    annotationProcessors("org.springframework.data.mongodb.repository.support.MongoAnnotationProcessor")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
