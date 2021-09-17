package com.rarible.flow.api.service

import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.protocol.dto.FlowOrderDto
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono


@Service
class OrderService(
    private val orderRepository: OrderRepository
) {

    suspend fun orderById(orderId: Long): Mono<FlowOrderDto> {
        val order = orderRepository.findById(orderId).awaitFirst()
        return OrderToDtoConverter.convert(order).toMono()
    }
}
