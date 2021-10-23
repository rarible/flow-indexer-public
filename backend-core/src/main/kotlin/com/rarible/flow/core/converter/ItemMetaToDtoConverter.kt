package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.ItemMeta
import com.rarible.protocol.dto.MetaAttributeDto
import com.rarible.protocol.dto.MetaDto
import org.springframework.core.convert.converter.Converter
import java.util.*

object ItemMetaToDtoConverter : Converter<ItemMeta, MetaDto> {

    override fun convert(source: ItemMeta): MetaDto {
        return MetaDto(
            name = source.name,
            description = source.description,
            attributes = source.attributes.map {
                MetaAttributeDto(
                    key = it.key,
                    value = it.value,
                    format = it.format,
                    type = it.type
                )
            },
            contents = source.contentUrls,
            raw = source.raw?.let { Base64.getEncoder().encodeToString(it) }
        )
    }
}
