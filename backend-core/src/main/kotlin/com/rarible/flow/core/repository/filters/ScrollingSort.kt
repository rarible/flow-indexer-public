package com.rarible.flow.core.repository.filters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.lastOrNull
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

interface ScrollingSort<T> {
    fun springSort(): org.springframework.data.domain.Sort
    fun scroll(criteria: Criteria, continuation: String?): Criteria
    fun scroll(filter: BuildsCriteria, continuation: String?, limit: Int?): Query =
        Query
            .query(this.scroll(filter.criteria(), continuation))
            .with(this.springSort())
            .limit(limit ?: DEFAULT_LIMIT)

    fun nextPage(entity: T): String

    fun nextPageSafe(entity: T?): String? {
        return if(entity == null) {
            null
        } else {
            nextPage(entity)
        }
    }

    suspend fun nextPage(entities: Flow<T>, size: Int?): String? {
        val expectedCount = size ?: DEFAULT_LIMIT
        return if(entities.count() < expectedCount) {
             null
        } else nextPageSafe(entities.lastOrNull())
    }

    companion object {
        const val DEFAULT_LIMIT = 50
    }
}