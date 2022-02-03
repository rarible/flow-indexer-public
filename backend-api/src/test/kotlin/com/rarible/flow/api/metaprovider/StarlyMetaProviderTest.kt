package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.mocks
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMetaAttribute
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono

internal class StarlyMetaProviderTest : FunSpec({

    val item = mockk<Item> {
        every { id } returns ItemId("A.1234.StarlyCard", 1337)
        every { owner } returns FlowAddress("0x01")
        every { tokenId } returns 1337
    }

    test("should get meta") {
        StarlyMetaProvider(
            mockk() {
                every { findById(any<ItemId>()) } returns Mono.just(item)
            },
            mocks.webClient("https://starly.io/c/JxznUdOwMHiO1vZ1B4hX/2/111.json", META),
            mockk {
                coEvery {
                    call(any(), any())
                } returns "JxznUdOwMHiO1vZ1B4hX/2/111"
            }
        ).getMeta(item.id) should { meta ->
            meta.name shouldBe "Boxer"
            meta.contentUrls shouldContainExactly listOf(
                "https://storage.googleapis.com/starly-prod.appspot.com/users/LIFBFA0ZEzLhL8zcsw7Ppg1kC5l1/collections/JxznUdOwMHiO1vZ1B4hX/cards/2/screenshot_cover_1200x1600.jpg",
                "https://storage.googleapis.com/starly-prod.appspot.com/users/LIFBFA0ZEzLhL8zcsw7Ppg1kC5l1/collections/JxznUdOwMHiO1vZ1B4hX/cards/2/converted_cover_1200x1600.mp4",
                "https://storage.googleapis.com/starly-prod.appspot.com/users/LIFBFA0ZEzLhL8zcsw7Ppg1kC5l1/collections/JxznUdOwMHiO1vZ1B4hX/cards/2/screenshot_cover_600x800.jpg",
                "https://storage.googleapis.com/starly-prod.appspot.com/users/LIFBFA0ZEzLhL8zcsw7Ppg1kC5l1/collections/JxznUdOwMHiO1vZ1B4hX/cards/2/converted_cover_600x800.mp4"
            )
            meta.description shouldStartWith "David Bowie"
            meta.attributes shouldContainAll listOf(
                ItemMetaAttribute("edition", "111"),
                ItemMetaAttribute("editions", "1000"),
                ItemMetaAttribute("rarity", "rare"),
            )
        }
    }

}) {
    companion object {
        val META = """
            {
                "title":"Boxer",
                "creator":{"id":"LIFBFA0ZEzLhL8zcsw7Ppg1kC5l1","name":"Melos Studio"},
                "collection":{"id":"JxznUdOwMHiO1vZ1B4hX","title":"David Bowie"},
                "description":"David Bowie backstage at the Waldbühne arena in Berlin on his 1983 Serious Moonlight tour (boxing).",
                "media_type":"video",
                "media_sizes":[
                    {
                        "width":520,
                        "height":800,
                        "url":"https://storage.googleapis.com/starly-prod.appspot.com/users/LIFBFA0ZEzLhL8zcsw7Ppg1kC5l1/collections/JxznUdOwMHiO1vZ1B4hX/cards/2/converted_cover_600x800.mp4",
                    "screenshot":"https://storage.googleapis.com/starly-prod.appspot.com/users/LIFBFA0ZEzLhL8zcsw7Ppg1kC5l1/collections/JxznUdOwMHiO1vZ1B4hX/cards/2/screenshot_cover_600x800.jpg"
                    },{
                        "width":1040,"height":1600,
                        "url":"https://storage.googleapis.com/starly-prod.appspot.com/users/LIFBFA0ZEzLhL8zcsw7Ppg1kC5l1/collections/JxznUdOwMHiO1vZ1B4hX/cards/2/converted_cover_1200x1600.mp4",
                        "screenshot":"https://storage.googleapis.com/starly-prod.appspot.com/users/LIFBFA0ZEzLhL8zcsw7Ppg1kC5l1/collections/JxznUdOwMHiO1vZ1B4hX/cards/2/screenshot_cover_1200x1600.jpg"}
                    ],
                "edition":"111","editions":"1000","rarity":"rare","url":"https://starly.io/c/JxznUdOwMHiO1vZ1B4hX/2/111"}
        """.trimIndent()
    }
}