package com.rarible.flow.api.meta.fetcher

import com.nftco.flow.sdk.bytesToHex
import com.nftco.flow.sdk.cadence.Field
import com.rarible.blockchain.scanner.flow.service.FlowApiFactory
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.flow.api.service.meta.MetaEventType
import com.rarible.flow.core.config.FeatureFlagsProperties
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.RawOnChainMeta
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.RawOnChainMetaCacheRepository
import com.rarible.flow.core.repository.findItemFirstTransfer
import com.rarible.flow.core.repository.findItemMint
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RawOnChainMetaFetcher(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemHistoryRepository: ItemHistoryRepository,
    private val rawOnChainMetaCacheRepository: RawOnChainMetaCacheRepository,
    private val sporkService: SporkService,
    private val flowApiFactory: FlowApiFactory,
    private val ff: FeatureFlagsProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getContent(itemId: ItemId, metaEventType: MetaEventType?): String? {
        if (ff.enableRawOnChainMetaCacheRead) {
            rawOnChainMetaCacheRepository.findById(itemId.toString()).awaitFirstOrNull()?.let { return it.data }
        }

        val mint = getMintEvent(itemId) ?: return null
        if (metaEventType == null) {
            logger.error("Can't find meta event type for $itemId")
            return null
        }

        val result = getEditionMetadataPayload(
            tokenId = itemId.tokenId,
            metaEventType = metaEventType.eventType,
            idField = metaEventType.id,
            blockHeight = mint.log.blockHeight,
            transaction = mint.log.transactionHash,
        )

        if (ff.enableRawOnChainMetaCacheWrite) {
            result?.let {
                val rawMeta = RawOnChainMeta(itemId.toString(), it)
                rawOnChainMetaCacheRepository.save(rawMeta).awaitFirst()
            }
        }

        return result
    }

    private suspend fun getMintEvent(itemId: ItemId): ItemHistory? {
        return itemHistoryRepository.findItemMint(itemId.contract, itemId.tokenId).firstOrNull()
            ?: itemHistoryRepository.findItemFirstTransfer(itemId.contract, itemId.tokenId)
    }

    private suspend fun getEditionMetadataPayload(
        metaEventType: String,
        idField: String,
        blockHeight: Long,
        transaction: String,
        tokenId: Long
    ): String? {
        val range = LongRange(blockHeight, blockHeight)
        val blockEvents =
            flowApiFactory.getApi(sporkService.spork(blockHeight)).getEventsForHeightRange(metaEventType, range).await()
                .run {
                    if (this.size != 1) {
                        logger.error("Found $size blocks by height $blockHeight, expected 1")
                        return null
                    } else single().events
                }
        val metadataEvent = blockEvents.firstOrNull {
            val id = it.event.get<Field<String>>(idField)?.value?.toLong()
            it.transactionId.bytes.bytesToHex() == transaction && id == tokenId
        } ?: run {
            logger.error("Can't find event (tx=$transaction, tokenId=$tokenId, type=$metaEventType)")
            return null
        }
        return metadataEvent.payload.stringValue
    }
}
