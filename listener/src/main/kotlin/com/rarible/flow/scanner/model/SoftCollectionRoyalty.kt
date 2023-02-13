package com.rarible.flow.scanner.model

import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import java.math.BigDecimal

@JsonCadenceConversion(SoftCollectionRoyaltyConverter::class)
data class SoftCollectionRoyalty(
    val address: String,
    val fee: BigDecimal,

)

class SoftCollectionRoyaltyConverter : JsonCadenceConverter<SoftCollectionRoyalty> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): SoftCollectionRoyalty =
        com.nftco.flow.sdk.cadence.unmarshall(value) {
            SoftCollectionRoyalty(address("address"), bigDecimal("fee"))
        }

    override fun marshall(value: SoftCollectionRoyalty, namespace: CadenceNamespace): Field<*> =
        com.nftco.flow.sdk.cadence.marshall {
            struct {
                compositeOfPairs(namespace.withNamespace("SoftCollection.Royalty")) {
                    listOf(
                        "address" to address(value.address),
                        "fee" to ufix64(value.fee),
                    )
                }
            }
        }
}
