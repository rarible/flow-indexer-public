package com.rarible.flow.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

@JvmName("coSave_1")
suspend fun <T : Any> coSave(repo: ReactiveMongoRepository<T, *>, entity: T): T =
    repo.save(entity).awaitSingle()

suspend fun <T : Any> ReactiveMongoRepository<T, *>.coSave(entity: T): T =
    this.save(entity).awaitSingle()


@JvmName("coFindById1")
suspend fun <T, ID : Any> coFindById(repo: ReactiveMongoRepository<T, ID>, id: ID): T? =
    repo.findById(id).awaitSingleOrNull()

suspend fun <T, ID : Any> ReactiveMongoRepository<T, ID>.coFindById(id: ID): T? =
    this.findById(id).awaitSingleOrNull()

fun <T : Any> coFindAll(repo: ReactiveMongoRepository<T, *>): Flow<T> =
    repo.findAll().asFlow()


suspend fun <T : Any, ID : Any, R> ReactiveMongoRepository<T, ID>.withEntity(id: ID, fn: suspend (T) -> R): R? {
    return this.coFindById(id)?.let {
        fn(it)
    }
}