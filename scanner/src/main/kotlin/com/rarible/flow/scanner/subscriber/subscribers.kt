package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.AddressField
import com.nftco.flow.sdk.cadence.OptionalField
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.BalanceHistory
import com.rarible.flow.core.domain.BalanceId
import java.math.BigDecimal
import java.time.Instant

@Deprecated(message = "Use the signature with Contracts type", replaceWith = ReplaceWith(
    "com.rarible.flow.scanner.subscriber.SubscribersKt.flowDescriptor(com.rarible.flow.Contracts, com.nftco.flow.sdk.FlowChainId, java.lang.Iterable<java.lang.String>, java.lang.Long, java.lang.String, java.lang.Iterable<java.lang.String>)"
)
)
internal fun flowDescriptor(
    address: String,
    contract: String,
    events: Iterable<String>,
    startFrom: Long? = null,
    dbCollection: String,
) = FlowDescriptor(
    id = "${contract}Descriptor",
    events = events.map { "A.$address.$contract.$it" }.toSet(),
    collection = dbCollection,
    startFrom = startFrom
)

internal fun flowDescriptor(
    contract: Contracts,
    chainId: FlowChainId,
    events: Iterable<String>,
    startFrom: Long? = null,
    dbCollection: String,
    additionalEvents: Iterable<String> = emptyList(),
): FlowDescriptor {
    val address = contract.deployments[chainId]?.base16Value
    val eventsSet = events.map { "${contract.fqn(chainId)}.$it" }.toSet()
    val additionalSet = additionalEvents.map { "A.${address}.$it" }
    return FlowDescriptor(
        id = contract.flowDescriptorName(),
        events = eventsSet + additionalSet,
        collection = dbCollection,
        startFrom = startFrom
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
            status = Log.Status.CONFIRMED,
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