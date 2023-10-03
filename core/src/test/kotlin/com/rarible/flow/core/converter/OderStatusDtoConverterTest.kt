package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.OrderStatus
import com.rarible.protocol.dto.FlowOrderStatusDto
import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

internal class OderStatusDtoConverterTest : FunSpec({
    test("should convert all statuses") {
        forAll(
            *FlowOrderStatusDto.values()
        ) { dto ->
            OderStatusDtoConverter.convert(dto) shouldBe (
                when (dto) {
                    FlowOrderStatusDto.ACTIVE -> OrderStatus.ACTIVE
                    FlowOrderStatusDto.FILLED -> OrderStatus.FILLED
                    FlowOrderStatusDto.HISTORICAL -> OrderStatus.HISTORICAL
                    FlowOrderStatusDto.INACTIVE -> OrderStatus.INACTIVE
                    FlowOrderStatusDto.CANCELLED -> OrderStatus.CANCELLED
                }
                )
        }
    }

    test("should convert null list") {
        OderStatusDtoConverter.convert(null) shouldBe emptyList()
    }

    test("should convert emtpty list") {
        OderStatusDtoConverter.convert(emptyList()) shouldBe emptyList()
    }

    test("should convert list") {
        OderStatusDtoConverter.convert(
            listOf(FlowOrderStatusDto.ACTIVE, FlowOrderStatusDto.FILLED)
        ) shouldContainAll listOf(OrderStatus.ACTIVE, OrderStatus.FILLED)
    }
})
