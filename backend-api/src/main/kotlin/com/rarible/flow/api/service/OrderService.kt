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
        size: Int?,
        sort: OrderFilter.Sort
    ): Flow<Order> {
        return orderRepository.search(
            OrderFilter.ByMaker(makerAddress, originAddress), cont, size, sort
        ).asFlow()
    }

    suspend fun findAll(cont: String?, size: Int?, sort: OrderFilter.Sort): Flow<Order> {
        return orderRepository.search(
            OrderFilter.All, cont, size, sort
        ).asFlow()
    }

    suspend fun getSellOrdersByCollection(
        collection: String,
        cont: String?,
        size: Int?,
        sort: OrderFilter.Sort
    ): Flow<Order> {
        return orderRepository.search(
            OrderFilter.ByCollection(collection), cont, size, sort
        ).asFlow()
    }

    suspend fun findAllByStatus(
        status: List<FlowOrderStatusDto>,
        cont: String?,
        size: Int?,
        sort: OrderFilter.Sort
    ): Flow<Order> {
        return orderRepository.search(
            OrderFilter.ByStatus(status), cont, size, sort
        ).asFlow()
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
        size: Int?,
        sort: OrderFilter.Sort
    ): Flow<Order> {
        return orderRepository.search(
            OrderFilter.ByItemId(itemId) *
                    OrderFilter.ByMaker(makerAddress) *
                    OrderFilter.ByStatus(status) *
                    OrderFilter.ByCurrency(currency),
            continuation,
            size,
            sort
        ).asFlow()
    }

    fun currenciesByItemId(itemId: String): Flow<FlowAsset> {
        return orderRepository.search(
            OrderFilter.ByItemId(ItemId.parse(itemId)) *
                    OrderFilter.ByStatus(listOf(FlowOrderStatusDto.ACTIVE)),
            cont = null,
            limit = null
        ).asFlow().map {
            FlowAssetFungible(contract = it.take.contract, value = BigDecimal.ZERO)
        }
    }
}
