package com.rarible.flow.scanner

import com.rarible.flow.scanner.model.FlowTransaction
import com.rarible.flow.scanner.repo.FlowTransactionRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Created by TimochkinEA at 26.05.2021
 */
@RestController
@RequestMapping("/tx")
class TransactionController(private val repo: FlowTransactionRepository) {

    @GetMapping("{id}")
    fun tx(@PathVariable id: String): Mono<FlowTransaction> = repo.findById(id)
}
