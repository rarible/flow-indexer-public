package com.rarible.flow.scanner.repo

import com.rarible.flow.scanner.model.FlowTransaction
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

/**
 * Created by TimochkinEA at 22.05.2021
 */
interface FlowTransactionRepository: MongoRepository<FlowTransaction, String>
