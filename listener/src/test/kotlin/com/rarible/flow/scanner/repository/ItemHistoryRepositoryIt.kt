package com.rarible.flow.scanner.repository

import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.findItemFirstTransfer
import com.rarible.flow.core.repository.findItemMint
import com.rarible.flow.core.test.randomFlowLog
import com.rarible.flow.core.test.randomItemHistory
import com.rarible.flow.core.test.randomMintActivity
import com.rarible.flow.core.test.randomTransferActivity
import com.rarible.flow.scanner.test.AbstractIntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ItemHistoryRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemHistoryRepository: ItemHistoryRepository

    @Test
    fun `get first transfer event`() = runBlocking<Unit> {
        val contract = randomString()
        val tokenId = randomLong()

        val transfer1 = randomItemHistory(
            log = randomFlowLog(blockHeight = 1, eventIndex = 0),
            activity = randomTransferActivity(contract = contract, tokenId = tokenId)
        )
        val transfer2 = randomItemHistory(
            log = randomFlowLog(blockHeight = 1, eventIndex = 1),
            activity = randomTransferActivity(contract = contract, tokenId = tokenId)
        )
        val transfer3 = randomItemHistory(
            log = randomFlowLog(blockHeight = 2, eventIndex = 0),
            activity = randomTransferActivity(contract = contract, tokenId = tokenId)
        )
        listOf(transfer1, transfer2, transfer3).shuffled().forEach { itemHistoryRepository.save(it).awaitFirst()  }

        val firstTransfer = itemHistoryRepository.findItemFirstTransfer(contract, tokenId)
        assertThat(firstTransfer).isNotNull
        assertThat(firstTransfer?.log?.blockHeight).isEqualTo(1)
        assertThat(firstTransfer?.log?.eventIndex).isEqualTo(0)
    }

    @Test
    fun `get mint event`() = runBlocking<Unit> {
        val contract = randomString()
        val tokenId = randomLong()

        val mint = randomItemHistory(
            log = randomFlowLog(blockHeight = 1, eventIndex = 0),
            activity = randomMintActivity(contract = contract, tokenId = tokenId)
        )
        val otherMint = randomItemHistory(
            log = randomFlowLog(blockHeight = 1, eventIndex = 0),
            activity = randomMintActivity(tokenId = tokenId)
        )
        val transfer = randomItemHistory(
            log = randomFlowLog(blockHeight = 1, eventIndex = 1),
            activity = randomTransferActivity(contract = contract, tokenId = tokenId)
        )
        listOf(otherMint, mint, transfer).shuffled().forEach { itemHistoryRepository.save(it).awaitFirst()  }

        val history = itemHistoryRepository.findItemMint(contract, tokenId).firstOrNull()
        assertThat(history).isNotNull
        assertThat(history?.id).isEqualTo(mint.id)
    }
}