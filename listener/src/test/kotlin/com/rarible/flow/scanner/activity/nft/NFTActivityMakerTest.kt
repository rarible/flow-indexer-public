package com.rarible.flow.scanner.activity.nft

import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.domain.TransferActivity
import io.mockk.coEvery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NFTActivityMakerTest : AbstractNftActivityTest() {
    private val generalActivityMaker = object : NFTActivityMaker(logRepository, txManager, chainId) {
        override val contractName: String
            get() = contractSpec.contractName

        override fun meta(logEvent: FlowLogEvent): Map<String, String> {
            return emptyMap()
        }
    }

    @Test
    fun `mint - ok, with deposit`() = runBlocking<Unit> {
        mockkFindAfter(mint, flowOf(deposit))
        val activities = generalActivityMaker.activities(listOf(mint))
        assertThat(activities).hasSize(1)

        val mintActivity = activities[mint.log] as MintActivity
        assertThat(mintActivity.contract).isEqualTo(contract)
        assertThat(mintActivity.collection).isEqualTo(collection)
        assertThat(mintActivity.tokenId).isEqualTo(tokenId)
        assertThat(mintActivity.owner).isEqualTo(to)
        assertThat(mintActivity.metadata).isEqualTo(emptyMap<String, String>())
        assertThat(mintActivity.royalties).isEqualTo(emptyList<Part>())
        assertThat(mintActivity.timestamp).isEqualTo(timestamp)
    }

    @Test
    fun `mint - ok, without deposit`() = runBlocking<Unit> {
        mockkFindAfter(mint, emptyFlow())
        val activities = generalActivityMaker.activities(listOf(mint))
        val mintActivity = activities[mint.log] as MintActivity
        assertThat(mintActivity.owner).isEqualTo(contractAddress)
    }

    @Test
    fun `burn - ok, with withdraw`() = runBlocking<Unit> {
        mockkFindBefore(burn, flowOf(withdraw))
        val activities = generalActivityMaker.activities(listOf(burn))
        val burnActivity = activities[burn.log] as BurnActivity
        assertThat(burnActivity.owner).isEqualTo(from)
        assertThat(burnActivity.contract).isEqualTo(contract)
        assertThat(burnActivity.tokenId).isEqualTo(tokenId)
        assertThat(burnActivity.timestamp).isEqualTo(timestamp)
    }

    @Test
    fun `burn - ok, without withdraw`() = runBlocking<Unit> {
        mockkFindBefore(burn, emptyFlow())
        val activities = generalActivityMaker.activities(listOf(burn))
        val burnActivity = activities[burn.log] as BurnActivity
        assertThat(burnActivity.owner).isEqualTo(contractAddress)
    }

    @Test
    fun `deposit - ok, with withdraw`() = runBlocking<Unit> {
        mockkFindBefore(deposit, flowOf(withdraw))
        val activities = generalActivityMaker.activities(listOf(deposit))
        val transferActivity = activities[deposit.log] as TransferActivity
        assertThat(transferActivity.contract).isEqualTo(contract)
        assertThat(transferActivity.tokenId).isEqualTo(tokenId)
        assertThat(transferActivity.to).isEqualTo(to)
        assertThat(transferActivity.from).isEqualTo(from)
        assertThat(transferActivity.timestamp).isEqualTo(timestamp)
    }

    @Test
    fun `deposit - ok, without withdraw`() = runBlocking<Unit> {
        mockkFindBefore(deposit, emptyFlow())
        val activities = generalActivityMaker.activities(listOf(deposit))
        val transferActivity = activities[deposit.log] as TransferActivity
        assertThat(transferActivity.to).isEqualTo(to)
        assertThat(transferActivity.from).isEqualTo(contractAddress)
    }

    @Test
    fun `deposit - skip, after mint`() = runBlocking<Unit> {
        mockkFindBefore(deposit, flowOf(mint))
        val activities = generalActivityMaker.activities(listOf(deposit))
        assertThat(activities).hasSize(0)
    }

    @Test
    fun `deposit - skip, no to address`() = runBlocking<Unit> {
        mockkFindBefore(deposit, emptyFlow())
        val activities = generalActivityMaker.activities(listOf(depositWithoutTo))
        assertThat(activities).hasSize(0)
    }

    @Test
    fun `withdraw - ok, no deposit or burn`() = runBlocking<Unit> {
        mockkFindAfter(withdraw, emptyFlow())
        val activities = generalActivityMaker.activities(listOf(withdraw))
        val transferActivity = activities[withdraw.log] as TransferActivity
        assertThat(transferActivity.contract).isEqualTo(contract)
        assertThat(transferActivity.tokenId).isEqualTo(tokenId)
        assertThat(transferActivity.from).isEqualTo(from)
        assertThat(transferActivity.to).isEqualTo(contractAddress)
        assertThat(transferActivity.timestamp).isEqualTo(timestamp)
    }

    @Test
    fun `withdraw - skip, no from`() = runBlocking<Unit> {
        mockkFindAfter(withdraw, emptyFlow())
        val activities = generalActivityMaker.activities(listOf(withdrawWithoutFrom))
        assertThat(activities).hasSize(0)
    }

    @Test
    fun `withdraw - skip, has deposit`() = runBlocking<Unit> {
        mockkFindAfter(withdraw, flowOf(deposit))
        val activities = generalActivityMaker.activities(listOf(withdraw))
        assertThat(activities).hasSize(0)
    }

    @Test
    fun `withdraw - skip, has burn`() = runBlocking<Unit> {
        mockkFindAfter(withdraw, flowOf(burn))
        val activities = generalActivityMaker.activities(listOf(withdraw))
        assertThat(activities).hasSize(0)
    }

    private fun mockkFindAfter(event: FlowLogEvent, returnEvents: Flow<FlowLogEvent>) {
        coEvery {
            logRepository.findAfterEventIndex(
                transactionHash = event.log.transactionHash,
                afterEventIndex = event.log.eventIndex,
                any(),
                any()
            )
        } returns returnEvents
    }

    private fun mockkFindBefore(event: FlowLogEvent, returnEvents: Flow<FlowLogEvent>) {
        coEvery {
            logRepository.findBeforeEventIndex(
                transactionHash = event.log.transactionHash,
                beforeEventIndex = event.log.eventIndex,
                any(),
                any()
            )
        } returns returnEvents
    }
}
