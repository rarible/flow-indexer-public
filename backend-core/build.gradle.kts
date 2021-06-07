plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation("io.grpc:grpc-stub:1.37.0")
    implementation("io.grpc:grpc-core:1.37.0")
    implementation("io.grpc:grpc-protobuf:1.37.0")
    implementation("net.devh:grpc-client-spring-boot-starter:2.12.0.RELEASE")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}