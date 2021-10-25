package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.protocol.dto.FlowOrderStatusDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import java.math.BigDecimal


@Service
class OrderService(
    private val orderRepository: OrderRepository
) {

    suspend fun orderById(orderId: Long): Order? {
        return orderRepository.coFindById(orderId)
    }

    suspend fun getSellOrdersByMaker(
        makerAddress: FlowAddress,
        originAddress: FlowAddress?,
        cont: String?,
        size: Int?
    ): Flow<Order> {
        return orderRepository.search(
            OrderFilter.ByMaker(makerAddress, originAddress), cont, size, OrderFilter.Sort.LAST_UPDATE
        )
    }

    suspend fun findAll(cont: String?, size: Int?): Flow<Order> {
        return orderRepository.search(
            OrderFilter.All, cont, size, OrderFilter.Sort.LAST_UPDATE
        )
    }

    suspend fun getSellOrdersByCollection(
        collection: String,
        cont: String?,
        size: Int?
    ): Flow<Order> {
        return orderRepository.search(
            OrderFilter.ByCollection(collection), cont, size, OrderFilter.Sort.LAST_UPDATE
        )
    }

    suspend fun findAllByStatus(
        status: List<FlowOrderStatusDto>,
        cont: String?,
        size: Int?
    ): Flow<Order> {
        return orderRepository.search(
            OrderFilter.ByStatus(status), cont, size, OrderFilter.Sort.LAST_UPDATE
        )
    }

    fun ordersByIds(ids: List<Long>): Flow<Order> {
        return orderRepository.findAllByIdIn(ids).asFlow()
    }

    suspend fun getSellOrdersByItemAndStatus(
        itemId: ItemId,
        makerAddress: FlowAddress?,
        currency: FlowAddress?,
        status: List<FlowOrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): Flow<Order> {
        return orderRepository.search(
            OrderFilter.ByItemId(itemId) *
                    OrderFilter.ByMaker(makerAddress) *
                    OrderFilter.ByStatus(status) *
                    OrderFilter.ByCurrency(currency),
            continuation,
            size,
            OrderFilter.Sort.MAKE_PRICE_ASC
        )
    }

    fun currenciesByItemId(itemId: String): Flow<FlowAsset> {
        return orderRepository.search(
            OrderFilter.ByItemId(ItemId.parse(itemId)) *
                    OrderFilter.ByStatus(listOf(FlowOrderStatusDto.ACTIVE)),
            cont = null,
            limit = null
        ).map {
            FlowAssetFungible(contract = it.take.contract, value = BigDecimal.ZERO)
        }
    }
}
