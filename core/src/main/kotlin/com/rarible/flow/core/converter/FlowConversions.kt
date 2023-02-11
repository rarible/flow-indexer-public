package com.rarible.flow.core.converter

import com.nftco.flow.sdk.FlowAddress
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter


object FlowConversions: Iterable<Converter<*, *>> {

    @ReadingConverter
    object ReadAddress: Converter<String, FlowAddress> {
        override fun convert(source: String): FlowAddress {
            return FlowAddress(source)
        }
    }

    @WritingConverter
    object WriteAddress: Converter<FlowAddress, String> {
        override fun convert(source: FlowAddress): String {
            return source.formatted
        }
    }

    override fun iterator(): Iterator<Converter<*, *>> {
        return listOf(
            ReadAddress,
            WriteAddress
        ).iterator()
    }
}
