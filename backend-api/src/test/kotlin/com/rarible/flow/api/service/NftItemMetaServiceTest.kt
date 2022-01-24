package com.rarible.flow.api.service

import com.rarible.flow.api.metaprovider.ItemMetaProvider
import com.rarible.flow.core.domain.ItemId
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono

internal class NftItemMetaServiceTest: FunSpec({

    test("should retry 5 times") {
        val metaProvider = mockk<ItemMetaProvider> {
            every {
                isSupported(any())
            } returns true

            coEvery {
                getMeta(any())
            } throws Exception("Retry")
        }
        val service = NftItemMetaService(
            listOf(
                metaProvider
            ),
            mockk {
                every { findById(any<ItemId>()) } returns Mono.empty()
                every { save(any()) } answers { Mono.just(arg(0)) }
            }
        )

        val itemId = ItemId("ABC", 123)
        service.getMetaByItemId(itemId)

        coVerify(exactly = 4) {
            metaProvider.isSupported(itemId)
            metaProvider.getMeta(itemId)
        }
    }

})