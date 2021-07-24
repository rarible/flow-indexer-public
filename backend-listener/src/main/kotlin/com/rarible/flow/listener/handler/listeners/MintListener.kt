package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Instant

@Component(MintListener.ID)
class MintListener(
    private val itemRepository: ItemRepository,
    private val itemMetaRepository: ItemMetaRepository,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher
): SmartContractEventHandler<Unit> {

    override suspend fun handle(contract: FlowAddress, tokenId: TokenId, fields: Map<String, Any?>) {
        log.info("Handling [$ID] at [$contract.$tokenId] with fields [${fields}]")

        val metadata = (fields["metadata"] ?: emptyMap<String, String>()) as Map<String, String>
        val to = FlowAddress(fields["creator"]!! as String)

        val existingEvent = itemRepository.coFindById(ItemId(contract, tokenId))
        if (existingEvent == null) {
            val item = Item(
                contract,
                tokenId,
                to,
                emptyList(),
                to,
                Instant.now(),
                ""
            )
            itemMetaRepository.coSave(
                ItemMeta(item.id, metadata["title"] ?: "", metadata["description"] ?: "", URI.create(metadata["uri"] ?: ""))
            )

            itemRepository.coSave(
                item.copy(meta = "/v0.1/items/meta/${item.id}")
            ).let {
                val result = protocolEventPublisher.onItemUpdate(it)
                EventHandler.log.info("item update message is sent: $result")
            }


            ownershipRepository.coSave(
                Ownership(
                    contract,
                    tokenId,
                    to,
                    Instant.now()
                )
            )
        }
    }

    companion object {
        const val ID =  "NFTProvider.Mint"

        val log by Log()
    }
}