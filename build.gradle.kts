import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.5.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.5.0"
    kotlin("plugin.spring") version "1.5.0"
}

group = "com.rarible.flow"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

allprojects {
    val p = this

    logger.warn("Project: [${p.name}]")

    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("io.spring.dependency-management")
    }

    repositories {
        mavenCentral()

        maven {
            url = uri("https://jitpack.io")
        }

        maven {
            name = "nexus-snapshots"
            url = uri("http://10.7.3.6:8081/nexus/content/repositories/snapshots/")
            isAllowInsecureProtocol = true
            metadataSources {
                mavenPom()
                artifact()
            }
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
}

subprojects {



    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
        implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

        implementation("org.springframework.boot:spring-boot-starter-webflux")
        implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
        implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("io.projectreactor:reactor-test")
        testImplementation("com.rarible.core:rarible-core-test-common:1.2-SNAPSHOT")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
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