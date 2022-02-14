package com.rarible.flow.scanner.listener

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.*
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventListener
import com.rarible.blockchain.scanner.subscriber.ProcessedBlockEvent
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.scanner.model.parse
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SoftCollectionEventsListener(
    private val itemCollectionRepository: ItemCollectionRepository,
    @Value("\${blockchain.scanner.flow.chainId}")
    private val chainId: FlowChainId
) : FlowLogEventListener {

    private val parser = JsonCadenceParser()

    private val softCollectionContractAddress by lazy {
        "A.${Flow.DEFAULT_ADDRESS_REGISTRY.addressOf("0xSOFTCOLLECTION", chainId)!!.base16Value}.SoftCollection"
    }

    override suspend fun onBlockLogsProcessed(blockEvent: ProcessedBlockEvent<FlowLog, FlowLogRecord<*>>) {
        blockEvent.records.filterIsInstance<FlowLogEvent>()
            .filter {
                it.type in setOf(FlowLogType.COLLECTION_MINT,
                    FlowLogType.COLLECTION_WITHDRAW,
                    FlowLogType.COLLECTION_DEPOSIT,
                    FlowLogType.COLLECTION_CHANGE,
                    FlowLogType.COLLECTION_BURN)
            }.forEach { event ->
                when (event.type) {
                    FlowLogType.COLLECTION_MINT -> createSoftCollection(event)
                    FlowLogType.COLLECTION_CHANGE -> updateSoftCollection(event)
                    else -> {}
                }
            }
    }

    private suspend fun createSoftCollection(event: FlowLogEvent) {
        val id by event.event.fields
        val parentId by event.event.fields
        val meta by event.event.fields
        val creator by event.event.fields
        val royalties by event.event.fields

        val creatorAddress = FlowAddress(parser.address(creator))
        val collectionMeta = meta.parse<CollectionMeta>()
        val collectionChainId = parser.long(id)
        val itemCollection = ItemCollection(
            id = "${softCollectionContractAddress}:$collectionChainId",
            owner = creatorAddress,
            name = collectionMeta.name,
            symbol = collectionMeta.symbol,
            createdDate = event.log.timestamp,
            features = setOf("BURN", "SECONDARY_SALE_FEES"),
            chainId = collectionChainId,
            chainParentId = parser.optional(parentId, JsonCadenceParser::long),
            royalties = parser.arrayValues(royalties) {
                it as StructField
                Part(
                    address = FlowAddress(address(it.value!!.getRequiredField("address"))),
                    fee = double(it.value!!.getRequiredField("fee"))
                )
            },
            isSoft = true,
            description = collectionMeta.description,
            icon = collectionMeta.icon,
            url = collectionMeta.url
        )
        itemCollectionRepository.save(itemCollection).awaitSingleOrNull()
    }

    private suspend fun updateSoftCollection(event: FlowLogEvent) {
        val id by event.event.fields
        val meta by event.event.fields

        val collectionMeta = meta.parse<CollectionMeta>()
        val collectionId = "${ItemId(softCollectionContractAddress, parser.long(id))}"
        val entity = itemCollectionRepository.findById(collectionId).awaitSingleOrNull() ?: throw IllegalStateException("Collection with id [$collectionId] not found")
        itemCollectionRepository.save(entity.copy(
            name = collectionMeta.name,
            symbol = collectionMeta.symbol,
            icon = collectionMeta.icon,
            description = collectionMeta.description,
            url = collectionMeta.url
        )).awaitSingleOrNull()
    }

    override suspend fun onPendingLogsDropped(logs: List<FlowLogRecord<*>>) {
        /** do nothing*/
    }
}

@JsonCadenceConversion(CollectionMetaConversion::class)
data class CollectionMeta(
    val name: String,
    val symbol: String,
    val icon: String?,
    val description: String?,
    val url: String?
)

class CollectionMetaConversion: JsonCadenceConverter<CollectionMeta> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): CollectionMeta = unmarshall(value) {
        CollectionMeta(
            name = string("name"),
            symbol = string("symbol"),
            description = optional("description", JsonCadenceParser::string),
            icon = optional("icon", JsonCadenceParser::string),
            url = optional("url", JsonCadenceParser::string),
        )
    }
}
