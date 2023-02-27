package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.AddressField
import com.nftco.flow.sdk.cadence.OptionalField
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.BalanceHistory
import com.rarible.flow.core.domain.BalanceId
import com.rarible.flow.scanner.model.SubscriberGroups
import java.math.BigDecimal
import java.time.Instant

object DescriptorFactory {
    @Deprecated(message = "Use the signature with Contracts type", replaceWith = ReplaceWith(""))
    private fun flowLegacyDescriptor(
        address: String,
        contract: String,
        events: Iterable<String>,
        startFrom: Long? = null,
        dbCollection: String,
        groupId: String,
        name: String,
    ) = FlowDescriptor(
        groupId = groupId,
        entityType = FlowLogRecord::class.java,
        id = "${contract}Descriptor",
        events = events.map { "A.$address.$contract.$it" }.toSet(),
        address = address,
        collection = dbCollection,
        startFrom = startFrom,
        alias = name
    )

    private fun flowDescriptor(
        contract: Contracts,
        chainId: FlowChainId,
        events: Iterable<String>,
        startFrom: Long? = null,
        dbCollection: String,
        additionalEvents: Iterable<String> = emptyList(),
        groupId: String,
        name: String,
    ): FlowDescriptor {
        val address = contract.deployments[chainId]!!.base16Value
        val eventsSet = events.map { "${contract.fqn(chainId)}.$it" }.toSet()
        val additionalSet = additionalEvents.map { "A.${address}.$it" }
        return FlowDescriptor(
            groupId = groupId,
            entityType = FlowLogRecord::class.java,
            id = contract.flowDescriptorName(),
            events = eventsSet + additionalSet,
            address = address,
            collection = dbCollection,
            startFrom = startFrom,
            alias = name
        )
    }

    @Deprecated(message = "Use the signature with Contracts type", replaceWith = ReplaceWith(""))
    fun flowNftOrderDescriptor(
        address: String,
        contract: String,
        events: Iterable<String>,
        startFrom: Long? = null,
        dbCollection: String,
        name: String,
    ) = flowLegacyDescriptor(
        address = address,
        contract = contract,
        events = events,
        startFrom = startFrom,
        dbCollection = dbCollection,
        groupId = SubscriberGroups.NFT_ORDER_HISTORY,
        name = name,
    )

    @Deprecated(message = "Use the signature with Contracts type", replaceWith = ReplaceWith(""))
    fun flowAuctionDescriptor(
        address: String,
        contract: String,
        events: Iterable<String>,
        startFrom: Long? = null,
        dbCollection: String,
        name: String,
    ) = flowLegacyDescriptor(
        address = address,
        contract = contract,
        events = events,
        startFrom = startFrom,
        dbCollection = dbCollection,
        groupId = SubscriberGroups.AUCTION_HISTORY,
        name = name,
    )

    @Deprecated(message = "Use the signature with Contracts type", replaceWith = ReplaceWith(""))
    fun flowBalanceDescriptor(
        address: String,
        contract: String,
        events: Iterable<String>,
        startFrom: Long? = null,
        dbCollection: String,
        name: String,
    ) = flowLegacyDescriptor(
        address = address,
        contract = contract,
        events = events,
        startFrom = startFrom,
        dbCollection = dbCollection,
        groupId = SubscriberGroups.BALANCE_HISTORY,
        name = name,
    )

    fun flowNftOrderDescriptor(
        contract: Contracts,
        chainId: FlowChainId,
        events: Iterable<String>,
        startFrom: Long? = null,
        dbCollection: String,
        additionalEvents: Iterable<String> = emptyList(),
        name: String,
    ): FlowDescriptor = flowDescriptor(
        contract = contract,
        chainId = chainId,
        events = events,
        startFrom = startFrom,
        dbCollection = dbCollection,
        additionalEvents = additionalEvents,
        groupId = SubscriberGroups.NFT_ORDER_HISTORY,
        name = name
    )

    fun flowVersusArtDescriptor(
        contract: Contracts,
        chainId: FlowChainId,
        events: Iterable<String>,
        startFrom: Long? = null,
        dbCollection: String,
        additionalEvents: Iterable<String> = emptyList(),
        name: String,
    ): FlowDescriptor = flowDescriptor(
        contract = contract,
        chainId = chainId,
        events = events,
        startFrom = startFrom,
        dbCollection = dbCollection,
        additionalEvents = additionalEvents,
        groupId = SubscriberGroups.VERSUS_ART_HISTORY,
        name = name
    )

    fun flowCollectionDescriptor(
        contract: Contracts,
        chainId: FlowChainId,
        events: Iterable<String>,
        startFrom: Long? = null,
        dbCollection: String,
        additionalEvents: Iterable<String> = emptyList(),
        name: String,
    ): FlowDescriptor = flowDescriptor(
        contract = contract,
        chainId = chainId,
        events = events,
        startFrom = startFrom,
        dbCollection = dbCollection,
        additionalEvents = additionalEvents,
        groupId = SubscriberGroups.COLLECTION_HISTORY,
        name = name,
    )
}


fun Contracts.flowDescriptorName() = "${this.contractName}Descriptor"

internal fun balanceHistory(
    balanceId: BalanceId,
    change: BigDecimal,
    block: FlowBlockchainBlock,
    logRecord: FlowBlockchainLog
): BalanceHistory {
    val time = Instant.ofEpochMilli(block.timestamp)
    return BalanceHistory(
        balanceId,
        change,
        time,
        log = FlowLog(
            transactionHash = logRecord.event.transactionId.base16Value,
            eventIndex = logRecord.event.eventIndex,
            eventType = logRecord.event.type,
            timestamp = time,
            blockHeight = block.number,
            blockHash = block.hash
        )
    )
}

internal fun balanceHistory(
    address: OptionalField,
    amount: BigDecimal,
    token: String,
    block: FlowBlockchainBlock,
    logRecord: FlowBlockchainLog
): BalanceHistory {
    val flowAddress = FlowAddress((address.value as AddressField).value!!)
    return balanceHistory(BalanceId(flowAddress, token), amount, block, logRecord)
}