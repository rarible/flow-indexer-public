package com.rarible.flow.api.meta.fetcher

import com.nftco.flow.sdk.bytesToHex
import com.nftco.flow.sdk.cadence.Field
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.flow.api.service.HWMetaEventTypeProvider
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.findItemMint
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HWMetaFetcher(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
    private val hwMetaEventTypeProvider: HWMetaEventTypeProvider,
    private val sporkService: SporkService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getContent(itemId: ItemId): String? {
        val mint = getMintEvent(itemId) ?: return null
        val metaEventType = getMetaEventType(itemId) ?: return null
        return getEditionMetadataPayload(
            tokenId = itemId.tokenId,
            metaEventType = metaEventType,
            blockHeight = mint.log.blockHeight,
            transaction = mint.log.transactionHash,
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
        tokenId: Long
    ): String? {
        val range = LongRange(blockHeight, blockHeight)
        val blockEvents = sporkService.spork(blockHeight).api.getEventsForHeightRange(metaEventType, range).await().run {
            if (this.size != 1) {
                logger.error("Found $size blocks by height $blockHeight, expected 1")
                return null
            } else single().events
        }
        val metadataEvent = blockEvents.firstOrNull {
            val id = it.event.get<Field<String>>("id")?.value?.toLong()
            it.transactionId.bytes.bytesToHex() == transaction && id == tokenId
        } ?: run {
            logger.error("Can't find event (tx=$transaction, tokenId=$tokenId, type=$metaEventType)")
            return null
        }
        return metadataEvent.payload.stringValue
    }
}

