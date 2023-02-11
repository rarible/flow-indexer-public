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
import java.math.BigDecimal
import java.time.Instant

@Deprecated(message = "Use the signature with Contracts type", replaceWith = ReplaceWith(
    "com.rarible.flow.scanner.subscriber.SubscribersKt.flowDescriptor(com.rarible.flow.Contracts, com.nftco.flow.sdk.FlowChainId, java.lang.Iterable<java.lang.String>, java.lang.Long, java.lang.String, java.lang.Iterable<java.lang.String>)"
))
private fun flowDescriptor(
    address: String,
    contract: String,
    events: Iterable<String>,
    startFrom: Long? = null,
    dbCollection: String,
    groupId: String,
) = FlowDescriptor(
    groupId = groupId,
    entityType = FlowLogRecord::class.java,
    id = "${contract}Descriptor",
    events = events.map { "A.$address.$contract.$it" }.toSet(),
    collection = dbCollection,
    startFrom = startFrom
)

@Deprecated(message = "Use the signature with Contracts type", replaceWith = ReplaceWith(
    "com.rarible.flow.scanner.subscriber.SubscribersKt.flowDescriptor(com.rarible.flow.Contracts, com.nftco.flow.sdk.FlowChainId, java.lang.Iterable<java.lang.String>, java.lang.Long, java.lang.String, java.lang.Iterable<java.lang.String>)"
))
fun flowNftDescriptor(
    address: String,
    contract: String,
    events: Iterable<String>,
    startFrom: Long? = null,
    dbCollection: String,
) = flowDescriptor(
    address = address,
    contract = contract,
    events = events,
    startFrom = startFrom,
    dbCollection = dbCollection,
    groupId = "item-history",
)

@Deprecated(message = "Use the signature with Contracts type", replaceWith = ReplaceWith(
    "com.rarible.flow.scanner.subscriber.SubscribersKt.flowDescriptor(com.rarible.flow.Contracts, com.nftco.flow.sdk.FlowChainId, java.lang.Iterable<java.lang.String>, java.lang.Long, java.lang.String, java.lang.Iterable<java.lang.String>)"
))
fun flowOrderDescriptor(
    address: String,
    contract: String,
    events: Iterable<String>,
    startFrom: Long? = null,
    dbCollection: String,
) = flowDescriptor(
    address = address,
    contract = contract,
    events = events,
    startFrom = startFrom,
    dbCollection = dbCollection,
    groupId = "order-history",
)

@Deprecated(message = "Use the signature with Contracts type", replaceWith = ReplaceWith(
    "com.rarible.flow.scanner.subscriber.SubscribersKt.flowDescriptor(com.rarible.flow.Contracts, com.nftco.flow.sdk.FlowChainId, java.lang.Iterable<java.lang.String>, java.lang.Long, java.lang.String, java.lang.Iterable<java.lang.String>)"
))
fun flowBalanceDescriptor(
    address: String,
    contract: String,
    events: Iterable<String>,
    startFrom: Long? = null,
    dbCollection: String,
) = flowDescriptor(
    address = address,
    contract = contract,
    events = events,
    startFrom = startFrom,
    dbCollection = dbCollection,
    groupId = "balance-history",
)


private fun flowDescriptor(
    contract: Contracts,
    chainId: FlowChainId,
    events: Iterable<String>,
    startFrom: Long? = null,
    dbCollection: String,
    additionalEvents: Iterable<String> = emptyList(),
    groupId: String
): FlowDescriptor {
    val address = contract.deployments[chainId]?.base16Value
    val eventsSet = events.map { "${contract.fqn(chainId)}.$it" }.toSet()
    val additionalSet = additionalEvents.map { "A.${address}.$it" }
    return FlowDescriptor(
        groupId = groupId,
        entityType = FlowLogRecord::class.java,
        id = contract.flowDescriptorName(),
        events = eventsSet + additionalSet,
        collection = dbCollection,
        startFrom = startFrom
    )
}

internal fun flowNftDescriptor(
    contract: Contracts,
    chainId: FlowChainId,
    events: Iterable<String>,
    startFrom: Long? = null,
    dbCollection: String,
    additionalEvents: Iterable<String> = emptyList(),
): FlowDescriptor = flowDescriptor(
    contract = contract,
    chainId = chainId,
    events = events,
    startFrom = startFrom,
    dbCollection = dbCollection,
    additionalEvents = additionalEvents,
    groupId = "item-history"
)

internal fun flowOrderDescriptor(
    contract: Contracts,
    chainId: FlowChainId,
    events: Iterable<String>,
    startFrom: Long? = null,
    dbCollection: String,
    additionalEvents: Iterable<String> = emptyList(),
): FlowDescriptor = flowDescriptor(
    contract = contract,
    chainId = chainId,
    events = events,
    startFrom = startFrom,
    dbCollection = dbCollection,
    additionalEvents = additionalEvents,
    groupId = "order-history"
)

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