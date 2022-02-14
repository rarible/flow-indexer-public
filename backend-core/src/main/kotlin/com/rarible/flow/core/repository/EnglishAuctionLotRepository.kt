package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.AuctionStatus
import com.rarible.flow.core.domain.EnglishAuctionLot
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import java.time.Instant

interface EnglishAuctionLotRepository: ReactiveMongoRepository<EnglishAuctionLot, Long> {

    fun findAllByStatusAndFinishAtLessThanEqualAndLastBidIsNotNull(status: AuctionStatus, finishAt: Instant): Flux<EnglishAuctionLot>

    fun findAllByStartAtAfterAndOngoingAndStatus(startAt: Instant, ongoing: Boolean = false, status: AuctionStatus = AuctionStatus.ACTIVE): Flux<EnglishAuctionLot>
}
