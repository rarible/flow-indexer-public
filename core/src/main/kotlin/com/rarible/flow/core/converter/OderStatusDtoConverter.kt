package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.OrderStatus
import com.rarible.protocol.dto.FlowOrderStatusDto
import org.springframework.core.convert.converter.Converter

object OderStatusDtoConverter : Converter<FlowOrderStatusDto, OrderStatus> {
    override fun convert(source: FlowOrderStatusDto): OrderStatus {
        return when (source) {
            FlowOrderStatusDto.ACTIVE -> OrderStatus.ACTIVE
            FlowOrderStatusDto.FILLED -> OrderStatus.FILLED
            FlowOrderStatusDto.HISTORICAL -> OrderStatus.HISTORICAL
            FlowOrderStatusDto.INACTIVE -> OrderStatus.INACTIVE
            FlowOrderStatusDto.CANCELLED -> OrderStatus.CANCELLED
        }
    }

    fun convert(source: Collection<FlowOrderStatusDto>?): List<OrderStatus> {
        return source?.map(this::convert) ?: emptyList()
    }
}
