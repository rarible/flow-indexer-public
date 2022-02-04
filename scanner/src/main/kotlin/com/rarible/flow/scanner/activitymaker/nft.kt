package com.rarible.flow.scanner.activitymaker

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.nftco.flow.sdk.cadence.NumberField
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.StructField
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.events.VersusArtMetadata
import kotlinx.coroutines.reactor.awaitSingleOrNull
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
                val owner = depositEvents
                    .firstOrNull { d -> cadenceParser.long(d.event.fields["id"]!!) == tokenId }
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
class TopShotActivityMaker(
    @Value("\${blockchain.scanner.flow.chainId}")
    private val chainId: FlowChainId,
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

@Component
class VersusArtActivityMaker : NFTActivityMaker() {

    override val contractName = "Art"

    override fun tokenId(logEvent: FlowLogEvent) = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent) = try {
        val meta = Flow.unmarshall(VersusArtMetadata::class, logEvent.event.fields["metadata"]!!)
        mapOf(
            "name" to meta.name,
            "artist" to meta.artist,
            "artistAddress" to meta.artistAddress,
            "description" to meta.description,
            "type" to meta.type,
            "edition" to meta.edition.toString(),
            "maxEdition" to meta.maxEdition.toString(),
        )
    } catch (_: Exception) {
        mapOf("metaURI" to cadenceParser.string(logEvent.event.fields["metadata"]!!))
    }

    override fun creator(logEvent: FlowLogEvent) = try {
        Flow.unmarshall(VersusArtMetadata::class, logEvent.event.fields["metadata"]!!).artistAddress
    } catch (_: Exception) {
        logEvent.event.eventId.contractAddress.formatted
    }
}

@Component
class RaribleV2ActivityMaker(
    private val collectionRepository: ItemCollectionRepository
): NFTActivityMaker() {

    override val contractName: String = "RaribleNFTv2"

    override fun tokenId(logEvent: FlowLogEvent): Long {
        return cadenceParser.long(logEvent.event.fields["id"]!!)
    }

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        return cadenceParser.dictionaryMap(logEvent.event.fields["metadata"]!!) { key, value ->
            string(key) to string(value)
        } + mapOf("parentId" to "${cadenceParser.long(logEvent.event.fields["parentId"]!!)}")
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
        val collection = collectionRepository.findByChainId(parentId).awaitSingleOrNull()
        return collection?.id ?: super.itemCollection(mintEvent)
    }
}

@Component
class DisruptArtActivityMaker(
    @Value("\${blockchain.scanner.flow.chainId}")
    private val chainId: FlowChainId,
) : NFTActivityMaker() {
    override val contractName: String = "DisruptArt"

    private val royaltyAddress = mapOf(
        FlowChainId.MAINNET to FlowAddress("0x420f47f16a214100"),
        FlowChainId.TESTNET to FlowAddress("0x439c2b49c0b2f62b"),
    )


    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val res = mutableMapOf(
            "content" to cadenceParser.string(logEvent.event.fields["content"]!!),
            "name" to cadenceParser.string(logEvent.event.fields["name"]!!)
        )

        if (logEvent.event.eventId.eventName == "GroupMint") {
            res["tokenGroupId"] = "${cadenceParser.long(logEvent.event.fields["tokenGroupId"]!!)}"
        }
        return res.toMap()
    }

    override fun creator(logEvent: FlowLogEvent): String = cadenceParser.optional(logEvent.event.fields["owner"]!!) {
        address(it)
    } ?: super.creator(logEvent)

    override fun royalties(logEvent: FlowLogEvent): List<Part> =
        listOf(Part(address = royaltyAddress[chainId]!!, fee = 0.15))
}
