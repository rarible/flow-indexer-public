package com.rarrible.flow.block

data class ServiceBlockInfo(
    val lastBlockInIndexer: BlockInfo,
    val lastBlockInBlockchain: BlockInfo
)

data class BlockInfo(val blockHeight: Long, val timestamp: Long)
