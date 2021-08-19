package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.*
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.listener.config.ListenerProperties
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.log.Log
import kotlinx.coroutines.runBlocking
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.util.*

@Component(MintListener.ID)
class MintListener(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val protocolEventPublisher: ProtocolEventPublisher,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
    private val props: ListenerProperties
): SmartContractEventHandler<Unit> {

    override suspend fun handle(
        contract: String,
        tokenId: TokenId,
        fields: Map<String, Any?>,
        blockInfo: BlockInfo
    ) = runBlocking {
        log.info("Handling [$ID] at [$contract.$tokenId] with fields [${fields}]")

        val metadata = (fields["metadata"] ?: "") as String
        val to = FlowAddress(fields["creator"]!! as String)
        val collection = (fields.getOrDefault("collection", props.defaultItemCollection.id) as String)

        val existingEvent = itemRepository.coFindById(ItemId(contract, tokenId))
        val royalties = getRoyalties(fields["royalties"])
        if (existingEvent == null) {
            val item = Item(
                contract,
                tokenId,
                to,
                royalties,
                to,
                Instant.now(Clock.systemUTC()),
                metadata,
                collection = collection
            )

            itemRepository.coSave(item).let {
                val result = protocolEventPublisher.onItemUpdate(it)
                EventHandler.log.info("item update message is sent: $result")
            }

            ownershipRepository.coSave(
                Ownership(
                    contract,
                    tokenId,
                    to,
                    Instant.now(Clock.systemUTC()),
                    creators = listOf(Payout(account = item.creator, value = BigDecimal.ONE))
                )
            )

            itemHistoryRepository.coSave(
                ItemHistory(
                    id = UUID.randomUUID().toString(),
                    date = Instant.now(Clock.systemUTC()),
                    activity = MintActivity(
                        owner = to,
                        contract = contract,
                        tokenId = tokenId,
                        value = 1L,
                        transactionHash = blockInfo.transactionId,
                        blockHash = blockInfo.blockId,
                        blockNumber = blockInfo.blockHeight,
                        collection = item.collection
                    )
                )
            )
        }
    }

    private fun getRoyalties(royalties: Any?): List<Part> {
        return if(royalties == null) {
            emptyList()
        } else {
            royalties as List<Map<String, String>>
            royalties.map { r ->
                Part(FlowAddress(r["address"] as String), r["fee"]?.toDouble() ?: 0.0)
            }
        }
    }

    companion object {
        const val ID =  "CommonNFT.Mint"

        val log by Log()
    }
}
