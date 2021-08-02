package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemCollection
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ItemCollectionRepository: ReactiveMongoRepository<ItemCollection, String>
