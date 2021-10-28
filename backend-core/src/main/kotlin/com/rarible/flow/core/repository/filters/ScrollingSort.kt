package com.rarible.flow.core.repository.filters

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

    companion object {
        const val DEFAULT_LIMIT = 50
    }
}