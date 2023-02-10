package com.rarible.flow.core.block

import java.util.concurrent.TimeUnit

data class ServiceBlockInfo(
    val lastBlockInIndexer: BlockInfo,
    val lastBlockInBlockchain: BlockInfo
) {

    val blockLatency: Long = lastBlockInBlockchain.blockHeight - lastBlockInIndexer.blockHeight
    val timeLatencyMs: Long = lastBlockInBlockchain.timestamp - lastBlockInIndexer.timestamp
    val timeLatency: String = timeLatencyMs.toHoursStr()
}

data class BlockInfo(val blockHeight: Long, val timestamp: Long)

private fun Long.toHoursStr(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(
        hours
    )
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(
        TimeUnit.MILLISECONDS.toMinutes(this)
    )
    return "${"%02d".format(hours)}h ${"%02d".format(minutes)}m ${"%02d".format(seconds)}s"
}
