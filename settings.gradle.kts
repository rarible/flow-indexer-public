rootProject.name = "flow-nft-indexer"
enableFeaturePreview("VERSION_CATALOGS")

include(
    "scanner",
    "backend-api-model",
    "backend-api",
    "backend-core",
    "backend-listener",
    "converters"
)

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("flow-sdk-ver", "0.2.4")
            alias("flow-sdk").to("com.nftco", "flow-jvm-sdk").versionRef("flow-sdk-ver")
        }

        create("testLibs") {
            version("kotest", "4.6.0")
            version("rarible-core", "1.2-SNAPSHOT")

            alias("kotest").to("io.kotest", "kotest-runner-junit5").versionRef("kotest")
            alias("rarible-core-test").to("com.rarible.core", "rarible-core-test-common").versionRef("rarible-core")
        }
    }
}

