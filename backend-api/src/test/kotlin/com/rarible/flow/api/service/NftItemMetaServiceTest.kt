package com.rarible.flow.api.service

import com.rarible.flow.api.metaprovider.ItemMetaProvider
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemMetaRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Mono

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
        val repository = mockk<ItemMetaRepository> {
            every { findById(any<ItemId>()) } returns Mono.empty()
            every { save(any()) } answers { Mono.just(arg(0)) }
        }
        val service = NftItemMetaService(
            listOf(metaProvider),
            repository
        )

        val itemId = ItemId("ABC", 123)
        service.getMetaByItemId(itemId)

        verify {
            repository.findById(itemId)
        }

        coVerify(exactly = 4) {
            metaProvider.isSupported(itemId)
            metaProvider.getMeta(itemId)
        }
    }

    test("should save meta") {
        val itemId = ItemId("ABC", 123)

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
            repository
        )

        service.getMetaByItemId(itemId)

        coVerifySequence {
            repository.findById(itemId)
            metaProvider.isSupported(itemId)
            metaProvider.getMeta(itemId)
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
            repository
        )

        service.getMetaByItemId(itemId)

        coVerifySequence {
            repository.findById(itemId)
        }
    }

})