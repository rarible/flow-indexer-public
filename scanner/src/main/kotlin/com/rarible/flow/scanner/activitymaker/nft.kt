package com.rarible.flow.scanner.activitymaker

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.StructField
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.core.apm.withSpan
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.events.RaribleNFTv2Meta
import com.rarible.flow.scanner.TxManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

abstract class NFTActivityMaker : ActivityMaker {

    abstract val contractName: String

    protected val cadenceParser: JsonCadenceParser = JsonCadenceParser()

    @Value("\${blockchain.scanner.flow.chainId}")
    lateinit var chainId: FlowChainId

    @Autowired
    protected lateinit var txManager: TxManager

    fun <T> parse(fn: JsonCadenceParser.() -> T): T {
        return fn(cadenceParser)
    }

    override fun isSupportedCollection(collection: String): Boolean =
        collection.substringAfterLast(".").lowercase() == contractName.lowercase()

    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
        val result: MutableMap<FlowLog, BaseActivity> = mutableMapOf()
        withSpan("generateNftActivities", "event") {
            val filtered = events.filterNot {
                when (it.type) {
                    FlowLogType.WITHDRAW -> cadenceParser.optional(it.event.fields["from"]!!) { value ->
                        address(value)
                    } == null
                    FlowLogType.DEPOSIT -> cadenceParser.optional(it.event.fields["to"]!!) { value ->
                        address(value)
                    } == null
                    else -> false
                }
            }

            val mintEvents = filtered.filter { it.type == FlowLogType.MINT }
            val withdrawEvents = filtered.filter { it.type == FlowLogType.WITHDRAW }
            val depositEvents = filtered.filter { it.type == FlowLogType.DEPOSIT }
            val burnEvents = filtered.filter { it.type == FlowLogType.BURN }
            val usedDeposit = mutableSetOf<FlowLogEvent>()

            mintEvents.forEach {
                val tokenId = tokenId(it)
                val owner = depositEvents
                    .firstOrNull { d -> cadenceParser.long(d.event.fields["id"]!!) == tokenId }
                    ?.also(usedDeposit::add)
                    ?.let { d ->
                        cadenceParser.optional(d.event.fields["to"]!!) { value ->
                            address(value)
                        }
                    }
                    ?: it.event.eventId.contractAddress.formatted
                result[it.log] = MintActivity(
                    creator = creator(it),
                    owner = owner,
                    contract = it.event.eventId.collection(),
                    tokenId = tokenId,
                    timestamp = it.log.timestamp,
                    metadata = meta(it),
                    royalties = royalties(it),
                    collection = itemCollection(it)
                )
            }

            withdrawEvents.forEach { w ->
                val tokenId = tokenId(w)
                val from: OptionalField by w.event.fields
                val depositActivity = depositEvents.find { d ->
                    val dTokenId = cadenceParser.long(d.event.fields["id"]!!)
                    dTokenId == tokenId && d.log.timestamp >= w.log.timestamp
                }?.also(usedDeposit::add)

                if (depositActivity != null) {
                    val to: OptionalField by depositActivity.event.fields
                    result[depositActivity.log] = TransferActivity(
                        contract = w.event.eventId.collection(),
                        tokenId = tokenId,
                        timestamp = depositActivity.log.timestamp,
                        from = cadenceParser.optional(from) {
                            address(it)
                        }!!,
                        to = cadenceParser.optional(to) {
                            address(it)
                        }!!
                    )
                } else {
                    val burnActivity = burnEvents.find { b ->
                        val bTokenId = cadenceParser.long(b.event.fields["id"]!!)
                        bTokenId == tokenId && b.log.timestamp >= w.log.timestamp
                    }
                    if (burnActivity != null) {
                        result[burnActivity.log] = BurnActivity(
                            contract = burnActivity.event.eventId.collection(),
                            tokenId = tokenId,
                            owner = cadenceParser.optional(from) {
                                address(it)
                            },
                            timestamp = burnActivity.log.timestamp
                        )
                    } else {
                        result[w.log] = TransferActivity(
                            contract = w.event.eventId.collection(),
                            tokenId = tokenId,
                            timestamp = w.log.timestamp,
                            from = cadenceParser.optional(from) {
                                address(it)
                            }!!,
                            to = w.event.eventId.contractAddress.formatted
                        )
                    }
                }
            }

            (depositEvents - usedDeposit).forEach { d ->
                val to: OptionalField by d.event.fields
                result[d.log] = TransferActivity(
                    contract = d.event.eventId.collection(),
                    tokenId = tokenId(d),
                    timestamp = d.log.timestamp,
                    from = d.event.eventId.contractAddress.formatted,
                    to = cadenceParser.optional(to) { address(it) }!!
                )
            }
        }
        return result.toMap()
    }

    abstract fun tokenId(logEvent: FlowLogEvent): Long

    abstract fun meta(logEvent: FlowLogEvent): Map<String, String>

    protected open fun royalties(logEvent: FlowLogEvent): List<Part> = emptyList()

    protected open fun creator(logEvent: FlowLogEvent): String = logEvent.event.eventId.contractAddress.formatted

    protected open suspend fun itemCollection(mintEvent: FlowLogEvent): String = mintEvent.event.eventId.collection()
}

@Component
class MotoGPActivityMaker : NFTActivityMaker() {

    override val contractName: String = "MotoGPCard"

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.MOTOGP.staticRoyalties(chainId)
    }
}

@Component
class RaribleNFTV2ActivityMaker: NFTActivityMaker() {

    override val contractName: String = Contracts.RARIBLE_NFTV2.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long {
        return cadenceParser.long(logEvent.event.fields["id"]!!)
    }

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val meta by logEvent.event.fields
        val rariMeta = cadenceParser.unmarshall<RaribleNFTv2Meta>(meta, Contracts.RARIBLE_NFTV2.deployments[chainId]!!)
        return rariMeta.toMap()
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return cadenceParser.arrayValues(logEvent.event.fields["royalties"]!!) {
            it as StructField
            Part(
                address = FlowAddress(address(it.value!!.getRequiredField("address"))),
                fee = double(it.value!!.getRequiredField("fee"))
            )
        }
    }

    override suspend fun itemCollection(mintEvent: FlowLogEvent): String {
        val parentId = cadenceParser.long(mintEvent.event.fields["parentId"]!!)
        return "${ItemId(Contracts.SOFT_COLLECTION.fqn(chainId), parentId)}"
    }
}

@Component
class EvolutionActivityMaker : NFTActivityMaker() {
    override val contractName: String = Contracts.EVOLUTION.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = mapOf(
        "itemId" to "${cadenceParser.int(logEvent.event.fields["itemId"]!!)}",
        "setId" to "${cadenceParser.int(logEvent.event.fields["setId"]!!)}",
        "serialNumber" to "${cadenceParser.int(logEvent.event.fields["serialNumber"]!!)}"
    )

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.EVOLUTION.staticRoyalties(chainId)
    }
}
