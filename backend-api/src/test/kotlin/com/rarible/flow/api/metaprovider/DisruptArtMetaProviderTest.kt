package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.api.mocks
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.repository.ItemRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import org.springframework.http.MediaType
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono


class DisruptArtMetaProviderTest : FunSpec({

    val itemId1 = ItemId(contract = "A.cd946ef9b13804c6.DisruptArt", tokenId = 1337L)
    val itemId2 = ItemId(contract = "A.cd946ef9b13804c6.DisruptArt", tokenId = 1L)

    val itemRepository = mockk<ItemRepository> {
        every {
            findById(itemId1)
        } returns Item(
            contract = "A.cd946ef9b13804c6.DisruptArt",
            tokenId = 1337L,
            creator = FlowAddress("0x376af0a85d1f6f57"),
            royalties = emptyList(),
            meta = "{\"content\":\"https://ipfs.perma.store/content/bafkreicv2agl472w5vsxbfea4a7w3r5bvzb3jrrkgqjcrk674e6l6hncde\",\"name\":\"Colonialism: A Play in 60 Acts\"}",
            collection = "A.cd946ef9b13804c6.DisruptArt",
            mintedAt = Instant.now(),
            updatedAt = Instant.now(),
            owner = FlowAddress("0xcd946ef9b13804c6")
        ).toMono()

            every {
                itemRepository.findById(ItemId(contract = "A.cd946ef9b13804c6.DisruptArt", tokenId = 1L))
            } returns Item(
                contract = "A.cd946ef9b13804c6.DisruptArt",
                tokenId = 1L,
                creator = FlowAddress("0x4e96267cf76199ef"),
                royalties = emptyList(),
                meta = "{\"content\":\"https://ipfs.infura.io/ipfs/QmZakpqL6yYdQL5gb2ESrWao9s7Vt6XqYCqLPB7vUbU5cW\",\"name\":\"Sample art test\",\"tokenGroupId\":\"1\"}",
                collection = "A.cd946ef9b13804c6.DisruptArt",
                mintedAt = Instant.now(),
                updatedAt = Instant.now(),
                owner = FlowAddress("0xcd946ef9b13804c6")
            ).toMono()
            return DisruptArtMetaProvider(webClient)
        }

    val appProps = mockk<AppProperties> {
        every { chainId } returns FlowChainId.MAINNET
    }

    test("should read metadata for existing item") {
        DisruptArtMetaProvider(
            itemRepository,
            mocks.webClient(
                "https://ipfs.perma.store/content/bafkreicv2agl472w5vsxbfea4a7w3r5bvzb3jrrkgqjcrk674e6l6hncde",
                META_JSON_ROYALTIES
            ),
            appProps
        ).getMeta(itemId1) shouldBe ItemMeta(
            itemId1,
            "Colonialism: A Play in 60 Acts",
            "Test GrpE for Meta Data Check",
            listOf(
                ItemMetaAttribute("Title", "Test GrpE for Meta Data Check"),
                ItemMetaAttribute("Creator", "dhilip test blz"),
                ItemMetaAttribute("TotalEditions", "4"),
            ),
            listOf(
                "https://ipfs.perma.store/content/bafkreidwr2pytghedsm5gsssuayay2zyc3hzdh5vwb2sye7ein2ca3li3u",
                "https://ipfs.perma.store/content/bafkreidwr2pytghedsm5gsssuayay2zyc3hzdh5vwb2sye7ein2ca3li3u",
            )
        )

    @Test
    internal fun jsonMetaTest() = runBlocking {
        val item = mockk<Item> {
            every { contract } returns "A.cd946ef9b13804c6.DisruptArt"
            every { tokenId } returns 1337L
            every { meta } returns jacksonObjectMapper().writeValueAsString(mapOf("content" to "https://ipfs.perma.store/content/bafybeiao36osolp7ef6e4ieymfbzlywhnaygxw3r7uf4wbijsmlteka3pi"))
        }
        val meta = metaProvider.getMeta(item)
        Assertions.assertNotNull(meta)
        meta!!
        Assertions.assertNotEquals("Untitled", meta.name)
        Assertions.assertFalse(meta.description.isEmpty())
        Assertions.assertFalse(meta.contentUrls.isEmpty())
        Assertions.assertFalse(meta.attributes.isEmpty())
        Assertions.assertEquals("https://ipfs.perma.store/content/bafybeiao36osolp7ef6e4ieymfbzlywhnaygxw3r7uf4wbijsmlteka3pi",
            meta.contentUrls[0])
    }

    @Test
    internal fun pictureMetaTest() = runBlocking {
        val itemId = ItemId(contract = "A.cd946ef9b13804c6.DisruptArt", tokenId = 1L)
        val item = mockk<Item> {
            every { meta } returns "{\"content\":\"https://ipfs.infura.io/ipfs/QmZakpqL6yYdQL5gb2ESrWao9s7Vt6XqYCqLPB7vUbU5cW\",\"name\":\"Sample art test\",\"tokenGroupId\":\"1\"}"
        }
        val meta = metaProvider.getMeta(item)
        Assertions.assertNotNull(meta)
        Assertions.assertNotEquals("Untitled", meta!!.name)
    }
}
