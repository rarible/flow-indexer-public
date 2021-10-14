package com.rarible.flow.api.service

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowChainId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class FlowSignatureServiceTest: FunSpec({

    val data = listOf(
        Triple(
            "528360c75ecf870d4c8a432d23710a2d0b71ac30222b55ed31e32717a6f3741dd54c07669d607f39d164d1bce6807d1c5645569bdae99e8b0c710af450aeac05",
            "e2cfa85c1539277500e8f5fcf4c85dc7aa4cbc433d43c26527d461637a5fe34d93ef7b62774dbff4f2a718a3577698aaacbab58a1fc32d40191cd5b95194505c",
            "test"
        ),

        Triple(
            "66b3acb064f9cc990b93796e8d09d4a8820b3dde809f9be69f631d0582d314e4e0a5f881f5e2bcdb8153d78de63d98712b899d4f5dec947822a3ada60230376d",
            "0272b63a2b6f59cfa5d36374d3781123c1a6ccfd7790f3be39b6175284dd6564b2018dd7c20a5047134b4f1421e68615402d05613aa725d2390ad14c828e0db6",
            "some messge"
        )
    )


    test("should verify signature").config(enabled = false) {
        val service = FlowSignatureService(
            FlowChainId.TESTNET,
            Flow.newAccessApi("access.devnet.nodes.onflow.org", 9000)
        )

        data.forEach { (pk, sign, message) ->
            service.verify(pk, sign, message) shouldBe true
        }
    }

    test("should fail").config(enabled = false) {
        val service = FlowSignatureService(
            FlowChainId.TESTNET,
            Flow.newAccessApi("access.devnet.nodes.onflow.org", 9000)
        )

        data.forEach { (pk, sign, _) ->
            service.verify(pk, sign, "fail") shouldBe false
        }
    }

})
