package com.rarible.flow.api.service

import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.protocol.dto.FlowNftItemsDto
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class OrderItemService(
    private val itemRepository: ItemRepository,
    private val orderRepository: OrderRepository
) {

    suspend fun getNftOrderItemsByOwner(owner: String, continuation: String?, size: Int?): Mono<FlowNftItemsDto> {
        return Mono.create { sink ->
            orderRepository.findAllByMakerAndTakerIsNull(FlowAddress(owner)).collectList().subscribe { orders ->
                itemRepository.findAllByIdIn(orders.map { it.itemId }).collectList().subscribe { items ->
                    val data = if (size != null) {
                        items.take(size)
                    } else items

                    sink.success(
                        FlowNftItemsDto(
                            items = data.map(ItemToDtoConverter::convert),
                            continuation = continuation
                        )
                    )
                }
            }
        }
    }


    suspend fun allOnSale(continuation: String?, size: Int?): Mono<FlowNftItemsDto> = Mono.create { sink ->
        orderRepository.findAllByTakerIsNull().collectList().subscribe { orders ->
            itemRepository.findAllByIdIn(orders.map { it.itemId }).collectList().subscribe { items ->
                val data = if (size != null) {
                    items.take(size)
                } else items

                sink.success(
                    FlowNftItemsDto(
                        items = data.map(ItemToDtoConverter::convert),
                        continuation = continuation
                    )
                )
            }
        }
    }

    suspend fun onSaleByCollection(collection: String, continuation: String?, size: Int?): Mono<FlowNftItemsDto> = Mono.create { sink ->
        orderRepository.findAllByTakerIsNullAndCollection(collection).collectList().subscribe { orders ->
            itemRepository.findAllByIdIn(orders.map { it.itemId }).collectList().subscribe { items ->
                val data = if (size != null) {
                    items.take(size)
                } else items

                sink.success(
                    FlowNftItemsDto(
                        items = data.map(ItemToDtoConverter::convert),
                        continuation = continuation
                    )
                )
            }
        }
    }

}
