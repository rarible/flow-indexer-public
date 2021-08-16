package com.rarible.flow.api.service

import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.toDto
import com.rarible.flow.core.repository.ActivityContinuation
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.protocol.dto.FlowActivitiesDto
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.ZoneOffset

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

        val cont = ActivityContinuation.of(continuation)

        var flux = if (continuation == null) {
            itemHistoryRepository.getNftOrderActivitiesByItem(types = types, contract = FlowAddress(contract), tokenId)
        } else {
            itemHistoryRepository.getNftOrderActivitiesByItemAfterDate(
                types = types,
                contract = FlowAddress(contract),
                tokenId,
                cont!!.afterDate
            )
        }

        if (size != null) {
            flux = flux.take(size.toLong())
        }
        return flux
            .collectList().flatMap {
                FlowActivitiesDto(
                    items = it.map { h -> h.activity.toDto(h.id, h.date) },
                    total = it.size,
                    continuation = "${ActivityContinuation(it.last().date.toInstant(ZoneOffset.UTC))}"
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
        val cont = ActivityContinuation.of(continuation)

        val activities: Flux<ItemHistory>
        val transferFromActivities: Flux<ItemHistory>
        val transferToActivities: Flux<ItemHistory>
        if (cont == null) {
            activities = itemHistoryRepository.getNftOrderActivitiesByUser(types, users)
            transferFromActivities = if (haveTransferFrom) {
                itemHistoryRepository.getNftOrderTransferFromActivitiesByUser(users)
            } else Flux.empty()

            transferToActivities = if (haveTransferTo) {
                itemHistoryRepository.getNfrOrderTransferToActivitiesByUser(users)
            } else Flux.empty()
        } else {
            activities = itemHistoryRepository.getNftOrderActivitiesByUserAfterDate(types, users, cont.afterDate)
            transferFromActivities = if (haveTransferFrom) {
                itemHistoryRepository.getNftOrderTransferFromActivitiesByUserAfterDate(users, cont.afterDate)
            } else Flux.empty()

            transferToActivities = if (haveTransferTo) {
                itemHistoryRepository.getNfrOrderTransferToActivitiesByUserAfterDate(users, cont.afterDate)
            } else Flux.empty()
        }

        val concat = Flux.concat(activities, transferFromActivities, transferToActivities)
        val resultFlux = if (size == null) concat else concat.take(size.toLong())

        return resultFlux.collectList().flatMap {
            FlowActivitiesDto(
                items = it.sortedBy(ItemHistory::date).map { h -> h.activity.toDto(h.id, h.date) },
                total = it.size,
                continuation = continuation
            ).toMono()
        }
    }

    fun getNftOrderAllActivities(type: List<String>, continuation: String?, size: Int?): Mono<FlowActivitiesDto> {
        val types = if (type.isEmpty()) {
            FlowActivityType.values().toList()
        } else {
            type.map { FlowActivityType.valueOf(it) }.toList()
        }

        val cont = ActivityContinuation.of(continuation)
        var flux = if (cont == null) {
            itemHistoryRepository.getAllActivities(types)
        } else {
            itemHistoryRepository.getAllActivitiesAfterDate(types, cont.afterDate)
        }

        if (size != null) {
            flux = flux.take(size.toLong())
        }

        return flux.collectList().flatMap {
            FlowActivitiesDto(
                items = it.map { h -> h.activity.toDto(h.id, h.date) },
                continuation = "${ActivityContinuation(it.last().date.toInstant(ZoneOffset.UTC))}",
                total = it.size
            ).toMono()
        }
    }

    fun getNfdOrderActivitiesByCollection(
        type: List<String>,
        collection: String,
        continuation: String?,
        size: Int?
    ): Mono<FlowActivitiesDto> {
        val types =
            if (type.isEmpty()) FlowActivityType.values().toList() else type.map { FlowActivityType.valueOf(it) }

        val cont = ActivityContinuation.of(continuation)

        var flux = if (cont == null) {
            itemHistoryRepository.getAllActivitiesByItemCollection(types, collection)
        } else {
            itemHistoryRepository.getAllActivitiesByItemCollectionAfterDate(types, collection, cont.afterDate)
        }

        if (size != null) {
            flux = flux.take(size.toLong())
        }

        return flux.collectList()
            .flatMap { history ->
                FlowActivitiesDto(
                    total = history.size,
                    continuation = "${ActivityContinuation(history.last().date.toInstant(ZoneOffset.UTC))}",
                    items = history.map { it.activity.toDto(it.id, it.date) }
                ).toMono()
            }
    }


}

