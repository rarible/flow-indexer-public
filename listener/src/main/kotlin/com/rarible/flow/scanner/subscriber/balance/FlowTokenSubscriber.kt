package com.rarible.flow.scanner.subscriber.balance

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.scanner.subscriber.DescriptorFactory

class FlowTokenSubscriber: AbstractFungibleTokenSubscriber() {

    private val events = supportedEvents()
    private val name = "flow"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to DescriptorFactory.flowBalanceDescriptor(
                address = "1654653399040a61",
                contract = "FlowToken",
                events = events,
                dbCollection = dbCollection,
                name = name,
            ),
            FlowChainId.TESTNET to DescriptorFactory.flowBalanceDescriptor(
                address = "7e60df042a9c0868",
                contract = "FlowToken",
                events = events,
                dbCollection = dbCollection,
                name = name,
            ),
            FlowChainId.EMULATOR to DescriptorFactory.flowBalanceDescriptor(
                address = "f8d6e0586b0a20c7", //TODO paste correct address
                contract = "FlowToken",
                events = events,
                dbCollection = dbCollection,
                name = name,
            )
        )
}
