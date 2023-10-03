package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.RawOnChainMeta
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface RawOnChainMetaCacheRepository : ReactiveMongoRepository<RawOnChainMeta, String>
