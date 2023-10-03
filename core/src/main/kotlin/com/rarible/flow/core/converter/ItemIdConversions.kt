package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.ItemId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

object ItemIdConversions : Iterable<Converter<*, *>> {

    @ReadingConverter
    object Read : Converter<String, ItemId> {
        override fun convert(source: String): ItemId? {
            return ItemId.parse(source)
        }
    }

    @WritingConverter
    object Write : Converter<ItemId, String> {
        override fun convert(source: ItemId): String? {
            return source.toString()
        }
    }

    override fun iterator(): Iterator<Converter<*, *>> {
        return listOf(
            Read,
            Write
        ).iterator()
    }
}
