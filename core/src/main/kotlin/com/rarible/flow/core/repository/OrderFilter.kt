package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.repository.filters.CriteriaProduct
import com.rarible.flow.core.repository.filters.DbFilter
import com.rarible.flow.core.repository.filters.ScrollingSort
import org.bson.types.Decimal128
import org.springframework.data.mapping.div
import org.springframework.data.mapping.toDotPath
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.gte
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.reflect.KProperty
import org.springframework.data.domain.Sort as SpringSort

sealed class OrderFilter : DbFilter<Order>, CriteriaProduct<OrderFilter> {
    enum class Sort : ScrollingSort<Order> {
        LATEST_FIRST {

            override fun springSort(): SpringSort = SpringSort.by(
                SpringSort.Order.desc(Order::lastUpdatedAt.name),
                SpringSort.Order.desc(Order::id.name)
            )

            override fun scroll(criteria: Criteria, continuation: String?): Criteria =
                Cont.scrollDesc(criteria, continuation, Order::lastUpdatedAt, Order::id)

            override fun nextPage(entity: Order): String {
                return Cont.toString(entity.lastUpdatedAt, entity.id)
            }
        },
        EARLIEST_FIRST {

            override fun springSort(): SpringSort = SpringSort.by(
                SpringSort.Order.asc(Order::lastUpdatedAt.name),
                SpringSort.Order.asc(Order::id.name)
            )

            override fun scroll(criteria: Criteria, continuation: String?): Criteria =
                Cont.scrollAsc(criteria, continuation, Order::lastUpdatedAt, Order::id)

            override fun nextPage(entity: Order): String {
                return Cont.toString(entity.lastUpdatedAt, entity.id)
            }
        },
        AMOUNT_ASC {

            override fun springSort(): SpringSort = SpringSort.by(
                SpringSort.Order.asc(Order::amount.name),
                SpringSort.Order.asc(Order::id.name)
            )

            override fun scroll(criteria: Criteria, continuation: String?): Criteria =
                Cont.scrollAsc(criteria, continuation, Order::amount, Order::id)

            override fun nextPage(entity: Order): String {
                return Cont.toString(entity.amount, entity.id)
            }
        },
        AMOUNT_DESC {

            override fun springSort(): SpringSort = SpringSort.by(
                SpringSort.Order.desc(Order::amount.name),
                SpringSort.Order.desc(Order::id.name)
            )

            override fun scroll(criteria: Criteria, continuation: String?): Criteria =
                Cont.scrollDesc(criteria, continuation, Order::amount, Order::id)

            override fun nextPage(entity: Order): String {
                return Cont.toString(entity.amount, entity.id)
            }
        },

        UPDATED_AT_ASC {

            override fun springSort() = SpringSort.by(
                SpringSort.Order.asc(Order::dbUpdatedAt.name),
                SpringSort.Order.asc(Order::id.name)
            )

            override fun scroll(criteria: Criteria, continuation: String?) =
                Cont.scrollAsc(criteria, continuation, Order::dbUpdatedAt, Order::id)

            override fun nextPage(entity: Order) =
                Cont.toString(entity.dbUpdatedAt, entity.id)
        },

        UPDATED_AT_DESC {

            override fun springSort() = SpringSort.by(
                SpringSort.Order.desc(Order::dbUpdatedAt.name),
                SpringSort.Order.desc(Order::id.name)
            )

            override fun scroll(criteria: Criteria, continuation: String?) =
                Cont.scrollDesc(criteria, continuation, Order::dbUpdatedAt, Order::id)

            override fun nextPage(entity: Order) =
                Cont.toString(entity.dbUpdatedAt, entity.id)
        },
    }

    override fun byCriteria(criteria: Criteria): OrderFilter {
        return ByCriteria(criteria)
    }

    private data class ByCriteria(private val criteria: Criteria) : OrderFilter() {

        override fun criteria(): Criteria {
            return criteria
        }
    }

    object All : OrderFilter() {

        override fun criteria(): Criteria {
            return Criteria()
        }
    }

    data class ByItemId(private val itemId: ItemId) : OrderFilter() {

        override fun criteria(): Criteria {
            return (Order::itemId isEqualTo itemId)
        }
    }

    data class ByCollection(val collectionId: String) : OrderFilter() {

        override fun criteria(): Criteria {
            return (Order::collection isEqualTo collectionId)
        }
    }

    class ByMaker(val maker: FlowAddress?, val origin: FlowAddress? = null) : OrderFilter() {

        override fun criteria(): Criteria {
            return if (maker == null) {
                Criteria()
            } else {
                Order::maker isEqualTo maker
            }
        }
    }

    class ByMakers(val makers: List<FlowAddress>) : OrderFilter() {

        override fun criteria(): Criteria {
            return if (makers.isEmpty()) {
                Criteria()
            } else {
                Order::maker inValues makers.map { it.formatted }
            }
        }
    }

    class BySellingCurrency(val currency: String?) : OrderFilter() {

        override fun criteria(): Criteria {
            return if (currency == null) {
                Criteria()
            } else {
                (Order::take / FlowAsset::contract).isEqualTo(currency)
            }
        }
    }

    class ByBiddingCurrency(val currency: String?) : OrderFilter() {

        override fun criteria(): Criteria {
            return if (currency == null) {
                Criteria()
            } else {
                (Order::make / FlowAsset::contract).isEqualTo(currency)
            }
        }
    }

    class ByStatus(val status: List<OrderStatus>?) : OrderFilter() {

        constructor(vararg statuses: OrderStatus) : this(statuses.asList())

        override fun criteria(): Criteria {
            return if (status == null || status.isEmpty()) {
                Criteria()
            } else {
                Order::status inValues status
            }
        }
    }

    data class ByMakeValue(val cmp: Comparator, val value: BigDecimal) : OrderFilter() {
        enum class Comparator {
            LTE, GT
        }

        override fun criteria(): Criteria {
            val criteria = Criteria((Order::make / FlowAsset::value).toDotPath())
            val decValue = Decimal128(value)
            return when (cmp) {
                Comparator.LTE -> criteria.lte(decValue)
                Comparator.GT -> criteria.gt(decValue)
            }
        }
    }

    data class ByDateAfter(val dateField: KProperty<LocalDateTime>, val start: LocalDateTime?) : OrderFilter() {
        constructor(dateField: KProperty<LocalDateTime>, inst: Instant?) : this(
            dateField, inst?.atZone(ZoneOffset.UTC)?.toLocalDateTime()
        )

        override fun criteria(): Criteria {
            return if (start == null) {
                Criteria()
            } else {
                dateField gte start
            }
        }
    }

    data class ByDateBefore(val dateField: KProperty<LocalDateTime>, val end: LocalDateTime?) : OrderFilter() {
        constructor(dateField: KProperty<LocalDateTime>, inst: Instant?) : this(
            dateField, inst?.atZone(ZoneOffset.UTC)?.toLocalDateTime()
        )

        override fun criteria(): Criteria {
            return if (end == null) {
                Criteria()
            } else {
                dateField lt end
            }
        }
    }

    object OnlySell : OrderFilter() {

        override fun criteria(): Criteria {
            return Criteria("${Order::make.name}.${FlowAssetNFT::tokenId.name}").exists(true)
        }
    }

    object OnlyBid : OrderFilter() {

        override fun criteria(): Criteria {
            return Criteria("${Order::take.name}.${FlowAssetNFT::tokenId.name}").exists(true)
        }
    }
}
