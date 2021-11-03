package com.rarible.flow.scanner.eventlisteners

import com.rarible.blockchain.scanner.framework.data.Source
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.scanner.service.OwnershipService
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class OwnershipEventListeners(
    val ownershipRepository: OwnershipRepository,
    val ownershipService: OwnershipService,
    val protocolEventPublisher: ProtocolEventPublisher
) {

    @EventListener
    fun afterWithdraw(event: ItemIsWithdrawn): Unit = runBlocking {
        val item = event.item
        val ownershipId = OwnershipId(item.contract, item.tokenId, event.from)
        ownershipRepository
            .coFindById(ownershipId)
            ?.let { ownership ->
                ownershipRepository.delete(ownership).subscribe()
                if (event.source != Source.REINDEX) {
                    protocolEventPublisher.onDelete(ownership)
                }
            }
    }

    @EventListener
    fun afterDeposit(event: ItemIsDeposited): Unit = runBlocking {
        val item = event.item
        val ownership = if(event.from == null) { // item is new
            ownershipService.createOwnership(item, event.to, event.activityTime)
        } else {
            ownershipService.transferOwnershipIfExists(item, event.from, event.to)
                ?: //transfer existing, or create a new
                ownershipService.createOwnership(item, event.to, event.activityTime)
        }

        if (event.source != Source.REINDEX) {
            protocolEventPublisher.onUpdate(ownership)
        }
    }

}
