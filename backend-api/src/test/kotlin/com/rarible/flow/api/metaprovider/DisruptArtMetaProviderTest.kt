package com.rarible.flow.api.metaprovider

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
import io.mockk.verify
import org.springframework.http.MediaType
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Instant


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
            findById(itemId2)
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

        every {
            save(any())
        } answers { Mono.just(arg(0)) }
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
                "https://ipfs.perma.store/content/bafkreidwr2pytghedsm5gsssuayay2zyc3hzdh5vwb2sye7ein2ca3li3u"
            )
        )

        verify {
            itemRepository.save(withArg {
                it.royalties shouldContainExactly listOf(
                    Part(FlowAddress("0x4e96267cf76199ef"), 0.1),
                    Part(FlowAddress("0x420f47f16a214100"), 0.05)
                )
            })
        }
    }

    test("should read image metadata for existing item") {
        DisruptArtMetaProvider(
            itemRepository,
            mocks.webClient(
                "https://ipfs.infura.io/ipfs/QmZakpqL6yYdQL5gb2ESrWao9s7Vt6XqYCqLPB7vUbU5cW",
                "dummy",
                contentType = MediaType.IMAGE_JPEG
            ),
            appProps
        ).getMeta(itemId2) shouldBe ItemMeta(
            itemId2,
            "Sample art test",
            "Sample art test",
            emptyList(),
            listOf(
                "https://ipfs.infura.io/ipfs/QmZakpqL6yYdQL5gb2ESrWao9s7Vt6XqYCqLPB7vUbU5cW"
            )
        )
    }
}) {
    companion object {
        const val META_JSON_ROYALTIES = """
            {"Title":"Test GrpE for Meta Data Check","Creator":"dhilip test blz","TotalEditions":4,"Description":"Test GrpE for Meta Data Check","MintedDate":"23-February-2022","Media":{"uri":"https://ipfs.perma.store/content/bafkreidwr2pytghedsm5gsssuayay2zyc3hzdh5vwb2sye7ein2ca3li3u","size":230311,"mimeType":"image/jpeg","dimensions":"950x635"},"MediaPreview":{"uri":"https://ipfs.perma.store/content/bafkreidwr2pytghedsm5gsssuayay2zyc3hzdh5vwb2sye7ein2ca3li3u","size":230311,"mimeType":"image/jpeg","dimensions":"950x635"},"PlatformInfo":{"Platform":"DisruptArt","MintedAt":"www.disrupt.art","UserID":"d.testblz123"},"tags":[],"royalties":[{"address":"0x4e96267cf76199ef","fee":0.1},{"address":"0x420f47f16a214100","fee":0.05}]}
        """
    }
}
