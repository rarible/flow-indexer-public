package com.rarible.flow.api.controller

import com.rarible.flow.api.service.ActivitiesService
import com.rarible.flow.core.converter.ItemHistoryToDtoConverter
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.repository.ItemHistoryFilter
import com.rarible.flow.core.repository.filters.ScrollingSort
import com.rarible.flow.enum.safeOf
import com.rarible.flow.log.Log
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftOrderActivityControllerApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@FlowPreview
@RestController
@CrossOrigin
class NftOrderActivityController(
    private val service: ActivitiesService
) : FlowNftOrderActivityControllerApi {

    override suspend fun getNftOrderActivitiesByCollection(
        type: List<String>,
        collection: String,
        continuation: String?,
        size: Int?,
        sort: String?,
    ): ResponseEntity<FlowActivitiesDto> {
        return result(type, sort, size) { types, srt ->
            service.getByCollection(types, collection, continuation, size, srt)
        }
    }

    override suspend fun getNftOrderActivitiesByItem(
        type: List<String>,
        contract: String,
        tokenId: Long,
        continuation: String?,
        size: Int?,
        sort: String?,
    ): ResponseEntity<FlowActivitiesDto> {
        return result(type, sort, size) { types, srt ->
            service.getByItem(types, contract, tokenId, continuation, size, srt)
        }
    }

    override suspend fun getNftOrderActivitiesByUser(
        type: List<String>,
        user: List<String>,
        from: Long?,
        to: Long?,
        continuation: String?,
        size: Int?,
        sort: String?,
    ): ResponseEntity<FlowActivitiesDto> {
        val start = from?.let {Instant.ofEpochMilli(from)}
        val end = to?.let {Instant.ofEpochMilli(to)}

        return result(type, sort, size) { types, srt ->
            service.getByUser(types, user, continuation, start, end, size, srt)
        }
    }

    override suspend fun getNftOrderAllActivities(
        type: List<String>,
        continuation: String?,
        size: Int?,
        sort: String?,
    ): ResponseEntity<FlowActivitiesDto> {
        logger.info("Getting all activities for types {}; cursor {}, size {}, sort {}", type, continuation, size, sort)
        return result(type, sort, size) { types, srt ->
            val activities = service.getAll(types, continuation, size, srt)
            logger.info("Converting activities result...")
            activities
        }
    }

    private suspend fun result(
        strTypes: List<String>,
        strSort: String?,
        size: Int?,
        fn: suspend (Collection<FlowActivityType>, ScrollingSort<ItemHistory>) -> Flow<ItemHistory>
    ): ResponseEntity<FlowActivitiesDto> {
        val sort = safeOf(strSort, ItemHistoryFilter.Sort.EARLIEST_LAST)!!
        val itemHistoryFlow = if (strTypes.isEmpty()) {
            emptyFlow()
        } else {
            fn(safeOf(strTypes), sort)
        }

        return ItemHistoryToDtoConverter.page(itemHistoryFlow, sort, size).okOr404IfNull()
    }

    companion object {
        private val logger by Log()
    }
}
