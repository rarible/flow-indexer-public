package com.rarible.flow.core.converter

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.Part
import com.rarible.protocol.dto.FlowCreatorDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowRoyaltyDto
import org.springframework.core.convert.converter.Converter
import java.math.BigDecimal
import java.math.BigInteger

object ItemToDtoConverter : Converter<Item, FlowNftItemDto> {

    override fun convert(item: Item): FlowNftItemDto {
        return FlowNftItemDto(
            id = item.id.toString(),
            collection = item.collection,
            tokenId = item.tokenId.toBigInteger(),
            creators = convert(item.creator),
            owner = item.owner?.formatted,
            mintedAt = item.mintedAt,
            lastUpdatedAt = item.updatedAt,
            royalties = convert(item.royalties),
            metaUrl = item.meta,
            supply = if (item.owner == null) BigInteger.ZERO else BigInteger.ONE,
            deleted = item.owner == null,

        )
    }

    private fun convert(creator: FlowAddress) = listOf(
        FlowCreatorDto(creator.formatted, BigDecimal.ONE)
    )

    private fun convert(royalties: List<Part>) = royalties.map {
        FlowRoyaltyDto(it.address.formatted, it.fee.toBigDecimal())
    }
}
