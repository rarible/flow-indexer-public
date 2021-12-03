package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Balance
import com.rarible.flow.core.domain.BalanceId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface BalanceRepository: ReactiveMongoRepository<Balance, BalanceId> {
}