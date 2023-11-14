package com.rarible.flow.scanner.task

import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.task.TaskHandler
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.util.offchainEventMarks
import com.rarible.flow.scanner.flowty.FlowtyClient
import com.rarible.flow.scanner.job.ItemCleanupJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@Component
class MigrateGamisodesFromFlowtyTask(
    private val job: ItemCleanupJob,
    private val flowtyClient: FlowtyClient,
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
) : TaskHandler<String> {

    override val type = "MIGRATE_GAMISODES_FROM_FLOWTY"

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: String?, param: String) = flow<String> {
        val lastTokenId = AtomicReference(from?.toLong() ?: 0)
        coroutineScope {
            while (true) {
                val currentTokenId = lastTokenId.get() ?: break

                (1..BATCH_SIZE).map { offset ->
                    async {
                        migrateToken(currentTokenId + offset)
                    }
                }.onEach {
                    val tokenId = it.await()
                    if (tokenId != null) {
                        emit(tokenId.toString())
                    }
                    lastTokenId.set(tokenId)
                }
            }
        }
    }

    private suspend fun migrateToken(tokenId: Long): Long? {
        val token = flowtyClient.getGamisodesToken(tokenId)
        log("Externally get token $tokenId, owner ${token.owner}")
        if (token.owner.isBlank()) return null

        val creator = FlowAddress("0x09e04bdbcccde6ca")
        val owner = FlowAddress(token.owner)
        val contract = "A.09e04bdbcccde6ca.Gamisodes"

        val itemId = ItemId(contract, tokenId)

        val savedItem = itemRepository.findById(itemId).awaitFirstOrNull()
        val item = if (savedItem == null) {
            val item = Item(
                tokenId = tokenId,
                contract = contract,
                collection = contract,
                creator = creator,
                owner = owner,
                mintedAt = Instant.now(),
                updatedAt = Instant.now(),
                royalties = emptyList(),
            )
            log("Saving tokenId $tokenId")
            itemRepository.save(item).awaitLast()
        } else {
            savedItem
        }
        if (item.owner != owner) {
            val updatedItem = item.copy(
                owner = owner,
                updatedAt = Instant.now(),
            )
            log("Updating tokenId $tokenId, previous owner ${item.owner}, new owner $owner")
            itemRepository.save(updatedItem).awaitLast()
        }

        val ownerships = ownershipRepository.findAllByContractAndTokenId(contract, tokenId).collectList().awaitFirst()
        when {
            ownerships.singleOrNull()?.owner == owner -> {
                log("Ownership already right: $tokenId, $owner")
            }
            else -> {
                ownerships.forEach {
                    protocolEventPublisher.onDelete(
                        ownership = it,
                        marks = offchainEventMarks()
                    )
                    ownershipRepository.deleteById(it.id).awaitFirst()
                    log("Remove ownership: $tokenId, ${it.owner}")
                }

                val ownership = Ownership(
                    tokenId = tokenId,
                    contract = contract,
                    owner = owner,
                    creator = creator,
                    date = Instant.now()
                )
                protocolEventPublisher.onUpdate(
                    ownership = ownership,
                    marks = offchainEventMarks()
                )
                ownershipRepository.save(ownership).awaitFirst()
                log("Save ownership: $tokenId, $owner")
            }
        }
        return tokenId
    }

    private fun log(message: String) {
        logger.info("$LOG_PREFIX $message")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(MigrateGamisodesFromFlowtyTask::class.java)
        const val BATCH_SIZE = 500
        const val LOG_PREFIX = "[Flowty]"
    }
}
