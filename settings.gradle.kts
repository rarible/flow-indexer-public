@file:Suppress("UnstableApiUsage")

rootProject.name = "flow-nft-indexer"
enableFeaturePreview("VERSION_CATALOGS")

include(
    "scanner",
    "backend-api-model",
    "backend-api",
    "backend-core",
    "util",
    "e2e"
)

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("flow-sdk-ver", "0.7.1.5")
            version("rarible-core", "2.5.1")
            version("rarible-currency", "1.38.0")
            version("flow-protocol-version", "3.1.13")
            version("blockchain-scanner", "1.1.3")

            alias("flow-sdk").to("com.nftco", "flow-jvm-sdk").versionRef("flow-sdk-ver")
            alias("rarible-core-apm-starter").to("com.rarible.core", "rarible-core-apm-starter").versionRef("rarible-core")
            alias("rarible-core-kafka").to("com.rarible.core", "rarible-core-kafka").versionRef("rarible-core")
            alias("rarible-core-daemon").to("com.rarible.core", "rarible-core-daemon").versionRef("rarible-core")
            alias("rarible-core-telemetry").to("com.rarible.core", "rarible-core-telemetry-starter").versionRef("rarible-core")
            alias("rarible-core-starter").to("com.rarible.core", "rarible-core-starter").versionRef("rarible-core")
            alias("rarible-core-logging").to("com.rarible.core", "rarible-core-logging").versionRef("rarible-core")
            alias("blockchain-scanner-flow").to("com.rarible.blockchain.scanner", "rarible-blockchain-scanner-flow").versionRef("blockchain-scanner")
            alias("blockchain-scanner-model").to("com.rarible.blockchain.scanner", "rarible-blockchain-scanner-flow-model").versionRef("blockchain-scanner")

            alias("flow-model-common").to("com.rarible.protocol.flow", "flow-protocol-model-common").versionRef("flow-protocol-version")
            alias("flow-model-nft").to("com.rarible.protocol.flow", "flow-protocol-model-nft").versionRef("flow-protocol-version")
            alias("flow-model-order").to("com.rarible.protocol.flow", "flow-protocol-model-order").versionRef("flow-protocol-version")

            alias("flow-api").to("com.rarible.protocol.flow", "flow-api").versionRef("flow-protocol-version")
            alias("flow-protocol-api-nft").to("com.rarible.protocol.flow", "flow-protocol-api-nft").versionRef("flow-protocol-version")
            alias("flow-protocol-api-order").to("com.rarible.protocol.flow", "flow-protocol-api-order").versionRef("flow-protocol-version")
            alias("flow-protocol-api-nftorder").to("com.rarible.protocol.flow", "flow-protocol-api-nft-order").versionRef("flow-protocol-version")

            alias("rarible-currency-starer").to("com.rarible.protocol.currency" ,"protocol-client-currency-starter").versionRef("rarible-currency")
            bundle("rarible-core", listOf("rarible-core-daemon", "rarible-core-telemetry"))
            bundle("flow-models", listOf("flow-model-common", "flow-model-nft", "flow-model-order"))
            bundle("flow-apis", listOf("flow-api", "flow-protocol-api-nft", "flow-protocol-api-order", "flow-protocol-api-nftorder"))
        }

        create("testLibs") {
            version("kotest", "4.6.2")
            version("rarible-core", "2.5.1")

            alias("kotest-runner").to("io.kotest", "kotest-runner-junit5").versionRef("kotest")
            alias("rarible-core-test").to("com.rarible.core", "rarible-core-test-common").versionRef("rarible-core")

            bundle("kotest", listOf("kotest-runner"))
        }
    }
}
