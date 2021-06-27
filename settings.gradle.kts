rootProject.name = "flow-nft-indexer"
enableFeaturePreview("VERSION_CATALOGS")

include(
    "scanner",
    "backend-api-model",
    "backend-api",
    "backend-core",
    "backend-listener",
    "converters",
//    "daemon",
//    "kafka"
)

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("flow-sdk-ver", "0.2.4")
            version("rarible-core", "1.3-spring25-SNAPSHOT")

            alias("flow-sdk").to("com.nftco", "flow-jvm-sdk").versionRef("flow-sdk-ver")
            alias("rarible-core-kafka").to("com.rarible.core", "rarible-core-kafka").versionRef("rarible-core")
            alias("rarible-core-daemon").to("com.rarible.core", "rarible-core-daemon").versionRef("rarible-core")
            alias("rarible-core-telemetry").to("com.rarible.core", "rarible-core-telemetry-starter").versionRef("rarible-core")

            bundle("rarible-core", listOf("rarible-core-daemon", "rarible-core-telemetry"))
        }

        create("testLibs") {
            version("kotest", "4.6.0")
            version("rarible-core", "1.3-SNAPSHOT.spring25")

            alias("kotest").to("io.kotest", "kotest-runner-junit5").versionRef("kotest")
            alias("rarible-core-test").to("com.rarible.core", "rarible-core-test-common").versionRef("rarible-core")
        }
    }
}

