package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.Item
import com.rarible.protocol.dto.FlowNftItemDto
import org.springframework.core.convert.converter.Converter
import java.time.Instant

object ItemToDtoConverter : Converter<Item, FlowNftItemDto> {

    override fun convert(item: Item): FlowNftItemDto {
        return FlowNftItemDto(
            id = item.id.toString(),
            contract = item.contract,
            tokenId = item.tokenId.toInt(),
            creator = item.creator.formatted,
            owner = item.owner?.formatted,
            meta = item.meta,
            date = item.date,
            listed = false,
            collection = item.collection
        )
    }
}
