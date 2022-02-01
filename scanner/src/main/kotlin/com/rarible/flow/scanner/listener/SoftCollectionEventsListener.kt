package com.rarible.flow.scanner.listener

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.nftco.flow.sdk.cadence.StructField
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventListener
import com.rarible.blockchain.scanner.subscriber.ProcessedBlockEvent
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.repository.ItemCollectionRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class SoftCollectionEventsListener(
    private val itemCollectionRepository: ItemCollectionRepository
) : FlowLogEventListener {

    private val parser = JsonCadenceParser()

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
        val collectionName = parser.string((meta as StructField).value!!.getRequiredField("name"))
        val itemCollection = ItemCollection(
            id = "A.${creatorAddress.base16Value}.${collectionName.replace(" ", "_")}",
            owner = creatorAddress,
            name = collectionName,
            symbol = parser.string((meta as StructField).value!!.getRequiredField("symbol")),
            createdDate = event.log.timestamp,
            features = setOf("BURN", "SECONDARY_SALE_FEES"),
            collectionOnChainId = parser.long(id),
            collectionOnChainParentId = parser.optional(parentId, JsonCadenceParser::long),
            royalties = parser.arrayValues(royalties) {
                it as StructField
                Part(
                    address = FlowAddress(address(it.value!!.getRequiredField("address"))),
                    fee = double(it.value!!.getRequiredField("fee"))
                )
            }
        )
        itemCollectionRepository.save(itemCollection).awaitSingleOrNull()
    }

    private fun updateSoftCollection(event: FlowLogEvent) {
        TODO("Not yet implemented")
    }

    override suspend fun onPendingLogsDropped(logs: List<FlowLogRecord<*>>) {
        /** do nothing*/
    }
}
