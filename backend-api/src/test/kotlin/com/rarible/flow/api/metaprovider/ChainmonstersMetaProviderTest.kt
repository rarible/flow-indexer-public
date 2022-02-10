package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.data
import com.rarible.flow.api.mocks
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ItemRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono

internal class ChainmonstersMetaProviderTest: FunSpec({
    val item = mockk<Item> {
        every { id } returns ItemId("A.1234.ChainmonstersRewards", 1337)
        every { owner } returns FlowAddress("0x01")
        every { tokenId } returns 1337
    }

    val scriptExecutor = mocks.scriptExecutor(
        "has_meta" to META,
        "no_meta" to data.CADENCE_NULL
    )

    val itemRepository = mockk<ItemRepository>() {
        every { findById(any<ItemId>()) } returns Mono.just(item)
    }

    val provider = ChainmonstersMetaProvider(
        scriptExecutor,
        mocks.resource("has_meta")
    )

    test("should support ChainmonstersRewards") {
        provider.isSupported(item.id) shouldBe true
    }

    test("should not support other NFT") {
        provider.isSupported(ItemId("A.1234.MotoGP", 42)) shouldBe false
    }

    test("should read ChainmonstersReward meta data") {
        ChainmonstersMetaProvider(
            scriptExecutor,
            mocks.resource("has_meta")
        ).getMeta(
            item
        ) should { meta ->
            meta!!.name shouldBe "Adventure Bundle"
            meta.description shouldBe ""
            meta.contentUrls shouldContainExactly listOf(
                "https://chainmonsters.com/images/rewards/flowfest2021/41.png"
            )
        }
    }

    test("should return empty meta") {
        ChainmonstersMetaProvider(
            scriptExecutor,
            mocks.resource("no_meta")
        ).getMeta(
            item
        ) shouldBe null
    }
}) {
    companion object {
        const val META = """
            {"type":"Optional","value":{"type":"Struct","value":{"id":"s.e120a54c3a7f51dba36161d5ea837ace3fe07fdb01ab48783f420a521bc96b21.Meta","fields":[{"name":"rewardId","value":{"type":"UInt32","value":"41"}},{"name":"title","value":{"type":"Optional","value":{"type":"String","value":"Adventure Bundle"}}}]}}}
        """
    }
}
