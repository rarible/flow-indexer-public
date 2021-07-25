package com.rarible.flow.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

fun <T: Any> ReactiveMongoRepository<T, *>.coSaveAll(entities: Iterable<T>): Flow<T> =
    this.saveAll(entities).asFlow()

suspend fun <T : Any> ReactiveMongoRepository<T, *>.coSave(entity: T): T =
    this.save(entity).awaitSingle()

suspend fun <T, ID : Any> ReactiveMongoRepository<T, ID>.coFindById(id: ID): T? =
    this.findById(id).awaitSingleOrNull()

fun <T : Any> coFindAll(repo: ReactiveMongoRepository<T, *>): Flow<T> =
    repo.findAll().asFlow()


suspend fun <T : Any, ID : Any, R> ReactiveMongoRepository<T, ID>.withEntity(id: ID, fn: suspend (T) -> R): R? {
    return this.coFindById(id)?.let {
        fn(it)
    }
}