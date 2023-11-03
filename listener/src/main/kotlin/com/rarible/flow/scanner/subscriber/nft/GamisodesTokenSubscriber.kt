package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.CachedSporksFlowGrpcApi
import com.rarible.blockchain.scanner.flow.FlowNetNewBlockPoller
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainClient
import com.rarible.blockchain.scanner.flow.configuration.FlowBlockchainScannerProperties
import com.rarible.blockchain.scanner.flow.http.FlowHttpClientImpl
import com.rarible.blockchain.scanner.flow.service.FlowApiFactoryImpl
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.blockchain.scanner.monitoring.BlockchainMonitor
import com.rarible.flow.Contracts
import com.rarible.flow.scanner.model.NonFungibleTokenEventType
import com.rarible.flow.scanner.subscriber.EnableGamisodesToken
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

@Component
@EnableGamisodesToken
class GamisodesTokenSubscriber : NonFungibleTokenSubscriber() {

    private val mintEventName = "NFTMinted"
    private val burnEventName = "NFTBurned"

    override val events = setOf(
        mintEventName,
        NonFungibleTokenEventType.DEPOSIT.eventName,
        NonFungibleTokenEventType.WITHDRAW.eventName,
        burnEventName
    )
    override val name = "gamisodes"
    override val contract = Contracts.GAMISODES

    override fun fromEventName(eventName: String) =
        when (eventName) {
            mintEventName -> NonFungibleTokenEventType.MINT
            burnEventName -> NonFungibleTokenEventType.BURN
            else -> super.fromEventName(eventName)
        }
}

fun main() = runBlocking<Unit> {
    val props = FlowBlockchainScannerProperties(chainId = FlowChainId.MAINNET)
    val factory = FlowApiFactoryImpl(BlockchainMonitor(SimpleMeterRegistry()), props)
    val api = CachedSporksFlowGrpcApi(
        sporkService = SporkService(props, factory),
        properties = props,
        flowApiFactory = factory
    )
    val client = FlowBlockchainClient(
        poller = FlowNetNewBlockPoller(props, api),
        api = api,
        httpApi = FlowHttpClientImpl(props),
        properties = props,
    )

    val descriptor = GamisodesTokenSubscriber().descriptors[props.chainId]!!
    val blocks = 64701155L..64701155L
    val result = client.getBlockLogs(
        descriptor = descriptor,
        blocks = blocks.map { FlowBlockchainBlock(it, "", "", 0L) },
        stable = true
    ).toList()
    println("--------------------------------")

    result.map { it.logs }.flatten().forEach { println(it.event.type + "  " + it.event.eventIndex) }
}
