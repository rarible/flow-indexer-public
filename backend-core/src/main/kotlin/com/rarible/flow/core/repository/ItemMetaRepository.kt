package com.rarible.flow.core.repository;

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface ItemMetaRepository: ReactiveMongoRepository<ItemMeta, ItemId>