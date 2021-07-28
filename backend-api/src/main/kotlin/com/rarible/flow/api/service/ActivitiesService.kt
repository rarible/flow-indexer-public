package com.rarible.flow.api.service

import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.toDto
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.protocol.dto.*
import org.onflow.sdk.Flow
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
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

    fun getNftOrderActivitiesByUser(
        type: List<String>,
        user: List<String>,
        continuation: String?,
        size: Int?
    ): Mono<FlowActivitiesDto> {

        val haveTransferTo = type.isEmpty() || type.contains("TRANSFER_TO")
        val haveTransferFrom = type.isEmpty() || type.contains("TRANSFER_FROM")

        val types = if (type.isEmpty()) {
            FlowActivityType.values().toList()
        } else {
            type.filter { "TRANSFER_TO" != it && "TRANSFER_FROM" != it }.map { FlowActivityType.valueOf(it) }
        }
        val users = user.map { FlowAddress(it) }
        val activities = itemHistoryRepository.getNftOrderActivitiesByUser(types, users)
        val transferFromActivities = if (haveTransferFrom) {
            itemHistoryRepository.getNftOrderTransferFromActivitiesByUser(users)
        } else {
            Flux.empty()
        }

        val transferToActivities = if (haveTransferTo) {
            itemHistoryRepository.getNfrOrderTransferToActivitiesByUser(users)
        } else {
            Flux.empty()
        }

        return Flux.concat(activities, transferFromActivities, transferToActivities).collectList().flatMap {
            FlowActivitiesDto(
                items = it.sortedBy { it.date }.map { it.activity.toDto(it.id, it.date) },
                continuation = continuation
            ).toMono()
        }
    }

    fun getNfdOrderActivitiesByCollection(
        type: List<String>,
        collection: String,
        continuation: String?,
        size: Int?
    ): Mono<FlowActivitiesDto> {
        TODO("Need realize! Or switch to PostgreSQL!")
    }


}

