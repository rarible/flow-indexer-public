package com.rarible.flow.scanner.model

import com.rarible.flow.core.domain.PaymentType
import java.math.BigDecimal

data class PayInfo(
    val address: String,
    val amount: BigDecimal,
    val currencyContract: String,
    val type: PaymentType
)
