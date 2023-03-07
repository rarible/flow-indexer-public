package com.rarible.flow.scanner.model

import com.rarible.flow.core.test.randomFlowLog
import com.rarible.flow.core.test.randomFlowLogEvent
import com.rarible.flow.scanner.BaseJsonEventTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GeneralNFTEventTest : BaseJsonEventTest() {
    @Test
    fun `creat - ok, deposit`() {
        val event = getEventMessage("/json/nft_deposit.json")
        val logEvent = randomFlowLogEvent().copy(event = event)
        val deposit = GeneralDepositEvent(logEvent)
        assertThat(deposit.tokenId).isEqualTo(2)
        assertThat(deposit.to).isEqualTo("0x8500afb0163a33c8")
    }

    @Test
    fun `creat - ok, withdraw`() {
        val event = getEventMessage("/json/nft_withdraw.json")
        val logEvent = randomFlowLogEvent().copy(event = event)
        val withdraw = GeneralWithdrawEvent(logEvent)
        assertThat(withdraw.tokenId).isEqualTo(2)
        assertThat(withdraw.from).isEqualTo("0x987ef81a43bb4780")
    }

    @Test
    fun `creat - ok, mint`() {
        val event = getEventMessage("/json/nft_mint.json")
        val logEvent = randomFlowLogEvent().copy(event = event)
        val mint = GeneralMintEvent(logEvent)
        assertThat(mint.tokenId).isEqualTo(19)
    }

    @Test
    fun `creat - ok, burn`() {
        val event = getEventMessage("/json/nft_burn.json")
        val logEvent = randomFlowLogEvent().copy(event = event)
        val burn = GeneralBurnEvent(logEvent)
        assertThat(burn.tokenId).isEqualTo(19)
    }
}