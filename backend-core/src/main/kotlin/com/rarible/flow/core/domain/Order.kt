package com.rarible.flow.core.domain

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal

/**
 * Description of an order
 * id - database ID,
 * itemId - nft id (address:tokenId)
 * bidder - address of a person who makes bid
 * amount - amount in flow
 * offeredNftId - nft id for nft-nft exchange
 */
@Document
data class Order(
    val id: ObjectId,
    val itemId: String,
    val bidder: Address,
    val amount: BigDecimal,
    val offeredNftId: String? = null
)
