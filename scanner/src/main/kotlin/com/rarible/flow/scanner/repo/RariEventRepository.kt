package com.rarible.flow.scanner.repo

import com.rarible.flow.scanner.model.RariEvent
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

/**
 * Created by TimochkinEA at 04.06.2021
 */
interface RariEventRepository : ReactiveMongoRepository<RariEvent, String>
