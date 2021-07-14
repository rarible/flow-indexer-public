package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.Item
import com.rarible.protocol.dto.FlowNftItemDto
import org.springframework.core.convert.converter.Converter

object ItemToDtoConverter : Converter<Item, FlowNftItemDto> {

    override fun convert(item: Item): FlowNftItemDto {
        return FlowNftItemDto(
            item.id,
            item.contract,
            item.tokenId.toInt(),
            item.creator.value,
            item.owner.value,
            item.meta,
            item.date,
            false
        )
    }
}