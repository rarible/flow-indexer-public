package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.ItemMeta
import com.rarible.protocol.dto.AudioContentDto
import com.rarible.protocol.dto.HtmlContentDto
import com.rarible.protocol.dto.ImageContentDto
import com.rarible.protocol.dto.MetaAttributeDto
import com.rarible.protocol.dto.MetaContentItemDto
import com.rarible.protocol.dto.MetaDto
import com.rarible.protocol.dto.Model3dContentDto
import com.rarible.protocol.dto.UnknownContentDto
import com.rarible.protocol.dto.VideoContentDto
import org.springframework.core.convert.converter.Converter
import java.util.*

object ItemMetaToDtoConverter : Converter<ItemMeta, MetaDto> {

    override fun convert(source: ItemMeta): MetaDto {
        return MetaDto(name = source.name,
            description = source.description,
            createdAt = source.createdAt,
            tags = source.tags,
            genres = source.genres,
            language = source.language,
            rights = source.rights,
            rightsUrl = source.rightsUrl,
            externalUri = source.externalUri,
            originalMetaUri = source.originalMetaUri,
            attributes = source.attributes.map {
                MetaAttributeDto(
                    key = it.key, value = it.value, format = it.format, type = it.type
                )
            },
            contents = source.contentUrls,
            content = source.content?.map(::convert),
            raw = source.raw?.let { Base64.getEncoder().encodeToString(it) })
    }

    private fun convert(source: ItemMeta.Content) = when (source.type) {
        ItemMeta.Content.Type.IMAGE -> ImageContentDto(
            fileName = source.fileName,
            url = source.url,
            representation = convert(source.representation),
            mimeType = source.mimeType,
            size = source.size,
            width = source.width,
            height = source.height,
        )
        ItemMeta.Content.Type.VIDEO -> VideoContentDto(
            fileName = source.fileName,
            url = source.url,
            representation = convert(source.representation),
            mimeType = source.mimeType,
            size = source.size,
            width = source.width,
            height = source.height,
        )
        ItemMeta.Content.Type.AUDIO -> AudioContentDto(
            fileName = source.fileName,
            url = source.url,
            representation = convert(source.representation),
            mimeType = source.mimeType,
            size = source.size,
        )
        ItemMeta.Content.Type.MODEL_3D -> Model3dContentDto(
            fileName = source.fileName,
            url = source.url,
            representation = convert(source.representation),
            mimeType = source.mimeType,
            size = source.size,
        )
        ItemMeta.Content.Type.HTML -> HtmlContentDto(
            fileName = source.fileName,
            url = source.url,
            representation = convert(source.representation),
            mimeType = source.mimeType,
            size = source.size,
        )
        ItemMeta.Content.Type.UNKNOWN -> UnknownContentDto(
            fileName = source.fileName,
            url = source.url,
            representation = convert(source.representation),
            mimeType = source.mimeType,
            size = source.size,
        )
    }

    private fun convert(source: ItemMeta.Content.Representation): MetaContentItemDto.Representation =
        source.name.let { MetaContentItemDto.Representation.valueOf(it) }
}
