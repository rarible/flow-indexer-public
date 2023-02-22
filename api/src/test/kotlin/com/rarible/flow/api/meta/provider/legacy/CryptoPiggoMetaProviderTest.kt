package com.rarible.flow.api.meta.provider.legacy

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.mocks
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.randomFlowAddress
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.time.Instant

class CryptoPiggoMetaProviderTest {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val resource = ClassPathResource("jsonData/cryptoPiggoMeta.json")

    private val expectedMeta = ItemMeta(
        itemId = ItemId(Contracts.CRYPTOPIGGO.fqn(FlowChainId.MAINNET), 17L),
        name = "Cryptopiggo #17",
        description = "",
        attributes = listOf(
            ItemMetaAttribute("Rarity", "Uncommon"), ItemMetaAttribute("Status", "claimed"),
            ItemMetaAttribute("Background", "Bittersweet"), ItemMetaAttribute("Type", "Green"),
            ItemMetaAttribute("Pants", "Bottomless"), ItemMetaAttribute("Shirts", "Seance Crew T Shirt"),
            ItemMetaAttribute("Expression", "Jaded"), ItemMetaAttribute("Beard", "Yellow Handlebar"),
            ItemMetaAttribute("Head", "Bald"), ItemMetaAttribute("Accessories", "Trotters")
        ),
        contentUrls = listOf("https://s3.us-west-2.amazonaws.com/crypto-piggo.nft/piggo-17.png"),
    ).apply {
        raw = mapper.writeValueAsBytes(
            mapper.readValue(resource.inputStream, PiggoItem::class.java)
        )
    }

    @Test
    internal fun `should parse meta`() {
        runBlocking {
            val provider = CryptoPiggoMetaProvider(
                mocks.webClient("https://rareworx.com/client-server/v1/piggos/17", resource.file.readText()),
                mockk {
                    every { chainId } returns FlowChainId.MAINNET
                }
            )

            val meta = provider.getMeta(
                Item(
                    contract = Contracts.CRYPTOPIGGO.fqn(FlowChainId.MAINNET),
                    tokenId = 17L,
                    creator = randomFlowAddress(),
                    owner = randomFlowAddress(),
                    royalties = emptyList(),
                    mintedAt = Instant.now(),
                    updatedAt = Instant.now(),
                    collection = Contracts.CRYPTOPIGGO.fqn(FlowChainId.MAINNET)
                )
            )
            assertThat(meta.itemId.contract).isEqualTo(expectedMeta.itemId.contract)
            assertThat(meta.itemId.tokenId).isEqualTo(expectedMeta.itemId.tokenId)
            assertThat(meta.name).isEqualTo(expectedMeta.name)
            assertThat(meta.description).isEqualTo(expectedMeta.description)
            assertThat(meta.attributes).isEqualTo(expectedMeta.attributes)
            assertThat(meta.contentUrls).isEqualTo(expectedMeta.contentUrls)
            assertThat(meta.raw).isEqualTo(expectedMeta.raw)
        }
    }
}
