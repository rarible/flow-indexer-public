package com.rarible.flow.api.meta.provider.legacy

import com.nftco.flow.sdk.Flow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class OneFootballMetaConverterTest : FunSpec({

    test("should unmarshall meta data") {
        Flow.unmarshall(
            OneFootballMeta::class,
            Flow.decodeJsonCadence(JSON)
        ) shouldBe META
    }
}) {
    companion object {
        val JSON = """
                {
                "type":"Struct",
                "value": {
                    "id":"s.ac134dc137ecd517119ff6da12db7852038791b80e191f0e39b3f6019f5278da.NFTData",
                    "fields":[
                        {"name":"id","value":{"type":"UInt64","value":"1"}},
                        {"name":"templateID","value":{"type":"UInt64","value":"0"}},
                        {"name":"seriesName","value":{"type":"String","value":"onefootballerz"}},
                        {"name":"name","value":{"type":"String","value":"Louis, OneFootballerz #1"}},
                        {"name":"description","value":{"type":"String","value":"OneFootballerz is a collection of personalised jerseys for OneFootball employees."}},
                        {"name":"preview","value":{"type":"String","value":"https://bafkreicbozp5kqoverw5uivz4hfwycb6xnjtxoxsmbegv4554hcg2q6opa.ipfs.dweb.link/"}},
                        {"name":"media","value":{"type":"String","value":"https://bafybeif26dqnotrgz5mkbuuj2vsv7wmpii7ymdwu7jhv5e4f55aewitela.ipfs.dweb.link/"}},
                        {
                            "name":"data","value":{
                                "type":"Dictionary",
                                "value":[
                                    {
                                        "key":{"type":"String","value":"location"},
                                        "value":{"type":"String","value":"Berlin"}
                                    },{
                                        "key":{"type":"String","value":"team"},
                                        "value":{"type":"String","value":"Data \u0026 Insights"}
                                    },{
                                        "key":{"type":"String","value":"title"},
                                        "value":{"type":"String","value":"Principal Machine Learning Engineer"}
                                    },{
                                        "key":{"type":"String","value":"of_id"},
                                        "value":{"type":"String","value":"79"}
                                    },{
                                        "key":{"type":"String","value":"player_name"},
                                        "value":{"type":"String","value":"Louis Guitton"}
                                    }
                                ]
                            }
                        }
                    ]
                }
            }
        """.trimIndent()

        val META = OneFootballMeta(
            id = 1,
            templateID = 0,
            seriesName = "onefootballerz",
            name = "Louis, OneFootballerz #1",
            description = "OneFootballerz is a collection of personalised jerseys for OneFootball employees.",
            preview = "https://bafkreicbozp5kqoverw5uivz4hfwycb6xnjtxoxsmbegv4554hcg2q6opa.ipfs.dweb.link/",
            media = "https://bafybeif26dqnotrgz5mkbuuj2vsv7wmpii7ymdwu7jhv5e4f55aewitela.ipfs.dweb.link/",
            data = mapOf(
                "location" to "Berlin",
                "team" to "Data \u0026 Insights",
                "title" to "Principal Machine Learning Engineer",
                "of_id" to "79",
                "player_name" to "Louis Guitton"
            )
        )
    }
}
