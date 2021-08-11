package com.rarible.flow.api.service

import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderContinuation
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.protocol.dto.FlowNftItemsDto
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink

@Service
class OrderItemService(
    private val itemRepository: ItemRepository,
    private val orderRepository: OrderRepository
) {

    suspend fun getNftOrderItemsByOwner(owner: String, continuation: String?, size: Int?): Mono<FlowNftItemsDto> {
        return Mono.create { sink ->
            val cont = OrderContinuation.of(continuation)
            val orderFlux = if (cont == null) {
                orderRepository.findAllByMakerAndTakerIsNullOrderByCreatedAtDesc(FlowAddress(owner))
            } else {
                orderRepository.findAllByMakerAndCreatedAtAfterAndTakerIsNullAndIdAfterOrderByCreatedAtDesc(
                    FlowAddress(
                        owner
                    ), cont.afterDate, cont.afterId
                )
            }
            doAnswer(orderFlux, size, sink)
        }
    }


    suspend fun allOnSale(continuation: String?, size: Int?): Mono<FlowNftItemsDto> = Mono.create { sink ->

        val cont = OrderContinuation.of(continuation)

        val flux = if (cont == null) {
            orderRepository.findAllByTakerIsNullOrderByCreatedAtDesc()
        } else {
            orderRepository.findAllByTakerIsNullAndCreatedAtAfterAndIdGreaterThanOrderByCreatedAtDesc(
                cont.afterDate,
                cont.afterId
            )
        }
        doAnswer(flux, size, sink)
    }

    suspend fun onSaleByCollection(collection: String, continuation: String?, size: Int?): Mono<FlowNftItemsDto> =
        Mono.create { sink ->

            val cont = OrderContinuation.of(continuation)

            val flux = if (cont == null) {
                orderRepository.findAllByTakerIsNullAndCollectionOrderByCreatedAtDesc(collection)
            } else {
                orderRepository.findAllByTakerIsNullAndCollectionAndCreatedAtAfterAndIdAfterOrderByCreatedAtDesc(
                    collection,
                    cont.afterDate,
                    cont.afterId
                )
            }

            doAnswer(flux, size, sink)
        }


    private fun doAnswer(
        flux: Flux<Order>,
        size: Int?,
        sink: MonoSink<FlowNftItemsDto>
    ) {
        flux.collectList().subscribe { orders ->
            val itemsFlux = if (size == null) {
                itemRepository.findAllByIdIn(orders.map { it.itemId })
            } else {
                itemRepository.findAllByIdIn(orders.map { it.itemId }).take(size.toLong(), true)
            }
            itemsFlux.collectList().subscribe { items ->

                sink.success(
                    FlowNftItemsDto(
                        total = items.size,
                        items = items.map(ItemToDtoConverter::convert),
                        continuation = "${OrderContinuation(orders.last().createdAt, orders.last().id)}"
                    )
                )
            }
        }
    }
}
