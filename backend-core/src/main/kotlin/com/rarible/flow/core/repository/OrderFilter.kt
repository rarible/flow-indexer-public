package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.protocol.dto.FlowOrderStatusDto
import org.springframework.data.domain.Sort as SpringSort
import org.springframework.data.mapping.div
import org.springframework.data.mapping.toDotPath
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.isEqualTo
import java.math.BigDecimal


sealed class OrderFilter() {
    enum class Sort {
        LAST_UPDATE,
        MAKE_PRICE_ASC,
        TAKE_PRICE_DESC
    }

    abstract fun criteria(): Criteria

    operator fun times(other: OrderFilter): OrderFilter {
        val empty = Criteria()

        @Suppress("ReplaceCallWithBinaryOperator")
        val finalCriteria = if(this.criteria().equals(empty) && other.criteria().equals(empty)) {
            empty
        } else if (this.criteria().equals(empty)) {
            other.criteria()
        } else if (other.criteria().equals(empty)){
            this.criteria()
        } else {
            Criteria().andOperator(
                this.criteria(),
                other.criteria()
            )
        }
        return ByCriteria(finalCriteria)
    }

    fun toQuery(continuation: String?, limit: Int?, sort: Sort = Sort.LAST_UPDATE): Query {
        val (querySort, criteria) = sortWithCriteria(continuation, sort)
        return Query
            .query(criteria)
            .with(querySort)
            .limit(limit ?: DEFAULT_LIMIT)
    }

    fun sortWithCriteria(continuation: String?, sort: Sort = Sort.LAST_UPDATE): Pair<SpringSort, Criteria> {
        return when (sort) {
            Sort.LAST_UPDATE -> SpringSort.by(
                SpringSort.Order.desc(Order::createdAt.name),
                SpringSort.Order.desc(Order::id.name)
            ) to Cont.scrollDesc(this.criteria(), continuation, Order::createdAt, Order::id)

            Sort.MAKE_PRICE_ASC -> SpringSort.by(
                SpringSort.Order.asc((Order::make / FlowAsset::value).toDotPath()),
                SpringSort.Order.asc(Order::id.name)
            ) to Cont.scrollAsc(this.criteria(), continuation, (Order::make / FlowAsset::value), Order::id)

            Sort.TAKE_PRICE_DESC -> SpringSort.by(
                SpringSort.Order.desc((Order::take / FlowAsset::value).toDotPath()),
                SpringSort.Order.desc(Order::id.name)
            ) to Cont.scrollDesc(this.criteria(), continuation, (Order::take / FlowAsset::value), Order::id)
        }
    }

    data class ByCriteria(private val criteria: Criteria): OrderFilter() {
        override fun criteria(): Criteria {
            return criteria
        }
    }

    object All: OrderFilter() {
        override fun criteria(): Criteria {
            return Criteria()
        }
    }

    data class ByItemId(private val itemId: ItemId): OrderFilter() {
        override fun criteria(): Criteria {
            return (Order::itemId isEqualTo itemId)
        }
    }

    data class ByCollection(val collectionId: String): OrderFilter() {
        override fun criteria(): Criteria {
            return (Order::collection isEqualTo collectionId)
        }
    }

    class ByMaker(val maker: FlowAddress?, val origin: FlowAddress? = null) : OrderFilter() {
        override fun criteria(): Criteria {
            return if(maker == null) {
                Criteria()
            } else {
                Order::maker isEqualTo maker
            }
        }
    }

    class ByCurrency(val currency: FlowAddress?): OrderFilter() {
        override fun criteria(): Criteria {
            if(currency == null) {
                return Criteria()
            } else {
                return (Order::take / FlowAsset::contract).isEqualTo(currency.formatted)
            }
        }
    }

    class ByStatus(val status: List<FlowOrderStatusDto>?) : OrderFilter() {
        override fun criteria(): Criteria {
            return if(status == null) {
                Criteria()
            } else {
                status.foldRight(Criteria()) { s, c ->
                    val additionalCriterias: Criteria = when(s) {
                        FlowOrderStatusDto.ACTIVE -> (Order::cancelled isEqualTo false).andOperator(
                            Order::fill isEqualTo BigDecimal.ZERO
                        )
                        FlowOrderStatusDto.FILLED -> (Order::fill gt BigDecimal.ZERO)
                        FlowOrderStatusDto.HISTORICAL -> Criteria() // TODO
                        FlowOrderStatusDto.INACTIVE -> (Order::cancelled isEqualTo true) //TODO
                        FlowOrderStatusDto.CANCELLED -> (Order::cancelled isEqualTo true)
                    }
                    return c.orOperator(additionalCriterias)
                }
            }
        }
    }

    companion object {
        val DEFAULT_LIMIT = 50
    }
}