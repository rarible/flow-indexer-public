package com.rarible.flow.core.domain


import org.bson.types.ObjectId
import org.onflow.sdk.FlowAddress
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
 * @property buyerFee       - fee for buyer
 * @property sellerFee      - fee for seller
 */
@Document
data class Order(
    @MongoId
    val id: ObjectId,
    val itemId: ItemId,
    val maker: FlowAddress,
    val taker: FlowAddress? = null,
    val amount: BigDecimal,
    val offeredNftId: String? = null,
    val fill: Int = 0,
    val canceled: Boolean = false,
    val buyerFee: BigDecimal,
    val sellerFee: BigDecimal
)
