package com.rarible.flow.scanner.model

import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.converter.RaribleNftMintConverter

@JsonCadenceConversion(RaribleNftMintConverter::class)
data class RaribleNftMint(
    val id: Long,
    val creator: String,
    val metadata: Map<String, String>,
    val royalties: List<Part>
)
