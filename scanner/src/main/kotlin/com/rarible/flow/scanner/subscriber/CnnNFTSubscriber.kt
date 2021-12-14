package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.*
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.DepositActivity
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.WithdrawnActivity
import com.rarible.flow.events.EventMessage
import com.rarible.flow.scanner.model.parse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.stereotype.Component
import java.time.Instant

@ExperimentalCoroutinesApi
@Component
class CnnNFTSubscriber : BaseItemHistoryFlowLogSubscriber() {
    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to FlowDescriptor(
                id = "CnnNFTSubscriber",
                events = setOf(
                    "A.329feb3ab062d289.CNN_NFT.Minted",
                    "A.329feb3ab062d289.CNN_NFT.Withdraw",
                    "A.329feb3ab062d289.CNN_NFT.Deposit",
                    "A.329feb3ab062d289.CNN_NFT.NFTDestroyed",
                ),
                collection = collection,
                startFrom = 15640000L,
            ),
            FlowChainId.TESTNET to FlowDescriptor(
                id = "CnnNFTSubscriber",
                events = setOf(
                    "A.ebf4ae01d1284af8.CNN_NFT.Minted",
                    "A.ebf4ae01d1284af8.CNN_NFT.Withdraw",
                    "A.ebf4ae01d1284af8.CNN_NFT.Deposit",
                    "A.ebf4ae01d1284af8.CNN_NFT.NFTDestroyed",
                ),
                collection = collection,
                startFrom = 53489946L
            ),
            FlowChainId.EMULATOR to FlowDescriptor(
                id = "CnnNFTSubscriber",
                events = setOf(
                    "A.f8d6e0586b0a20c7.CNN_NFT.Mint",
                    "A.f8d6e0586b0a20c7.CNN_NFT.Withdraw",
                    "A.f8d6e0586b0a20c7.CNN_NFT.Deposit",
                    "A.f8d6e0586b0a20c7.CNN_NFT.NFTDestroyed",
                ),
                collection = collection,
            ),
        )

    override suspend fun activity(block: FlowBlockchainBlock, log: FlowBlockchainLog, msg: EventMessage): BaseActivity {
        val id: NumberField by msg.fields
        val tokenId = id.toLong()!!
        val contract = msg.eventId.collection()
        val timestamp = Instant.ofEpochMilli(block.timestamp)
        val eventId = "${msg.eventId}"
        return when {
            eventId.endsWith("Minted") -> {
                MintActivity(
                    owner = msg.eventId.contractAddress.formatted,
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = timestamp,
                    value = 1L, royalties = emptyList(), metadata = emptyMap()
                )
            }

            eventId.endsWith("Withdraw") -> {
                val from: OptionalField by msg.fields
                WithdrawnActivity(
                    contract = contract,
                    tokenId = tokenId,
                    from = (from.value as? AddressField)?.value,
                    timestamp = timestamp
                )
            }
            eventId.endsWith("Deposit") -> {
                val to: OptionalField by msg.fields
                DepositActivity(
                    contract = contract,
                    tokenId = tokenId,
                    to = (to.value as? AddressField)?.value,
                    timestamp = timestamp
                )
            }
            eventId.endsWith("NFTDestroyed") -> {
                BurnActivity(
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = timestamp
                )
            }
            else -> throw IllegalStateException("Unsupported eventId: $eventId")
        }
    }
}
