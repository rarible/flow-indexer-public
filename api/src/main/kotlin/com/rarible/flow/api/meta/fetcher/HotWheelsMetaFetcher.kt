package com.rarible.flow.api.meta.fetcher

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.bytesToHex
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.findItemMint
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HotWheelsMetaFetcher(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
    private val hwMetaEventTypeProvider: HotWheelsMetaEventTypeProvider,
    private val accessApi: AsyncFlowAccessApi,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getContent(itemId: ItemId): String? {
        val mint = getMintEvent(itemId) ?: return null
        val metaEventType = getMetaEventType(itemId) ?: return null
        return getEditionMetadataPayload(
            metaEventType = metaEventType,
            blockHeight = mint.log.blockHeight,
            transaction = mint.log.transactionHash,
            eventIndex = mint.log.eventIndex
        )
    }

    private suspend fun getMintEvent(itemId: ItemId): ItemHistory? {
        return itemHistoryRepository.findItemMint(itemId.contract, itemId.tokenId).run {
            if (this.size != 1) {
                logger.error( "Found $size mints for item $itemId, expected 1 event")
                null
            } else single()
        }
    }

    private fun getMetaEventType(itemId: ItemId): String? {
        val type = hwMetaEventTypeProvider.getMetaEventType(itemId)
        if (type == null) logger.error("Can't find meta event type for $itemId")
        return type
    }

    private suspend fun getEditionMetadataPayload(
        metaEventType: String,
        blockHeight: Long,
        transaction: String,
        eventIndex: Int
    ): String? {
        val range = LongRange(blockHeight, blockHeight)
        val blockEvents = accessApi.getEventsForHeightRange(metaEventType, range).await().run {
            if (this.size != 1) {
                logger.error("Found $size blocks by height $blockHeight, expected 1")
                return null
            } else single().events
        }
        val metadataEvent = blockEvents.firstOrNull {
            it.transactionId.bytes.bytesToHex() == transaction && it.eventIndex == eventIndex
        } ?: run {
            logger.error("Can't find event (tx=$transaction, eventIndex=$eventIndex, type=$metaEventType)")
            return null
        }
        return metadataEvent.payload.stringValue
    }
}

