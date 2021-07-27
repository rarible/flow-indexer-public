package com.rarible.flow.events

/**
 * Information about block for events
 *
 * @property transactionId  transaction ID (uuid)
 * @property blockHeight    block height
 * @property blockId        block ID (uuid)
 */
data class BlockInfo(
    val transactionId: String = "",
    val blockHeight: Long = 0L,
    val blockId: String = ""
)
