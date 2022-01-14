package com.rarible.flow.api.controller

import com.rarible.flow.api.service.EnglishAuctionApiService
import com.rarible.flow.core.converter.AuctionToDtoConverter
import com.rarible.flow.core.domain.AuctionActivityBidOpened
import com.rarible.flow.core.repository.ActivityContinuation
import com.rarible.protocol.dto.*
import com.rarible.protocol.flow.nft.api.controller.FlowAuctionControllerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class AuctionApiController : FlowAuctionControllerApi {

    @Autowired
    private lateinit var auctionService: EnglishAuctionApiService

    override suspend fun getAuctionBidsById(
        id: Long,
        continuation: String?,
        size: Int?,
    ): ResponseEntity<FlowAuctionBidsPaginationDto> {
        return if (auctionService.existsById(id)) {
            val bidsHistory = auctionService.bidsByAuctionId(id, continuation, size)
            val bids = bidsHistory.map {
                val a = it.activity as AuctionActivityBidOpened
                FlowAuctionBidDto(
                    address = a.bidder,
                    amount = a.amount
                )
            }
            val cont = when {
                bids.isEmpty() -> null
                bids.size <= (size ?: 50) -> null
                else -> "${
                    ActivityContinuation(
                        beforeDate = bidsHistory.last().date,
                        beforeId = bidsHistory.last().id
                    )
                }"
            }
            ResponseEntity.ok(
                FlowAuctionBidsPaginationDto(
                    bids = bids,
                    continuation = cont
                )
            )
        } else {
            ResponseEntity.notFound().build()
        }
    }

    override suspend fun getAuctionById(id: Long): ResponseEntity<FlowAuctionDto> {
        val auction = auctionService.byId(id) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.ok(AuctionToDtoConverter.convert(auction))
    }

    override suspend fun getAuctionsByCollection(
        contract: String,
        seller: String?,
        status: FlowAuctionStatusDto?,
        continuation: String?,
        size: Int?,
    ): ResponseEntity<FlowAuctionsPaginationDto> {
        val auctions =
            auctionService.byCollection(contract, seller, status, continuation, size)
                .map { AuctionToDtoConverter.convert(it) }

        val cont = when {
            auctions.isEmpty() -> null
            auctions.size < (size ?: 50) -> null
            else -> "${auctions.last().lastUpdatedAt!!.toEpochMilli()}_${auctions.last().id}"
        }
        return ResponseEntity.ok(
            FlowAuctionsPaginationDto(
                auctions = auctions,
                continuation = cont
            )
        )
    }

    override fun getAuctionsByIds(flowAuctionIdsDto: FlowAuctionIdsDto): ResponseEntity<Flow<FlowAuctionDto>> {
        return ResponseEntity.ok(auctionService.byIds(flowAuctionIdsDto.ids).map(AuctionToDtoConverter::convert))
    }

    override suspend fun getAuctionsByItem(
        contract: String,
        tokenId: Long,
        seller: String?,
        sort: FlowAuctionSortDto?,
        status: FlowAuctionStatusDto?,
        currencyId: String?,
        continuation: String?,
        size: Int?,
    ): ResponseEntity<FlowAuctionsPaginationDto> {
        val auctions = auctionService.byItem(contract, tokenId, seller, sort, status, currencyId, continuation, size)
        val cont = when {
            auctions.isEmpty() -> null
            auctions.size <= (size ?: 50) -> null
            sort == FlowAuctionSortDto.BUY_PRICE_ASC -> {
                "${auctions.last().hammerPrice}_${auctions.last().id}"
            }
            else -> "${auctions.last().lastUpdatedAt.toEpochMilli()}_${auctions.last().id}"
        }
        return ResponseEntity.ok(
            FlowAuctionsPaginationDto(
                auctions = auctions.map { AuctionToDtoConverter.convert(it) },
                continuation = cont
            )
        )
    }

    override suspend fun getAuctionsBySeller(
        seller: String,
        continuation: String?,
        size: Int?,
    ): ResponseEntity<FlowAuctionsPaginationDto> {
        val auctions = auctionService.bySeller(seller, continuation, size)
        val cont = when {
            auctions.isEmpty() -> null
            auctions.size < (size ?: 50) -> null
            else -> "${auctions.last().lastUpdatedAt}_${auctions.last().id}"
        }
        return ResponseEntity.ok(
            FlowAuctionsPaginationDto(
                auctions = auctions.map { AuctionToDtoConverter.convert(it) },
                continuation = cont
            )
        )
    }
}
