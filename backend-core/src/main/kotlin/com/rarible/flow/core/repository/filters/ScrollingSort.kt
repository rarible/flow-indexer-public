package com.rarible.flow.core.repository.filters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.lastOrNull
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

interface ScrollingSort<T> {
    fun springSort(): org.springframework.data.domain.Sort
    fun scroll(criteria: Criteria, continuation: String?): Criteria
    fun scroll(filter: DbFilter<T>, continuation: String?, limit: Int?): Query =
        Query
            .query(this.scroll(filter.criteria(), continuation))
            .with(this.springSort())
            .limit(pageSize(limit))

    fun nextPage(entity: T): String

    fun nextPageSafe(entity: T?): String? {
        return if(entity == null) {
            null
        } else {
            nextPage(entity)
        }
    }

    suspend fun nextPage(entities: Flow<T>, limit: Int?): String? {
        val expectedCount = pageSize(limit)
        return if(entities.count() < expectedCount) {
             null
        } else nextPageSafe(entities.lastOrNull())
    }

    fun nextPage(entities: Collection<T>, limit: Int?): String? {
        val expectedCount = pageSize(limit)
        return if(entities.count() < expectedCount) {
            null
        } else nextPageSafe(entities.lastOrNull())
    }

    companion object {
        const val DEFAULT_LIMIT = 50
        const val MAX_LIMIT = 1000

        fun pageSize(incomingSize: Int?): Int =
            minOf(incomingSize?.takeIf { it > 0 } ?: DEFAULT_LIMIT, MAX_LIMIT)
    }
}