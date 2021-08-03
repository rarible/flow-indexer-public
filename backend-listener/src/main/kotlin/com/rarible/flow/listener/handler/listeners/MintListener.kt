package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.config.ListenerProperties
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

@Component(MintListener.ID)
class MintListener(
    private val itemRepository: ItemRepository,
    private val itemMetaRepository: ItemMetaRepository,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
    private val props: ListenerProperties
): SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: FlowAddress,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ) {
        log.info("Handling [$ID] at [$contract.$tokenId] with fields [${fields}]")

        val metadata = (fields["metadata"] ?: "") as String
        val to = FlowAddress(fields["creator"]!! as String)
        val collection = (fields.getOrDefault("collection", props.defaultItemCollection.id) as String)

        val existingEvent = itemRepository.coFindById(ItemId(contract, tokenId))
        if (existingEvent == null) {
            val item = Item(
                contract,
                tokenId,
                to,
                emptyList(),
                to,
                Instant.now(),
                "",
                collection = collection
            )
            //TODO return later
//            itemMetaRepository.coSave(
//                ItemMeta(item.id, metadata["title"] ?: "", metadata["description"] ?: "", URI.create(metadata["uri"] ?: ""))
//            )

            itemRepository.coSave(
                item.copy(meta = metadata)
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

            itemHistoryRepository.coSave(
                ItemHistory(
                    id = UUID.randomUUID().toString(),
                    date = LocalDateTime.now(),
                    activity = MintActivity(
                        owner = to,
                        contract = contract,
                        tokenId = tokenId,
                        value = 1L,
                        transactionHash = blockInfo.transactionId,
                        blockHash = blockInfo.blockId,
                        blockNumber = blockInfo.blockHeight
                    )
                )
            )
        }
    }

    companion object {
        const val ID =  "CommonNFT.Mint"

        val log by Log()
    }
}
