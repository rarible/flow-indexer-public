package com.rarible.flow.api.service

import com.rarible.flow.core.domain.EnglishAuctionLot
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.repository.ActivityContinuation
import com.rarible.flow.core.repository.Cont
import com.rarible.flow.core.repository.EnglishAuctionLotRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.filters.ScrollingSort.Companion.DEFAULT_LIMIT
import com.rarible.protocol.dto.FlowAuctionSortDto
import com.rarible.protocol.dto.FlowAuctionStatusDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.domain.Sort
import org.springframework.data.mapping.div
import org.springframework.data.mapping.toDotPath
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class EnglishAuctionApiService(
    private val repo: EnglishAuctionLotRepository,
    private val mongo: ReactiveMongoTemplate,
) {

    suspend fun byId(id: Long): EnglishAuctionLot? = repo.coFindById(id)
    suspend fun existsById(id: Long): Boolean = repo.existsById(id).awaitSingle()
    suspend fun bidsByAuctionId(id: Long, continuation: String?, size: Int?): List<ItemHistory> {
        val cont = ActivityContinuation.of(continuation)
        val criteria = Criteria.where("activity.type").`in`(FlowActivityType.OPEN_BID, FlowActivityType.INCREASE_BID)
            .and("activity.lotId").isEqualTo(id)
        if (cont != null) {
            criteria.and("id").ne(cont.beforeId).and("date").lte(cont.beforeDate)
        }
        val query = Query().limit(size ?: DEFAULT_LIMIT)
            .with(Sort.by(Sort.Direction.DESC, "date", "log.transactionHash", "log.eventIndex"))
        return mongo.find(query, ItemHistory::class.java).asFlow().toList()

    }

    fun byIds(ids: List<Long>): Flow<EnglishAuctionLot> {
        return repo.findAllById(ids).asFlow()
    }

    suspend fun byCollection(
        contract: String,
        seller: String?,
        status: List<FlowAuctionStatusDto>?,
        continuation: String?,
        size: Int?,
    ): List<EnglishAuctionLot> {
        val criteria = Criteria.where((EnglishAuctionLot::sell / FlowAsset::contract).toDotPath()).isEqualTo(contract)
        Cont.scrollDesc(criteria, continuation, EnglishAuctionLot::lastUpdatedAt, EnglishAuctionLot::id)
        if (seller != null) {
            criteria.and(EnglishAuctionLot::seller.name).isEqualTo(seller)
        }
        if (!status.isNullOrEmpty()) {
            criteria.and(EnglishAuctionLot::status.name).`in`(status)
        }

        if (continuation != null) {
            val (dateStr, idStr) = continuation.split("_")
            criteria.and(EnglishAuctionLot::createdAt.name).lte(Instant.ofEpochMilli(dateStr.toLong()))
            criteria.and(EnglishAuctionLot::id.name).ne(idStr.toLong())
        }

        val query =
            Query().limit(size ?: DEFAULT_LIMIT)
                .with(Sort.by(Sort.Direction.DESC, EnglishAuctionLot::createdAt.name))
                .addCriteria(criteria)

        return mongo.find(query, EnglishAuctionLot::class.java).asFlow().toList()
    }

    suspend fun byItem(
        contract: String,
        tokenId: Long,
        seller: String?,
        sort: FlowAuctionSortDto? = FlowAuctionSortDto.BUY_PRICE_ASC,
        status: List<FlowAuctionStatusDto>?,
        currencyId: String?,
        continuation: String?,
        size: Int?,
    ): List<EnglishAuctionLot> {
        val criteria = Criteria.where("sell.contract").isEqualTo(contract)
            .and("sell.tokenId").isEqualTo(tokenId)

        when {
            seller != null -> criteria.and("seller").isEqualTo(seller)
            !status.isNullOrEmpty() -> criteria.and("status").`in`(status)
            currencyId != null -> criteria.and("currency").isEqualTo(currencyId)
        }

        val query = Query().limit(size ?: DEFAULT_LIMIT)
        when (sort) {
            FlowAuctionSortDto.LAST_UPDATE_ASC -> {
                query.with(Sort.by(Sort.Direction.ASC, EnglishAuctionLot::lastUpdatedAt.name))
                if (continuation != null) {
                    val (dateStr, idStr) = continuation.split("_")
                    criteria.and(EnglishAuctionLot::lastUpdatedAt.name).gte(Instant.ofEpochMilli(dateStr.toLong()))
                    criteria.and(EnglishAuctionLot::id.name).ne(idStr.toLong())
                }
            }
            FlowAuctionSortDto.LAST_UPDATE_DESC -> {
                query.with(Sort.by(Sort.Direction.DESC, EnglishAuctionLot::lastUpdatedAt.name))
                if (continuation != null) {
                    val (dateStr, idStr) = continuation.split("_")
                    criteria.and(EnglishAuctionLot::lastUpdatedAt.name).lte(Instant.ofEpochMilli(dateStr.toLong()))
                    criteria.and(EnglishAuctionLot::id.name).ne(idStr.toLong())
                }
            }
            FlowAuctionSortDto.BUY_PRICE_ASC -> {
                query.with(Sort.by(Sort.Direction.ASC, EnglishAuctionLot::hammerPrice.name))
                if (continuation != null) {
                    val (hammerPriceStr, idStr) = continuation.split("_")
                    criteria.and(EnglishAuctionLot::hammerPrice.name).lte(hammerPriceStr.toBigDecimal())
                    criteria.and(EnglishAuctionLot::id.name).ne(idStr.toLong())
                }
            }
        }
        return mongo.find(query.addCriteria(criteria), EnglishAuctionLot::class.java).asFlow().toList()
    }

    suspend fun bySeller(seller: String, status: List<FlowAuctionStatusDto>?, continuation: String?, size: Int?): List<EnglishAuctionLot> {
        val criteria = where(EnglishAuctionLot::seller).isEqualTo(seller)
        if (!status.isNullOrEmpty()) {
            criteria.and(EnglishAuctionLot::status).`in`(status)
        }
        if (continuation != null) {
            val (dateStr, idStr) = continuation.split("_")
            criteria.and(EnglishAuctionLot::lastUpdatedAt.name).lte(Instant.ofEpochMilli(dateStr.toLong()))
            criteria.and(EnglishAuctionLot::id.name).ne(idStr.toLong())
        }

        val query = Query().limit(size ?: DEFAULT_LIMIT).with(Sort.by(Sort.Direction.DESC, EnglishAuctionLot::lastUpdatedAt.name))
        return mongo.find(query.addCriteria(criteria), EnglishAuctionLot::class.java).asFlow().toList()
    }
}
