package com.rarible.flow.scanner.subscriber

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-rarible-card"], havingValue = "true")
annotation class EnableRaribleCard

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-rarible-card-v2"], havingValue = "true")
annotation class EnableRaribleCardV2

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-rarible-pack"], havingValue = "true")
annotation class EnableRariblePack

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-rarible-pack-v2"], havingValue = "true")
annotation class EnableRariblePackV2

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-rarible-nft"], havingValue = "true")
annotation class EnableRaribleNft

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-storefront-v1"], havingValue = "true")
annotation class EnableStorefrontV1

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-storefront-v2"], havingValue = "true")
annotation class EnableStorefrontV2
