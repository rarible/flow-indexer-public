package com.rarible.flow.api.service

import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.repository.ItemHistoryFilter
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.filters.ScrollingSort
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import java.time.Instant

@FlowPreview
@Service
class ActivitiesService(
    private val itemHistoryRepository: ItemHistoryRepository
) {

    suspend fun getByItem(
        types: Collection<FlowActivityType>,
        contract: String,
        tokenId: Long,
        cursor: String?,
        size: Int?,
        sort: ScrollingSort<ItemHistory>,
    ): Flow<ItemHistory> {
        return itemHistoryRepository.search(
            ItemHistoryFilter.ByTypes(types) * ItemHistoryFilter.ByItem(contract, tokenId),
            cursor,
            size,
            sort
        ).asFlow()
    }

    suspend fun getByUser(
        types: Collection<FlowActivityType>,
        users: List<String>,
        cursor: String?,
        from: Instant?,
        to: Instant?,
        size: Int?,
        sort: ScrollingSort<ItemHistory>,
    ): Flow<ItemHistory> {
        return itemHistoryRepository.search(
            ItemHistoryFilter.ByUsers(types, users) * ItemHistoryFilter.From(from) * ItemHistoryFilter.To(to),
            cursor,
            size,
            sort
        ).asFlow()
    }

    suspend fun getAll(
        types: Collection<FlowActivityType>,
        cursor: String?,
        size: Int?,
        sort: ScrollingSort<ItemHistory>
    ): Flow<ItemHistory> {
        return itemHistoryRepository.search(
            ItemHistoryFilter.ByTypes(types),
            cursor,
            size,
            sort
        ).asFlow()
    }

    suspend fun getByCollection(
        types: Collection<FlowActivityType>,
        collection: String,
        cursor: String?,
        size: Int?,
        sort: ScrollingSort<ItemHistory>
    ): Flow<ItemHistory> {
        return itemHistoryRepository.search(
            ItemHistoryFilter.ByTypes(types) * ItemHistoryFilter.ByCollection(collection),
            cursor,
            size,
            sort
        ).asFlow()
    }
}
