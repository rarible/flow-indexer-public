package com.rarible.flow.api.service

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.bytesToHex
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class FlowSignatureServiceTest: FunSpec({

    test("should verify signature").config(enabled = false) {
        val service = FlowSignatureService(
            FlowChainId.TESTNET,
            Flow.newAccessApi("access.devnet.nodes.onflow.org", 9000)
        )

        service.verify(
            "0x66b3acb064f9cc990b93796e8d09d4a8820b3dde809f9be69f631d0582d314e4e0a5f881f5e2bcdb8153d78de63d98712b899d4f5dec947822a3ada60230376d",
            "0272b63a2b6f59cfa5d36374d3781123c1a6ccfd7790f3be39b6175284dd6564b2018dd7c20a5047134b4f1421e68615402d05613aa725d2390ad14c828e0db6",
            "some messge".toByteArray().bytesToHex()
        ) shouldBe true
    }

})
