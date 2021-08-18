import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.5.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.5.21"
    kotlin("plugin.spring") version "1.5.21"
    kotlin("plugin.serialization") version "1.5.21"
}

group = "com.rarible.flow"
version = "0.0.2-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2020.0.3")
    }
}

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.openapitools:openapi-generator-gradle-plugin:5.0.0")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.5.21")
    }
}

allprojects {

    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("io.spring.dependency-management")
    }

    repositories {
        mavenLocal()

        mavenCentral()

        maven {
            url = uri("https://jitpack.io")
        }

        maven {
            name = "nexus-maven-public"
            url = uri("http://nexus.rarible.int/repository/maven-public/")
            isAllowInsecureProtocol = true
            metadataSources {
                mavenPom()
                artifact()
            }
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + listOf("-progressive", "-Xskip-metadata-version-check")
        }

    }
}

subprojects {
    dependencyManagement {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2020.0.3")
        }
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
        implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
        implementation(rootProject.libs.flow.sdk)

        implementation("org.springframework.boot:spring-boot-autoconfigure")
        implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
        implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("io.projectreactor:reactor-test")
        testImplementation(rootProject.testLibs.rarible.core.test)
        testImplementation(rootProject.testLibs.bundles.kotest)

    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "1.8"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
