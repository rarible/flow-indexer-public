package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.OwnershipId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

object OwnershipIdConversions : Iterable<Converter<*, *>> {

    @ReadingConverter
    object Read : Converter<String, OwnershipId> {
        override fun convert(source: String): OwnershipId {
            return OwnershipId.parse(source)
        }
    }

    @WritingConverter
    object Write : Converter<OwnershipId, String> {
        override fun convert(source: OwnershipId): String {
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
