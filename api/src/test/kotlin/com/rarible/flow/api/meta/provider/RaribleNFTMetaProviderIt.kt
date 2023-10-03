package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.IntegrationTest
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.fetcher.RawRemoteMetaFetcher
import com.rarible.flow.api.service.UrlService
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class RaribleNFTMetaProviderIt {

    @Autowired
    lateinit var urlService: UrlService
    lateinit var provider: RaribleMetaProvider
    private val rawPropertiesProvider = mockk<RawRemoteMetaFetcher>()

    private val itemId = ItemId("A.1234.RaribleNFT", 1337)
    val json = """{
            "name":"test_name",
            "description":"test_desc",
            "image":"test_image",
            "attributes":[]
            }"""

    @BeforeEach
    fun beforeEach() {
        clearMocks(rawPropertiesProvider)
        provider = RaribleMetaProvider(rawPropertiesProvider, urlService)
    }

    @Test
    fun `get meta - ok`() = runBlocking<Unit> {
        val item = mockk<Item> {
            every { id } returns itemId
            every { meta } returns "ipfs://QmR9VnAcSKLxmr3zHYaFpHAQdz13G25pvz8eWoLGHs3oAi"
        }

        val expectedUrlResource = urlService.parseUrl("ipfs://QmR9VnAcSKLxmr3zHYaFpHAQdz13G25pvz8eWoLGHs3oAi", "")!!
        coEvery { rawPropertiesProvider.getContent(itemId, expectedUrlResource) } returns json

        val meta = provider.getMeta(item)!!

        val expectedContent = ItemMetaContent(
            "test_image",
            ItemMetaContent.Type.IMAGE
        )

        assertThat(meta.name).isEqualTo("test_name")
        assertThat(meta.description).isEqualTo("test_desc")
        assertThat(meta.content).isEqualTo(listOf(expectedContent))
    }

    @Test
    fun `get meta - ok, url in json`() = runBlocking<Unit> {
        val item = mockk<Item> {
            every { id } returns itemId
            every { meta } returns """{"metaURI":"ipfs://QmR9VnAcSKLxmr3zHYaFpHAQdz13G25pvz8eWoLGHs3oAi"}"""
        }

        val expectedUrlResource = urlService.parseUrl("ipfs://QmR9VnAcSKLxmr3zHYaFpHAQdz13G25pvz8eWoLGHs3oAi", "")!!
        coEvery { rawPropertiesProvider.getContent(itemId, expectedUrlResource) } returns json

        val meta = provider.getMeta(item)!!

        assertThat(meta.name).isEqualTo("test_name")
        assertThat(meta.description).isEqualTo("test_desc")
    }

    @Test
    fun `supports - ok, true`() = runBlocking<Unit> {
        assertThat(provider.isSupported(itemId)).isTrue
    }

    @Test
    fun `supports - ok, false`() = runBlocking<Unit> {
        assertThat(provider.isSupported(ItemId("abc", 123L))).isFalse()
    }
}
