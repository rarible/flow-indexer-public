package com.rarible.flow.api.controller

import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = [
    "/v0.1/items"
])
class NftApiController(
    private val itemRepository: ItemRepository
) {

    @GetMapping("/")
    suspend fun findAll(): Flow<Item> {
        return itemRepository.findAll();
    }

    @GetMapping("/byAccount")
    suspend fun findByAccount(address: String): Flow<Item> {
        return itemRepository.findAllBuAccount(address)
    }
}