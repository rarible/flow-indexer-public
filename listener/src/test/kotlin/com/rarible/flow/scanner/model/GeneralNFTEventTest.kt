package com.rarible.flow.scanner.model

import com.rarible.flow.scanner.BaseJsonEventTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GeneralNFTEventTest : BaseJsonEventTest() {
    @Test
    fun `creat - ok, deposit`() {
        val event = getEventMessage("/json/nft_deposit.json")
        val deposit = GeneralDepositEvent(event)
        assertThat(deposit.id).isEqualTo(2)
        assertThat(deposit.to).isEqualTo("0x8500afb0163a33c8")
    }

    @Test
    fun `creat - ok, withdraw`() {
        val event = getEventMessage("/json/nft_withdraw.json")
        val withdraw = GeneralWithdrawEvent(event)
        assertThat(withdraw.id).isEqualTo(2)
        assertThat(withdraw.from).isEqualTo("0x987ef81a43bb4780")
    }

    @Test
    fun `creat - ok, mint`() {
        val event = getEventMessage("/json/nft_mint.json")
        val mint = GeneralMintEvent(event)
        assertThat(mint.id).isEqualTo(19)
    }

    @Test
    fun `creat - ok, burn`() {
        val event = getEventMessage("/json/nft_burn.json")
        val burn = GeneralBurnEvent(event)
        assertThat(burn.id).isEqualTo(19)
    }
}