package com.rarible.flow.api.service

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.rarible.blockchain.scanner.block.BlockRepository
import com.rarible.flow.core.block.BlockInfo
import com.rarible.flow.core.block.ServiceBlockInfo
import kotlinx.coroutines.future.await
import org.springframework.stereotype.Service
import java.time.ZoneOffset

@Service
class BlockInfoService(
    private val api: AsyncFlowAccessApi,
    private val blockRepository: BlockRepository,
) {

    suspend fun info(): ServiceBlockInfo {
        val blockOnChain = api.getLatestBlock(true).await()
        val blockInDb = blockRepository.getLastBlock()

        return ServiceBlockInfo(
            lastBlockInBlockchain = BlockInfo(
                blockHeight = blockOnChain.height,
                timestamp = blockOnChain.timestamp.toInstant(ZoneOffset.UTC).toEpochMilli()
            ),
            lastBlockInIndexer = blockInDb?.let {
                BlockInfo(
                    blockHeight = it.id,
                    timestamp = it.timestamp
                )
            }
        )
    }
}
