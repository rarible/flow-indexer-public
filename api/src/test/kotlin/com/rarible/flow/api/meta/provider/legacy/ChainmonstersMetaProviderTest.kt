package com.rarible.flow.api.meta.provider.legacy

import com.netflix.graphql.dgs.client.GraphQLResponse
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.api.meta.ItemMetaAttribute
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
        every { id } returns ItemId("A.93615d25d14fa337.ChainmonstersRewards", 1337)
        every { owner } returns FlowAddress("0x01")
        every { meta } returns "{\"rewardId\": \"10\"}"
        every { tokenId } returns 1337
    }

    val itemRepository = mockk<ItemRepository>() {
        every { findById(any<ItemId>()) } returns Mono.just(item)
    }

    val apiProperties = mockk<ApiProperties> {
        every { chainId } returns FlowChainId.MAINNET
    }

    val graphQl = mockk<WebClientGraphQLClient> {
        every {
            reactiveExecuteQuery(eq(ChainmonstersMetaProvider.getReward("10")))
        } returns Mono.just(GraphQLResponse(OK))
    }

    val graphQlErr = mockk<WebClientGraphQLClient> {
        every {
            reactiveExecuteQuery(eq(ChainmonstersMetaProvider.getReward("10")))
        } returns Mono.just(GraphQLResponse(ERROR))
    }
    val provider = ChainmonstersMetaProvider(
        itemRepository,
        graphQl,
        apiProperties
    )

    test("should support ChainmonstersRewards") {
        provider.isSupported(item.id) shouldBe true
    }

    test("should not support other NFT") {
        provider.isSupported(ItemId("A.1234.MotoGP", 42)) shouldBe false
    }

    test("should read ChainmonstersReward meta data") {
        ChainmonstersMetaProvider(
            itemRepository,
            graphQl,
            apiProperties
        ).getMeta(
            item
        ) should { meta ->
            meta!!.name shouldBe "Chainmon Designer"
            meta.description shouldBe "Help us design a Chainmon..."
            meta.contentUrls shouldContainExactly listOf(
                "https://chainmonsters.com/images/rewards/kickstarter/1.png"
            )
            meta.attributes shouldContainExactly listOf(
                ItemMetaAttribute("season", "Kickstarter")
            )
        }
    }

    test("should return empty meta on GraphQL error") {
        ChainmonstersMetaProvider(
            itemRepository,
            graphQlErr,
            apiProperties
        ).getMeta(
            item
        ) shouldBe null
    }
}) {
    companion object {
        const val OK = """
            {"data":{"reward":{"id":"1","name":"Chainmon Designer","desc":"Help us design a Chainmon...","img":"https://chainmonsters.com/images/rewards/kickstarter/1.png","season":"Kickstarter"}}}
        """

        const val ERROR = """
            {"errors":[{"message":"ERROR"}]}
        """
    }
}
