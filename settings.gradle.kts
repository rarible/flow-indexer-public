rootProject.name = "flow-nft-indexer"
enableFeaturePreview("VERSION_CATALOGS")

include(
    "scanner",
    "backend-api-model",
    "backend-api",
    "backend-core",
    "backend-listener",
    "converters",
    "util",
    "e2e"
)

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("flow-sdk-ver", "0.3.0")
            version("rarible-core", "1.3-spring25-SNAPSHOT")
            version("flow-protocol-version", "2.0.6-SNAPSHOT")

            alias("flow-sdk").to("org.onflow", "flow-jvm-sdk").versionRef("flow-sdk-ver")
            alias("rarible-core-kafka").to("com.rarible.core", "rarible-core-kafka").versionRef("rarible-core")
            alias("rarible-core-daemon").to("com.rarible.core", "rarible-core-daemon").versionRef("rarible-core")
            alias("rarible-core-telemetry").to("com.rarible.core", "rarible-core-telemetry-starter").versionRef("rarible-core")
            alias("rarible-core-starter").to("com.rarible.core", "rarible-core-starter").versionRef("rarible-core")
            alias("rarible-core-logging").to("com.rarible.core", "rarible-core-logging").versionRef("rarible-core")

            alias("flow-model-common").to("com.rarible.protocol", "flow-protocol-model-common").versionRef("flow-protocol-version")
            alias("flow-model-nft").to("com.rarible.protocol", "flow-protocol-model-nft").versionRef("flow-protocol-version")
            alias("flow-model-order").to("com.rarible.protocol", "flow-protocol-model-order").versionRef("flow-protocol-version")

            alias("flow-api").to("com.rarible.protocol", "flow-api").versionRef("flow-protocol-version")
            alias("flow-protocol-api-nft").to("com.rarible.protocol", "flow-protocol-api-nft").versionRef("flow-protocol-version")
            alias("flow-protocol-api-order").to("com.rarible.protocol", "flow-protocol-api-order").versionRef("flow-protocol-version")
            alias("flow-protocol-api-nftorder").to("com.rarible.protocol", "flow-protocol-api-nft-order").versionRef("flow-protocol-version")

            bundle("rarible-core", listOf("rarible-core-daemon", "rarible-core-telemetry"))
            bundle("flow-models", listOf("flow-model-common", "flow-model-nft", "flow-model-order"))
            bundle("flow-apis", listOf("flow-api", "flow-protocol-api-nft", "flow-protocol-api-order", "flow-protocol-api-nftorder"))
        }

        create("testLibs") {
            version("kotest", "4.6.0")
            version("rarible-core", "1.3-SNAPSHOT.spring25")

            alias("kotest-runner").to("io.kotest", "kotest-runner-junit5").versionRef("kotest")
            alias("kotest-spring").to("io.kotest", "kotest-extensions-spring").versionRef("kotest")
            alias("kotest-test-containers").to("io.kotest", "kotest-extensions-testcontainers").version("kotest")
            alias("rarible-core-test").to("com.rarible.core", "rarible-core-test-common").versionRef("rarible-core")

            bundle("kotest", listOf("kotest-runner"))
        }
    }
}
