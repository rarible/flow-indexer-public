package com.rarible.flow.core.domain

import java.math.BigDecimal

data class EstimatedFee(
    val receivers: List<String>,
    val amount: BigDecimal
)