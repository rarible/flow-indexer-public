package com.rarible.flow.core.domain

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal

//todo document as part of FB-112
@Document
data class Order(
    val id: ObjectId,
    val itemId: String,
    val bidder: Address,
    val amount: BigDecimal
)
