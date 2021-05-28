package com.rarible.flow.scanner.api

import com.rarible.flow.scanner.model.Item
import com.rarible.flow.scanner.repo.ItemRepository
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/items")
class ItemsController(
    val itemRepository: ItemRepository
) {

    @RequestMapping("/")
    fun allItems(): Flux<Item> {
        return itemRepository.findAll()
    }
}