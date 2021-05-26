package com.rarible.flow.scanner.events

import com.rarible.flow.scanner.model.FlowBlock

/**
 * Created by TimochkinEA at 22.05.2021
 */
data class FlowBlockRangeRequest(val from: Long, val to: Long)

data class FlowBlockReceived(val block: FlowBlock)

data class FlowBlockReadyForPersist(val block: FlowBlock)

data class FlowBlockPersisted(val block: FlowBlock)

data class FlowBlockSealed(val block: FlowBlock)

data class CalculateTransactionsCount(val block: FlowBlock)
