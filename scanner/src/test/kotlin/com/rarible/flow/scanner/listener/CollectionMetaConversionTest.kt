package com.rarible.flow.scanner.listener

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.cadence.StructField
import com.rarible.flow.scanner.model.parse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class CollectionMetaConversionTest: FunSpec({

    test("should parse meta from CHANGE event") {
        Flow
            .decodeJsonCadence<StructField>(META)
            .parse<CollectionMeta>() shouldBe CollectionMeta(
                "coll_02_03_01", "coll_02_03_01", null, null, null
            )
    }

}) {

    companion object {
        const val META = """
            {
                "type": "Struct",
                "value": {
                    "id": "A.ebf4ae01d1284af8.SoftCollection.Meta",
                    "fields": [{
                        "name": "name",
                        "value": {
                            "type": "String",
                            "value": "coll_02_03_01"
                        }
                    }, {
                        "name": "symbol",
                        "value": {
                            "type": "String",
                            "value": "coll_02_03_01"
                        }
                    }, {
                        "name": "icon",
                        "value": {
                            "type": "Optional",
                            "value": null
                        }
                    }, {
                        "name": "description",
                        "value": {
                            "type": "Optional",
                            "value": null
                        }
                    }, {
                        "name": "url",
                        "value": {
                            "type": "Optional",
                            "value": null
                        }
                    }]
                }
            }
            
        """
    }
}