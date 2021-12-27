package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.EnglishAuctionLot
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface EnglishAuctionLotRepository: ReactiveMongoRepository<EnglishAuctionLot, Long>
