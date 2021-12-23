package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowScript
import com.nftco.flow.sdk.FlowScriptResponse
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.domain.ItemId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.util.concurrent.CompletableFuture


internal class CnnNFTMetaProviderTest : FunSpec({

    fun resource(script: String) = mockk<Resource>() {
        every {
            inputStream
        } returns ByteArrayInputStream(script.toByteArray())
    }

    test("shoud read metadata for existing item") {
        val itemId = ItemId("A.329feb3ab062d289.CNN_NFT", 2909)
        val metaProvider = CnnNFTMetaProvider(
            mockk("itemRepository") {
                every { findById(any<ItemId>()) } returns Mono.just(
                    mockk("item") {
                        every { id } returns itemId
                        every { owner } returns FlowAddress("0xe969a6097b773709")
                        every { tokenId } returns 2909
                    }
                )
            },
            ScriptExecutor(
                mockk() {
                    every {
                        executeScriptAtLatestBlock(eq(FlowScript("cnnNft")), any())
                    } returns CompletableFuture.completedFuture(
                        FlowScriptResponse(CNN_NFT.toByteArray())
                    )

                    every {
                        executeScriptAtLatestBlock(eq(FlowScript("ipfs")), any())
                    } returns CompletableFuture.completedFuture(
                        FlowScriptResponse(IPFS_HASH.toByteArray())
                    )
                },
                AppProperties("test", "", FlowChainId.EMULATOR)
            ),
            WebClient.builder()
                .exchangeFunction { clientRequest: ClientRequest? ->
                    Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                            .header("content-type", "application/json")
                            .body(IPFS_META)
                            .build()
                    )
                }.build(),

            resource("cnnNft"),
            resource("ipfs")
        )

        metaProvider.getMeta(itemId) should { meta ->
            meta.itemId shouldBe itemId
            meta.name shouldBe "2015: US Supreme Court Ruling Guarantees Right to Same-Sex Marriage"
            meta.description shouldStartWith "June 26, 2015"
        }
    }


}) {
    companion object {
        val CNN_NFT = """
            {"type":"Optional","value":{"type":"Optional","value":{"type":"Resource","value":{"id":"A.329feb3ab062d289.CNN_NFT.NFT","fields":[{"name":"uuid","value":{"type":"UInt64","value":"49237558"}},{"name":"id","value":{"type":"UInt64","value":"2909"}},{"name":"setId","value":{"type":"UInt32","value":"4"}},{"name":"editionNum","value":{"type":"UInt32","value":"903"}}]}}}}
        """.trimIndent()

        val IPFS_HASH = """
            {"type":"Optional","value":{"type":"String","value":"Qmb1QwvaUF5xiqp2bXiRo4jzwXZ4MLJuk5srt1FYvH3Zqc"}}
        """.trimIndent()

        val NULL = """
            {"type":"Optional","value":null}
        """.trimIndent()

        val IPFS_META = """
            {"name":"2015: US Supreme Court Ruling Guarantees Right to Same-Sex Marriage","description":"June 26, 2015 - The Supreme Court of the United States ruled in Obergefell v. Hodges that the Constitution guarantees same-sex couples the right to marry, declaring one of humanity’s oldest and most precious unions to be, finally, recognized for all. \nComing 28 years after the Second National March on Washington for Lesbian and Gay Rights, where advocates staged a mass same-sex wedding and demanded legal recognition for same-sex relationships, Obergefell v. Hodges would require all 50 states and the District of Columbia to grant that recognition.\nIn 2004, Massachusetts became the first US state to recognize same-sex marriage, with more states following suit over the next decade. By 2015, 36 states had established same-sex marriage, but fourteen continued to ban it — some staunchly. A wave of court cases began to challenge the constitutionality of these bans, and when they produced a split between different Circuit Courts, the cases were combined and brought to the Supreme Court to resolve. \nIn the court’s majority opinion, Justice Anthony Kennedy wrote, “No union is more profound than marriage, for it embodies the highest ideals of love, fidelity, devotion, sacrifice, and family. In forming a marital union, two people become something greater than once they were. As some of the petitioners in these cases demonstrate, marriage embodies a love that may endure even past death. It would misunderstand these men and women to say they disrespect the idea of marriage. Their plea is that they do respect it, respect it so deeply that they seek to find its fulfillment for themselves. Their hope is not to be condemned to live in loneliness, excluded from one of civilization’s oldest institutions. They ask for equal dignity in the eyes of the law. The Constitution grants them that right.”\nSince the decision, hundreds of thousands of same-sex couples have married, and popular support for same-sex couples has continued to grow. Meanwhile, the LGBTQ+ civil rights movement continues its march towards full equality in law and society regardless of one’s sexual orientation or gender expression.","image":"https://giglabs.mypinata.cloud/ipfs/Qmco85RcMDZ5fu6sHqye9dntGiTyurijU6WUXM7paWHHRq","preview":"https://giglabs.mypinata.cloud/ipfs/QmNT7yZQSzr3XXJSCzJKEoihcnkwT9aXUaZe5rgB3SgduR","external_url":"vault.cnn.com","creator_name":"CNN","sha256_image_hash":"fc3ae0c573bea5ad9276dfd3d2c7b26cb4999849b8852ad6d659785a780a47f5","ipfs_image_hash":"Qmco85RcMDZ5fu6sHqye9dntGiTyurijU6WUXM7paWHHRq","image_file_type":"mp4","seriesName":"CNN Defining Moments","seriesDescription":"Moments that have defined the last four decades of CNN.","series_id":"2","set_id":"11","edition":"508","max_editions":"1000"}
        """.trimIndent()
    }
}