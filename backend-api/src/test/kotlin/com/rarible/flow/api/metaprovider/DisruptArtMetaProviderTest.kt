package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ItemRepository
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.core.publisher.toMono
import java.time.Instant


@SpringBootTest(properties = [
    "application.environment = dev",
    "spring.cloud.service-registry.auto-registration.enabled = false",
    "spring.cloud.discovery.enabled = false",
    "spring.cloud.consul.config.enabled = false",
    "logging.logstash.tcp-socket.enabled = false"
])
@ActiveProfiles("test")
@Disabled("mock webclient")
class DisruptArtMetaProviderTest {

    @TestConfiguration
    @Import(CoreConfig::class)
    @MockkBean(ItemRepository::class)
    class Conf {

        @Bean
        fun metaProvider(itemRepository: ItemRepository, webClient: WebClient): DisruptArtMetaProvider {

            `when` {
                itemRepository.findById(ItemId(contract = "A.cd946ef9b13804c6.DisruptArt", tokenId = 1337L))
            }
            every {
                itemRepository.findById(ItemId(contract = "A.cd946ef9b13804c6.DisruptArt", tokenId = 1337L))
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
            return DisruptArtMetaProvider(itemRepository, webClient)
        }

    }

    @Autowired
    private lateinit var metaProvider: DisruptArtMetaProvider

    @Test
    internal fun jsonMetaTest() = runBlocking {
        val itemId = ItemId(contract = "A.cd946ef9b13804c6.DisruptArt", tokenId = 1337L)
        val meta = metaProvider.getMeta(itemId)
        Assertions.assertNotNull(meta)
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
        val meta = metaProvider.getMeta(itemId)
        Assertions.assertNotNull(meta)
        Assertions.assertNotEquals("Untitled", meta.name)
    }
}
