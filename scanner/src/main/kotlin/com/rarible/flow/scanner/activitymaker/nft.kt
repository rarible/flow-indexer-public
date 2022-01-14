package com.rarible.flow.scanner.activitymaker

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.nftco.flow.sdk.cadence.NumberField
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.StructField
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

sealed class NFTActivityMaker : ActivityMaker {

    abstract val contractName: String

    protected val cadenceParser: JsonCadenceParser = JsonCadenceParser()

    override fun isSupportedCollection(collection: String): Boolean =
        collection.split(".").last().lowercase() == contractName.lowercase()

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

            mintEvents.forEach {
                val tokenId = tokenId(it)
                val deposit = depositEvents.first { d -> cadenceParser.long(d.event.fields["id"]!!) == tokenId }
                result[deposit.log] = MintActivity(
                    creator = creator(it),
                    owner = cadenceParser.optional(deposit.event.fields["to"]!!) { value ->
                        address(value)
                    }!!,
                    contract = it.event.eventId.collection(),
                    tokenId = tokenId,
                    timestamp = deposit.log.timestamp,
                    metadata = meta(it),
                    royalties = royalties(it)
                )
            }

            withdrawEvents.forEach { w ->
                val tokenId = tokenId(w)
                val from: OptionalField by w.event.fields
                val depositActivity = depositEvents.find { d ->
                    val dTokenId = cadenceParser.long(d.event.fields["id"]!!)
                    dTokenId == tokenId && d.log.timestamp >= w.log.timestamp
                }

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
                    }
                }
            }
        }
        return result.toMap()
    }

    abstract fun tokenId(logEvent: FlowLogEvent): Long

    abstract fun meta(logEvent: FlowLogEvent): Map<String, String>

    protected open fun royalties(logEvent: FlowLogEvent): List<Part> = emptyList()

    protected open fun creator(logEvent: FlowLogEvent): String = logEvent.event.eventId.contractAddress.formatted
}

@Component
class TopShotActivityMaker(
    @Value("\${blockchain.scanner.flow.chainId}")
    private val chainId: FlowChainId
) : NFTActivityMaker() {
    override val contractName: String = "TopShot"

    private val royaltyAddress = mapOf(
        FlowChainId.MAINNET to FlowAddress("0xbd69b6abdfcf4539"),
        FlowChainId.TESTNET to FlowAddress("0x01658d9b94068f3c"),
    )

    override fun tokenId(logEvent: FlowLogEvent): Long = when (logEvent.type) {
        FlowLogType.MINT -> cadenceParser.long(logEvent.event.fields["momentID"]!!)
        else -> cadenceParser.long(logEvent.event.fields["id"]!!)
    }

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val msg = logEvent.event
        val playID: NumberField by msg.fields
        val setID: NumberField by msg.fields
        val serialNumber: NumberField by msg.fields
        return mapOf(
            "playID" to playID.value.toString(),
            "setID" to setID.value.toString(),
            "serialNumber" to serialNumber.value.toString()
        )
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return listOf(Part(address = royaltyAddress[chainId]!!, fee = 0.05))
    }
}

@Component
class MotoGPActivityMaker : NFTActivityMaker() {

    override val contractName: String = "MotoGPCard"

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()
}

@Component
class EvolutionActivityMaker : NFTActivityMaker() {
    override val contractName: String = "Evolution"

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = mapOf(
        "itemId" to "${cadenceParser.int(logEvent.event.fields["itemId"]!!)}",
        "setId" to "${cadenceParser.int(logEvent.event.fields["setId"]!!)}",
        "serialNumber" to "${cadenceParser.int(logEvent.event.fields["serialNumber"]!!)}"
    )

}

@Component
class RaribleNFTActivityMaker : NFTActivityMaker() {
    override val contractName: String = "RaribleNFT"

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        return try {
            cadenceParser.dictionaryMap(logEvent.event.fields["metadata"]!!) { key, value ->
                string(key) to string(value)
            }
        } catch (_: Exception) {
            mapOf("metaURI" to cadenceParser.string(logEvent.event.fields["metadata"]!!))
        }
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

    override fun creator(logEvent: FlowLogEvent): String {
        return cadenceParser.address(logEvent.event.fields["creator"]!!)
    }
}
