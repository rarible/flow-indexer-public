package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.core.apm.SpanType
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.EnglishAuctionLotRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.log.Log
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.TimeUnit

@Service
class EnglishAuctionService(
    private val repo: EnglishAuctionLotRepository,
    @Value("\${blockchain.scanner.flow.chainId}")
    private val chainId: FlowChainId,
    private val publisher: ProtocolEventPublisher,
    private val mongo: ReactiveMongoTemplate
) {

    private val logger by Log()

    suspend fun openLot(activityLot: AuctionActivityLot): EnglishAuctionLot {
        val status = when {
            activityLot.finishAt != null && activityLot.finishAt!!.isBefore(Instant.now()) -> AuctionStatus.FINISHED
            else -> AuctionStatus.ACTIVE
        }

        val lot = EnglishAuctionLot(
            id = activityLot.lotId,
            status = status,
            seller = FlowAddress(activityLot.seller),
            sell = FlowAssetNFT(
                contract = activityLot.contract,
                tokenId = activityLot.tokenId,
                value = BigDecimal.ONE
            ),
            currency = activityLot.currency,
            createdAt = activityLot.timestamp,
            lastUpdatedAt = activityLot.timestamp,
            startAt = activityLot.startAt,
            finishAt = activityLot.finishAt,
            startPrice = activityLot.startPrice,
            buyoutPrice = activityLot.buyoutPrice,
            minStep = activityLot.minStep,
            duration = activityLot.duration,
            contract = Contracts.ENGLISH_AUCTION.fqn(chainId),
        )

        return repo.coSave(lot)
    }

    suspend fun openBid(activity: AuctionActivityBidOpened): EnglishAuctionLot {
        val lot = repo.coFindById(activity.lotId)
            ?: throw IllegalStateException("English auction lot ${activity.lotId} not founded!")

        return repo.coSave(
            lot.copy(
                lastBid = Bid(
                    address = FlowAddress(activity.bidder),
                    amount = activity.amount,
                    bidAt = activity.timestamp
                ),
                lastUpdatedAt = activity.timestamp
            )
        )
    }

    suspend fun hammerLot(activity: AuctionActivityLotHammered): EnglishAuctionLot {
        val lot = repo.coFindById(activity.lotId)
            ?: throw IllegalStateException("English auction lot ${activity.lotId} not founded!")

        return repo.coSave(
            lot.copy(
                status = AuctionStatus.FINISHED,
                buyer = activity.winner,
                hammerPrice = activity.hammerPrice,
                hammerPriceUsd = activity.hammerPriceUsd,
                payments = activity.payments,
                originFees = activity.originFees,
                lastUpdatedAt = activity.timestamp,
                ongoing = false
            )
        )
    }

    suspend fun changeLotEndTime(activity: AuctionActivityLotEndTimeChanged): EnglishAuctionLot {
        val lot = repo.coFindById(activity.lotId)
            ?: throw IllegalStateException("English auction lot ${activity.lotId} not founded!")

        return repo.coSave(
            lot.copy(
                finishAt = activity.finishAt,
                lastUpdatedAt = activity.timestamp
            )
        )
    }

    suspend fun finalizeLot(activity: AuctionActivityLotCleaned): EnglishAuctionLot {
        val lot = repo.coFindById(activity.lotId)
            ?: throw IllegalStateException("English auction lot ${activity.lotId} not founded!")

        return repo.coSave(
            lot.copy(
                cleaned = true,
                lastUpdatedAt = activity.timestamp
            )
        )
    }

    suspend fun cancelLot(activity: AuctionActivityLotCanceled): EnglishAuctionLot {
        val lot = repo.coFindById(activity.lotId)
            ?: throw IllegalStateException("English auction lot ${activity.lotId} not founded!")

        return repo.coSave(
            lot.copy(status = AuctionStatus.CANCELLED, lastUpdatedAt = activity.timestamp, ongoing = false)
        )
    }

    suspend fun increaseBid(activity: AuctionActivityBidIncreased): EnglishAuctionLot {
        val lot = repo.coFindById(activity.lotId)
            ?: throw IllegalStateException("English auction lot ${activity.lotId} not founded!")

        if (lot.lastBid == null) {
            throw IllegalStateException("Lot have no actual bid!")
        }

        if (lot.lastBid!!.address.formatted != activity.bidder) {
            throw IllegalStateException("Last bid address not equal activity address!")
        }

        return repo.coSave(
            lot.copy(
                lastBid = lot.lastBid!!.copy(amount = activity.amount, bidAt = activity.timestamp),
                lastUpdatedAt = activity.timestamp
            )
        )
    }

    @Scheduled(initialDelay = 30L, fixedDelay = 30L, timeUnit = TimeUnit.SECONDS)
    @com.rarible.core.apm.CaptureSpan(type = SpanType.KAFKA)
    fun processOngoing() {
        logger.info("Processes ongoing auctions ...")
        runBlocking {
            try {
                val now = Instant.now()
                val started = mongo.find(
                    Query.query(
                        where(EnglishAuctionLot::startAt).lte(now)
                            .and(EnglishAuctionLot::finishAt).exists(true).gte(now)
                            .and(EnglishAuctionLot::status).isEqualTo(AuctionStatus.ACTIVE)
                            .and(EnglishAuctionLot::ongoing).isEqualTo(false)
                    ),
                    EnglishAuctionLot::class.java
                ).asFlow().toList()
                val finished = mongo.find(
                    Query.query(
                        where(EnglishAuctionLot::status).isEqualTo(AuctionStatus.ACTIVE)
                            .and(EnglishAuctionLot::finishAt).exists(true).lte(Instant.now())
                            .and(EnglishAuctionLot::ongoing).isEqualTo(true)
                    ),
                    EnglishAuctionLot::class.java
                ).asFlow().toList()
                logger.info("Found: ${started.size} not started auctions ...")
                logger.info("Found: ${finished.size} finished auctions ...")
                if (started.isNotEmpty()) {
                    repo.saveAll(started.map { it.copy(ongoing = true, lastUpdatedAt = Instant.now()) }).asFlow().collect {
                        publisher.auction(it).ensureSuccess()
                    }
                }
                if (finished.isNotEmpty()) {
                    repo.saveAll(finished.map { it.copy(ongoing = false, lastUpdatedAt = Instant.now()) }).asFlow().collect {
                        publisher.auction(it).ensureSuccess()
                    }
                }
            } catch (e: Throwable) {
                logger.error("Unable to process started/finished auctions: ${e.message}", e)
            }
        }
    }
}
