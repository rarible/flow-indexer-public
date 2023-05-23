package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.ItemCollection
import com.rarible.protocol.dto.FlowNftCollectionDto

object FlowNftCollectionDtoConverter {
    fun convert(source: ItemCollection): FlowNftCollectionDto {
        return FlowNftCollectionDto(
            id = source.id,
            owner = source.owner.formatted,
            name = source.name,
            symbol = source.symbol,
            features = source.features.map { f -> FlowNftCollectionDto.Features.valueOf(f) }
        )
    }
}