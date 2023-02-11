package com.rarible.flow.scanner.subscriber.fungible

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.scanner.subscriber.flowBalanceDescriptor
import org.springframework.stereotype.Component

@Component
class FusdSubscriber: AbstractFungibleTokenSubscriber() {

    private val events = supportedEvents()

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowBalanceDescriptor(
                address = "3c5959b568896393",
                contract = "FUSD",
                events = events,
                dbCollection = dbCollection,
            ),
            FlowChainId.TESTNET to flowBalanceDescriptor(
                address = "e223d8a629e49c68",
                contract = "FUSD",
                events = events,
                dbCollection = dbCollection
            ),
            FlowChainId.EMULATOR to flowBalanceDescriptor(
                address = "0ae53cb6e3f42a79",
                contract = "FUSD",
                events = events,
                dbCollection = dbCollection,
            )
        )

}
