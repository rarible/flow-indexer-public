package com.rarible.flow.scanner.converter

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.StructField
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.model.RaribleNftMint

class RaribleNftMintConverter : JsonCadenceConverter<RaribleNftMint> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): RaribleNftMint =
        com.nftco.flow.sdk.cadence.unmarshall(value) {
            RaribleNftMint(
                id = long("id"),
                creator = address("creator"),
                metadata = try {
                    dictionaryMap("metadata") { key, value ->
                        string(key) to string(value)
                    }
                } catch (_: Exception) {
                    mapOf("metaURI" to string("metadata"))
                },
                royalties = arrayValues("royalties") {
                    it as StructField
                    Part(
                        address = FlowAddress(address(it.value!!.getRequiredField("address"))),
                        fee = double(it.value!!.getRequiredField("fee"))
                    )
                }
            )
        }
}
