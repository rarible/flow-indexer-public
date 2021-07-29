package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.Item
import com.rarible.protocol.dto.FlowNftItemDto
import org.springframework.core.convert.converter.Converter

object ItemToDtoConverter : Converter<Item, FlowNftItemDto> {

    override fun convert(item: Item): FlowNftItemDto {
        return FlowNftItemDto(
            item.id.toString(),
            item.contract.formatted,
            item.tokenId.toInt(),
            item.creator.formatted,
            item.owner?.formatted,
            item.meta,
            item.date,
            false
        )
    }
}