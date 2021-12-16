package com.rarible.flow.scanner.subscriber.fungible

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.scanner.subscriber.flowDescriptor
import org.springframework.stereotype.Component

@Component
class FusdSubscriber(
    override val balanceRepository: BalanceRepository
) : AbstractFungibleTokenSubscriber(balanceRepository) {

    private val events = supportedEvents()

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                address = "3c5959b568896393",
                contract = "FUSD",
                events = events,
                startFrom = 1L,
                dbCollection = "",
            ),
            FlowChainId.TESTNET to flowDescriptor(
                address = "e223d8a629e49c68",
                contract = "FUSD",
                events = events,
                startFrom = 1L,
                dbCollection = ""
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                address = "0ae53cb6e3f42a79",
                contract = "FUSD",
                events = events,
                dbCollection = "",
            )
        )

}
