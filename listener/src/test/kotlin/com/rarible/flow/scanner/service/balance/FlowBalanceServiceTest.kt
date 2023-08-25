package com.rarible.flow.scanner.service.balance

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowScriptResponse
import com.nftco.flow.sdk.cadence.Field
import com.rarible.blockchain.scanner.flow.service.AsyncFlowAccessApi
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.core.util.Log
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture


internal class FlowBalanceServiceTest : FunSpec({

    val balanceResponse = Flow.decodeJsonCadence<Field<*>>(
        """
            {"type":"Array","value":[{"type":"Struct","value":{"id":"s.080e0f27130f7029c8b9651c6fa4d6ffa56fdfa646e9cc04d8e8723ac7564715.Balance","fields":[{"name":"account","value":{"type":"Address","value":"0x1c0a1528f6966cb8"}},{"name":"token","value":{"type":"String","value":"A.1654653399040a61.FlowToken"}},{"name":"amount","value":{"type":"UFix64","value":"0.01000000"}}]}},{"type":"Struct","value":{"id":"s.080e0f27130f7029c8b9651c6fa4d6ffa56fdfa646e9cc04d8e8723ac7564715.Balance","fields":[{"name":"account","value":{"type":"Address","value":"0x1c0a1528f6966cb8"}},{"name":"token","value":{"type":"String","value":"A.3c5959b568896393.FUSD"}},{"name":"amount","value":{"type":"UFix64","value":"0.00000000"}}]}}]}
        """.trimIndent()
    )

    val flowApiMock = mockk<AsyncFlowAccessApi> {
        every {
            executeScriptAtLatestBlock(any(), any())
        } returns
            CompletableFuture.completedFuture(FlowScriptResponse(balanceResponse))
    }

    val balanceRepository = mockk<BalanceRepository> {
        every {
            save(any())
        } answers { Mono.just(arg(0)) }
    }

    val service = FlowBalanceService(
        FlowChainId.TESTNET,
        flowApiMock,
        balanceRepository
    )

    test("should init balances and save") {
        service.initBalances(
            FlowAddress("0x1c0a1528f6966cb8"),
            "A.1654653399040a61.FlowToken"
        )

        verify {
            flowApiMock.executeScriptAtLatestBlock(any(), withArg { args ->
                logger.info("Verifying flowApiMock.executeScriptAtLatestBlock args: {}", args)
            })

            balanceRepository.save( withArg {
                it.account shouldBe FlowAddress("0x1c0a1528f6966cb8")
                it.token shouldBe "A.1654653399040a61.FlowToken"
                it.balance shouldBe BigDecimal("0.01000000")
            })

        }
    }

    test("should convert script result") {
        FlowBalanceService.convert(
            balanceResponse
        ).map { Triple(it.account, it.token, it.balance) } shouldContainAll listOf(
            Triple(
                FlowAddress("0x1c0a1528f6966cb8"),
                "A.1654653399040a61.FlowToken",
                BigDecimal("0.01000000")
            ),

            Triple(
                FlowAddress("0x1c0a1528f6966cb8"),
                "A.3c5959b568896393.FUSD",
                BigDecimal("0.00000000")
            ),
        )
    }

}) {
    companion object {
        val logger by Log()
    }
}