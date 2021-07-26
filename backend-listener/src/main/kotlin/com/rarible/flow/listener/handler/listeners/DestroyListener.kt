package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(DestroyListener.ID)
class DestroyListener(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher
): SmartContractEventHandler<Void> {

    override suspend fun handle(contract: FlowAddress, tokenId: TokenId, fields: Map<String, Any?>) = coroutineScope {

        val itemId = ItemId(contract, tokenId)
        val items = async {
            itemRepository.deleteById(itemId).awaitSingle()
        }
        val ownerships = async {
            ownershipRepository.deleteAllByContractAndTokenId(contract, tokenId).awaitSingle()
        }

        items.await()?.let { _ ->
            val result = protocolEventPublisher.onItemDelete(itemId)
            EventHandler.log.info("item delete message is sent: $result")

        }
        ownerships.await()
    }

    companion object {
        const val ID = "NFTProvider.Destroy"
        val log by Log()
    }
}