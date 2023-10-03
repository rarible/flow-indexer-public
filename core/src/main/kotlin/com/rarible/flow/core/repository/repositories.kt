package com.rarible.flow.core.repository

import com.rarible.flow.core.repository.filters.DbFilter
import com.rarible.flow.core.repository.filters.ScrollingSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

suspend fun <T : Any> ReactiveMongoRepository<T, *>.coSave(entity: T): T =
    this.save(entity).awaitSingle()

suspend fun <T, ID : Any> ReactiveMongoRepository<T, ID>.coFindById(id: ID): T? =
    this.findById(id).awaitSingleOrNull()

fun <T : Any> ReactiveMongoRepository<T, *>.coFindAll(): Flow<T> =
    this.findAll().asFlow()

suspend fun <T : Any> ReactiveMongoRepository<T, *>.coSaveAll(entities: Collection<T>): List<T> =
    this.saveAll(entities).collectList().awaitSingle()

suspend fun <T : Any> ReactiveMongoRepository<T, *>.coSaveAll(vararg entities: T): List<T> =
    this.coSaveAll(entities.toList())

suspend fun <T : Any, ID : Any, R> ReactiveMongoRepository<T, ID>.withEntity(id: ID, fn: suspend (T) -> R): R? {
    return this.coFindById(id)?.let {
        fn(it)
    }
}

suspend fun <T : Any> ScrollingRepository<T>.forEach(
    filter: DbFilter<T>,
    continuation: String?,
    size: Int?,
    sort: ScrollingSort<T> = this.defaultSort(),
    fn: suspend (T) -> Unit
) {
    val page = this.search(filter, continuation, size, sort).asFlow()
    val (pageSize, last) = page.fold(0 to (null as T?)) { (count, _), value ->
        fn(value)
        (count + 1) to value
    }
    if (pageSize < ScrollingSort.pageSize(size) || last == null) {
        return
    } else {
        return this.forEach(filter, sort.nextPage(last), size, sort, fn)
    }
}
