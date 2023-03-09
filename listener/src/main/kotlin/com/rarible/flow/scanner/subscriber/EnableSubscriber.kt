package com.rarible.flow.scanner.subscriber

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-rarible-card"], havingValue = "true")
annotation class EnableRaribleCard

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-rarible-pack"], havingValue = "true")
annotation class EnableRariblePack

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.feature-flags.enable-rarible-nft"], havingValue = "true")
annotation class EnableRaribleNft
