package com.rarible.flow.api.service

import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.toDto
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.protocol.dto.*
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Service
class ActivitiesService(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository
) {
    fun getNftOrderActivitiesByItem(
        type: List<String>,
        contract: String,
        tokenId: Long,
        continuation: String?,
        size: Int?
    ): Mono<FlowActivitiesDto> {
        var types = type.map { FlowActivityType.valueOf(it) }

        if (types.isEmpty()) {
            types = FlowActivityType.values().toList()
        }

        return itemHistoryRepository.getNftOrderActivitiesByItem(types, contract = FlowAddress(contract), tokenId)
            .collectList().flatMap {
            FlowActivitiesDto(
                items = it.map { it.activity.toDto(it.id, it.date) },
                continuation = continuation
            ).toMono()
        }

    }


}

