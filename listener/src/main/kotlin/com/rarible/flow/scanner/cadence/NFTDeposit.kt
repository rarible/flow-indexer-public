package com.rarible.flow.scanner.cadence

import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.nftco.flow.sdk.cadence.unmarshall

@JsonCadenceConversion(NFTDepositConverter::class)
data class NFTDeposit(
    val id: Long,
    val to: String?,
)

class NFTDepositConverter : JsonCadenceConverter<NFTDeposit> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): NFTDeposit =
        unmarshall(value) { NFTDeposit(long("id"), optional("to", JsonCadenceParser::address)) }
}
