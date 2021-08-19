package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemHistory
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor
import org.springframework.stereotype.Repository

/**
 * Item history repo
 */
@Repository
interface ItemHistoryRepository: ReactiveMongoRepository<ItemHistory, String>, ReactiveQuerydslPredicateExecutor<ItemHistory>
