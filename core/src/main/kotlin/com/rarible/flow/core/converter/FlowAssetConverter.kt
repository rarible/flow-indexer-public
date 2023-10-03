package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.FlowAssetEmpty
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.protocol.dto.FlowAssetDto
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAssetNFTDto
import org.springframework.core.convert.converter.Converter
import java.math.BigDecimal

object FlowAssetConverter : Converter<FlowAsset, FlowAssetDto> {
    override fun convert(source: FlowAsset): FlowAssetDto {
        return when (source) {
            is FlowAssetNFT -> FlowAssetNFTDto(
                contract = source.contract,
                value = source.value,
                tokenId = source.tokenId.toBigInteger()
            )
            is FlowAssetFungible -> FlowAssetFungibleDto(
                contract = source.contract,
                value = source.value,
            )
            is FlowAssetEmpty -> FlowAssetFungibleDto(
                contract = "",
                value = BigDecimal.ZERO
            )
        }
    }
}
