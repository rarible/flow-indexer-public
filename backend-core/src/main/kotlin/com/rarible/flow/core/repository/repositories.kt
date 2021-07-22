package com.rarible.flow.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

suspend fun <T: Any> coSave(repo: ReactiveMongoRepository<T, *>, entity: T): T =
    repo.save(entity).awaitSingle()


suspend fun <T, ID: Any> coFindById(repo: ReactiveMongoRepository<T, ID>, id: ID): T? =
    repo.findById(id).awaitSingleOrNull()

fun <T: Any> coFindAll(repo: ReactiveMongoRepository<T, *>): Flow<T> =
    repo.findAll().asFlow()