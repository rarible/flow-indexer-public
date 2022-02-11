package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.Flow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*

internal class JambbMomentsMetaConverterTest : FunSpec({

    test("should convert meta") {
        Flow.unmarshall(
            JambbMomentsMeta::class,
            Flow.decodeJsonCadence(JSON)
        ) shouldBe META
    }
}) {
    companion object {
        val META = JambbMomentsMeta(
            "0x51b4f164ffd85547",
            "20th PPP loan",
            "Chaunté Wayans noticed a lot of nice new cars during quarantine.",
            "https://content-images.jambb.com/card-front/29849042-6fc8-4f13-8fa8-6a09501c6ea8.jpg",
            "https://content-videos.jambb.com/compressed/29849042-6fc8-4f13-8fa8-6a09501c6ea8.mp4",
            "Non-Fungible Jokin'",
            "Founder's Edition",
            false,
            "UNCOMMON"
        )

        val JSON = """
            {
                "type": "Struct",
                "value": {
                    "id": "A.d4ad4740ee426334.Moments.MomentMetadata",
                    "fields": [{
                        "name": "id",
                        "value": {
                            "type": "UInt64",
                            "value": "10000"
                        }
                    }, {
                        "name": "serialNumber",
                        "value": {
                            "type": "UInt64",
                            "value": "206"
                        }
                    }, {
                        "name": "contentID",
                        "value": {
                            "type": "UInt64",
                            "value": "15"
                        }
                    }, {
                        "name": "contentCreator",
                        "value": {
                            "type": "Address",
                            "value": "0x51b4f164ffd85547"
                        }
                    }, {
                        "name": "contentCredits",
                        "value": {
                            "type": "Dictionary",
                            "value": []
                        }
                    }, {
                        "name": "contentName",
                        "value": {
                            "type": "String",
                            "value": "20th PPP loan"
                        }
                    }, {
                        "name": "contentDescription",
                        "value": {
                            "type": "String",
                            "value": "Chaunté Wayans noticed a lot of nice new cars during quarantine."
                        }
                    }, {
                        "name": "previewImage",
                        "value": {
                            "type": "String",
                            "value": "https://content-images.jambb.com/card-front/29849042-6fc8-4f13-8fa8-6a09501c6ea8.jpg"
                        }
                    }, {
                        "name": "videoURI",
                        "value": {
                            "type": "String",
                            "value": "https://content-videos.jambb.com/compressed/29849042-6fc8-4f13-8fa8-6a09501c6ea8.mp4"
                        }
                    }, {
                        "name": "videoHash",
                        "value": {
                            "type": "String",
                            "value": "QmVoKN72cEyQ87FkphUxuc2jMnsNUSB5zoSxEitGLBypPr"
                        }
                    }, {
                        "name": "seriesID",
                        "value": {
                            "type": "UInt64",
                            "value": "1"
                        }
                    }, {
                        "name": "seriesName",
                        "value": {
                            "type": "String",
                            "value": "Non-Fungible Jokin'"
                        }
                    }, {
                        "name": "seriesArt",
                        "value": {
                            "type": "Optional",
                            "value": {
                                "type": "String",
                                "value": "https://prod-jambb-issuance-static-public.s3.amazonaws.com/issuance-ui/promo/animated-nfj-back.gif"
                            }
                        }
                    }, {
                        "name": "seriesDescription",
                        "value": {
                            "type": "String",
                            "value": "Non-Fungible Jokin', a Jambb original production, is the first comedy series ever produced and sold on the blockchain."
                        }
                    }, {
                        "name": "setID",
                        "value": {
                            "type": "UInt64",
                            "value": "1"
                        }
                    }, {
                        "name": "setName",
                        "value": {
                            "type": "String",
                            "value": "Founder's Edition"
                        }
                    }, {
                        "name": "setArt",
                        "value": {
                            "type": "Optional",
                            "value": {
                                "type": "String",
                                "value": "https://prod-jambb-issuance-static-public.s3.amazonaws.com/issuance-ui/promo/animated-nfj-back.gif"
                            }
                        }
                    }, {
                        "name": "setDescription",
                        "value": {
                            "type": "String",
                            "value": "NFJ1 Founder's Edition was filmed July 30-31, 2021 in Los Angeles, CA and features Maria Bamford (Lady Dynamite), Pete Holmes (Home Sweet Home Alone, Crashing), Zainab Johnson (Upload), Beth Stelling (HBO's Girl Daddy), Chaunte Wayans (Tiffany Haddish Presents: They Ready), Adam Ray (Hacks), Ian Edwards (Black Dynamite, Tangerine), and Moses Storm (Arrested Development, I'm Dying Up Here)."
                        }
                    }, {
                        "name": "retired",
                        "value": {
                            "type": "Bool",
                            "value": false
                        }
                    }, {
                        "name": "contentEditionID",
                        "value": {
                            "type": "UInt64",
                            "value": "16"
                        }
                    }, {
                        "name": "rarity",
                        "value": {
                            "type": "String",
                            "value": "UNCOMMON"
                        }
                    }, {
                        "name": "run",
                        "value": {
                            "type": "UInt64",
                            "value": "932"
                        }
                    }]
                }
            }
        """.trimIndent()
    }
}