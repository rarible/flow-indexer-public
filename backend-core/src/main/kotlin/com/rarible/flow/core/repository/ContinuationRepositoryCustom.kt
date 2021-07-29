package com.rarible.flow.core.repository

import kotlinx.coroutines.flow.Flow

/**
 * Interface to describe methods with continuation based pagination
 */
interface ContinuationRepositoryCustom<E, F> {
    fun search(filter: F, cont: Continuation?, limit: Int?): Flow<E>
}