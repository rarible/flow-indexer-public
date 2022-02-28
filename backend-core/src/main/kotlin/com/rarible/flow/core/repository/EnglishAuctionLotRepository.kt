package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.EnglishAuctionLot
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface EnglishAuctionLotRepository : ReactiveMongoRepository<EnglishAuctionLot, Long>


