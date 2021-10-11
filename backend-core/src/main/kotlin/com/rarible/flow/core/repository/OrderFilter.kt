package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Order
import com.rarible.protocol.dto.FlowOrderStatusDto
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.isEqualTo
import java.math.BigDecimal


sealed class OrderFilter(open val sort: Sort = Sort.LAST_UPDATE) {
    enum class Sort {
        LAST_UPDATE
    }

    abstract fun criteria(): Criteria

    object All: OrderFilter() {
        override fun criteria(): Criteria {
            return Criteria()
        }
    }

    data class ByCollection(val collectionId: String): OrderFilter() {
        override fun criteria(): Criteria {
            return (Order::collection isEqualTo collectionId)
        }
    }

    class ByMaker(val maker: FlowAddress, val origin: FlowAddress?) : OrderFilter() {
        override fun criteria(): Criteria {
            return Order::maker isEqualTo maker
        }
    }

    class ByStatus(val status: List<FlowOrderStatusDto>) : OrderFilter() {
        override fun criteria(): Criteria {
            return status.foldRight(Criteria()) { s, c ->
                val additionalCriteris: Criteria = when(s) {
                    FlowOrderStatusDto.ACTIVE -> (Order::cancelled isEqualTo false).andOperator(
                        Order::fill isEqualTo BigDecimal.ZERO
                    )
                    FlowOrderStatusDto.FILLED -> (Order::fill gt BigDecimal.ZERO)
                    FlowOrderStatusDto.HISTORICAL -> Criteria() // TODO
                    FlowOrderStatusDto.INACTIVE -> (Order::cancelled isEqualTo true) //TODO
                    FlowOrderStatusDto.CANCELLED -> (Order::cancelled isEqualTo true)
                }
                return c.orOperator(additionalCriteris)
            }
        }
    }

}