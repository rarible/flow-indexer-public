package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.FlowLogEvent
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface FlowLogEventRepository : ReactiveMongoRepository<FlowLogEvent, String>
