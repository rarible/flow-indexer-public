package com.rarible.flow.scanner.service.balance

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Balance
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import java.math.BigDecimal

internal class FlowBalanceServiceTest: FunSpec({

    test("should convert script result") {
        FlowBalanceService.convert(
            Flow.decodeJsonCadence(
                """
                    {"type":"Array","value":[{"type":"Struct","value":{"id":"s.080e0f27130f7029c8b9651c6fa4d6ffa56fdfa646e9cc04d8e8723ac7564715.Balance","fields":[{"name":"account","value":{"type":"Address","value":"0x1c0a1528f6966cb8"}},{"name":"token","value":{"type":"String","value":"A.1654653399040a61.FlowToken"}},{"name":"amount","value":{"type":"UFix64","value":"0.01000000"}}]}},{"type":"Struct","value":{"id":"s.080e0f27130f7029c8b9651c6fa4d6ffa56fdfa646e9cc04d8e8723ac7564715.Balance","fields":[{"name":"account","value":{"type":"Address","value":"0x1c0a1528f6966cb8"}},{"name":"token","value":{"type":"String","value":"A.3c5959b568896393.FUSD"}},{"name":"amount","value":{"type":"UFix64","value":"0.00000000"}}]}}]}
                """.trimIndent()
            )
        ) shouldContainAll listOf(
            Balance(
                FlowAddress("0x1c0a1528f6966cb8"),
                "A.1654653399040a61.FlowToken",
                BigDecimal("0.01000000")
            ),

            Balance(
                FlowAddress("0x1c0a1528f6966cb8"),
                "A.3c5959b568896393.FUSD",
                BigDecimal("0.00000000")
            ),
        )
    }

})