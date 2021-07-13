package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.Order
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

/**
 * Created by TimochkinEA at 13.07.2021
 */
@Repository
interface OrderReactiveRepository: ReactiveMongoRepository<Order, ULong>
