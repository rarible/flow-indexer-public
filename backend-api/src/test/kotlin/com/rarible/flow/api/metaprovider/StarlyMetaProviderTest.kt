package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.mocks
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono

internal class StarlyMetaProviderTest : FunSpec({

    val videoItemId = ItemId("A.1234.StarlyCard", 1337)
    val imageItemId = ItemId("A.1234.StarlyCard", 1338)

    val videoItem = mockk<Item> {
        every { id } returns videoItemId
        every { owner } returns FlowAddress("0x01")
        every { tokenId } returns 1337
    }

    val imageItem = mockk<Item> {
        every { id } returns imageItemId
        every { owner } returns FlowAddress("0x02")
        every { tokenId } returns 1338
    }

    val itemRepository = mockk<ItemRepository>() {
        every { findById(eq(videoItemId)) } returns Mono.just(videoItem)
        every { findById(eq(imageItemId)) } returns Mono.just(imageItem)
    }

    val script = mockk<StarlyMetaScript> {
        coEvery {
            call(FlowAddress("0x01"), 1337)
        } returns "JxznUdOwMHiO1vZ1B4hX/2/111"

        coEvery {
            call(FlowAddress("0x02"), 1338)
        } returns "iD5LK1QPWjQP1lorykFj/11/523"
    }

    test("should get video meta") {
        StarlyMetaProvider(
            itemRepository,
            mocks.webClient("https://starly.io/c/JxznUdOwMHiO1vZ1B4hX/2/111.json", META),
            script
        ).getMeta(videoItem.id) should { meta ->
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

    test("should get image meta") {
        StarlyMetaProvider(
            itemRepository,
            mocks.webClient("https://starly.io/c/iD5LK1QPWjQP1lorykFj/11/523.json", META_IMAGE),
            script
        ).getMeta(imageItemId) shouldNotBe ItemMeta.empty(imageItemId)
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

        val META_IMAGE = """
            {"title":"futakuchi-onna.","creator":{"id":"PRzeyZLjs3hveifcuXeNlcCFhh82","name":"Dead Seagull"},"collection":{"id":"iD5LK1QPWjQP1lorykFj","title":"NIGHT CREATURES"},"description":"Type of yōkai, a Japanese monster. Characterized by her two mouths – a normal one located on her face and a second one on the back of the head beneath the hair.","media_type":"image","media_sizes":[{"width":600,"height":800,"url":"https://storage.googleapis.com/starly-prod.appspot.com/users/PRzeyZLjs3hveifcuXeNlcCFhh82/collections/iD5LK1QPWjQP1lorykFj/cards/11/resized_600x800_cover1639004086646"},{"width":1200,"height":1600,"url":"https://storage.googleapis.com/starly-prod.appspot.com/users/PRzeyZLjs3hveifcuXeNlcCFhh82/collections/iD5LK1QPWjQP1lorykFj/cards/11/resized_1200x1600_cover1639004086646"}],"edition":"523","editions":"600","rarity":"common","url":"https://starly.io/c/iD5LK1QPWjQP1lorykFj/11/523"}
        """.trimIndent()
    }
}