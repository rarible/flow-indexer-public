package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.ItemMeta
import com.rarible.protocol.dto.FlowItemMetaDto
import org.springframework.core.convert.converter.Converter

object ItemMetaToDtoConverter : Converter<ItemMeta, FlowItemMetaDto> {

    override fun convert(source: ItemMeta): FlowItemMetaDto {
        return FlowItemMetaDto(
            itemId = source.itemId.toString(),
            description = source.description,
            title = source.title,
            uri = source.uri.toString()
        )
    }
}