package com.rarible.flow.scanner.subscriber

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-rarible-card"], havingValue = "true")
annotation class EnableRaribleCard

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-rarible-card-v2-meta"], havingValue = "true")
annotation class EnableRaribleCardV2Meta

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-barbie-token"], havingValue = "true", matchIfMissing = false)
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
@ConditionalOnProperty(name = ["app.feature-flags.enable-rarible-barbie-card"], havingValue = "true")
annotation class EnableRaribleBarbieCard

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-rarible-barbie-pack"], havingValue = "true")
annotation class EnableRaribleBarbiePack

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-rarible-barbie-token"], havingValue = "true")
annotation class EnableRaribleBarbieToken

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
@ConditionalOnProperty(name = ["app.feature-flags.enable-barbie-card"], havingValue = "true", matchIfMissing = false)
annotation class EnableBarbieCard

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-barbie-pack"], havingValue = "true", matchIfMissing = false)
annotation class EnableBarbiePack

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-barbie-token"], havingValue = "true", matchIfMissing = false)
annotation class EnableBarbieToken
