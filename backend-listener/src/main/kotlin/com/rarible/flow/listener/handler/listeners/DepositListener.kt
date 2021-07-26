package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.log.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.asFlow
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component

@Component(DepositListener.ID)
class DepositListener(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository
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
            ownershipRepository.findAllByContractAndTokenId(
                contract, tokenId
            ).map { it.copy(owner = to) }.let { ownershipRepository.saveAll(it).asFlow() }
        }

        items.await()
        ownership.await()
        return@coroutineScope
    }

    companion object {
        const val ID = "NFTProvider.Deposit"
        val log by Log()
    }
}
