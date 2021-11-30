package com.rarible.flow.scanner.subscriber.fungible

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.scanner.subscriber.flowDescriptor
import org.springframework.stereotype.Component

@Component
class FungibleTokenSubscriber(
    override val balanceRepository: BalanceRepository
) : AbstractFungibleTokenSubscriber(balanceRepository) {

    private val events = supportedEvents()

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                address = "1654653399040a61",
                contract = "FlowToken",
                events = events,
                startFrom = 1L,
                dbCollection = ""
            ),
            FlowChainId.TESTNET to flowDescriptor(
                address = "01658d9b94068f3c", //TODO paste correct address
                contract = "FlowToken",
                events = events,
                startFrom = 1L,
                dbCollection = ""
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                address = "f8d6e0586b0a20c7", //TODO paste correct address
                contract = "FlowToken",
                events = events,
                dbCollection = ""
            )
        )

}
