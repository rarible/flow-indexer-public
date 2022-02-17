package com.rarible.flow.api.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.flow.api.imageprovider.VersusArtItemImageProvider
import com.rarible.flow.api.metaprovider.ItemMetaProvider
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemMetaRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.Assertions
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

internal class NftItemMetaServiceTest: FunSpec({

    test("should retry 4 times") {
        val metaProvider = mockk<ItemMetaProvider> {
            every {
                isSupported(any())
            } returns true

            coEvery {
                getMeta(any())
            } throws Exception("Retry")
        }
        val itemId = ItemId("ABC", 123)
        val item = mockk<Item>() {
            every { id } returns itemId
        }
        val repository = mockk<ItemMetaRepository> {
            every { findById(any<ItemId>()) } returns Mono.empty()
            every { save(any()) } answers { Mono.just(arg(0)) }
        }

        val service = NftItemMetaService(
            listOf(metaProvider),
            repository,
            emptyList(),
            mockk {
                every { findById(any<ItemId>()) } returns Mono.just(item)
            },

        )


        service.getMetaByItemId(itemId)

        verify {
            repository.findById(itemId)
        }

        coVerify(exactly = 4) {
            metaProvider.isSupported(itemId)
            metaProvider.getMeta(item)
        }
    }

    test("should save meta") {
        val itemId = ItemId("ABC", 123)
        val item = mockk<Item> {
            every { id } returns itemId
        }
        val metaProvider = mockk<ItemMetaProvider> {
            every {
                isSupported(any())
            } returns true

            coEvery {
                getMeta(any())
            } returns ItemMeta(
                itemId, "Test meta", "Description", emptyList(), emptyList()
            )
        }

        val repository = mockk<ItemMetaRepository> {
            every { findById(any<ItemId>()) } returns Mono.empty()
            every { save(any()) } answers { Mono.just(arg(0)) }
        }

        val service = NftItemMetaService(
            listOf(metaProvider),
            repository,
            emptyList(),
            mockk {
                every { findById(any<ItemId>()) } returns Mono.just(item)
            }
        )

        service.getMetaByItemId(itemId)


        coVerifySequence {
            repository.findById(itemId)
            metaProvider.isSupported(itemId)
            metaProvider.getMeta(item)
            repository.save(withArg { meta ->
                meta.itemId shouldBe itemId
            })
        }
    }

    test("should return existing meta") {
        val itemId = ItemId("ABC", 123)
        val itemMeta = ItemMeta(
            itemId, "Test meta", "Description", emptyList(), emptyList()
        )

        val metaProvider = mockk<ItemMetaProvider> {
            every {
                isSupported(any())
            } returns true

            coEvery {
                getMeta(any())
            } returns itemMeta
        }

        val repository = mockk<ItemMetaRepository> {
            every { findById(any<ItemId>()) } returns Mono.just(itemMeta)
            every { save(any()) } answers { Mono.just(arg(0)) }
        }
        val service = NftItemMetaService(
            listOf(metaProvider),
            repository,
            emptyList(),
            mockk {
                every { findById(any<ItemId>()) } returns Mono.just(mockk())
            }
        )

        service.getMetaByItemId(itemId)

        coVerifySequence {
            repository.findById(itemId)
        }
    }

    test("should return jpeg from base64") {
        val itemId = ItemId.parse("A.d796ff17107bbff6.Art:245")
        val resource = ClassPathResource("jsonData/artMeta.json")
        val itemMeta = jacksonObjectMapper().readValue(resource.inputStream, ItemMeta::class.java)


        val service = NftItemMetaService(
            emptyList(),
            mockk {
                every { findById(itemId) } returns itemMeta.toMono()
            },
            listOf(VersusArtItemImageProvider()),
            mockk {
                every { findById(any<ItemId>()) } returns Mono.just(mockk())
            }
        )

        val image = service.imageFromMeta(itemId)

        Assertions.assertNotNull(image)
        Assertions.assertEquals(MediaType.IMAGE_PNG_VALUE, image!!.first.toString())
    }

})
