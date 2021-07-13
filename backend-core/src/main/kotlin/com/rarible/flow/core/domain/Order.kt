package com.rarible.flow.core.domain

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import java.math.BigDecimal

/**
 * Description of an order
 * @property id             - database ID,
 * @property itemId         - nft id (address:tokenId)
 * @property taker          - buyer
 * @property maker          - seller
 * @property amount         - amount in flow
 * @property offeredNftId   - nft id for nft-nft exchange
 * @property fill           - TODO add  doc
 * @property canceled       - order canceled
 */
@Document
data class Order(
    @MongoId
    val id: ULong,
    val itemId: String,
    val maker: Address,
    val taker: Address? = null,
    val amount: BigDecimal,
    val offeredNftId: String? = null,
    val fill: Int = 0,
    val canceled: Boolean = false
)
