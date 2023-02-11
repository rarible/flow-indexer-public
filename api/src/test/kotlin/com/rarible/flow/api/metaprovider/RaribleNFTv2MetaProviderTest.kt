package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemMeta
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant

class RaribleNFTv2MetaProviderTest {

    @Test
    internal fun metaParseTest() = runBlocking {
        val provider = RaribleNFTv2MetaProvider()
        val item = Item(
            contract = "A.f8d6e0586b0a20c7.RaribleNFTv2",
            tokenId = 0L,
            meta = """{"name":"First Awesome Item","description":"Item description","cid":"QmNe7Hd9xiqm1MXPtQQjVtksvWX6ieq9Wr6kgtqFo9D4CU","attributes":{},"contentUrls":[]}""".trimIndent(),
            creator = FlowAddress("0xf8d6e0586b0a20c7"),
            owner = FlowAddress("0xf8d6e0586b0a20c7"),
            royalties = emptyList(),
            mintedAt = Instant.now(),
            updatedAt = Instant.now(),
            collection = "A.f8d6e0586b0a20c7.SoftCollection:0"
        )

        val expectedMeta = ItemMeta(
            itemId = item.id,
            name = "First Awesome Item",
            description = "Item description",
            attributes = emptyList(),
            contentUrls = emptyList()
        )

        val actualMeta = provider.getMeta(item)

        Assertions.assertNotNull(actualMeta)
        Assertions.assertEquals(expectedMeta.itemId, actualMeta?.itemId)
        Assertions.assertEquals(expectedMeta.name, actualMeta?.name)
        Assertions.assertEquals(expectedMeta.description, actualMeta?.description)
        Assertions.assertEquals(expectedMeta.attributes, actualMeta?.attributes)
        Assertions.assertEquals(expectedMeta.contentUrls, actualMeta?.contentUrls)
    }
}
