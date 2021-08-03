package com.rarible.flow.core.repository

import kotlinx.coroutines.flow.Flow

/**
 * Interface to describe methods with continuation based pagination
 */
interface ContinuationRepositoryCustom<Entity, Filter> {
    fun search(filter: Filter, cont: Continuation?, limit: Int?): Flow<Entity>
}
