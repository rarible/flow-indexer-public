package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(DepositListener.ID)
class DepositListener(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher
) : SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ): Unit = coroutineScope {
        val to = FlowAddress(fields["to"]!! as String)

        val items = async {
            itemRepository.coFindById(ItemId(contract, tokenId))
                ?.let {
                    itemRepository.coSave(it.copy(owner = to))
                }
        }
        val ownership = async {
            ownershipRepository
                .findAllByContractAndTokenIdOrderByDateDesc(contract, tokenId)
                .collectList()
                .awaitFirstOrDefault(emptyList())
                .map { it.copy(owner = to) }
                .let {
                    ownershipRepository.saveAll(it).asFlow().collect { updatedOwnership ->
                        protocolEventPublisher.onUpdate(updatedOwnership)
                    }
                }
        }

        items.await()
        ownership.await()
    }

    companion object {
        const val ID = "CommonNFT.Deposit"
    }
}
