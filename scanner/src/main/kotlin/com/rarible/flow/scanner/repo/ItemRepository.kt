package com.rarible.flow.scanner.repo

import com.rarible.flow.scanner.model.Item
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository


@Repository
interface  ItemRepository: ReactiveMongoRepository<Item, String>, ItemRepositoryCustom