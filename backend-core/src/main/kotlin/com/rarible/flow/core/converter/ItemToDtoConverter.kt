package com.rarible.flow.core.converter

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.Part
import com.rarible.protocol.dto.FlowCreatorDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowRoyaltyDto
import com.rarible.protocol.dto.MetaDto
import org.springframework.core.convert.converter.Converter
import java.math.BigInteger

object ItemToDtoConverter : Converter<Item, FlowNftItemDto> {

    override fun convert(item: Item): FlowNftItemDto {
        return FlowNftItemDto(
            id = item.id.toString(),
            collection = item.contract,
            tokenId = item.tokenId.toBigInteger(),
            creators = convert(item.creator),
            owners = item.owner?.let { listOf(it.formatted) } ?: emptyList(),
            meta = MetaDto(
                "", "", emptyList(), emptyList(), ""
            ),
            mintedAt = item.mintedAt,
            lastUpdatedAt = item.updatedAt,
            royalties = convert(item.royalties),
            metaUrl = item.meta,
            supply = BigInteger.ONE,
            deleted = item.owner == null
        )
    }

    private fun convert(creator: FlowAddress) = listOf(
        FlowCreatorDto(creator.formatted, 10000.toBigDecimal())
    )

    private fun convert(royalties: List<Part>) = royalties.map {
        FlowRoyaltyDto(it.address.formatted, it.fee.toBigDecimal())
    }
}
