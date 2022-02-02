package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.EnglishAuctionLotRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

@Service
class EnglishAuctionService(
    private val repo: EnglishAuctionLotRepository
) {

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
            duration = activityLot.duration
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
}
