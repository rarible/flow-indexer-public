package com.rarible.flow.api.controller

import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.protocol.dto.FlowAggregationDataDto
import com.rarible.protocol.flow.nft.api.controller.OrderAggregationControllerApi
import kotlinx.coroutines.flow.Flow
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@CrossOrigin
class OrderAggregationController(
    private val itemHistoryRepository: ItemHistoryRepository
): OrderAggregationControllerApi {
    override fun aggregateNftPurchaseBuyCollection(
        startDate: Long,
        endDate: Long,
        size: Long?
    ): ResponseEntity<Flow<FlowAggregationDataDto>> {
        return itemHistoryRepository.aggregatePurchaseByCollection(
            Instant.ofEpochSecond(startDate),
            Instant.ofEpochSecond(endDate),
            size
        ).okOr404IfNull()
    }

    override fun aggregateNftPurchaseByTaker(
        startDate: Long,
        endDate: Long,
        size: Long?
    ): ResponseEntity<Flow<FlowAggregationDataDto>> {
        return itemHistoryRepository.aggregatePurchaseByTaker(
            Instant.ofEpochSecond(startDate),
            Instant.ofEpochSecond(endDate),
            size
        ).okOr404IfNull()
    }

    override fun aggregateNftSellByMaker(
        startDate: Long,
        endDate: Long,
        size: Long?
    ): ResponseEntity<Flow<FlowAggregationDataDto>> {
        return itemHistoryRepository.aggregateSellByMaker(
            Instant.ofEpochSecond(startDate),
            Instant.ofEpochSecond(endDate),
            size
        ).okOr404IfNull()
    }
}