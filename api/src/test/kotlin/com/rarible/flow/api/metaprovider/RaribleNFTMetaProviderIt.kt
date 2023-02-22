package com.rarible.flow.api.metaprovider

import com.rarible.flow.api.IntegrationTest
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
    lateinit var provider: RaribleNFTMetaProvider
    private val rawPropertiesProvider = mockk<RawPropertiesProvider>()

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
        provider = RaribleNFTMetaProvider(rawPropertiesProvider, urlService)
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

        assertThat(meta.name).isEqualTo("test_name")
        assertThat(meta.description).isEqualTo("test_desc")
        assertThat(meta.contentUrls).isEqualTo(listOf("test_image"))
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
        assertThat(meta.contentUrls).isEqualTo(listOf("test_image"))
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