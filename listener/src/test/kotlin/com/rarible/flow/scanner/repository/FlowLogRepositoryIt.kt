package com.rarible.flow.scanner.repository

import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.test.randomFlowLogEvent
import com.rarible.flow.core.util.findAfterEventIndex
import com.rarible.flow.core.util.findBeforeEventIndex
import com.rarible.flow.scanner.test.AbstractIntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FlowLogRepositoryIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var flowLogRepository: FlowLogRepository

    @Test
    fun `find logs before index`() = runBlocking<Unit> {
        logs.shuffled().forEach {
            flowLogRepository.save(FlowLogEvent.COLLECTION, it)
        }
        val logBefore = flowLogRepository.findBeforeEventIndex(transactionHash, 4).toList()
        assertThat(logBefore).hasSize(3)
        assertThat(logBefore[0].log.eventIndex).isEqualTo(3)
        assertThat(logBefore[1].log.eventIndex).isEqualTo(2)
        assertThat(logBefore[2].log.eventIndex).isEqualTo(1)
    }

    @Test
    fun `find logs after index`() = runBlocking<Unit> {
        logs.shuffled().forEach {
            flowLogRepository.save(FlowLogEvent.COLLECTION, it)
        }
        val logBefore = flowLogRepository.findAfterEventIndex(transactionHash, 3).toList()
        assertThat(logBefore).hasSize(4)
        assertThat(logBefore[0].log.eventIndex).isEqualTo(4)
        assertThat(logBefore[1].log.eventIndex).isEqualTo(5)
        assertThat(logBefore[2].log.eventIndex).isEqualTo(6)
        assertThat(logBefore[3].log.eventIndex).isEqualTo(7)
    }

    private val transactionHash = "b19d97e28c94ea1d51499184928057ce43e184a31dc51c4f83d877cf9fe35539"

    private val logs = listOf(
        randomFlowLogEvent(
            transactionHash = transactionHash,
            eventIndex = 1,
            event = getEventMessage("/json/nft_burn.json")
        ),
        randomFlowLogEvent(
            transactionHash = transactionHash,
            eventIndex = 2,
            event = getEventMessage("/json/nft_deposit.json")
        ),
        randomFlowLogEvent(
            transactionHash = transactionHash,
            eventIndex = 3,
            event = getEventMessage("/json/nft_mint.json")
        ),
        randomFlowLogEvent(
            transactionHash = transactionHash,
            eventIndex = 4,
            event = getEventMessage("/json/nft_withdraw.json")
        ),
        randomFlowLogEvent(
            transactionHash = transactionHash,
            eventIndex = 5,
            event = getEventMessage("/json/nft_storefront_v2_cancel.json")
        ),
        randomFlowLogEvent(
            transactionHash = transactionHash,
            eventIndex = 6,
            event = getEventMessage("/json/nft_storefront_v2_listing.json")
        ),
        randomFlowLogEvent(
            transactionHash = transactionHash,
            eventIndex = 7,
            event = getEventMessage("/json/nft_storefront_v2_purchase.json")
        ),
    )
}