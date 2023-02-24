package com.rarible.flow.api.service

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowPublicKey
import com.nftco.flow.sdk.FlowSignature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.apache.commons.lang3.SystemUtils

internal class SignatureServiceTest : FunSpec({

    val api = Flow.newAsyncAccessApi("access.devnet.nodes.onflow.org", 9000)

    val service = SignatureService(
        FlowChainId.TESTNET,
        api
    )

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
        ),

        Triple(
            "88e13e765ebfd8900b80b3276b16a0ff99c32edaa7bb3c4ccd456b794a46a380dfd81f659f590c974d8b163ef2feb590a737a84125cdb77e0196576d948b6e8e",
            "c11896958bc55411008072a6b874156839e60f58eb2b08d0dea1b05f32d296991a524a35f37ae4760c99e2d50ea557eda8b2d925958539fff648a6d880ef2403",
            "I would like to save like for itemId: FLOW-A.01658d9b94068f3c.CommonNFT:460"
        )
    )

    test("should verify signature").config(enabledIf = { !SystemUtils.IS_OS_WINDOWS && apiAvailable(api) }) {
        data.forEach { (pk, sign, message) ->
            service.verify(pk, sign, message) shouldBe true
        }
    }

    test("should verify signature - flow type").config(enabledIf = { !SystemUtils.IS_OS_WINDOWS && apiAvailable(api) }) {
        data.forEach { (pk, sign, message) ->
            service.verify(FlowPublicKey(pk), FlowSignature(sign), message) shouldBe true
        }
    }

    test("should fail").config(enabledIf = { !SystemUtils.IS_OS_WINDOWS && apiAvailable(api) }) {
        data.forEach { (pk, sign, _) ->
            service.verify(pk, sign, "fail") shouldBe false
        }
    }

    test("should check account").config(enabledIf = {
        apiAvailable(api)
    }) {
        service.checkPublicKey(
            FlowAddress("0xeeec6511cadbc0e2"),
            FlowPublicKey("66b3acb064f9cc990b93796e8d09d4a8820b3dde809f9be69f631d0582d314e4e0a5f881f5e2bcdb8153d78de63d98712b899d4f5dec947822a3ada60230376d")
        ) shouldBe true
    }

    test("should check account - false").config(enabledIf = {
        apiAvailable(api)
    }) {
        service.checkPublicKey(
            FlowAddress("0xeeec6511cadbc0e2"),
            FlowPublicKey(
                "528360c75ecf870d4c8a432d23710a2d0b71ac30222b55ed31e32717a6f3741dd54c07669d607f39d164d1bce6807d1c5645569bdae99e8b0c710af450aeac05"
            )
        ) shouldBe false
    }



})

fun apiAvailable(api: AsyncFlowAccessApi) = try {
    api.ping().get()
    true
} catch (e: Exception) {
    false
}
