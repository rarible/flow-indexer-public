package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.rarible.core.apm.SpanType
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.EnglishAuctionLotRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.log.Log
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

@Service
class EnglishAuctionService(
    private val repo: EnglishAuctionLotRepository,
    @Value("\${blockchain.scanner.flow.chainId}")
    private val chainId: FlowChainId,
    private val publisher: ProtocolEventPublisher
) {

    private val logger by Log()

    private val engAucNs: CadenceNamespace by lazy {
        CadenceNamespace.ns(
            address = Contracts.ENGLISH_AUCTION.deployments[chainId]!!,
            values = arrayOf(Contracts.ENGLISH_AUCTION.contractName)
        )
    }

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
            contract = engAucNs.value,
            ongoing = activityLot.startAt >= Instant.now()
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
                lastUpdatedAt = activity.timestamp
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
            lot.copy(status = AuctionStatus.CANCELLED, lastUpdatedAt = activity.timestamp)
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

    @Scheduled(initialDelay = 30L, fixedDelay = 30L)
    @com.rarible.core.apm.CaptureSpan(type = SpanType.KAFKA)
    fun processOngoing() {
        logger.info("Processes ongoing auctions ...")
        runBlocking {
            try {
                val started = repo.findAllByStartAtAfterAndOngoingAndStatus(Instant.now()).asFlow().map {
                    it.copy(ongoing = true)
                }.toList()
                logger.info("Found: ${started.size} not started auctions ...")
                if (started.isNotEmpty()) {
                    repo.saveAll(started).asFlow().collect {
                        publisher.auction(it).ensureSuccess()
                    }
                }
            } catch (e: Throwable) {
                logger.error("Unable to process started auctions: ${e.message}", e)
            }
        }
    }
}
