package com.rarible.flow.api.service

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.rarible.blockchain.scanner.flow.model.FlowBlock
import com.rarrible.flow.block.BlockInfo
import com.rarrible.flow.block.ServiceBlockInfo
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.ZoneOffset

@Service
class BlockInfoService(
    private val api: AsyncFlowAccessApi,
    private val mongo: ReactiveMongoTemplate
) {

    suspend fun info(): ServiceBlockInfo {
        val blockOnChain = api.getLatestBlock(true).await()
        val query = Query().addCriteria(Criteria()).limit(1).with(Sort.by(Sort.Direction.DESC, FlowBlock::id.name))
        val blockInDb = mongo.find(query, FlowBlock::class.java, "flowBlock").awaitFirst()

        return ServiceBlockInfo(
            lastBlockInBlockchain = BlockInfo(
                blockHeight = blockOnChain.height,
                timestamp = blockOnChain.timestamp.toInstant(ZoneOffset.UTC).toEpochMilli()
            ),
            lastBlockInIndexer = BlockInfo(
                blockHeight = blockInDb.id,
                timestamp = blockInDb.timestamp().toEpochMilli()
            )
        )
    }
}
